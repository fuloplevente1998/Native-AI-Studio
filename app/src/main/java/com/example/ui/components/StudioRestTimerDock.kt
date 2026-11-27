package com.example.ui.components

import com.example.ui.theme.glassTintedTile
import com.example.ui.theme.glassTile
import com.example.ui.theme.trainPilotGlassRim
import com.example.ui.theme.GlassMaterialRole

import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AppLanguage
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.theme.readablePrimaryLabel
import com.example.ui.viewmodel.TrainPilotUiState
import com.repforge.app.ui.theme.TrainPilotButton as Button
import com.repforge.app.ui.theme.TrainPilotOutlinedButton
import com.repforge.app.ui.theme.trainPilotBorder
import com.repforge.app.ui.theme.trainPilotClickable

/**
 * 2778: one rest-timer dock for every measurement screen (strength, bilateral, timed, optional outdoor):
 * compact strip <-> large card with ring, -15 / +15 / skip. Same state, same callbacks everywhere.
 */
@Composable
internal fun StudioRestTimerDock(
    uiState: TrainPilotUiState,
    large: Boolean,
    onLarge: (Boolean) -> Unit,
    onAdjust: (Int) -> Unit,
    onToggle: () -> Unit,
    onSkip: () -> Unit,
    skipLabel: String,
    /** false: an untouched, not running rest (the default before the first set) stays hidden. */
    showIdle: Boolean = true,
) {
    val palette = LocalTrainPilotPalette.current
    val lang = uiState.language
    fun tr(l: AppLanguage, hu: String, en: String, de: String, ro: String) = when (l) {
        AppLanguage.HU -> hu; AppLanguage.EN -> en; AppLanguage.DE -> de; AppLanguage.RO -> ro
    }
    val secWord = tr(lang, "mp", "s", "Sek.", "sec")
    val formattedRest = String.format("%02d:%02d", uiState.restTimerRemainingSec / 60, uiState.restTimerRemainingSec % 60)
    val idle = !uiState.restTimerRunning && uiState.restTimerRemainingSec == uiState.defaultRestSec
    if (com.repforge.app.StudioRestTimer.visible(uiState) && (showIdle || !idle)) {
    val progress = (uiState.restTimerRemainingSec.toFloat() / uiState.defaultRestSec.coerceAtLeast(1)).coerceIn(0f, 1f)
    // 2779: compact <-> large is a quick fade + size change instead of a jump.
    androidx.compose.animation.AnimatedContent(
        targetState = large,
        transitionSpec = {
            (androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(160, delayMillis = 60)) togetherWith
                androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(90)))
                .using(androidx.compose.animation.SizeTransform(clip = true) { _, _ -> androidx.compose.animation.core.tween(220) })
        },
        contentAlignment = Alignment.BottomCenter,
        label = "rest_dock_size",
        modifier = Modifier.fillMaxWidth().testTag("rest_dock_animated")
    ) { isLarge ->
    if (!isLarge) {
        // COMPACT FLAT STICKY REST TIMER BAR
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = palette.glassTintedTile(palette.surfaceCard),
            modifier = Modifier
                .fillMaxWidth()
                .trainPilotGlassRim(palette, RoundedCornerShape(14.dp), GlassMaterialRole.Performance)
                .trainPilotBorder(
                    width = 1.dp,
                    color = if (uiState.restTimerRunning) palette.primary else palette.cardBorder,
                    shape = RoundedCornerShape(14.dp)
                )
                .trainPilotClickable(RoundedCornerShape(14.dp)) { onLarge(true) }
                .testTag("sticky_rest_timer_compact")
        ) {
            com.example.ui.components.StudioCompactRestControls(
                language = lang, remainingSec = uiState.restTimerRemainingSec,
                running = uiState.restTimerRunning,
                enabled = uiState.phoneControlsWorkout && !uiState.watchControlPending,
                onAdjust = onAdjust, onToggle = onToggle,
                onSkip = onSkip, onExpand = { onLarge(true) },
                modifier = Modifier.padding(start = 8.dp, end = 0.dp, top = 2.dp, bottom = 2.dp))
        }
    } else {
        // LARGE EXPANDED STICKY REST TIMER CARD (shown when scrolled down or expanded)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = palette.glassTintedTile(palette.surfaceCard),
            modifier = Modifier
                .fillMaxWidth()
                .trainPilotGlassRim(palette, RoundedCornerShape(16.dp), GlassMaterialRole.Performance)
                .trainPilotBorder(0.5.dp, palette.primary.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                .testTag("sticky_rest_timer_large")
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = tr(lang, "Pihenőidő", "Rest timer", "Pausenzeit", "Timp de pauză"),
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.primary,
                                fontWeight = FontWeight.Bold
                            )
                            // #347: only the arrow (pointing down = collapse); no "compact mode" caption.
                            val collapse = tr(lang, "Pihenő összecsukása", "Collapse rest timer", "Pause zuklappen", "Restrânge pauza")
                            Box(Modifier.size(48.dp).trainPilotClickable(RoundedCornerShape(8.dp)) { onLarge(false) }
                                .semantics { contentDescription = collapse }.testTag("rest_dock_collapse"),
                                contentAlignment = Alignment.Center) {
                                Surface(shape = RoundedCornerShape(8.dp), color = palette.glassTile(palette.surfaceElevated, elevated = true)) {
                                    Icon(Icons.Default.ExpandMore, null, tint = palette.textSecondary,
                                        modifier = Modifier.padding(4.dp).size(18.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formattedRest,
                            style = MaterialTheme.typography.headlineLarge.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = if (uiState.restTimerRunning) {
                                tr(lang, "Visszaszámlálás fut • Rezgés a végén", "Countdown running • Vibrates at end", "Countdown läuft • Vibration am Ende", "Numărătoare inversă • Vibrație la final")
                            } else {
                                tr(lang, "Koppints a körre vagy az Indításra", "Tap the ring or Start", "Tippe auf den Kreis oder Start", "Atinge inelul sau Start")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .trainPilotClickable(CircleShape, enabled = uiState.phoneControlsWorkout && !uiState.watchControlPending) { onToggle() },
                        contentAlignment = Alignment.Center
                    ) {
                        val ringColor = palette.primary
                        val trackColor = palette.surfaceElevated
                        Canvas(modifier = Modifier.size(48.6.dp)) {
                            drawArc(
                                color = trackColor,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = ringColor,
                                startAngle = -90f,
                                sweepAngle = 360f * progress,
                                useCenter = false,
                                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Icon(
                            imageVector = if (uiState.restTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val enabled = uiState.phoneControlsWorkout && !uiState.watchControlPending
                    val shape = RoundedCornerShape(12.dp)
                    @Composable fun restAction(label: String, weight: Float, primary: Boolean, onClick: () -> Unit) {
                        Box(Modifier.weight(weight).height(48.dp)
                            .trainPilotClickable(shape, enabled = enabled, onClick = onClick),
                            contentAlignment = Alignment.Center) {
                            Surface(shape = shape, color = if (primary) palette.primary else palette.surfaceElevated,
                                modifier = Modifier.fillMaxWidth().height(36.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    TrainPilotFitText(label, style = MaterialTheme.typography.labelLarge,
                                        color = if (primary) palette.readablePrimaryLabel() else palette.textPrimary,
                                        fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    restAction("-15 $secWord", 1f, false) { onAdjust(-15) }
                    restAction("+15 $secWord", 1f, false) { onAdjust(15) }
                    restAction(skipLabel, 1.2f, true, onSkip)
                }
            }
        }
    }

    }
    }
}
