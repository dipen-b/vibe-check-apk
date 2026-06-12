// Unit tests for the pure entitlement decision (no emulator/googleapis needed).
// Run: node test/billing.test.js

const assert = require("assert");
const { entitlementFromSubscription } = require("../entitlement");

const NOW = 1_700_000_000_000;
const future = new Date(NOW + 30 * 24 * 60 * 60 * 1000).toISOString();
const past = new Date(NOW - 24 * 60 * 60 * 1000).toISOString();

let passed = 0;
function test(name, fn) { fn(); passed += 1; console.log(`  ok - ${name}`); }

test("active subscription grants entitlement until expiry", () => {
  const sub = { subscriptionState: "SUBSCRIPTION_STATE_ACTIVE", lineItems: [{ expiryTime: future }] };
  const r = entitlementFromSubscription(sub, NOW);
  assert.strictEqual(r.active, true);
  assert.strictEqual(r.plusUntil, Date.parse(future));
});

test("grace period still grants access", () => {
  const sub = { subscriptionState: "SUBSCRIPTION_STATE_IN_GRACE_PERIOD", lineItems: [{ expiryTime: future }] };
  assert.strictEqual(entitlementFromSubscription(sub, NOW).active, true);
});

test("expired line item is not entitled", () => {
  const sub = { subscriptionState: "SUBSCRIPTION_STATE_ACTIVE", lineItems: [{ expiryTime: past }] };
  const r = entitlementFromSubscription(sub, NOW);
  assert.strictEqual(r.active, false);
  assert.strictEqual(r.plusUntil, 0);
});

test("cancelled/expired state is not entitled even with future expiry", () => {
  const sub = { subscriptionState: "SUBSCRIPTION_STATE_EXPIRED", lineItems: [{ expiryTime: future }] };
  assert.strictEqual(entitlementFromSubscription(sub, NOW).active, false);
});

test("uses the latest expiry across line items", () => {
  const sub = {
    subscriptionState: "SUBSCRIPTION_STATE_ACTIVE",
    lineItems: [{ expiryTime: past }, { expiryTime: future }],
  };
  assert.strictEqual(entitlementFromSubscription(sub, NOW).plusUntil, Date.parse(future));
});

test("missing/empty input is safe", () => {
  assert.deepStrictEqual(entitlementFromSubscription(null, NOW), { active: false, plusUntil: 0 });
  assert.deepStrictEqual(
    entitlementFromSubscription({ subscriptionState: "SUBSCRIPTION_STATE_ACTIVE" }, NOW),
    { active: false, plusUntil: 0 },
  );
});

console.log(`\n${passed} tests passed.`);
