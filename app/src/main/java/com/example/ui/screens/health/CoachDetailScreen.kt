package com.example.ui.screens

import com.example.ui.theme.liquidDropMaterial
import com.example.ui.theme.GlassMaterialRole

import com.example.ui.theme.glassCardFill
import com.example.ui.theme.trainPilotGlassMaterial
import com.repforge.app.ui.theme.trainPilotFrame
import com.example.ui.theme.trainPilotGlassRim
import com.example.ui.theme.glassTile

import com.example.ui.theme.trainPilotScreenBackdrop
import com.repforge.app.ui.theme.TrainPilotCardSpacing

import com.repforge.app.ui.theme.trainPilotClickable
import com.repforge.app.ui.theme.trainPilotBorder

import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
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
import com.example.ui.theme.LocalGlassTopInset
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
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(nativeState.variationSaved) { if (nativeState.variationSaved) onCoachProgramChanged() }
    val readiness = nativeState.readiness
    val hasScore = readiness?.score != null
    var coachInputsObserved by remember(model) { mutableStateOf(false) }
    LaunchedEffect(uiState.loggedWorkouts, uiState.programs, uiState.dailyHealthLastSyncIso,
        uiState.primaryHealthSourceSamsung, uiState.permissionSamsungHealthEnabled, uiState.todayRecoveryReading, uiState.language) {
        val firstAttachment = !coachInputsObserved
        coachInputsObserved = true
        // The model already starts its first read. Do not restart it on initial composition.
        if (firstAttachment && nativeState.initialLoading) return@LaunchedEffect
        model.reload()
    }

    val upperContent: @Composable () -> Unit = {
        if (uiState.currentSubScreen == SubScreen.COACH_DETAIL) {
            SubScreenTopBar(leadingIcon = com.example.ui.components.TrainPilotCoachIcon,
                title = "TrainPilot Coach", onBack = onBack, onOpenSettings = onOpenSettings)
        }
        StudioCoachReadinessCard(uiState, readiness)
        if (!nativeState.initialLoading && (!hasScore || nativeState.error || nativeState.refreshFailed || nativeState.refreshing)) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (nativeState.refreshFailed) Text(
                        tr(uiState.language, "Az egészségadatok frissítése sikertelen.", "Health data refresh failed.",
                            "Gesundheitsdaten konnten nicht aktualisiert werden.", "Actualizarea datelor de sănătate a eșuat."),
                        color = palette.danger, modifier = Modifier.testTag("studio_coach_refresh_failed")
                    )
                    com.repforge.app.ui.theme.TrainPilotOutlinedButton(
                        onClick = { model.refreshHealth(context as? android.app.Activity) },
                        enabled = !nativeState.loading && !nativeState.refreshing,
                        modifier = Modifier.fillMaxWidth().testTag("studio_coach_refresh_health")
                    ) {
                        Text(if (nativeState.refreshing)
                            tr(uiState.language, "Frissítés…", "Refreshing…", "Wird aktualisiert…", "Se actualizează…")
                        else tr(uiState.language, "Egészségadatok frissítése", "Refresh health data",
                            "Gesundheitsdaten aktualisieren", "Actualizează datele de sănătate"))
                    }
                }
        }
        com.example.ui.components.StudioCoachNextTrainingCard(nativeState, onStartCoachTarget, onOpenWorkout)
    }
    CoachPageLayout(
        modifier = Modifier.fillMaxSize().trainPilotScreenBackdrop(palette).testTag("coach_detail_screen"),
        upper = upperContent,
        recommendations = { pinned, modifier ->
            com.example.ui.components.StudioCoachRecommendations(nativeState,
                onStartCoachTarget, onOpenWorkout, onOpenExerciseDetail,
                model::previewProgramApplication, model::closeVariation, model::saveVariation,
                showTarget = false, programChangeAllowed = !uiState.isWorkoutSessionActive,
                modifier = modifier, pinnedList = pinned)
        }
    )
}

/** Normal portrait layouts reserve the remaining space for the independently scrolling advice.
 * Compact/large-text layouts use a single scroll so the upper cards and all actions stay reachable.
 * A bounded upper scroll is a safety valve for longer translated or Samsung-specific content.
 */
