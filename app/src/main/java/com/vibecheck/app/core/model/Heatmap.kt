package com.vibecheck.app.core.model

enum class HeatmapScope { LOCAL, NATIONAL, GLOBAL }

/**
 * A coarse city/region bucket. The full US/UK city list ships with the app;
 * check-ins reference these ids so no precise location ever leaves the device.
 */
data class RegionInfo(
    val regionId: String,
    val name: String,
    val countryCode: String, // ISO 3166-1 alpha-2: "US" or "GB"
    val latitude: Double,
    val longitude: Double,
)

/** Aggregated, anonymised mood density for one region. */
data class RegionMoodAggregate(
    val region: RegionInfo,
    val checkInCount: Int,
    val averageValence: Float,
)
