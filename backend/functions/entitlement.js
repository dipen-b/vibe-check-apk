// Pure entitlement logic — no Firebase/googleapis imports, so it unit-tests
// cheaply. Decides VibeCheck Plus entitlement from a Google Play Developer API
// SubscriptionPurchaseV2 resource.

// States that grant access. A user in grace period (failed renewal, retrying)
// keeps access until it resolves.
const ACTIVE_STATES = new Set([
  "SUBSCRIPTION_STATE_ACTIVE",
  "SUBSCRIPTION_STATE_IN_GRACE_PERIOD",
]);

/**
 * @param {object} sub  SubscriptionPurchaseV2 (subscriptionState, lineItems[].expiryTime)
 * @param {number} now  epoch millis
 * @returns {{active: boolean, plusUntil: number}}
 *   plusUntil = latest line-item expiry in millis (0 when not entitled).
 */
function entitlementFromSubscription(sub, now) {
  if (!sub || !ACTIVE_STATES.has(sub.subscriptionState)) {
    return { active: false, plusUntil: 0 };
  }
  const expiries = (sub.lineItems || [])
    .map((item) => (item && item.expiryTime ? Date.parse(item.expiryTime) : NaN))
    .filter((ms) => !Number.isNaN(ms));
  const plusUntil = expiries.length ? Math.max(...expiries) : 0;
  const active = plusUntil > now;
  return { active, plusUntil: active ? plusUntil : 0 };
}

module.exports = { entitlementFromSubscription, ACTIVE_STATES };