@Composable
internal fun CoachPageLayout(
    modifier: Modifier = Modifier,
    upper: @Composable () -> Unit,
    recommendations: @Composable (Boolean, Modifier) -> Unit
) {
    val inset = LocalGlassTopInset.current
    val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale
    BoxWithConstraints(modifier) {
        val usable = maxHeight - inset
        val pinned = usable >= 420.dp && maxWidth >= 320.dp && fontScale <= 1.3f
        if (!pinned) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().testTag("coach_fallback_scroll"),
                contentPadding = PaddingValues(start = TrainPilotCardSpacing.ScreenGutter,
                    end = TrainPilotCardSpacing.ScreenGutter, top = 16.dp + LocalGlassTopInset.current, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { upper() } }
                item { recommendations(false, Modifier.fillMaxWidth()) }
            }
        } else {
            Column(Modifier.fillMaxSize().padding(horizontal = TrainPilotCardSpacing.ScreenGutter)
                .padding(top = inset + 6.dp, bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Column(Modifier.fillMaxWidth().heightIn(max = (usable - 112.dp).coerceAtLeast(1.dp))
                    .verticalScroll(rememberScrollState()).testTag("studio_coach_fixed_upper"),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) { upper() }
                recommendations(true, Modifier.weight(1f).fillMaxWidth())
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StudioCoachReadinessCard(
    uiState: TrainPilotUiState,
    readiness: com.repforge.app.ui.coach.NativeCoachReadiness.Result?
) {
    val palette = LocalTrainPilotPalette.current
    val hasScore = readiness?.score != null
    val displayScore = readiness?.score?.toInt()?.coerceIn(0, 100) ?: 0
    val samsungScore = readiness?.samsungScore?.roundToInt()?.coerceIn(0, 100) ?: 0
    val ownReadinessScore = readiness?.trainpilotScore ?: 0
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = palette.glassCardFill(palette.surfaceCard),
        modifier = Modifier
            .fillMaxWidth().trainPilotGlassMaterial(palette, RoundedCornerShape(22.dp))
            .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(22.dp))
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(4.dp)
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

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth().testTag("studio_coach_final_readiness"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoachReadinessGauge(displayScore.takeIf { hasScore }, compact = androidx.compose.ui.platform.LocalDensity.current.fontScale <= 1.3f)
                Spacer(modifier = Modifier.width(10.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
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

            if (uiState.permissionSamsungHealthEnabled) Spacer(modifier = Modifier.height(8.dp))

            if (uiState.permissionSamsungHealthEnabled) Row(
                modifier = Modifier.fillMaxWidth().testTag("studio_coach_score_breakdown"),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (palette.isLiquidGlass) androidx.compose.ui.graphics.Color.Transparent else palette.surfaceElevated,
                    modifier = Modifier
                        .weight(1f)
                        .liquidDropMaterial(palette, RoundedCornerShape(14.dp), GlassMaterialRole.Tile, live = false)
                        .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
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
                    color = if (palette.isLiquidGlass) androidx.compose.ui.graphics.Color.Transparent else palette.surfaceElevated,
                    modifier = Modifier
                        .weight(1f)
                        .liquidDropMaterial(palette, RoundedCornerShape(14.dp), GlassMaterialRole.Tile, live = false)
                        .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
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
                    color = if (palette.isLiquidGlass) androidx.compose.ui.graphics.Color.Transparent else palette.surfaceElevated,
                    modifier = Modifier
                        .weight(1f)
                        .liquidDropMaterial(palette, RoundedCornerShape(14.dp), GlassMaterialRole.Tile, live = false)
                        .trainPilotBorder(1.dp, palette.primary.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(tr(uiState.language, "Végső Coach", "Final Coach", "Finaler Coach", "Coach final"), style = MaterialTheme.typography.labelSmall, color = palette.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = if (hasScore) "$displayScore / 100" else "—",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.primary
                        )
                        if (uiState.permissionSamsungHealthEnabled && hasScore && readiness?.samsungScore != null) {
                            Text("80% TrainPilot · 20% Samsung",
                                style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                        }
                    }
                }
            }
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
        color = if (palette.isLiquidGlass) androidx.compose.ui.graphics.Color.Transparent else palette.surfaceElevated,
        modifier = Modifier
            .fillMaxWidth()
            .liquidDropMaterial(palette, RoundedCornerShape(12.dp), GlassMaterialRole.Button)
            .trainPilotClickable(RoundedCornerShape(12.dp)) { onClick() }
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
