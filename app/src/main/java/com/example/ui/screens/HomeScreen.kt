package com.example.ui.screens

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
    onOpenPersonalPlanner: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val activeProgram = state.programs.firstOrNull { it.isActive } ?: state.programs.firstOrNull()
    val currentDay = activeProgram?.days?.firstOrNull()
    var showExtendedMainCards by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Mini nézetben a Profil és új terv (Kezdőlapról a köszönés eltűnt, maradt mini nézetben a Profil és új terv)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = palette.surface,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
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

        // 3. HERO WORKOUT CARD (Mockup 1/4 #1: "Szüneteltetve • Teljes test A • 8 gyakorlat • kb. 60 perc • ▶ Folytatás")
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = palette.surface,
            modifier = Modifier
                .fillMaxWidth()
                .border(
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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                            .border(
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
                            if (state.homeDemoMode == HomeDemoMode.PAUSED_WORKOUT) {
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
                                    text = if (activeProgram != null) {
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
                            text = currentDay?.localizedTitle(state.language) ?: "Teljes test A",
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 21.sp),
                            color = palette.textPrimary
                        )

                        Text(
                            text = "${currentDay?.exercises?.size ?: 8} gyakorlat • kb. ${currentDay?.durationMinutes ?: 60} perc",
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
                Button(
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
                .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
                .clickable { onOpenQuickWorkout() }
                .testTag("home_quick_workout_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
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

        // 5. WEAR OS WATCH CARD (Mockup 1/4 #1: "Galaxy Watch7 • Csatlakoztatva • 92% • ●")
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = palette.surface,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
                .clickable { onOpenWatchAndSync() }
                .testTag("home_watch_status_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
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
                        text = state.watchName,
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary
                    )
                    Text(
                        text = if (state.watchConnected) {
                            strings.connectedWithBattery(state.watchBatteryPercent)
                        } else {
                            "Offline"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                }
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(if (state.watchConnected) palette.success else palette.warning)
                )
            }
        }

        // 6. 2x2 VITALS & READINESS BENTO GRID (Mockup 1/4 #1: Alvás, Nyugalmi pulzus, Lépések, Samsung energia) + Expandable Dropdown Card
        var expandedHomeVitalKey by remember { mutableStateOf<String?>(null) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top-Left: Alvás (7 ó 24 p • Jó minőség)
            HomeVitalCard(
                modifier = Modifier
                    .weight(1f)
                    .testTag("home_vital_sleep_card"),
                icon = Icons.Default.Bedtime,
                iconTint = palette.sleepAccent,
                title = strings.sleepTitle,
                mainValue = state.sleepDurationLabel,
                statusText = strings.sleepQualityGood,
                statusColor = palette.success,
                onClick = {
                    expandedHomeVitalKey = if (expandedHomeVitalKey == "sleep") null else "sleep"
                }
            )

            // Top-Right: Nyugalmi pulzus (62 bpm • Normális)
            HomeVitalCard(
                modifier = Modifier
                    .weight(1f)
                    .testTag("home_vital_hr_card"),
                icon = Icons.Default.Favorite,
                iconTint = palette.heartAccent,
                title = strings.restingHrTitle,
                mainValue = "${state.restingHrBpm} bpm",
                statusText = strings.restingHrNormal,
                statusColor = palette.success,
                onClick = {
                    expandedHomeVitalKey = if (expandedHomeVitalKey == "hr") null else "hr"
                }
            )
        }

        val stepsProgress = (state.dailySteps.toFloat() / state.stepsGoal.toFloat()).coerceIn(0f, 1f)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Bottom-Left: Lépések (8 421 • Cél: 10 000 + cyan progress bar)
            HomeVitalCard(
                modifier = Modifier
                    .weight(1f)
                    .testTag("home_vital_steps_card"),
                icon = Icons.AutoMirrored.Filled.DirectionsRun,
                iconTint = palette.stepsAccent,
                title = strings.stepsTitle,
                mainValue = formatThousand(state.dailySteps),
                statusText = strings.stepsGoalWithCount(state.stepsGoal),
                statusColor = palette.textSecondary,
                progressFraction = stepsProgress,
                progressColor = palette.stepsAccent,
                onClick = {
                    expandedHomeVitalKey = if (expandedHomeVitalKey == "steps") null else "steps"
                }
            )

            // Bottom-Right: Samsung energia (87 / 100 • Jó állapot + green progress bar + Forrás: Samsung Health)
            val energyProgress = (state.samsungEnergyScore / 100f).coerceIn(0f, 1f)
            HomeVitalCard(
                modifier = Modifier
                    .weight(1f)
                    .testTag("home_vital_energy_card"),
                icon = Icons.Default.Eco,
                iconTint = palette.energyAccent,
                title = strings.samsungEnergyTitle,
                mainValue = "${state.samsungEnergyScore} / 100",
                statusText = strings.energyGoodState,
                statusColor = if (state.samsungEnergyScore >= 75) palette.success else palette.warning,
                progressFraction = energyProgress,
                progressColor = palette.energyAccent,
                footerCaption = strings.sourceSamsungHealth,
                onClick = {
                    expandedHomeVitalKey = if (expandedHomeVitalKey == "energy") null else "energy"
                }
            )
        }

        // Expandable Dropdown Card for the 4 Home Vitals (matches the 4 expandable cards under Összes volumen on Fejlődés)
        AnimatedVisibility(visible = expandedHomeVitalKey != null) {
            val activeKey = expandedHomeVitalKey ?: "sleep"
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, palette.primary.copy(alpha = 0.65f), RoundedCornerShape(18.dp))
                    .testTag("home_vital_expanded_dropdown_card")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val (headerTitle, accentColor, summaryRows, targetActionLabel, onJumpTarget) = when (activeKey) {
                        "hr" -> Quintuple(
                            "Nyugalmi & napi pulzus • ${state.restingHrBpm} bpm",
                            palette.heartAccent,
                            listOf(
                                "Mai nyugalmi pulzus" to "${state.restingHrBpm} bpm (Normális)",
                                "Aktuális / utolsó mérés" to "${if (state.currentHrBpm > 0) state.currentHrBpm else state.restingHrBpm + 8} bpm",
                                "7 napos tartomány" to "58 – 146 bpm (Galaxy Watch7)"
                            ),
                            "Ugrás a 24 órás Pulzustrendhez >",
                            onOpenHealthHeart
                        )
                        "steps" -> Quintuple(
                            "Napi & heti lépésszám • ${formatThousand(state.dailySteps)} lépés",
                            palette.stepsAccent,
                            listOf(
                                "Mai teljesítés" to "${formatThousand(state.dailySteps)} / ${formatThousand(state.stepsGoal)} (${(stepsProgress * 100).toInt()}%)",
                                "Becsült távolság & kalória" to "${"%.1f".format(state.dailySteps * 0.00075f)} km • ${(state.dailySteps * 0.04f).toInt()} kcal",
                                "Adatforrás" to "Health Connect + Galaxy Watch7"
                            ),
                            "Ugrás az Egészségnapló lépéseihez >",
                            onOpenHealthSteps
                        )
                        "energy" -> Quintuple(
                            "Samsung Energia & Készenlét • ${state.samsungEnergyScore}/100",
                            palette.energyAccent,
                            listOf(
                                "Energia pontszám" to "${state.samsungEnergyScore} / 100 (Jó állapot)",
                                "TrainPilot Coach készenlét" to "${state.coachReadinessScore} / 100",
                                "Javasolt mai terhelés" to if (state.samsungEnergyScore >= 70) "Teljes terhelés • 1–2 RIR" else "Könnyített regeneráló nap"
                            ),
                            "Ugrás a TrainPilot Coach elemzéshez >",
                            onOpenCoachReadiness
                        )
                        else -> Quintuple(
                            "Alvásminőség & Regeneráció • ${state.sleepDurationLabel}",
                            palette.sleepAccent,
                            listOf(
                                "Mai alvásidő" to "${state.sleepDurationLabel} (${state.sleepHoursDecimal})",
                                "Mélyalvás & REM arány" to "Mély: 1 ó 45 p • REM: 1 ó 50 p",
                                "Éjszakai regeneráció" to "Jó minőség • Nyugalmi HR: ${state.restingHrBpm} bpm"
                            ),
                            "Ugrás az Egészség alvásnaplóhoz >",
                            onOpenHealthSleep
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = headerTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = accentColor
                        )
                        Text(
                            text = "Bezárás ▲",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = palette.primary,
                            modifier = Modifier.clickable { expandedHomeVitalKey = null }
                        )
                    }

                    summaryRows.forEach { (label, value) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(palette.surfaceElevated)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                            Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = palette.primary.copy(alpha = 0.16f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, palette.primary, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onJumpTarget() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = targetActionLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = palette.primary
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // 8. MAIN BRANCH HOME CARDS (Célok beállítása, Mai javaslat / Coach, Személyes edzéstervező, Aktív kalória)
        AnimatedVisibility(visible = showExtendedMainCards) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Célok beállítása (1/15 edzés >) from main Home screenshot
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
                        .clickable { onOpenGoalsDialog() }
                        .testTag("home_goals_row_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
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
                                text = strings.activeCaloriesAndWorkoutsToday(state.activeCaloriesKcal, state.weeklyGoalCompleted),
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

                // MAI JAVASLAT / TrainPilot Coach Card from main Home screenshot
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = palette.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = palette.primary.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .clickable { onOpenCoachReadiness() }
                        .testTag("home_coach_advice_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = palette.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = strings.coachSuggestionHeader,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = palette.primary
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = palette.primary
                            )
                        }

                        val isLightDay = state.homeDemoMode == HomeDemoMode.COACH_LIGHT_DAY
                        Text(
                            text = if (isLightDay) strings.coachLightDayTitle else strings.coachNormalDayTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.textPrimary
                        )
                        Text(
                            text = if (isLightDay) strings.coachLightDayBody else strings.coachNormalDayBody,
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = palette.surfaceElevated,
                                modifier = Modifier.border(1.dp, palette.cardBorder, RoundedCornerShape(12.dp))
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                    Text(
                                        text = strings.readinessTodayLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.textSecondary
                                    )
                                    Text(
                                        text = "${state.coachReadinessScore}/100",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = palette.textPrimary
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = palette.surfaceElevated,
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, palette.cardBorder, RoundedCornerShape(12.dp))
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                    Text(
                                        text = strings.nextWorkoutLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.textSecondary
                                    )
                                    Text(
                                        text = if (activeProgram != null) {
                                            "${activeProgram.localizedName(state.language)} • ${currentDay?.dayCode ?: "A"}"
                                        } else {
                                            "—"
                                        },
                                        style = MaterialTheme.typography.titleSmall,
                                        color = palette.textPrimary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
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
            .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
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
            .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
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

