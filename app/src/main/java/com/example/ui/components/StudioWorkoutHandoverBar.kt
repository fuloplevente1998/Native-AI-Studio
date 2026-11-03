package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.localization.LocalizedStrings
import com.example.model.AppLanguage
import com.example.model.SubScreen
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.viewmodel.TrainPilotUiState
import com.repforge.app.ui.theme.TrainPilotFrames
import com.repforge.app.ui.theme.TrainPilotOutlinedButton

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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(when { state.watchControlPending -> Icons.Default.Sync
                            state.phoneControlsWorkout -> Icons.Default.PhoneAndroid; else -> Icons.Default.Watch },
                            contentDescription = null, tint = palette.primary, modifier = Modifier.size(20.dp))
                        Column {
                            Text(status, style = MaterialTheme.typography.labelMedium, color = palette.textPrimary)
                            if (!state.watchAppAvailable && state.phoneControlsWorkout && !state.watchControlPending)
                                Text(word("Óravezérlés nem elérhető", "Watch control unavailable", "Uhrsteuerung nicht verfügbar", "Control ceas indisponibil"),
                                    color = palette.textSecondary, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                val action: @Composable () -> Unit = {
                    TrainPilotOutlinedButton(onClick = onToggle, enabled = enabled,
                        shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = palette.tintedSurface(0.12f),
                            contentColor = palette.primary, disabledContainerColor = palette.surfaceElevated,
                            disabledContentColor = palette.textSecondary.copy(alpha = 0.6f)),
                        modifier = Modifier.heightIn(min = 48.dp).testTag("workout_handover_action")) {
                        Text(if (state.watchControlPending) word("Folyamatban…", "In progress…", "Läuft…", "În curs…")
                            else if (state.phoneControlsWorkout) strings.handoverToWatchAction else strings.reclaimToPhoneAction,
                            style = MaterialTheme.typography.labelMedium)
                    }
                }
                val fontScale = LocalDensity.current.fontScale
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    if (maxWidth < 300.dp || fontScale > 1.3f) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { statusContent(); action() }
                    } else Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.weight(1f)) { statusContent() }
                        Box(Modifier.widthIn(max = maxWidth * 0.52f)) { action() }
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
