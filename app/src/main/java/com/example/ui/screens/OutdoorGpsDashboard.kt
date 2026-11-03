package com.example.ui.screens

import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

import com.repforge.app.ui.theme.TrainPilotOutlinedButton

import com.repforge.app.ui.theme.trainPilotBorder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import android.graphics.Rect
import android.os.Build
import android.view.WindowInsets
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppLanguage
import com.example.localization.LocalizedStrings
import com.example.ui.theme.LocalTrainPilotPalette

internal val LocalOutdoorMapContentPadding = staticCompositionLocalOf { PaddingValues(0.dp) }

/**
 * The Android 16 device can report a fullscreen Dialog Compose root taller than the
 * physically visible display. Constrain the *content*, rather than assuming that
 * decorFitsSystemWindows protects the bottom action buttons.
 */
internal data class OutdoorVisibleViewportPx(val topInset: Int, val height: Int)

internal fun outdoorVisibleViewportPx(
    hostHeight: Int,
    viewTopOnScreen: Int,
    visibleTopOnScreen: Int,
    visibleBottomOnScreen: Int,
    navigationBarBottom: Int,
    bottomReserve: Int
): OutdoorVisibleViewportPx {
    if (hostHeight <= 0) return OutdoorVisibleViewportPx(0, 0)
    val topInset = (visibleTopOnScreen - viewTopOnScreen).coerceIn(0, hostHeight - 1)
    val displayBottom = if (visibleBottomOnScreen > visibleTopOnScreen)
        visibleBottomOnScreen else viewTopOnScreen + hostHeight
    // On edge-to-edge devices getWindowVisibleDisplayFrame can include the gesture bar.
    // Cap by the actual root-window inset as a second independent safety boundary.
    val safeBottom = minOf(displayBottom,
        viewTopOnScreen + hostHeight - navigationBarBottom.coerceAtLeast(0))
    val remaining = (safeBottom - viewTopOnScreen - topInset - bottomReserve.coerceAtLeast(0))
        .coerceIn(1, hostHeight - topInset)
    return OutdoorVisibleViewportPx(topInset, remaining)
}

/** Closing the map never changes the native workout. */
@Composable
internal fun OutdoorGpsFullscreenDialog(
    state: OutdoorGpsDisplay,
    strings: LocalizedStrings,
    actions: OutdoorGpsActions,
    onDismissRequest: () -> Unit,
    mapContent: @Composable (Modifier) -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = true)
    ) {
        val palette = LocalTrainPilotPalette.current
        val view = LocalView.current
        val density = LocalDensity.current
        var visibleViewport by remember(view) { mutableStateOf<OutdoorVisibleViewportPx?>(null) }
        BoxWithConstraints(
            Modifier.fillMaxSize()
                .background(palette.background)
                .onGloballyPositioned { coordinates ->
                    val visible = Rect()
                    val viewLocation = IntArray(2)
                    view.getWindowVisibleDisplayFrame(visible)
                    view.getLocationOnScreen(viewLocation)
                    val bottomInset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        view.rootWindowInsets?.getInsets(WindowInsets.Type.navigationBars())?.bottom ?: 0
                    } else 0
                    val reserve = with(density) { 12.dp.roundToPx() }
                    val next = outdoorVisibleViewportPx(
                        coordinates.size.height, viewLocation[1], visible.top, visible.bottom,
                        bottomInset, reserve
                    )
                    if (visibleViewport != next) visibleViewport = next
                }
                .testTag("outdoor_dialog_safe_host")
        ) {
            val top = visibleViewport?.let { with(density) { it.topInset.toDp() } } ?: 0.dp
            val safeHeight = visibleViewport?.let { with(density) { it.height.toDp() } }
                ?.coerceAtMost(maxHeight) ?: maxHeight
            Box(
                Modifier.fillMaxWidth().offset(y = top).height(safeHeight)
                    .testTag("outdoor_dialog_visible_viewport")
            ) {
                OutdoorGpsDashboard(
                    state = state,
                    strings = strings,
                    actions = actions.copy(back = onDismissRequest),
                    modifier = Modifier.fillMaxSize(),
                    fullscreen = true,
                    mapContent = mapContent
                )
            }
        }
    }
}

/** Display projection only; timing, GPS, ownership and finalization stay in the native engine. */
internal data class OutdoorGpsDisplay(
    val title: String,
    val status: String,
    val gpsEnabled: Boolean,
    val started: Boolean,
    val paused: Boolean,
    val elapsed: String,
    val distance: String,
    val pace: String,
    val calories: String,
    val heartRate: String,
    val pointCount: Int,
    val lapCount: Int,
    val latestLap: String,
    val error: String,
    val language: AppLanguage = AppLanguage.HU
)

