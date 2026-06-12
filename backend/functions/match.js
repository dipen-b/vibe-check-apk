/**
 * Anonymous mood-matched chat — matchmaking + session lifecycle.
 *
 * Callable functions (clients can't write chatSessions directly; rules deny
 * it, so all session mutation goes through here under the Admin SDK):
 *   - requestMatch:  claim a waiting peer or enqueue; returns { sessionId|null }
 *   - cancelMatch:   remove my queue entry
 *   - leaveSession:  mark a session closed (participant only)
 *   - reportPeer:    file a report + close the session (participant only)
 * Scheduled:
 *   - closeExpiredSessions: close past-expiry sessions and purge their messages
 *
 * Privacy: chat is ephemeral. Messages live only for the session and are
 * deleted shortly after it closes. No content is retained server-side.
 */

const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { logger } = require("firebase-functions");
const admin = require("firebase-admin");

const db = admin.firestore();

const CHAT_DURATION_MS = 5 * 60 * 1000; // AppConfig.CHAT_DURATION_MINUTES
const MOOD_WINDOW_MS = 2 * 60 * 60 * 1000; // AppConfig.MATCH_MOOD_WINDOW_HOURS
const VALENCE_TOLERANCE = 0.2; // "on your wavelength"

function requireAuth(request) {
  const uid = request.auth && request.auth.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Sign in required.");
  return uid;
}

/**
 * Find a compatible waiting peer and atomically claim it; otherwise enqueue.
 * Returns { sessionId } when matched immediately, { sessionId: null } when the
 * caller was placed in the queue (client then listens to matchQueue/{uid}).
 */
exports.requestMatch = onCall(async (request) => {
  const uid = requireAuth(request);
  const { mood, valence, regionId } = request.data || {};
  if (typeof mood !== "string" || typeof valence !== "number") {
    throw new HttpsError("invalid-argument", "mood and valence are required.");
  }

  const now = Date.now();
  const cutoff = now - MOOD_WINDOW_MS;

  // Candidate pool: other users waiting, recent, with a close-enough valence.
  const snap = await db
    .collection("matchQueue")
    .where("state", "==", "waiting")
    .where("createdAt", ">=", cutoff)
    .orderBy("createdAt", "asc")
    .limit(20)
    .get();

  const candidates = snap.docs.filter(
    (d) => d.id !== uid && Math.abs((d.data().valence || 0) - valence) <= VALENCE_TOLERANCE,
  );

  for (const candidate of candidates) {
    const sessionId = await tryClaim(candidate.id, uid, mood, valence, now);
    if (sessionId) return { sessionId };
  }

  // No one to match: enqueue (merge so a re-request refreshes the timestamp).
  await db.collection("matchQueue").doc(uid).set(
    { uid, mood, valence, regionId: regionId || null, createdAt: now, state: "waiting" },
    { merge: true },
  );
  return { sessionId: null };
});

/** Atomically turn a waiting peer + the caller into a chat session. */
async function tryClaim(peerUid, uid, myMood, myValence, now) {
  const peerRef = db.collection("matchQueue").doc(peerUid);
  const myRef = db.collection("matchQueue").doc(uid);
  const sessionRef = db.collection("chatSessions").doc();

  try {
    await db.runTransaction(async (tx) => {
      const peer = await tx.get(peerRef);
      if (!peer.exists || peer.data().state !== "waiting") {
        throw new HttpsError("aborted", "peer-taken");
      }
      const peerData = peer.data();
      tx.set(sessionRef, {
        participants: [uid, peerUid],
        moods: { [uid]: myMood, [peerUid]: peerData.mood },
        startedAt: now,
        expiresAt: now + CHAT_DURATION_MS,
        closed: false,
      });
      tx.update(peerRef, { state: "matched", sessionId: sessionRef.id });
      // Caller's own entry (if any) resolves to matched too.
      tx.set(myRef, { state: "matched", sessionId: sessionRef.id }, { merge: true });
    });
    return sessionRef.id;
  } catch (e) {
    if (e instanceof HttpsError && e.message === "peer-taken") return null;
    logger.warn("tryClaim failed", { error: e.message });
    return null;
  }
}

exports.cancelMatch = onCall(async (request) => {
  const uid = requireAuth(request);
  await db.collection("matchQueue").doc(uid).delete().catch(() => {});
  return { ok: true };
});

exports.leaveSession = onCall(async (request) => {
  const uid = requireAuth(request);
  const { sessionId } = request.data || {};
  if (!sessionId) throw new HttpsError("invalid-argument", "sessionId required.");
  await closeIfParticipant(sessionId, uid);
  return { ok: true };
});

exports.reportPeer = onCall(async (request) => {
  const uid = requireAuth(request);
  const { sessionId, reason } = request.data || {};
  if (!sessionId) throw new HttpsError("invalid-argument", "sessionId required.");

  const ref = db.collection("chatSessions").doc(sessionId);
  const snap = await ref.get();
  if (!snap.exists || !(snap.data().participants || []).includes(uid)) {
    throw new HttpsError("permission-denied", "Not your session.");
  }
  await db.collection("reports").add({
    sessionId,
    reporterUid: uid,
    reason: typeof reason === "string" ? reason.slice(0, 200) : "user_report",
    createdAt: Date.now(),
  });
  await ref.update({ closed: true });
  return { ok: true };
});

async function closeIfParticipant(sessionId, uid) {
  const ref = db.collection("chatSessions").doc(sessionId);
  const snap = await ref.get();
  if (!snap.exists) return;
  if (!(snap.data().participants || []).includes(uid)) {
    throw new HttpsError("permission-denied", "Not your session.");
  }
  await ref.update({ closed: true });
}

/**
 * Close any session past its expiry and delete its messages. Runs often so
 * chat content does not linger (SOW: auto-delete).
 */
exports.closeExpiredSessions = onSchedule("every 2 minutes", async () => {
  const now = Date.now();
  const expired = await db
    .collection("chatSessions")
    .where("expiresAt", "<=", now)
    .where("closed", "==", false)
    .get();

  for (const doc of expired.docs) {
    await doc.ref.update({ closed: true });
  }

  // Purge messages of sessions closed for over a minute.
  const stale = await db
    .collection("chatSessions")
    .where("closed", "==", true)
    .where("expiresAt", "<=", now - 60_000)
    .get();

  for (const doc of stale.docs) {
    const msgs = await doc.ref.collection("messages").get();
    let batch = db.batch();
    let n = 0;
    for (const m of msgs.docs) {
      batch.delete(m.ref);
      if (++n === 450) { await batch.commit(); batch = db.batch(); n = 0; }
    }
    if (n > 0) await batch.commit();
  }

  logger.info("closeExpiredSessions", { closed: expired.size, purged: stale.size });
});
