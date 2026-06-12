/**
 * VibeCheck Cloud Functions — core-flow backend.
 *
 * Owns the heatmap aggregation pipeline, privacy-driven retention cleanup,
 * and the anonymous chat matchmaking + session lifecycle (see match.js).
 * Purchase-validation belongs to the billing module owner.
 *
 * Firestore layout (see CONTRACTS.md):
 *   checkins/{id}  { regionId, mood, valence, timestamp }   // anonymous, no uid
 *   regions/{id}   { name, countryCode, lat, lng, count24h, valenceSum24h, updatedAt }
 *   users/{uid}    { ageBracket, chatOptIn, createdAt, lastActiveAt, ... }
 */

const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { logger } = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

const REGIONS = require("./regions");
const { aggregateWindow, DAY_MS } = require("./aggregate");
const RETENTION_DAYS = 90;

/**
 * Live increment: when a check-in lands, bump its region's running totals so
 * the heatmap reflects it within seconds. The hourly recompute below is the
 * authoritative self-healing pass that also ages out old check-ins.
 */
exports.onCheckinCreated = onDocumentCreated("checkins/{checkInId}", async (event) => {
  const data = event.data && event.data.data();
  if (!data) return;

  const regionId = data.regionId;
  const valence = typeof data.valence === "number" ? data.valence : null;
  if (!regionId || valence === null || !REGIONS[regionId]) {
    logger.debug("Skipping checkin with no usable region/valence", { regionId });
    return;
  }

  const meta = REGIONS[regionId];
  await db.collection("regions").doc(regionId).set(
    {
      name: meta.name,
      countryCode: meta.countryCode,
      lat: meta.lat,
      lng: meta.lng,
      count24h: admin.firestore.FieldValue.increment(1),
      valenceSum24h: admin.firestore.FieldValue.increment(valence),
      updatedAt: Date.now(),
    },
    { merge: true },
  );
});

/**
 * Authoritative hourly recompute of the rolling 24h window for every region.
 * Counts only check-ins newer than 24h, so the live increments self-correct
 * and stale entries drop off the heatmap.
 */
exports.rollupRegions = onSchedule("every 60 minutes", async () => {
  const now = Date.now();
  const cutoff = now - DAY_MS;

  const recent = await db.collection("checkins").where("timestamp", ">=", cutoff).get();
  const checkins = recent.docs.map((doc) => doc.data());
  const totals = aggregateWindow(checkins, Object.keys(REGIONS), now);

  const batch = db.batch();
  for (const [id, meta] of Object.entries(REGIONS)) {
    const t = totals[id];
    batch.set(
      db.collection("regions").doc(id),
      {
        name: meta.name,
        countryCode: meta.countryCode,
        lat: meta.lat,
        lng: meta.lng,
        count24h: t.count,
        valenceSum24h: t.sum,
        updatedAt: now,
      },
      { merge: true },
    );
  }
  await batch.commit();
  logger.info("rollupRegions recomputed", { regions: Object.keys(REGIONS).length });
});

/**
 * Retention cleanup (SOW: "all data deleted if user inactive for 90 days").
 *  - Deletes user docs whose lastActiveAt is older than 90 days, and their
 *    anonymous auth account.
 *  - Purges checkin docs older than 90 days (anonymous, so they can't be tied
 *    to a user; they're only ever needed for the 24h heatmap window anyway).
 */
exports.cleanupInactiveData = onSchedule("every 24 hours", async () => {
  const cutoff = Date.now() - RETENTION_DAYS * DAY_MS;

  // Inactive users
  const stale = await db.collection("users").where("lastActiveAt", "<", cutoff).get();
  let deletedUsers = 0;
  for (const doc of stale.docs) {
    await doc.ref.delete();
    await admin.auth().deleteUser(doc.id).catch((e) =>
      logger.warn("auth delete failed", { uid: doc.id, error: e.message }),
    );
    deletedUsers += 1;
  }

  // Old anonymous check-ins
  const oldCheckins = await db.collection("checkins").where("timestamp", "<", cutoff).get();
  let batch = db.batch();
  let n = 0;
  let deletedCheckins = 0;
  for (const doc of oldCheckins.docs) {
    batch.delete(doc.ref);
    deletedCheckins += 1;
    if (++n === 450) {
      await batch.commit();
      batch = db.batch();
      n = 0;
    }
  }
  if (n > 0) await batch.commit();

  logger.info("cleanupInactiveData done", { deletedUsers, deletedCheckins });
});

// ---- Anonymous chat (match module) ----------------------------------
// Required after initializeApp() so the Admin SDK is ready when match.js
// calls admin.firestore() at load.
const match = require("./match");
exports.requestMatch = match.requestMatch;
exports.cancelMatch = match.cancelMatch;
exports.leaveSession = match.leaveSession;
exports.reportPeer = match.reportPeer;
exports.closeExpiredSessions = match.closeExpiredSessions;

// ---- Server-trusted entitlement (billing) ---------------------------
const billing = require("./billing");
exports.validatePurchase = billing.validatePurchase;