internal data class OutdoorGpsActions(
    val back: () -> Unit,
    val settings: () -> Unit,
    val selectActivity: (String) -> Unit,
    val setGpsEnabled: (Boolean) -> Unit,
    val start: () -> Unit,
    val pause: () -> Unit,
    val lap: () -> Unit,
    val finish: () -> Unit,
    val expandMap: () -> Unit
)

/** The map yields height first; primary controls never belong to a scrolling list. */
@Composable
internal fun OutdoorGpsDashboard(
    state: OutdoorGpsDisplay,
    strings: LocalizedStrings,
    actions: OutdoorGpsActions,
    modifier: Modifier = Modifier,
    fullscreen: Boolean = false,
    mapContent: @Composable (Modifier) -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    BoxWithConstraints(modifier.fillMaxSize().background(palette.background)
        .testTag(if (fullscreen) "fullscreen_map_overlay" else "active_outdoor_gps_screen")) {
        val viewportHeight = maxHeight
        if (fullscreen) {
            val density = LocalDensity.current
            var headerHeight by remember { mutableIntStateOf(0) }
            var footerHeight by remember { mutableIntStateOf(0) }
            // Reserve the *measured* overlay space for Google Maps controls/logo.
            // Never guess a tall fixed footer in pixels; first layout has zero insets.
            val mapPadding = with(density) { PaddingValues(top = headerHeight.toDp() + 20.dp,
                bottom = footerHeight.toDp() + 20.dp) }
            Box(Modifier.fillMaxSize().clipToBounds().testTag("outdoor_fullscreen_map_bounds")) {
                CompositionLocalProvider(LocalOutdoorMapContentPadding provides mapPadding) {
                    mapContent(Modifier.fillMaxSize())
                }
                Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.onSizeChanged { headerHeight = it.height }) { DashboardHeader(state, actions, true) }
                    Spacer(Modifier.weight(1f))
                    Column(Modifier.onSizeChanged { footerHeight = it.height }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Column(Modifier.heightIn(max = (viewportHeight * 0.24f).coerceIn(52.dp, 180.dp))
                            .verticalScroll(rememberScrollState()).testTag("outdoor_fullscreen_metrics_scroll")) {
                            MetricsPanel(state, strings, showTimer = false)
                            if (state.error.isNotBlank()) Text(state.error, color = palette.danger,
                                modifier = Modifier.testTag("outdoor_finish_error"))
                        }
                        DashboardControls(state, strings, actions)
                    }
                }
            }
            return@BoxWithConstraints
        }
        // The compact map has a bounded height; metrics and pinned actions get the remaining space.
        val scrollSecondary = maxHeight < 560.dp || LocalDensity.current.fontScale > 1.35f || state.error.isNotBlank()
        // This card also contains the activity/GPS row (~56 dp). Reserve enough
        // height for the *map itself*, instead of counting that row as map area.
        val compactMapHeight = (maxHeight * 0.40f).coerceIn(210.dp, 300.dp)
        Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            DashboardHeader(state, actions, fullscreen)
            Column(
                    Modifier.weight(1f).then(if (scrollSecondary) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                        .testTag("outdoor_dashboard_body"),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(shape = RoundedCornerShape(20.dp), color = palette.surfaceCard,
                        modifier = Modifier.fillMaxWidth()
                            .height(compactMapHeight).testTag("outdoor_compact_map_card")
                            .trainPilotBorder(1.dp, palette.primary.copy(alpha = 0.45f), RoundedCornerShape(20.dp))) {
                        Column(Modifier.padding(6.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                ActivityPicker(state, actions, Modifier.weight(1f))
                                Text("GPS", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                                Switch(state.gpsEnabled, actions.setGpsEnabled, modifier = Modifier.testTag("outdoor_gps_toggle"))
                            }
                            Box(Modifier.fillMaxWidth().weight(1f).testTag("outdoor_compact_map_viewport")) {
                                mapContent(Modifier.fillMaxSize())
                                FilledTonalIconButton(onClick = actions.expandMap,
                                    modifier = Modifier.align(Alignment.TopStart).padding(4.dp).size(48.dp)
                                        .testTag("expand_map_fullscreen_btn")) {
                                    Icon(Icons.Default.Fullscreen, contentDescription = outdoorCopy(state.language).fullscreen)
                                }
                            }
                        }
                    }
                    MetricsPanel(state, strings, showTimer = !fullscreen)
                    if (state.error.isNotBlank()) Text(state.error, color = palette.danger,
                        style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("outdoor_finish_error"))
            }
            DashboardControls(state, strings, actions)
        }
    }
}

