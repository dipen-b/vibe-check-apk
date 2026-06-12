package com.vibecheck.app.data.real

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import com.vibecheck.app.core.Cities
import com.vibecheck.app.core.model.HeatmapScope
import com.vibecheck.app.core.model.RegionInfo
import com.vibecheck.app.core.model.RegionMoodAggregate
import com.vibecheck.app.data.HeatmapRepository
import java.util.Locale
import kotlinx.coroutines.tasks.await

/**
 * Reads the server-maintained per-region rollups ("regions/{regionId}":
 * count24h, valenceSum24h — kept fresh by the rollupRegions Cloud Function).
 * Region resolution snaps a coarse last-known location to the nearest shipped
 * city bucket; only that bucket id ever leaves the device.
 */
class RealHeatmapRepository(
    private val context: Context,
    private val firestore: FirebaseFirestore,
) : HeatmapRepository {

    private var cachedRegion: RegionInfo? = null

    override suspend fun aggregates(scope: HeatmapScope): Result<List<RegionMoodAggregate>> =
        runCatching {
            val snapshot = firestore.collection("regions").get().await()
            val all = snapshot.documents.mapNotNull { doc ->
                val region = Cities.byId(doc.id) ?: return@mapNotNull null
                val count = (doc.getLong("count24h") ?: 0L).toInt()
                if (count <= 0) return@mapNotNull null
                val sumValence = doc.getDouble("valenceSum24h") ?: 0.0
                RegionMoodAggregate(
                    region = region,
                    checkInCount = count,
                    averageValence = (sumValence / count).toFloat().coerceIn(0f, 1f),
                )
            }

            val me = resolveMyRegion().getOrNull() ?: Cities.ALL.first()
            when (scope) {
                HeatmapScope.LOCAL -> all.filter {
                    Cities.distanceKm(
                        it.region.latitude, it.region.longitude,
                        me.latitude, me.longitude,
                    ) < 400
                }
                HeatmapScope.NATIONAL -> all.filter { it.region.countryCode == me.countryCode }
                HeatmapScope.GLOBAL -> all
            }
        }

    @SuppressLint("MissingPermission")
    override suspend fun resolveMyRegion(): Result<RegionInfo> = runCatching {
        cachedRegion?.let { return@runCatching it }

        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

        val fromLocation = if (granted) {
            runCatching {
                LocationServices.getFusedLocationProviderClient(context)
                    .lastLocation.await()
            }.getOrNull()?.let { Cities.nearest(it.latitude, it.longitude) }
        } else null

        val region = fromLocation ?: localeFallback()
        cachedRegion = region
        region
    }

    /** No location permission: pick the country's default hub from the locale. */
    private fun localeFallback(): RegionInfo {
        val country = Locale.getDefault().country.uppercase()
        val id = if (country == "GB") "gb-lon" else "us-nyc"
        return Cities.byId(id) ?: Cities.ALL.first()
    }
}
