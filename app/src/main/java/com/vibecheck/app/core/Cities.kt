package com.vibecheck.app.core

import com.vibecheck.app.core.model.RegionInfo
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The coarse region buckets check-ins snap to (SOW: US/UK city level).
 * Shipping the list in the app means a device's precise location is only
 * ever compared locally — only the chosen regionId leaves the device.
 */
object Cities {
    val ALL: List<RegionInfo> = listOf(
        RegionInfo("us-nyc", "New York", "US", 40.7128, -74.0060),
        RegionInfo("us-la", "Los Angeles", "US", 34.0522, -118.2437),
        RegionInfo("us-chi", "Chicago", "US", 41.8781, -87.6298),
        RegionInfo("us-hou", "Houston", "US", 29.7604, -95.3698),
        RegionInfo("us-phx", "Phoenix", "US", 33.4484, -112.0740),
        RegionInfo("us-phl", "Philadelphia", "US", 39.9526, -75.1652),
        RegionInfo("us-sat", "San Antonio", "US", 29.4241, -98.4936),
        RegionInfo("us-sd", "San Diego", "US", 32.7157, -117.1611),
        RegionInfo("us-dal", "Dallas", "US", 32.7767, -96.7970),
        RegionInfo("us-sf", "San Francisco", "US", 37.7749, -122.4194),
        RegionInfo("us-sea", "Seattle", "US", 47.6062, -122.3321),
        RegionInfo("us-den", "Denver", "US", 39.7392, -104.9903),
        RegionInfo("us-bos", "Boston", "US", 42.3601, -71.0589),
        RegionInfo("us-atl", "Atlanta", "US", 33.7490, -84.3880),
        RegionInfo("us-mia", "Miami", "US", 25.7617, -80.1918),
        RegionInfo("gb-lon", "London", "GB", 51.5074, -0.1278),
        RegionInfo("gb-bir", "Birmingham", "GB", 52.4862, -1.8904),
        RegionInfo("gb-man", "Manchester", "GB", 53.4808, -2.2426),
        RegionInfo("gb-gla", "Glasgow", "GB", 55.8642, -4.2518),
        RegionInfo("gb-liv", "Liverpool", "GB", 53.4084, -2.9916),
        RegionInfo("gb-lee", "Leeds", "GB", 53.8008, -1.5491),
        RegionInfo("gb-edi", "Edinburgh", "GB", 55.9533, -3.1883),
        RegionInfo("gb-bri", "Bristol", "GB", 51.4545, -2.5879),
        RegionInfo("gb-car", "Cardiff", "GB", 51.4816, -3.1791),
        RegionInfo("gb-bel", "Belfast", "GB", 54.5973, -5.9301),
    )

    fun byId(regionId: String): RegionInfo? = ALL.firstOrNull { it.regionId == regionId }

    /** Nearest shipped city to the given coarse coordinates. */
    fun nearest(latitude: Double, longitude: Double): RegionInfo =
        ALL.minByOrNull { distanceKm(latitude, longitude, it.latitude, it.longitude) } ?: ALL.first()

    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}
