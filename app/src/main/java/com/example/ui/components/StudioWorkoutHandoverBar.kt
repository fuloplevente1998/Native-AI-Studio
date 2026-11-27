package com.example.ui.components

import com.example.ui.theme.glassTile
import com.example.ui.theme.LocalGlassChromeUnderlap
import com.example.ui.theme.glassHeaderMaterial
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.trainPilotGlassRim
import com.example.ui.theme.GlassMaterialRole

import com.repforge.app.ui.theme.trainPilotClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.localization.LocalizedStrings
import com.example.model.AppLanguage
import com.example.model.SubScreen
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.theme.selectionColors
import com.example.ui.viewmodel.TrainPilotUiState
import com.repforge.app.ActiveWorkoutStore
import com.repforge.app.StudioWorkoutSessionPresentation
import com.repforge.app.ui.theme.TrainPilotFrames
import kotlinx.coroutines.delay
import java.time.Instant

internal fun isActiveMeasurementScreen(screen: SubScreen): Boolean = screen in setOf(
    SubScreen.ACTIVE_STRENGTH, SubScreen.ACTIVE_TIMED, SubScreen.ACTIVE_BILATERAL, SubScreen.ACTIVE_OUTDOOR_GPS)

/** Compact presentation only. The native ownership, ACK and command guards remain unchanged. */
@Composable
internal fun StudioWorkoutHandoverBar(state: TrainPilotUiState, strings: LocalizedStrings, onToggle: () -> Unit) {
    val palette = LocalTrainPilotPalette.current
    val banner = state.statusBannerMessage?.takeIf { it.isNotBlank() }
    if (!state.isWorkoutSessionActive && banner == null) return
    val selection = remember(palette) { palette.selectionColors() }
    fun word(hu: String, en: String, de: String, ro: String) = when (state.language) {
        AppLanguage.HU -> hu; AppLanguage.EN -> en; AppLanguage.DE -> de; AppLanguage.RO -> ro
    }
    val frame by ActiveWorkoutStore.snapshotFlow.collectAsState()
    val liveHealth by com.repforge.app.WearLiveHealthStore.liveHealthFlow.collectAsState()
    val expectedNode by com.repforge.app.WearLiveHealthStore.expectedNode351.collectAsState()
    var measurementNow by remember(state.activeWorkoutId) { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state.activeWorkoutId) {
        while(state.isWorkoutSessionActive) { measurementNow=System.currentTimeMillis();delay(1000) }
    }
    val measurement = com.repforge.app.PhoneMeasurementReconnect351.freshReceipt(liveHealth,state.activeWorkoutId,
        frame?.optLong("ownerEpoch193") ?: -1L,expectedNode,measurementNow)
    val measurementStatus = when {
        measurement != null && state.watchConnected -> when (measurement.optString("state")) {
            "active" -> word("Órás mérés aktív", "Watch recording active", "Uhrmessung aktiv", "Măsurare ceas activă")
            "paused" -> word("Órás mérés szünetel", "Watch recording paused", "Uhrmessung pausiert", "Măsurare ceas în pauză")
            "starting", "reconnecting", "ending" -> word("Órás mérés: folyamatban…", "Watch recording: pending…", "Uhrmessung: läuft an…", "Măsurare ceas: în curs…")
            "foregroundRequired" -> word("Nyisd meg a TrainPilotot az órán", "Open TrainPilot on the watch", "TrainPilot auf der Uhr öffnen", "Deschide TrainPilot pe ceas")
            "disabled", "permissionRequired", "error", "ended" -> measurement.optString("message").ifBlank { word("Órás mérés nem aktív", "Watch recording inactive", "Uhrmessung inaktiv", "Măsurare ceas inactivă") }
            else -> null
        }
        measurement != null && !state.watchConnected -> word("Órás mérés: nincs élő kapcsolat", "Watch recording: disconnected", "Uhrmessung: keine Verbindung", "Măsurare ceas: deconectat")
        liveHealth?.optString("workoutId")==state.activeWorkoutId && state.activeWorkoutId.isNotBlank() -> word("Órás mérés: nincs friss visszaigazolás", "Watch recording: no recent confirmation", "Uhrmessung: keine aktuelle Bestätigung", "Măsurare ceas: fără confirmare recentă")
        else -> null
    } ?: state.watchMeasurementStatus.takeIf { state.watchMeasurementStatusId == state.activeWorkoutId }
    val currentSnapshot = frame?.takeIf {
        it.optBoolean("active") &&
            (state.activeWorkoutId.isBlank() || it.optString("workoutId") == state.activeWorkoutId) &&
            (state.activeWorkoutGeneration <= 0L || it.optLong("generation") == state.activeWorkoutGeneration)
    }
    var now by remember(state.activeWorkoutId) { mutableStateOf(Instant.now()) }
    val paused = currentSnapshot?.optBoolean("workoutPaused") == true
    LaunchedEffect(state.activeWorkoutId, paused, state.isWorkoutSessionActive) {
        now = Instant.now()
        while (state.isWorkoutSessionActive && !paused) {
            delay(1000)
            now = Instant.now()
        }
    }
    val elapsedSeconds = currentSnapshot?.let { StudioWorkoutSessionPresentation.elapsedSeconds(it, now) }
        ?: state.workoutStartedEpochMs.takeIf { it > 0L }?.let { ((now.toEpochMilli() - it) / 1000L).coerceAtLeast(0L) }
    val elapsedText = elapsedSeconds?.let(StudioWorkoutSessionPresentation::durationLabel) ?: "00:00"
    val pauseLabel = word("Szünet", "Paused", "Pause", "Pauză")
    val enabled = state.isWorkoutSessionActive && !state.watchControlPending && !state.isSavingWorkout &&
        (!state.phoneControlsWorkout || state.showConnectedWatch)
    val ownerLabel = if (state.phoneControlsWorkout) word("Telefon", "Phone", "Telefon", "Telefon")
        else word("Óra", "Watch", "Uhr", "Ceas")
    val actionLabel = if (state.watchControlPending) word("Átadás folyamatban", "Transfer in progress", "Übergabe läuft", "Transfer în curs")
        else if (state.phoneControlsWorkout) strings.handoverToWatchAction else strings.reclaimToPhoneAction
    val liveChrome = LocalGlassChromeUnderlap.current && palette.isLiquidGlass
    val shape = RoundedCornerShape(10.dp)
    val fill = if (palette.isLiquidGlass) palette.glassTile(selection.surface, elevated = true) else selection.surface
    val foreground = if (palette.isLiquidGlass) palette.textPrimary else selection.foreground
    Surface(color = if (liveChrome) Color.Transparent else palette.glassTile(palette.surfaceCard, elevated = true), shape = RoundedCornerShape(14.dp),
        border = com.repforge.app.ui.theme.trainPilotFrameStroke(TrainPilotFrames.stroke(0.75.dp, palette.cardBorder)),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
            .then(if (liveChrome) Modifier.glassHeaderMaterial(palette, RoundedCornerShape(14.dp))
                else Modifier.trainPilotGlassRim(palette, RoundedCornerShape(14.dp), GlassMaterialRole.Performance))
            .testTag("workout_sticky_handover")) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (state.isWorkoutSessionActive) {
                val statusContent: @Composable () -> Unit = {
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(word("Vezérlés:", "Control:", "Steuerung:", "Control:"),
                                style = MaterialTheme.typography.labelMedium, color = palette.textPrimary)
                            Icon(if (state.phoneControlsWorkout) Icons.Default.PhoneAndroid else Icons.Default.Watch,
                                contentDescription = ownerLabel, tint = palette.primary, modifier = Modifier.size(18.dp).testTag("workout_control_owner"))
                        }
                        if (state.showWatchWorkoutControls) measurementStatus?.let {
                            Text(it, color = palette.textSecondary, style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.testTag("watch_measurement_status"))
                        }
                        if (state.watchControlPending) Text(actionLabel, color = palette.textSecondary, style = MaterialTheme.typography.labelSmall)
                        if (!state.showConnectedWatch && !state.phoneControlsWorkout && !state.watchControlPending)
                            Text(word("Óravezérlés nem elérhető", "Watch control unavailable", "Uhrsteuerung nicht verfügbar", "Control ceas indisponibil"),
                                color = palette.textSecondary, style = MaterialTheme.typography.labelSmall)
                        if (paused) Text(pauseLabel, color = palette.textSecondary, style = MaterialTheme.typography.labelSmall)
                    }
                }
                val fontScale = LocalDensity.current.fontScale
                // The two visible frames share a single geometry. The button's outer touch
                // target is larger than its paint; the timer can grow with accessibility text.
                val timeStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                val measurer = androidx.compose.ui.text.rememberTextMeasurer()
                val density = LocalDensity.current
                val timeSize = measurer.measure(elapsedText, style = timeStyle, maxLines = 1).size
                val frameWidth = maxOf(72.dp, with(density) { timeSize.width.toDp() } + 12.dp)
                val frameHeight = maxOf(36.dp, with(density) { timeSize.height.toDp() } + 6.dp)
                val elapsedFrame: @Composable () -> Unit = {
                        Surface(shape = shape, color = fill, border = TrainPilotFrames.stroke(1.dp, selection.border),
                            modifier = Modifier.size(frameWidth, frameHeight)
                                .trainPilotGlassRim(palette, shape, GlassMaterialRole.Performance)
                                .testTag("active_workout_elapsed")) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(elapsedText, style = timeStyle, color = foreground, maxLines = 1)
                            }
                        }
                }
                val transferFrame: @Composable () -> Unit = {
                        if (state.showWatchWorkoutControls) {
                            Box(Modifier.width(frameWidth).heightIn(min = maxOf(48.dp, frameHeight))
                                .trainPilotClickable(shape, enabled = enabled, onClick = onToggle)
                                .semantics { contentDescription = actionLabel }
                                .testTag("workout_handover_action"), contentAlignment = Alignment.Center) {
                                Surface(shape = shape, color = if (enabled) fill else palette.glassTile(palette.surfaceElevated, elevated = true),
                                    border = TrainPilotFrames.stroke(1.dp, if (enabled) selection.border else palette.cardBorder),
                                    modifier = Modifier.size(frameWidth, frameHeight)
                                        .trainPilotGlassRim(palette, shape, GlassMaterialRole.Performance)
                                        .testTag("workout_handover_frame")) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                        val tint = if (enabled) foreground else palette.textSecondary.copy(alpha = 0.6f)
                                        Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Icon(if (state.phoneControlsWorkout) Icons.Default.Watch else Icons.Default.PhoneAndroid,
                                            contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                }
                val controls: @Composable () -> Unit = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        elapsedFrame()
                        transferFrame()
                    }
                }
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val availableWidth = maxWidth
                    val controlsWidth = if (state.showWatchWorkoutControls) frameWidth * 2 + 6.dp else frameWidth
                    if (availableWidth - controlsWidth < 120.dp || fontScale > 1.3f) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            statusContent()
                            if (availableWidth < controlsWidth) {
                                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    elapsedFrame()
                                    transferFrame()
                                }
                            } else Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) { controls() }
                        }
                    } else Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.weight(1f)) { statusContent() }
                        controls()
                    }
                }
            }
            banner?.let {
                Text(it, color = palette.textPrimary, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.testTag("workout_inline_status"))
            }
        }
    }
}
