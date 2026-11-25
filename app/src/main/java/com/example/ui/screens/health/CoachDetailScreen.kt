package com.example.ui.screens
import com.example.ui.theme.glassTileBackdrop
import com.repforge.app.ui.theme.trainPilotFrame
import com.example.ui.theme.trainPilotGlassRim
import com.example.ui.theme.glassTile

import com.example.ui.theme.trainPilotScreenBackdrop
import com.repforge.app.ui.theme.TrainPilotCardSpacing

import com.repforge.app.ui.theme.trainPilotClickable
import com.repforge.app.ui.theme.trainPilotBorder

import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.localization.LocalizedStrings
import com.example.model.SubScreen
import com.example.ui.components.SubScreenTopBar
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.viewmodel.TrainPilotUiState

/**
 * 6. COACH (Mockup 3/4 #6 + main TrainPilot Coach)
 */
@Composable
internal fun CoachDetailScreen(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onStartSuggestedWorkout: () -> Unit,
    onOpenExerciseDetail: (String) -> Unit,
    onStartCoachTarget: (String, String, String?) -> Unit = { _, _, _ -> onStartSuggestedWorkout() },
    onOpenWorkout: () -> Unit = {},
    onCoachProgramChanged: () -> Unit = {},
    model: com.repforge.app.ui.coach.CoachViewModel = androidx.lifecycle.viewmodel.compose.viewModel(key = "studio-canonical-coach")
) {
    val palette = LocalTrainPilotPalette.current
    val nativeState by model.state.collectAsStateWithLifecycle()
    LaunchedEffect(nativeState.variationSaved) { if (nativeState.variationSaved) onCoachProgramChanged() }
    val readiness = nativeState.readiness
    val hasScore = readiness?.score != null
    val displayScore = readiness?.score?.toInt()?.coerceIn(0, 100) ?: 0
    val samsungScore = readiness?.samsungScore?.roundToInt()?.coerceIn(0, 100) ?: 0
    val ownReadinessScore = readiness?.trainpilotScore ?: 0
    var coachInputsObserved by remember(model) { mutableStateOf(false) }
    LaunchedEffect(uiState.loggedWorkouts, uiState.programs, uiState.dailyHealthLastSyncIso,
        uiState.primaryHealthSourceSamsung, uiState.todayRecoveryReading, uiState.language) {
        val firstAttachment = !coachInputsObserved
        coachInputsObserved = true
        // The model already starts its first read. Do not restart it on initial composition.
        if (firstAttachment && nativeState.initialLoading) return@LaunchedEffect
        model.reload()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .trainPilotScreenBackdrop(palette)
            .testTag("coach_detail_screen"),
        contentPadding = PaddingValues(horizontal = TrainPilotCardSpacing.ScreenGutter, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(TrainPilotCardSpacing.CardGap)
    ) {
        if (uiState.currentSubScreen == SubScreen.COACH_DETAIL) {
            item {
                SubScreenTopBar(
                    leadingIcon = com.example.ui.components.TrainPilotCoachIcon,
                    title = "TrainPilot Coach",
                    onBack = onBack,
                    onOpenSettings = onOpenSettings
                )
            }
        }

        // Screenshot 4: Mai felkészültség (Arc gauge + 3 distinct D10 readiness indicators)
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = palette.glassTile(palette.surfaceCard),
                modifier = Modifier
                    .fillMaxWidth().trainPilotGlassRim(palette, RoundedCornerShape(22.dp))
                    .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(22.dp)).glassTileBackdrop(palette, RoundedCornerShape(22.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tr(uiState.language, "Mai felkészültség", "Today's Readiness", "Heutige Bereitschaft", "Pregătirea de azi"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        val statusBadgeText = when {
                            displayScore >= 75 -> tr(uiState.language, "OPTIMÁLIS KÉSZENLÉT", "OPTIMAL READINESS", "OPTIMALE BEREITSCHAFT", "PREGĂTIRE OPTIMĂ")
                            displayScore >= 55 -> tr(uiState.language, "MÉRSÉKELT KÉSZENLÉT", "MODERATE READINESS", "MÄSSIGE BEREITSCHAFT", "PREGĂTIRE MODERATĂ")
                            hasScore -> tr(uiState.language, "KÖNNYÍTETT TERHELÉS", "LIGHTER LOAD", "LEICHTERE BELASTUNG", "SARCINĂ MAI UȘOARĂ")
                            else -> tr(uiState.language, "NINCS MÉRT ADAT", "NO MEASURED DATA", "KEINE MESSDATEN", "FĂRĂ DATE MĂSURATE")
                        }
                        val statusBadgeColor = when {
                            displayScore >= 75 -> palette.success
                            displayScore >= 55 -> palette.primary
                            hasScore -> palette.warning
                            else -> palette.textSecondary
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = statusBadgeColor.copy(alpha = 0.16f),
                            modifier = Modifier.trainPilotFrame(1.dp, statusBadgeColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                text = statusBadgeText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = statusBadgeColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CoachReadinessGauge(displayScore.takeIf { hasScore })
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = tr(uiState.language, "Végső Coach készenlét", "Final Coach readiness", "Finale Coach-Bereitschaft", "Pregătirea finală Coach"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = palette.primary
                            )
                            Text(
                                text = com.example.ui.components.studioCoachReadinessText(readiness, uiState.language),
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = palette.glassTile(palette.surfaceElevated, elevated = true),
                            modifier = Modifier
                                .weight(1f)
                                .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(14.dp)).glassTileBackdrop(palette, RoundedCornerShape(14.dp))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Samsung Energy", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    text = if (readiness?.samsungScore != null) "$samsungScore / 100" else "—",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = if (readiness?.samsungScore != null) "Samsung Health" else tr(uiState.language, "Nincs adat", "No data", "Keine Daten", "Fără date"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (readiness?.samsungScore != null) palette.success else palette.textSecondary
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = palette.glassTile(palette.surfaceElevated, elevated = true),
                            modifier = Modifier
                                .weight(1f)
                                .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(14.dp)).glassTileBackdrop(palette, RoundedCornerShape(14.dp))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("TrainPilot", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    text = if (readiness?.trainpilotScore != null) "$ownReadinessScore / 100" else "—",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary
                                )
                                Text(tr(uiState.language, "Saját becslés", "Own estimate", "Eigene Schätzung", "Estimare proprie"),
                                    style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = palette.glassTile(palette.surfaceElevated, elevated = true),
                            modifier = Modifier
                                .weight(1f)
                                .trainPilotBorder(1.dp, palette.primary.copy(alpha = 0.55f), RoundedCornerShape(14.dp)).glassTileBackdrop(palette, RoundedCornerShape(14.dp))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(tr(uiState.language, "Végső Coach", "Final Coach", "Finaler Coach", "Coach final"), style = MaterialTheme.typography.labelSmall, color = palette.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    text = if (hasScore) "$displayScore / 100" else "—",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.primary
                                )
                                if (hasScore && readiness?.samsungScore != null) {
                                    Text("80% TrainPilot · 20% Samsung",
                                        style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Put the next actionable day immediately after readiness.
        item {
            com.example.ui.components.StudioCoachNextTrainingCard(
                nativeState, onStartCoachTarget, onOpenWorkout
            )
        }

        item {
            com.example.ui.components.StudioCoachRecommendations(nativeState,
                onStartCoachTarget, onOpenWorkout, onOpenExerciseDetail,
                model::previewProgramApplication, model::closeVariation, model::saveVariation,
                showTarget = false, programChangeAllowed = !uiState.isWorkoutSessionActive)
        }
    }
}

@Composable
internal fun CoachExerciseAdviceRow(
    name: String,
    advice: String,
    dotColor: Color,
    onClick: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = palette.glassTile(palette.surfaceElevated, elevated = true),
        modifier = Modifier
            .fillMaxWidth()
            .trainPilotClickable(RoundedCornerShape(12.dp)) { onClick() }.glassTileBackdrop(palette, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(name, fontWeight = FontWeight.Bold, color = palette.textPrimary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dotColor))
                Spacer(modifier = Modifier.width(6.dp))
                Text(advice, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = palette.textSecondary)
            }
        }
    }
}
