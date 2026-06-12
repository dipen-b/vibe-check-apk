// Pure aggregation helpers — no Firebase imports, so they unit-test cheaply.

const DAY_MS = 24 * 60 * 60 * 1000;

/**
 * Reduce a list of check-ins into per-region { count, sum } totals,
 * counting only those within the rolling 24h window ending at `now`.
 *
 * @param {Array<{regionId:string, valence:number, timestamp:number}>} checkins
 * @param {Set<string>|Array<string>} knownRegionIds  regions to keep
 * @param {number} now  epoch millis (window end)
 * @returns {Object<string,{count:number,sum:number}>}
 */
function aggregateWindow(checkins, knownRegionIds, now) {
  const known = knownRegionIds instanceof Set ? knownRegionIds : new Set(knownRegionIds);
  const cutoff = now - DAY_MS;
  const totals = {};
  for (const id of known) totals[id] = { count: 0, sum: 0 };

  for (const c of checkins) {
    if (!known.has(c.regionId)) continue;
    if (typeof c.valence !== "number") continue;
    if (typeof c.timestamp !== "number" || c.timestamp < cutoff) continue;
    totals[c.regionId].count += 1;
    totals[c.regionId].sum += c.valence;
  }
  return totals;
}

/** Average valence for a region total, or null when there are no check-ins. */
function averageValence(total) {
  return total && total.count > 0 ? total.sum / total.count : null;
}

module.exports = { aggregateWindow, averageValence, DAY_MS };
