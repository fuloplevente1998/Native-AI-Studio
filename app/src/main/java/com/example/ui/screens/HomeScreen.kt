package com.example.ui.screens

import com.repforge.app.ui.theme.TrainPilotCardSpacing

import com.repforge.app.ui.theme.trainPilotBorder

import com.example.model.localWorkoutDate

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.LocalizedStrings
import com.example.model.HomeDemoMode
import com.example.ui.components.TrainPilotMountainLogo
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.viewmodel.TrainPilotUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    state: TrainPilotUiState,
    strings: LocalizedStrings,
    onOpenPalettePicker: () -> Unit,
    onOpenSettings: () -> Unit,
    onCycleDemoMode: (HomeDemoMode) -> Unit,
    onStartOrContinueWorkout: () -> Unit,
    onOpenProgramDayDetails: () -> Unit,
    onOpenQuickWorkout: () -> Unit,
    onOpenWatchAndSync: () -> Unit,
    onOpenHealthSleep: () -> Unit,
    onOpenHealthHeart: () -> Unit,
    onOpenHealthSteps: () -> Unit,
    onOpenCoachReadiness: () -> Unit,
    onOpenProgramsList: () -> Unit,
    onOpenCalendarPlanner: () -> Unit,
    onOpenGoalsDialog: () -> Unit,
    onOpenPersonalPlanner: () -> Unit,
    onDiscardWorkout: (String) -> Unit = {},
    onOpenHealthToday: () -> Unit = onOpenHealthSteps
) {
    val palette = LocalTrainPilotPalette.current
    val activeProgram = state.programs.firstOrNull { it.isActive } ?: state.programs.firstOrNull()
    val currentDay = activeProgram?.days?.firstOrNull()
    var showExtendedMainCards by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = TrainPilotCardSpacing.ScreenGutter)
            .padding(top = 8.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(TrainPilotCardSpacing.CardGap)
    ) {
        if (!state.profileComplete || state.programs.none { it.isActive && it.isCustom && it.days.isNotEmpty() }) {
        // Mini nézetben a Profil és új terv (Kezdőlapról a köszönés eltűnt, maradt mini nézetben a Profil és új terv)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = palette.surface,
            modifier = Modifier
                .fillMaxWidth()
                .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
                .clickable { onOpenPersonalPlanner() }
                .testTag("home_personal_planner_mini_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = palette.primary.copy(alpha = 0.16f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = strings.profileAndNewPlanBtn,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = palette.textPrimary
                        )
                        Text(
                            text = "${strings.personalPlannerTitle} • ${state.trainingStyleHu} • ${state.progressionRuleHu}",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Button(
                    onClick = onOpenPersonalPlanner,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primary,
                        contentColor = palette.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .heightIn(min = 38.dp)
                        .testTag("home_profile_new_plan_button")
                ) {
                    Text(
                        text = strings.profileAndNewPlanBtn,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        }

        // 3. HERO WORKOUT CARD (Mockup 1/4 #1: "Szüneteltetve • Teljes test A • 8 gyakorlat • kb. 60 perc • ▶ Folytatás")
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = palette.surface,
            modifier = Modifier
                .fillMaxWidth()
                .trainPilotBorder(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            palette.primary.copy(alpha = 0.55f),
                            palette.cardBorder
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .clickable { onOpenProgramDayDetails() }
                .testTag("home_hero_workout_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                palette.surfaceElevated.copy(alpha = 0.65f),
                                palette.surface
                            )
                        )
                    )
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Dumbbell Icon in warm dark gold container
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = palette.primary.copy(alpha = 0.14f),
                        modifier = Modifier
                            .size(56.dp)
                            .trainPilotBorder(
                                width = 1.dp,
                                color = palette.primary.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(14.dp)
                            )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (state.isWorkoutSessionActive && state.homeDemoMode == HomeDemoMode.PAUSED_WORKOUT) {
                                Icon(
                                    imageVector = Icons.Default.Pause,
                                    contentDescription = null,
                                    tint = palette.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = strings.pausedBadge,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = palette.primary
                                )
                            } else {
                                Text(
                                    text = if (state.isWorkoutSessionActive) "Aktív edzés" else if (activeProgram != null) {
                                        "${strings.activeProgramBadge} • ${activeProgram.localizedName(state.language)}"
                                    } else {
                                        strings.activeProgramBadge
                                    },
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = palette.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Text(
                            text = if (state.isWorkoutSessionActive) state.activeWorkoutTitleHu else currentDay?.localizedTitle(state.language) ?: "Teljes test A",
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.textPrimary
                        )

                        Text(
                            text = if (state.isWorkoutSessionActive) "${state.activeWorkoutExercises.size} gyakorlat • Aktív edzés"
                                else "${currentDay?.exercises?.size ?: 8} gyakorlat • kb. ${currentDay?.durationMinutes ?: 60} perc",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = strings.detailsAction,
                        tint = palette.textSecondary
                    )
                }

                // Full-width gold primary CTA Button ("▶ Folytatás" / "▶ Tervezett edzés indítása")
                if (state.isWorkoutSessionActive && state.activeWorkoutId.isNotBlank()) {
                    com.example.ui.components.StudioPausedWorkoutActions(state.activeWorkoutId, state.phoneControlsWorkout,
                        state.isSavingWorkout, strings.continueBtn, onStartOrContinueWorkout, onDiscardWorkout)
                } else                 Button(
                    onClick = onStartOrContinueWorkout,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primary,
                        contentColor = palette.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag("home_continue_workout_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (state.homeDemoMode == HomeDemoMode.PAUSED_WORKOUT) {
                            strings.continueBtn
                        } else {
                            strings.startPlannedBtn
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        // 4. QUICK WORKOUT CARD (Mockup 1/4 #1: "Gyors edzés • Indíts egy rövid edzést most >")
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = palette.surface,
            modifier = Modifier
                .fillMaxWidth()
                .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(18.dp))
                .clickable { onOpenQuickWorkout() }
                .testTag("home_quick_workout_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = palette.surfaceElevated,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.quickWorkoutTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary
                    )
                    Text(
                        text = strings.quickWorkoutSub,
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = palette.textSecondary
                )
            }
        }

        // 5. WEAR OS WATCH CARD (Mockup 1/4 #1: "Wear OS connection")
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = palette.surface,
            modifier = Modifier
                .fillMaxWidth()
                .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(18.dp))
                .clickable(enabled = !state.watchInfoLoading) { onOpenWatchAndSync() }
                .testTag("home_watch_status_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = palette.surfaceElevated,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Watch,
                            contentDescription = null,
                            tint = palette.textPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (state.watchAppAvailable) state.watchName else "Nincs csatlakoztatott Wear OS óra",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary
                    )
                    Text(
                        text = when {
                            state.watchAppAvailable -> {
                                if (state.watchInfoLoading) "Óraadatok lekérése…" else if (state.watchBatteryPercent in 0..100) strings.connectedWithBattery(state.watchBatteryPercent)
                                else "Csatlakoztatva • TrainPilot óraapp elérhető"
                            }
                            state.watchConnected -> "Wear OS rendszerkapcsolat • TrainPilot óraapp nincs telepítve"
                            else -> "Offline • Nincs párosított óraalkalmazás"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                }
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(if (state.watchAppAvailable) palette.success else palette.warning)
                )
            }
        }

        // Shared compact 3 × 2 today summary. Details open on their existing destination pages.
        StudioTodayStatusGrid(state, compact = true) { key ->
            when (key) {
                "sleep" -> onOpenHealthSleep()
                "pulse" -> onOpenHealthHeart()
                "steps" -> onOpenHealthSteps()
                "recovery" -> onOpenCoachReadiness()
                else -> onOpenHealthToday()
            }
        }

        // 8. MAIN BRANCH HOME CARDS (Célok beállítása, Mai javaslat / Coach, Személyes edzéstervező, Aktív kalória)
        AnimatedVisibility(visible = showExtendedMainCards) {
            Column(verticalArrangement = Arrangement.spacedBy(TrainPilotCardSpacing.CardGap)) {
                // Célok beállítása (1/15 edzés >) from main Home screenshot
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
                        .clickable { onOpenGoalsDialog() }
                        .testTag("home_goals_row_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.goalsSetupTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary
                            )
                            Text(
                                text = if ("activeCalories" in state.dailyHealthAvailableFields)
                                    strings.activeCaloriesAndWorkoutsToday(state.activeCaloriesKcal,
                                        state.loggedWorkouts.count { it.localWorkoutDate() == java.time.LocalDate.now() })
                                    else "Mai aktív kalória: nincs lekért adat",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${state.weeklyGoalCompleted}/${state.weeklyGoalTarget}",
                                style = MaterialTheme.typography.labelLarge,
                                color = palette.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = palette.primary
                            )
                        }
                    }
                }

                Surface(shape = RoundedCornerShape(18.dp), color = palette.surface,
                    modifier = Modifier.fillMaxWidth()
                        .trainPilotBorder(1.dp, palette.primary.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                        .clickable { onOpenCoachReadiness() }.testTag("home_coach_advice_card")) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.AutoAwesome, null, tint = palette.primary, modifier = Modifier.size(20.dp))
                        Column(Modifier.weight(1f)) {
                            Text(strings.coachSuggestionHeader, color = palette.primary, style = MaterialTheme.typography.labelMedium)
                            Text(if (state.coachReadinessScore > 0) "Mai készenlét: ${state.coachReadinessScore}/100" else "Nézd meg a mai javaslatot",
                                color = palette.textPrimary, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = palette.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeVitalCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    mainValue: String,
    statusText: String,
    statusColor: Color,
    progressFraction: Float? = null,
    progressColor: Color = iconTint,
    footerCaption: String? = null,
    onClick: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = palette.surface,
        modifier = modifier
            .heightIn(min = 48.dp)
            .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(18.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = mainValue,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = palette.textPrimary
            )

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = statusColor
            )

            if (progressFraction != null) {
                Spacer(modifier = Modifier.height(2.dp))
                LinearProgressIndicator(
                    progress = { progressFraction },
                    color = progressColor,
                    trackColor = palette.surfaceElevated,
                    strokeCap = StrokeCap.Round,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }

            if (footerCaption != null) {
                Text(
                    text = footerCaption,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun HomeQuickShortcutTile(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = palette.surface,
        modifier = modifier
            .heightIn(min = 48.dp)
            .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = palette.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = palette.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatThousand(value: Int): String {
    val s = value.toString()
    return if (s.length > 3) {
        "${s.dropLast(3)} ${s.takeLast(3)}"
    } else s
}

private data class Quintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)

