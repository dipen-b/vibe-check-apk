// Plain-node unit tests for the rollup aggregation math (no emulator needed).
// Run: node test/rollup.test.js   (or: npm test)

const assert = require("assert");
const { aggregateWindow, averageValence, DAY_MS } = require("../aggregate");

const NOW = 1_700_000_000_000; // fixed clock
const KNOWN = ["us-nyc", "gb-lon"];

let passed = 0;
function test(name, fn) {
  fn();
  passed += 1;
  console.log(`  ok - ${name}`);
}

test("counts and sums valence within the 24h window", () => {
  const checkins = [
    { regionId: "us-nyc", valence: 0.9, timestamp: NOW - 1000 },
    { regionId: "us-nyc", valence: 0.5, timestamp: NOW - 2000 },
    { regionId: "gb-lon", valence: 0.2, timestamp: NOW - 3000 },
  ];
  const t = aggregateWindow(checkins, KNOWN, NOW);
  assert.strictEqual(t["us-nyc"].count, 2);
  assert.strictEqual(t["us-nyc"].sum, 1.4);
  assert.strictEqual(t["gb-lon"].count, 1);
  assert.ok(Math.abs(averageValence(t["us-nyc"]) - 0.7) < 1e-9);
});

test("excludes check-ins older than 24h", () => {
  const checkins = [
    { regionId: "us-nyc", valence: 1.0, timestamp: NOW - DAY_MS - 1 }, // too old
    { regionId: "us-nyc", valence: 0.4, timestamp: NOW - DAY_MS + 1 }, // just inside
  ];
  const t = aggregateWindow(checkins, KNOWN, NOW);
  assert.strictEqual(t["us-nyc"].count, 1);
  assert.strictEqual(t["us-nyc"].sum, 0.4);
});

test("ignores unknown regions and malformed valence", () => {
  const checkins = [
    { regionId: "xx-zzz", valence: 0.9, timestamp: NOW },
    { regionId: "us-nyc", valence: "high", timestamp: NOW },
    { regionId: "us-nyc", timestamp: NOW },
  ];
  const t = aggregateWindow(checkins, KNOWN, NOW);
  assert.strictEqual(t["us-nyc"].count, 0);
  assert.strictEqual(t["xx-zzz"], undefined);
});

test("empty region yields null average", () => {
  const t = aggregateWindow([], KNOWN, NOW);
  assert.strictEqual(t["us-nyc"].count, 0);
  assert.strictEqual(averageValence(t["us-nyc"]), null);
});

console.log(`\n${passed} tests passed.`);
