/**
 * Server-trusted subscription entitlement (CONTRACTS.md).
 *
 * The client mirrors Play purchase state optimistically, but the authoritative
 * entitlement is what this function records in users/{uid}.plusUntil. The app
 * treats plusUntil > now as "subscribed". Firestore rules forbid clients from
 * writing plusUntil, so only this function (Admin SDK) can grant Plus.
 */

const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { logger } = require("firebase-functions");
const admin = require("firebase-admin");
const { google } = require("googleapis");
const { entitlementFromSubscription } = require("./entitlement");

const db = admin.firestore();
const PACKAGE_NAME = "com.vibecheck.app";

exports.validatePurchase = onCall(async (request) => {
  const uid = request.auth && request.auth.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Sign in required.");

  const { purchaseToken } = request.data || {};
  if (typeof purchaseToken !== "string" || !purchaseToken) {
    throw new HttpsError("invalid-argument", "purchaseToken is required.");
  }

  // Uses Application Default Credentials — the Functions service account must be
  // linked in Play Console with the androidpublisher scope. No key file needed.
  const auth = new google.auth.GoogleAuth({
    scopes: ["https://www.googleapis.com/auth/androidpublisher"],
  });
  const androidpublisher = google.androidpublisher({ version: "v3", auth });

  let sub;
  try {
    const res = await androidpublisher.purchases.subscriptionsv2.get({
      packageName: PACKAGE_NAME,
      token: purchaseToken,
    });
    sub = res.data;
  } catch (e) {
    logger.error("validatePurchase: Play API verify failed", { error: e.message });
    throw new HttpsError("permission-denied", "Could not verify the purchase with Google Play.");
  }

  const { active, plusUntil } = entitlementFromSubscription(sub, Date.now());

  await db.collection("users").doc(uid).set(
    { plusUntil, lastActiveAt: Date.now() },
    { merge: true },
  );

  logger.info("validatePurchase recorded", { active, plusUntil });
  return { active, plusUntil };
});
