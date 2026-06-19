package com.vibecheck.app.ui.heatmap

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import com.vibecheck.app.core.model.HeatmapScope
import com.vibecheck.app.core.model.RegionMoodAggregate
import com.vibecheck.app.data.AppContainer
import com.vibecheck.app.ui.components.HeatmapListSkeleton
import com.vibecheck.app.ui.theme.ValenceHigh
import com.vibecheck.app.ui.theme.ValenceLow
import com.vibecheck.app.ui.theme.ValenceMid
import kotlin.math.max
import kotlinx.coroutines.launch

private enum class ViewMode { MAP, LIST }
private enum class ScopeChoice(val label: String, val coreScope: HeatmapScope, val countryFilter: String?) {
    LOCAL("Local", HeatmapScope.LOCAL, null),
    // US/UK fetch the global aggregate set and filter client-side, so the
    // selected country never depends on where the *viewer* happens to be.
    US("🇺🇸 US", HeatmapScope.GLOBAL, "US"),
    UK("🇬🇧 UK", HeatmapScope.GLOBAL, "GB"),
    GLOBAL("Global", HeatmapScope.GLOBAL, null),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeatmapScreen(container: AppContainer) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var scope by remember { mutableStateOf(ScopeChoice.UK) }
    var viewMode by remember { mutableStateOf(ViewMode.LIST) }
    var aggregates by remember { mutableStateOf<List<RegionMoodAggregate>?>(null) }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val cache = remember { mutableStateMapOf<ScopeChoice, List<RegionMoodAggregate>>() }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var refreshing by remember { mutableStateOf(false) }

    // Resolve default country on first load.
    LaunchedEffect(Unit) {
        val region = container.heatmapRepository.resolveMyRegion().getOrNull()
        scope = when (region?.countryCode) {
            "US" -> ScopeChoice.US
            "GB" -> ScopeChoice.UK
            else -> ScopeChoice.UK
        }
    }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Local view needs coarse location.")
            }
            scope = ScopeChoice.UK
        }
    }

    LaunchedEffect(scope, refreshTrigger) {
        if (scope == ScopeChoice.LOCAL) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                refreshing = false
                return@LaunchedEffect
            }
        }
        val cached = cache[scope]
        if (cached != null && !refreshing) {
            aggregates = cached
            return@LaunchedEffect
        }
        if (aggregates == null) loading = true
        errorMessage = null
        container.heatmapRepository.aggregates(scope.coreScope).fold(
            onSuccess = { result ->
                val filtered = scope.countryFilter
                    ?.let { cc -> result.filter { it.region.countryCode == cc } }
                    ?: result
                aggregates = filtered
                cache[scope] = filtered
            },
            onFailure = { errorMessage = it.message ?: "Couldn't load the heatmap." },
        )
        loading = false
        refreshing = false
    }

    val cameraPositionState = rememberCameraPositionState {
        position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(
            LatLng(54.0, -2.5), 5.2f,
        )
    }
    LaunchedEffect(scope, viewMode) {
        val (target, zoom) = when (scope) {
            ScopeChoice.LOCAL -> LatLng(54.0, -2.5) to 7f
            ScopeChoice.US -> LatLng(39.8, -98.5) to 3.6f
            ScopeChoice.UK -> LatLng(54.0, -2.5) to 5.2f
            ScopeChoice.GLOBAL -> LatLng(30.0, -30.0) to 1.8f
        }
        val position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(target, zoom)
        // CameraUpdateFactory is only initialized once a GoogleMap has been
        // composed. The screen opens in list view, so animate() would NPE
        // ("CameraUpdateFactory is not initialized"). Animate only when the map
        // is visible; otherwise set the camera directly (no factory needed).
        if (viewMode == ViewMode.MAP) {
            runCatching { cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(target, zoom)) }
                .onFailure { cameraPositionState.position = position }
        } else {
            cameraPositionState.position = position
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                "Mood Map",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "See how people are feeling across regions — all anonymous, no individual data.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
                    ScopeChoice.entries.forEachIndexed { index, choice ->
                        SegmentedButton(
                            selected = scope == choice,
                            onClick = { scope = choice },
                            shape = SegmentedButtonDefaults.itemShape(index, ScopeChoice.entries.size),
                        ) { Text(choice.label, fontSize = 12.sp) }
                    }
                }
                Spacer(Modifier.width(8.dp))
                FilledIconToggleButton(
                    checked = viewMode == ViewMode.MAP,
                    onCheckedChange = {
                        viewMode = if (viewMode == ViewMode.MAP) ViewMode.LIST else ViewMode.MAP
                    },
                ) {
                    Icon(
                        imageVector = if (viewMode == ViewMode.MAP)
                            Icons.AutoMirrored.Outlined.List else Icons.Outlined.Map,
                        contentDescription = "Toggle view",
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = {
                    refreshing = true
                    cache.remove(scope)
                    refreshTrigger++
                },
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    loading && aggregates == null -> HeatmapListSkeleton()
                    errorMessage != null && aggregates == null ->
                        Column(
                            Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(errorMessage!!)
                            TextButton(onClick = { cache.remove(scope); refreshTrigger++ }) {
                                Text("Retry")
                            }
                        }
                    else -> {
                        val data = aggregates.orEmpty()
                        if (data.isEmpty()) {
                            Column(
                                Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text("🌍", fontSize = 40.sp)
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "No check-ins here yet",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    "Pull down to refresh",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                        } else if (viewMode == ViewMode.MAP) {
                            MapView(data, cameraPositionState)
                        } else {
                            ListView(data)
                        }
                    }
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
            snackbar = { Snackbar(it) },
        )
    }
}

