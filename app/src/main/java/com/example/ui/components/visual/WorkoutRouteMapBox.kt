package com.example.ui.components

import androidx.compose.runtime.remember

import com.repforge.app.ui.theme.trainPilotBorder

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.repforge.app.ui.theme.TrainPilotTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.localization.LocalTrainPilotStrings
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.components.mapcontrols.MapAppearanceSelector

import com.example.model.AppLanguage

/**
 * High-detail Margitsziget / Danube / Margit híd GPS Route Map Canvas
 * Matching Mockup 3/4 #3 (`Edzésútvonal`) & Mockup 2/4 #4 (`Kültéri mérés`).
 */
/**
 * Shared saved-workout / live-outdoor GPS map.
 * Styling changes only the map presentation, not its recorded coordinates or tracking lifecycle.
 */
@Composable
fun NativeWorkoutRouteMapBox(
    modifier: Modifier = Modifier,
    interactiveControls: Boolean = true,
    routeKeys: List<String> = emptyList(),
    refreshToken: Int = 0,
    mapContentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(0.dp),
    showZoomControls: Boolean = true
) {
    val palette = LocalTrainPilotPalette.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferences = remember(context) {
        com.repforge.app.data.store.TrainPilotSettingsStore(context.applicationContext)
    }
    val mapAppearance by preferences.mapAppearanceFlow.collectAsState(
        initial = com.repforge.app.data.store.MapAppearance.FOLLOW_APP)
    val darkMapMode = mapAppearance.isDark(palette.isDark)
    val preferenceScope = rememberCoroutineScope()
    val mapStyle = remember(context, darkMapMode) {
        if (darkMapMode) com.google.android.gms.maps.model.MapStyleOptions.loadRawResourceStyle(
            context, com.repforge.app.R.raw.trainpilot_map_dark
        ) else null
    }
    // A different workout must never momentarily show the previous workout's private route.
    var segments by remember(routeKeys) {
        mutableStateOf<List<List<Pair<Double, Double>>>>(emptyList())
    }
    androidx.compose.runtime.LaunchedEffect(routeKeys, refreshToken) {
        segments = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            com.repforge.app.ui.journal.WorkoutRouteCoordinates.loadAll(context, routeKeys)?.segments.orEmpty()
                .map { segment -> segment.filter { (lat, lon) ->
                    lat.isFinite() && lon.isFinite() && lat in -90.0..90.0 && lon in -180.0..180.0
                } }
        }
    }
    val points = remember(segments) {
        segments.flatten().map { (lat, lon) -> com.google.android.gms.maps.model.LatLng(lat, lon) }
    }
    val drawableSegments = remember(segments) {
        recordedPolylineSegments(segments).map { segment ->
            segment.map { (lat, lon) -> com.google.android.gms.maps.model.LatLng(lat, lon) }
        }
    }
    val endpoints = remember(segments) { recordedMapEndpoints(segments) }
    val configured = remember(context) { com.repforge.app.NativeMapsConfiguration.configured(context) }
    val playServicesAvailable = remember(context) {
        runCatching { com.google.android.gms.common.GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(context) == com.google.android.gms.common.ConnectionResult.SUCCESS }.getOrDefault(false)
    }
    var loaded by remember { mutableStateOf(false) }
    var timedOut by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    androidx.compose.runtime.LaunchedEffect(configured, playServicesAvailable, retry) {
        loaded = false
        timedOut = false
        if (configured && playServicesAvailable) {
            kotlinx.coroutines.delay(15_000)
            if (!loaded) timedOut = true
        }
    }
    BoxWithConstraints(modifier.clip(RoundedCornerShape(24.dp)).background(palette.background)
        .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(24.dp)), contentAlignment = Alignment.Center) {
        // Native Google zoom/logo controls must fit between the actual map overlays.
        // On cramped windows gesture zoom remains enabled; avoid buried +/- buttons.
        val googleControlsHeight = maxHeight - mapContentPadding.calculateTopPadding() -
            mapContentPadding.calculateBottomPadding()
        val lang = LocalTrainPilotStrings.current.language
        if (!configured || !playServicesAvailable) {
            val missingMapsText = if (!configured) {
                when (lang) {
                    AppLanguage.HU -> "Google Maps nincs beállítva. Az útvonal helyben megmarad."
                    AppLanguage.EN -> "Google Maps is not configured. The route remains saved locally."
                    AppLanguage.DE -> "Google Maps ist nicht konfiguriert. Die Route bleibt lokal gespeichert."
                    AppLanguage.RO -> "Google Maps nu este configurat. Traseul rămâne salvat local."
                }
            } else {
                when (lang) {
                    AppLanguage.HU -> "A térképhez Google Play-szolgáltatások szükségesek. Az útvonal helyben megmarad."
                    AppLanguage.EN -> "Google Play Services are required for the map. The route remains saved locally."
                    AppLanguage.DE -> "Für die Karte sind Google Play-Dienste erforderlich. Die Route bleibt lokal gespeichert."
                    AppLanguage.RO -> "Serviciile Google Play sunt necesare pentru hartă. Traseul rămâne salvat local."
                }
            }
            Text(missingMapsText,
                style = MaterialTheme.typography.bodySmall, color = palette.textSecondary,
                modifier = Modifier.padding(12.dp))
        } else {
            val startAndFinishTitle = when (lang) {
                AppLanguage.HU -> "Rajt és cél"
                AppLanguage.EN -> "Start and finish"
                AppLanguage.DE -> "Start und Ziel"
                AppLanguage.RO -> "Start și sosire"
            }
            val startTitle = when (lang) {
                AppLanguage.HU -> "Rajt"
                AppLanguage.EN -> "Start"
                AppLanguage.DE -> "Start"
                AppLanguage.RO -> "Start"
            }
            val finishTitle = when (lang) {
                AppLanguage.HU -> "Cél"
                AppLanguage.EN -> "Finish"
                AppLanguage.DE -> "Ziel"
                AppLanguage.RO -> "Sosire"
            }
            androidx.compose.runtime.key(retry) {
                val camera = com.google.maps.android.compose.rememberCameraPositionState()
                // Only actual recorded coordinates move the camera. No synthetic GPS location.
                androidx.compose.runtime.LaunchedEffect(loaded, points.firstOrNull()) {
                    if (loaded) points.firstOrNull()?.let { point ->
                        camera.move(com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(point, 16f))
                    }
                }
                com.google.maps.android.compose.GoogleMap(
                    modifier = Modifier.fillMaxSize(), cameraPositionState = camera,
                    properties = com.google.maps.android.compose.MapProperties(mapStyleOptions = mapStyle),
                    contentPadding = mapContentPadding,
                    onMapLoaded = { loaded = true; timedOut = false },
                    uiSettings = com.google.maps.android.compose.MapUiSettings(
                        zoomControlsEnabled = interactiveControls && showZoomControls && googleControlsHeight >= 180.dp,
                        scrollGesturesEnabled = interactiveControls,
                        zoomGesturesEnabled = interactiveControls, rotationGesturesEnabled = interactiveControls)
                ) {
                    // Distinct paused/ownership segments are never connected by a synthetic line.
                    drawableSegments.forEach { segment ->
                        com.google.maps.android.compose.Polyline(points = segment, color = palette.primary, width = 8f)
                    }
                    endpoints?.let { recorded ->
                        val startPoint = com.google.android.gms.maps.model.LatLng(
                            recorded.start.first, recorded.start.second)
                        com.google.maps.android.compose.Marker(
                            state = com.google.maps.android.compose.MarkerState(position = startPoint),
                            title = if (recorded.finish == null) startAndFinishTitle else startTitle,
                            icon = com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(
                                com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_AZURE)
                        )
                        recorded.finish?.let { (lat, lon) ->
                            com.google.maps.android.compose.Marker(
                                state = com.google.maps.android.compose.MarkerState(
                                    position = com.google.android.gms.maps.model.LatLng(lat, lon)),
                                title = finishTitle,
                                icon = com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(
                                    com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_ORANGE)
                            )
                        }
                    }
                }
            }
            MapAppearanceSelector(
                appearance = mapAppearance,
                onSelect = { selected -> preferenceScope.launch { preferences.setMapAppearance(selected) } },
                modifier = Modifier.align(Alignment.TopEnd)
                    .padding(top = mapContentPadding.calculateTopPadding() + 8.dp, end = 8.dp)
            )
            if (!loaded) {
                val mapStatusText = if (timedOut) {
                    when (lang) {
                        AppLanguage.HU -> "A térkép nem töltődött be. Ellenőrizd az internetet és a Maps-kulcs engedélyezését."
                        AppLanguage.EN -> "The map did not load. Check your internet connection and Maps API key."
                        AppLanguage.DE -> "Karte wurde nicht geladen. Internetverbindung und Maps-Schlüssel prüfen."
                        AppLanguage.RO -> "Harta nu s-a încărcat. Verifică internetul și cheia Maps."
                    }
                } else {
                    when (lang) {
                        AppLanguage.HU -> "Google Maps betöltése…"
                        AppLanguage.EN -> "Loading Google Maps…"
                        AppLanguage.DE -> "Google Maps wird geladen…"
                        AppLanguage.RO -> "Se încarcă Google Maps…"
                    }
                }
                val retryLabel = when (lang) {
                    AppLanguage.HU -> "Újrapróbálás"
                    AppLanguage.EN -> "Retry"
                    AppLanguage.DE -> "Erneut versuchen"
                    AppLanguage.RO -> "Reîncearcă"
                }
                Surface(color = palette.surfaceCard.copy(alpha = 0.94f), shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(mapStatusText, style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary)
                        if (timedOut) TextButton(onClick = { retry += 1 }) { Text(retryLabel) }
                    }
                }
            }
        }
    }
}