@Composable
private fun DashboardHeader(state: OutdoorGpsDisplay, actions: OutdoorGpsActions, fullscreen: Boolean) {
    val copy = outdoorCopy(state.language)
    val palette = LocalTrainPilotPalette.current
    Surface(shape = RoundedCornerShape(16.dp), color = palette.surfaceCard,
        modifier = Modifier.trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(16.dp))) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (fullscreen) {
                com.example.ui.components.TrainPilotRedCloseButton(actions.back, testTag = "gps_close_fullscreen")
            } else {
                IconButton(actions.back, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = copy.back, tint = palette.textPrimary)
                }
            }
            Column(Modifier.weight(1f).padding(vertical = 4.dp)) {
                Text(state.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                    color = palette.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(state.status, style = MaterialTheme.typography.labelSmall,
                    color = if (state.paused) palette.warning else palette.textSecondary,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            com.example.ui.components.StudioWorkoutHealthAction()
            if (fullscreen) Text(state.elapsed, color = palette.primary, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp).testTag("gps_elapsed_timer_text"))
            else IconButton(actions.settings, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Settings, contentDescription = copy.settings, tint = palette.textSecondary)
            }
        }
    }
}

@Composable
private fun ActivityPicker(state: OutdoorGpsDisplay, actions: OutdoorGpsActions, modifier: Modifier) {
    val copy = outdoorCopy(state.language)
    var expanded by remember { mutableStateOf(false) }
    val activities = listOf("🏃 Futás" to "Kültéri futás", "🚶 Séta" to "Kültéri séta",
        "🚴 Kerékpár" to "Kerékpározás", "🛼 Görkorcsolya" to "Görkorcsolyázás",
        "🥾 Túrázás" to "Kültéri túrázás", "⚡ Egyéb" to "Egyéb kültéri edzés")
    val translated = activities.mapIndexed { index, (_, name) -> copy.activities[index] to name }
    Box(modifier) {
        TextButton(onClick = { expanded = true }, enabled = !state.started,
            modifier = Modifier.heightIn(min = 48.dp).testTag("outdoor_activity_picker")) {
            Text(translated.firstOrNull { it.second == state.title }?.first ?: state.title,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(" ▾")
        }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            translated.forEach { (label, name) ->
                DropdownMenuItem(text = { Text(label) }, onClick = {
                    expanded = false; actions.selectActivity(name)
                }, modifier = Modifier.testTag("outdoor_chip_${name.lowercase()}"))
            }
        }
    }
}

