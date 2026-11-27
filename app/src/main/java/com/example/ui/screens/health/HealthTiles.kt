package com.example.ui.screens

import com.example.ui.theme.liquidDropMaterial

import com.example.ui.theme.GlassMaterialRole
import com.example.ui.theme.trainPilotGlassRim
import com.example.ui.theme.glassTile

import com.repforge.app.ui.theme.trainPilotSoftGlow

import com.repforge.app.ui.theme.trainPilotClickable
import com.repforge.app.ui.theme.trainPilotBorder

import kotlin.math.roundToInt
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.viewmodel.TrainPilotUiState

@Composable
internal fun MainTodayStatusTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    titleTag: String = "today_tile_title"
) {
    val palette = LocalTrainPilotPalette.current
    val shape = RoundedCornerShape(if (palette.isLiquidGlass) 20.dp else 16.dp)
    Surface(
        shape = shape,
        color = palette.glassTile(palette.background.copy(alpha = 0.55f)),
        modifier = modifier
            .trainPilotGlassRim(palette, shape, GlassMaterialRole.Tile)
            .trainPilotBorder(1.dp, palette.cardBorder, shape)
            .trainPilotClickable(shape, onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = if (compact) 5.dp else 6.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = palette.primary,
                    modifier = Modifier.size(if (compact) 14.dp else 16.dp)
                )
                // 2788: whole word, never "Aktív ka…": the font steps down, wraps only as a last resort.
                com.example.ui.components.TrainPilotFitText(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                    minSize = 8.sp,
                    modifier = Modifier.weight(1f).testTag(titleTag)
                )
            }
            Text(
                text = value,
                style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary,
                maxLines = 1
            )
        }
    }
}

@Composable
internal fun PulseInnerBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = palette.glassTile(palette.background.copy(alpha = 0.45f)),
        modifier = modifier
            .trainPilotGlassRim(palette, RoundedCornerShape(14.dp), GlassMaterialRole.Tile)
            .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary,
                maxLines = 1
            )
        }
    }
}

@Composable
internal fun HealthSectionLauncher(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String?,
    onOpen: () -> Unit,
    customLeadingBadge: (@Composable () -> Unit)? = null
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = palette.glassTile(palette.surfaceCard),
        modifier = Modifier
            .fillMaxWidth()
            .trainPilotSoftGlow(palette.primary, RoundedCornerShape(20.dp), 3.dp)
            .trainPilotGlassRim(palette, RoundedCornerShape(20.dp))
            .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(20.dp))
            .trainPilotClickable(RoundedCornerShape(20.dp), onClick = onOpen)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (customLeadingBadge != null) {
                    customLeadingBadge()
                } else {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = palette.primary.copy(alpha = 0.14f),
                        modifier = Modifier
                            .size(34.dp)
                            .trainPilotBorder(1.dp, palette.primary.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ExpandLess,
                    contentDescription = null,
                    tint = palette.primary
                )
            }
        }
    }
}

@Composable
internal fun SummarySmallPill(label: String, value: String, modifier: Modifier = Modifier) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (palette.isLiquidGlass) androidx.compose.ui.graphics.Color.Transparent else palette.surfaceElevated,
        modifier = modifier.liquidDropMaterial(palette, RoundedCornerShape(12.dp), GlassMaterialRole.Tile, live = false)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = palette.textSecondary, maxLines = 1)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = palette.textPrimary)
        }
    }
}

@Composable
internal fun CoachReadinessGauge(score: Int?, compact: Boolean = false) {
    val palette = LocalTrainPilotPalette.current
    Box(
        modifier = Modifier.size(if (compact) 76.dp else 110.dp),
        contentAlignment = Alignment.Center
    ) {
        val trackCol = palette.surfaceElevated
        val ringCol = if (score != null) palette.success else palette.cardBorder
        Canvas(modifier = Modifier.size(if (compact) 68.dp else 98.dp)) {
            drawArc(
                color = trackCol,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = (if (compact) 7.dp else 10.dp).toPx(), cap = StrokeCap.Round)
            )
            if (score != null) {
                drawArc(
                    color = ringCol,
                    startAngle = 135f,
                    sweepAngle = 270f * (score / 100f),
                    useCenter = false,
                    style = Stroke(width = (if (compact) 7.dp else 10.dp).toPx(), cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = score?.toString() ?: "—",
                style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
            Text(
                text = "/ 100",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary
            )
        }
    }
}

@Composable
internal fun CompactRecoveryReadiness(state: TrainPilotUiState) {
    val reading = state.todayRecoveryReading
    val lang = state.language
    val palette = LocalTrainPilotPalette.current
    val score = reading?.coachScore?.roundToInt()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CoachReadinessGauge(score, compact = true)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(tr(lang, "Végső Coach készenlét", "Final Coach readiness", "Finale Coach-Bereitschaft", "Pregătirea finală Coach"),
                style = MaterialTheme.typography.titleSmall, color = palette.primary)
            val detail = when {
                score == null -> tr(lang, "Nincs mai pontszám", "No score today", "Heute kein Wert", "Niciun scor azi")
                score >= 75 -> tr(lang, "Optimális készenlét", "Optimal readiness", "Optimale Bereitschaft", "Pregătire optimă")
                score >= 55 -> tr(lang, "Mérsékelt készenlét", "Moderate readiness", "Mäßige Bereitschaft", "Pregătire moderată")
                else -> tr(lang, "Könnyített terhelés", "Lighter load", "Leichtere Belastung", "Sarcină mai ușoară")
            }
            Text(detail, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
            if (score != null && reading?.coachHasRecoverySignal == false) {
                Text(tr(lang, "Terhelésalapú becslés", "Load-based estimate", "Belastungsbasierte Schätzung", "Estimare bazată pe sarcină"),
                    style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
            }
        }
    }
    if (state.permissionSamsungHealthEnabled) Row(Modifier.fillMaxWidth().testTag("recovery_score_breakdown"), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SummarySmallPill("Samsung Energy", reading?.samsungEnergy?.let { "${it.roundToInt()} / 100" } ?: "—", Modifier.weight(1f))
        SummarySmallPill("TrainPilot", reading?.trainPilotEstimate?.let { "$it / 100" } ?: "—", Modifier.weight(1f))
    }
}