@Composable
private fun MapView(data: List<RegionMoodAggregate>, cameraPositionState: CameraPositionState) {
    var selected by remember { mutableStateOf<RegionMoodAggregate?>(null) }
    val maxCount = remember(data) { max(1, data.maxOfOrNull { it.checkInCount } ?: 1) }

    Box(Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = false),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                compassEnabled = false,
            ),
        ) {
            data.forEach { agg ->
                val color = valenceColor(agg.averageValence)
                val radius = 18_000.0 + (agg.checkInCount.toDouble() / maxCount) * 42_000.0
                Circle(
                    center = LatLng(agg.region.latitude, agg.region.longitude),
                    radius = radius,
                    fillColor = color.copy(alpha = 0.45f),
                    strokeColor = color,
                    strokeWidth = 2f,
                    clickable = true,
                    onClick = { selected = agg },
                )
            }
        }
        LegendBar(modifier = Modifier.align(Alignment.BottomStart).padding(8.dp))
        AnimatedVisibility(
            visible = selected != null,
            modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp),
        ) {
            selected?.let { RegionInfoCard(it) }
        }
    }
}

@Composable
private fun ListView(data: List<RegionMoodAggregate>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
        items(data.sortedByDescending { it.checkInCount }) { agg ->
            RegionRow(agg)
        }
    }
}

@Composable
private fun RegionRow(agg: RegionMoodAggregate) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .then(Modifier),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(14.dp).clip(CircleShape).background(valenceColor(agg.averageValence)),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    "${agg.region.name} ${flag(agg.region.countryCode)}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
                Text(
                    "${agg.checkInCount} check-ins",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                moodLabel(agg.averageValence),
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = valenceColor(agg.averageValence),
            )
        }
    }
}

@Composable
private fun RegionInfoCard(agg: RegionMoodAggregate) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(14.dp).clip(CircleShape).background(valenceColor(agg.averageValence)),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "${agg.region.name} ${flag(agg.region.countryCode)}",
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    "${agg.checkInCount} check-ins · ${moodLabel(agg.averageValence)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LegendBar(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shape = RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text("low", style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.width(6.dp))
        Box(
            Modifier
                .width(60.dp).height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    Brush.horizontalGradient(listOf(ValenceLow, ValenceMid, ValenceHigh))
                )
        )
        Spacer(Modifier.width(6.dp))
        Text("high", style = MaterialTheme.typography.labelSmall)
    }
}

private fun valenceColor(v: Float): Color {
    val clamped = v.coerceIn(0f, 1f)
    return if (clamped < 0.5f) lerp(ValenceLow, ValenceMid, clamped * 2f)
    else lerp(ValenceMid, ValenceHigh, (clamped - 0.5f) * 2f)
}

private fun moodLabel(v: Float): String = when {
    v >= 0.65f -> "mostly upbeat"
    v >= 0.45f -> "mixed"
    else -> "heavy"
}

private fun flag(countryCode: String): String = when (countryCode.uppercase()) {
    "US" -> "🇺🇸"
    "GB" -> "🇬🇧"
    else -> ""
}