@Composable
private fun MetricsPanel(state: OutdoorGpsDisplay, strings: LocalizedStrings, showTimer: Boolean) {
    val copy = outdoorCopy(state.language)
    val palette = LocalTrainPilotPalette.current
    Surface(shape = RoundedCornerShape(18.dp), color = palette.surfaceCard) {
        Column(Modifier.fillMaxWidth().padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (showTimer) Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (state.paused) copy.paused else if (state.started) copy.elapsed else copy.ready,
                    color = palette.textSecondary, style = MaterialTheme.typography.labelMedium)
                Text(state.elapsed, color = palette.textPrimary, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.testTag("gps_elapsed_timer_text"))
            }
            val metrics = listOf(Triple(strings.distanceLabel, state.distance, "${state.pointCount} ${copy.points}"),
                Triple(strings.avgPaceLabel, state.pace, if (state.latestLap.isBlank()) "${state.lapCount}. ${copy.lap}" else state.latestLap),
                Triple(copy.caloriesPulse, state.calories, state.heartRate))
            if (LocalDensity.current.fontScale > 1.35f) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    metrics.forEachIndexed { i, metric -> MetricTile(metric, i == 0, Modifier.fillMaxWidth()) }
                }
            } else Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                metrics.forEachIndexed { i, metric -> MetricTile(metric, i == 0, Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun MetricTile(metric: Triple<String, String, String>, highlighted: Boolean, modifier: Modifier) {
    val palette = LocalTrainPilotPalette.current
    Surface(shape = RoundedCornerShape(12.dp), color = palette.background,
        modifier = modifier.trainPilotBorder(1.dp, if (highlighted) palette.primary.copy(alpha = 0.5f) else palette.cardBorder,
            RoundedCornerShape(12.dp))) {
        Column(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(metric.first, style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
            Text(metric.second, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                color = if (highlighted) palette.primary else palette.textPrimary)
            Text(metric.third, style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
        }
    }
}

@Composable
private fun DashboardControls(state: OutdoorGpsDisplay, strings: LocalizedStrings, actions: OutdoorGpsActions) {
    val copy = outdoorCopy(state.language)
    val palette = LocalTrainPilotPalette.current
    Surface(shape = RoundedCornerShape(16.dp), color = palette.surfaceCard) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(6.dp)) {
            val stack = maxWidth < 300.dp || LocalDensity.current.fontScale > 1.35f
            if (!state.started) Button(actions.start, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .testTag("gps_start_workout_btn")) {
                Text(if (state.gpsEnabled) copy.gpsStart else copy.indoorStart)
            } else {
                val pause: @Composable (Modifier) -> Unit = { modifier ->
                    TrainPilotOutlinedButton(actions.pause, modifier.heightIn(min = 48.dp).testTag("gps_pause_resume_btn"),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)) {
                        Text(if (state.paused) strings.continueBtn else copy.pause)
                    }
                }
                val lap: @Composable (Modifier) -> Unit = { modifier ->
                    TrainPilotOutlinedButton(actions.lap, modifier.heightIn(min = 48.dp).testTag("gps_lap_btn"),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)) { Text("+ ${copy.lap} (${state.lapCount})") }
                }
                val finish: @Composable (Modifier) -> Unit = { modifier ->
                    Button(actions.finish, modifier.heightIn(min = 48.dp).testTag("gps_finish_workout_btn"),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary)) {
                        Text(copy.finish, fontWeight = FontWeight.Bold)
                    }
                }
                if (stack) Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { pause(Modifier.weight(1f)); lap(Modifier.weight(1f)) }
                    finish(Modifier.fillMaxWidth())
                } else Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    pause(Modifier.weight(1f)); lap(Modifier.weight(1f)); finish(Modifier.weight(1.15f))
                }
            }
        }
    }
}

private data class OutdoorCopy(
    val close: String, val back: String, val settings: String, val fullscreen: String,
    val gpsStart: String, val indoorStart: String, val paused: String, val elapsed: String,
    val ready: String, val pause: String, val finish: String, val lap: String,
    val points: String, val caloriesPulse: String, val activities: List<String>
)

private fun outdoorCopy(language: AppLanguage): OutdoorCopy = when (language) {
    AppLanguage.EN -> OutdoorCopy("Close", "Back", "Settings", "Fullscreen", "Start GPS", "Start indoor timer",
        "Paused", "Elapsed", "Ready", "Pause", "Finish", "Lap", "GPS points", "Calories / pulse",
        listOf("🏃 Run", "🚶 Walk", "🚴 Cycle", "🛼 Skate", "🥾 Hike", "⚡ Other"))
    AppLanguage.DE -> OutdoorCopy("Schließen", "Zurück", "Einstellungen", "Vollbild", "GPS starten", "Indoor-Zeit starten",
        "Pausiert", "Dauer", "Bereit", "Pause", "Beenden", "Runde", "GPS-Punkte", "Kalorien / Puls",
        listOf("🏃 Laufen", "🚶 Gehen", "🚴 Rad", "🛼 Skaten", "🥾 Wandern", "⚡ Andere"))
    AppLanguage.RO -> OutdoorCopy("Închide", "Înapoi", "Setări", "Ecran complet", "Pornește GPS", "Pornește cronometrul",
        "În pauză", "Durată", "Pregătit", "Pauză", "Finalizare", "Tură", "puncte GPS", "Calorii / puls",
        listOf("🏃 Alergare", "🚶 Mers", "🚴 Ciclism", "🛼 Role", "🥾 Drumeție", "⚡ Altele"))
    AppLanguage.HU -> OutdoorCopy("Bezárás", "Vissza", "Beállítások", "Teljes képernyő", "GPS-mérés indítása", "Beltéri mérés indítása",
        "Szüneteltetve", "Eltelt idő", "Indításra kész", "Szünet", "Befejezés", "Kör", "GPS-pont", "Kalória / pulzus",
        listOf("🏃 Futás", "🚶 Séta", "🚴 Kerékpár", "🛼 Görkorcsolya", "🥾 Túrázás", "⚡ Egyéb"))
}
