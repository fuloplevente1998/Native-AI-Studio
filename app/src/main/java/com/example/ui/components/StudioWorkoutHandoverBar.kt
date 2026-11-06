package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.localization.LocalizedStrings
import com.example.model.AppLanguage
import com.example.model.SubScreen
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.viewmodel.TrainPilotUiState
import com.repforge.app.ActiveWorkoutStore
import com.repforge.app.StudioWorkoutSessionPresentation
import com.repforge.app.ui.theme.TrainPilotFrames
import com.repforge.app.ui.theme.TrainPilotOutlinedButton
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
    fun word(hu: String, en: String, de: String, ro: String) = when (state.language) {
        AppLanguage.HU -> hu; AppLanguage.EN -> en; AppLanguage.DE -> de; AppLanguage.RO -> ro
    }
    val frame by ActiveWorkoutStore.snapshotFlow.collectAsState()
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
    val elapsedText = (elapsedSeconds?.let(StudioWorkoutSessionPresentation::durationLabel) ?: "00:00") +
        if (paused) " · " + word("Szünet", "Paused", "Pause", "Pauză") else ""

    val enabled = state.isWorkoutSessionActive && !state.watchControlPending && !state.isSavingWorkout &&
        (!state.phoneControlsWorkout || state.watchAppAvailable)
    val status = when {
        state.watchControlPending -> word("Átadás folyamatban", "Transfer in progress", "Übergabe läuft", "Transfer în curs")
        state.phoneControlsWorkout -> word("Vezérlés: telefon", "Control: phone", "Steuerung: Telefon", "Control: telefon")
        else -> word("Vezérlés: óra", "Control: watch", "Steuerung: Uhr", "Control: ceas")
    }
    Surface(color = palette.surfaceCard, shape = RoundedCornerShape(14.dp),
        border = TrainPilotFrames.stroke(0.75.dp, palette.cardBorder),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).testTag("workout_sticky_handover")) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (state.isWorkoutSessionActive) {
                val statusContent: @Composable () -> Unit = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(when { state.watchControlPending -> Icons.Default.Sync
                            state.phoneControlsWorkout -> Icons.Default.PhoneAndroid; else -> Icons.Default.Watch },
                            contentDescription = null, tint = palette.primary, modifier = Modifier.size(18.dp))
                        Column {
                            Text(status, style = MaterialTheme.typography.labelMedium, color = palette.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (!state.watchAppAvailable && state.phoneControlsWorkout && !state.watchControlPending)
                                Text(word("Óravezérlés nem elérhető", "Watch control unavailable", "Uhrsteuerung nicht verfügbar", "Control ceas indisponibil"),
                                    color = palette.textSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                val elapsedContent: @Composable () -> Unit = {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = palette.tintedSurface(0.18f),
                        border = TrainPilotFrames.stroke(1.dp, palette.primary.copy(alpha = 0.55f)),
                        modifier = Modifier.testTag("active_workout_elapsed")
                    ) {
                        Text(
                            text = elapsedText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = palette.primary,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                val action: @Composable () -> Unit = {
                    val actionLabel = if (state.watchControlPending) word("Folyamatban…", "In progress…", "Läuft…", "În curs…")
                        else if (state.phoneControlsWorkout) strings.handoverToWatchAction else strings.reclaimToPhoneAction
                    TrainPilotOutlinedButton(onClick = onToggle, enabled = enabled,
                        shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = palette.tintedSurface(0.12f),
                            contentColor = palette.primary, disabledContainerColor = palette.surfaceElevated,
                            disabledContentColor = palette.textSecondary.copy(alpha = 0.6f)),
                        modifier = Modifier.heightIn(min = 48.dp).testTag("workout_handover_action")) {
                        Icon(Icons.Default.Watch, contentDescription = null,
                            tint = palette.primary, modifier = Modifier.size(18.dp))
                        Text(actionLabel, style = MaterialTheme.typography.labelMedium,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                val fontScale = LocalDensity.current.fontScale
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val actionMaxWidth = maxWidth * 0.44f
                    if (maxWidth < 300.dp || fontScale > 1.3f) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.weight(1f)) { statusContent() }
                                elapsedContent()
                            }
                            action()
                        }
                    } else Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) { statusContent() }
                        elapsedContent()
                        Box(Modifier.widthIn(max = actionMaxWidth)) { action() }
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
