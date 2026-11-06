package com.example.ui.screens

import com.repforge.app.ui.theme.TrainPilotCardSpacing

import com.repforge.app.ui.theme.trainPilotBorder
import com.repforge.app.ui.theme.trainPilotSoftGlow

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
    fun tr(hu: String, en: String, de: String, ro: String) = when (state.language) {
        com.example.model.AppLanguage.HU -> hu
        com.example.model.AppLanguage.EN -> en
        com.example.model.AppLanguage.DE -> de
        com.example.model.AppLanguage.RO -> ro
    }

    val scrollState = rememberScrollState()

    com.example.ui.components.TrainPilotScrollableBox(
        scrollState = scrollState,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = TrainPilotCardSpacing.ScreenGutter)
                .padding(top = 8.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(TrainPilotCardSpacing.CardGap)
        ) {
        // A kompakt Profil és új terv belépő mindig elérhető a Kezdőlapon.
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = palette.surface,
            modifier = Modifier
                .fillMaxWidth()
                .trainPilotSoftGlow(palette.primary, RoundedCornerShape(14.dp), 3.dp)
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
                        color = palette.tintedSurface(0.16f),
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
                            text = "${strings.personalPlannerTitle} • ${com.example.localization.StudioProfileOptionLabels.label(state.trainingStyleHu, state.language)} • ${com.example.localization.StudioProfileOptionLabels.label(state.progressionRuleHu, state.language)}",
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

        // 3. HERO WORKOUT CARD (Mockup 1/4 #1: "Szüneteltetve • Teljes test A • 8 gyakorlat • kb. 60 perc • ▶ Folytatás")
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = palette.surface,
            modifier = Modifier
                .fillMaxWidth()
                .trainPilotSoftGlow(palette.primary, RoundedCornerShape(20.dp))
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
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Dumbbell Icon in warm dark gold container
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = palette.tintedSurface(0.14f),
                        modifier = Modifier
                            .size(44.dp)
                            .trainPilotBorder(
                                width = 1.dp,
                                color = palette.tintedSurface(0.3f),
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

                    Spacer(modifier = Modifier.width(10.dp))

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
                                    text = if (state.isWorkoutSessionActive) tr("Aktív edzés", "Active Workout", "Aktives Training", "Antrenament activ") else if (activeProgram != null) {
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
                            text = if (state.isWorkoutSessionActive) {
                                com.example.model.LoggedWorkoutEntry(id = "active", titleHu = state.activeWorkoutTitleHu)
                                    .localizedTitle(state.language, state.programs, state.exercises)
                            } else currentDay?.localizedTitle(state.language) ?: tr("Teljes test A", "Full Body A", "Ganzkörper A", "Full Body A"),
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary
                        )

                        Text(
                            text = if (state.isWorkoutSessionActive) {
                                tr(
                                    "${state.activeWorkoutExercises.size} gyakorlat • Aktív edzés",
                                    "${state.activeWorkoutExercises.size} exercises • Active workout",
                                    "${state.activeWorkoutExercises.size} Übungen • Aktives Training",
                                    "${state.activeWorkoutExercises.size} exerciții • Antrenament activ"
                                )
                            } else {
                                tr(
                                    "${currentDay?.exercises?.size ?: 8} gyakorlat • kb. ${currentDay?.durationMinutes ?: 60} perc",
                                    "${currentDay?.exercises?.size ?: 8} exercises • ~${currentDay?.durationMinutes ?: 60} min",
                                    "${currentDay?.exercises?.size ?: 8} Übungen • ca. ${currentDay?.durationMinutes ?: 60} Min.",
                                    "${currentDay?.exercises?.size ?: 8} exerciții • aprox. ${currentDay?.durationMinutes ?: 60} min"
                                )
                            },
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

        com.example.ui.components.HomeCoachAdviceCard(state, strings, onOpenCoachReadiness)

        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
            val sideBySide = maxWidth >= 300.dp && androidx.compose.ui.platform.LocalDensity.current.fontScale <= 1.3f
            val quick: @Composable (Modifier) -> Unit = { card ->
                HomeCompactShortcut(card.testTag("home_quick_workout_card"), Icons.AutoMirrored.Filled.DirectionsRun,
                    strings.quickWorkoutTitle, strings.quickWorkoutSub, onOpenQuickWorkout)
            }
            val watch: @Composable (Modifier) -> Unit = { card ->
                val status = when {
                    state.watchInfoLoading -> tr("Óraadatok lekérése…", "Fetching watch info…", "Uhrendaten werden geladen…", "Se preiau datele ceasului…")
                    state.watchAppAvailable && state.watchBatteryPercent in 0..100 -> strings.connectedWithBattery(state.watchBatteryPercent)
                    state.watchAppAvailable -> tr("TrainPilot óraapp elérhető", "TrainPilot watch app available", "TrainPilot-Uhren-App verfügbar", "Aplicația de ceas TrainPilot disponibilă")
                    state.watchConnected -> tr("Wear OS kapcsolat • óraapp nem igazolt", "Wear OS connected • watch app unverified", "Wear OS verbunden • Uhren-App unbestätigt", "Wear OS conectat • aplicație ceas neverificată")
                    else -> tr("Nincs órakapcsolat", "No watch connected", "Keine Uhrenverbindung", "Nicio conexiune cu ceasul")
                }
                HomeCompactShortcut(card.testTag("home_watch_status_card"), Icons.Default.Watch,
                    if (state.watchAppAvailable) state.watchName else strings.watchConnectedSub, status, onOpenWatchAndSync,
                    enabled = !state.watchInfoLoading, connected = state.watchAppAvailable)
            }
            if (sideBySide) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TrainPilotCardSpacing.CardRowGap)) {
                quick(Modifier.weight(1f)); watch(Modifier.weight(1f))
            } else Column(verticalArrangement = Arrangement.spacedBy(TrainPilotCardSpacing.CardGap)) {
                quick(Modifier.fillMaxWidth()); watch(Modifier.fillMaxWidth())
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
                                    else tr("Mai aktív kalória: nincs lekért adat", "Today's active calories: no data fetched", "Heutige aktive Kalorien: keine Daten geladen", "Calorii active azi: fără date preluate"),
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


            }
        }
    }
}
}

@Composable
private fun HomeCompactShortcut(modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String, subtitle: String, onClick: () -> Unit, enabled: Boolean = true, connected: Boolean? = null) {
    val p = LocalTrainPilotPalette.current
    val shape = RoundedCornerShape(16.dp)
    Surface(color = p.surface, shape = shape,
        modifier = modifier.trainPilotSoftGlow(p.primary, shape, 3.dp)
            .trainPilotBorder(1.dp, Brush.linearGradient(listOf(p.sectionBorder, p.cardBorder)), shape)
            .clickable(enabled = enabled, onClick = onClick)) {
        Column(Modifier.fillMaxWidth().heightIn(min = 88.dp).padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(icon, null, tint = p.primary, modifier = Modifier.size(20.dp))
                Text(title, color = p.textPrimary, style = MaterialTheme.typography.titleSmall,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (connected != null) Box(Modifier.size(7.dp).clip(CircleShape)
                    .background(if (connected) p.success else p.warning))
            }
            Text(subtitle, color = p.textSecondary, style = MaterialTheme.typography.labelSmall,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
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

