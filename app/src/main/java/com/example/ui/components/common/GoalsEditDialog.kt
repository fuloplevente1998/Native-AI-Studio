package com.example.ui.components

import com.repforge.app.ui.theme.trainPilotClickable

import com.repforge.app.ui.theme.TrainPilotOutlinedButton

import com.repforge.app.ui.theme.trainPilotBorder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import com.example.ui.components.TrainPilotSlider as Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.AppLanguage
import com.example.ui.theme.LocalTrainPilotPalette

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.localization.LocalTrainPilotStrings

@Composable
fun GoalsEditDialog(
    currentWorkouts: Int,
    currentPeriod: com.example.model.WorkoutGoalPeriod = com.example.model.WorkoutGoalPeriod.WEEKLY,
    currentStepsGoal: Int,
    onSave: (Int, com.example.model.WorkoutGoalPeriod, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val strings = LocalTrainPilotStrings.current
    var period by remember { mutableStateOf(currentPeriod) }
    val minWorkouts = 1
    val maxWorkouts = if (period == com.example.model.WorkoutGoalPeriod.WEEKLY) 14 else 60
    var workouts by remember(period) {
        val initial = if (period == currentPeriod) currentWorkouts
        else if (period == com.example.model.WorkoutGoalPeriod.WEEKLY) (currentWorkouts / 4).coerceIn(1, 14)
        else (currentWorkouts * 4).coerceIn(1, 60)
        mutableIntStateOf(initial.coerceIn(minWorkouts, maxWorkouts))
    }
    var steps by remember { mutableIntStateOf(currentStepsGoal.coerceIn(2000, 30000)) }
    var workoutsInputText by remember(workouts) { mutableStateOf(workouts.toString()) }
    var stepsInputText by remember(steps) { mutableStateOf(steps.toString()) }

    com.example.ui.components.TrainPilotFormSheet(scrollText = false,
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        titleContentColor = palette.textPrimary,
        textContentColor = palette.textSecondary,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.goalsSetupTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                TrainPilotRedCloseButton(onClick = onDismiss)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Period Selector (Heti vs Havi cél)
                if (palette.isLiquidGlass) {
                    val labels = when (strings.language) {
                        AppLanguage.HU -> listOf("Heti cél (1–14)", "Havi cél (1–60)")
                        AppLanguage.EN -> listOf("Weekly (1–14)", "Monthly (1–60)")
                        AppLanguage.DE -> listOf("Wöchentlich (1–14)", "Monatlich (1–60)")
                        AppLanguage.RO -> listOf("Săptămânal (1–14)", "Lunar (1–60)")
                    }
                    LiquidSegmentedTabs(labels, if (period == com.example.model.WorkoutGoalPeriod.WEEKLY) 0 else 1,
                        { period = if (it == 0) com.example.model.WorkoutGoalPeriod.WEEKLY else com.example.model.WorkoutGoalPeriod.MONTHLY },
                        "goal_period_track", itemTag = { if (it == 0) "goal_period_weekly_btn" else "goal_period_monthly_btn" }) { _, label, _ ->
                        TrainPilotFitText(label, style = MaterialTheme.typography.labelMedium)
                    }
                } else Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isWeekly = period == com.example.model.WorkoutGoalPeriod.WEEKLY
                    val isMonthly = period == com.example.model.WorkoutGoalPeriod.MONTHLY
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isWeekly) palette.primary else palette.surfaceCard,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .trainPilotBorder(1.dp, if (isWeekly) palette.primary else palette.cardBorder, RoundedCornerShape(12.dp))
                            .trainPilotClickable(RoundedCornerShape(12.dp)) { period = com.example.model.WorkoutGoalPeriod.WEEKLY }
                            .testTag("goal_period_weekly_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)) {
                            Text(
                                text = when (strings.language) {
                                    com.example.model.AppLanguage.HU -> "Heti cél (1–14)"
                                    com.example.model.AppLanguage.EN -> "Weekly (1–14)"
                                    com.example.model.AppLanguage.DE -> "Wöchentlich (1–14)"
                                    com.example.model.AppLanguage.RO -> "Săptămânal (1–14)"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isWeekly) FontWeight.Bold else FontWeight.Medium,
                                color = if (isWeekly) palette.onPrimary else palette.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isMonthly) palette.primary else palette.surfaceCard,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .trainPilotBorder(1.dp, if (isMonthly) palette.primary else palette.cardBorder, RoundedCornerShape(12.dp))
                            .trainPilotClickable(RoundedCornerShape(12.dp)) { period = com.example.model.WorkoutGoalPeriod.MONTHLY }
                            .testTag("goal_period_monthly_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)) {
                            Text(
                                text = when (strings.language) {
                                    com.example.model.AppLanguage.HU -> "Havi cél (1–60)"
                                    com.example.model.AppLanguage.EN -> "Monthly (1–60)"
                                    com.example.model.AppLanguage.DE -> "Monatlich (1–60)"
                                    com.example.model.AppLanguage.RO -> "Lunar (1–60)"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isMonthly) FontWeight.Bold else FontWeight.Medium,
                                color = if (isMonthly) palette.onPrimary else palette.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // 2. Dynamic summary card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = palette.primary.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth().trainPilotBorder(1.dp, palette.primary.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = strings.periodStepsFormat(workouts, period == com.example.model.WorkoutGoalPeriod.MONTHLY, steps),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.primary
                        )
                        Text(
                            text = when (strings.language) {
                                com.example.model.AppLanguage.HU -> "Heti ~${steps * 7} lépés ekvivalens • ${if (period == com.example.model.WorkoutGoalPeriod.MONTHLY) "$workouts edzés/hónap" else "$workouts edzés/hét"}"
                                com.example.model.AppLanguage.EN -> "Weekly ~${steps * 7} steps equivalent • ${if (period == com.example.model.WorkoutGoalPeriod.MONTHLY) "$workouts workouts/month" else "$workouts workouts/week"}"
                                com.example.model.AppLanguage.DE -> "Wöchentlich ~${steps * 7} Schritte • ${if (period == com.example.model.WorkoutGoalPeriod.MONTHLY) "$workouts Trainings/Monat" else "$workouts Trainings/Woche"}"
                                com.example.model.AppLanguage.RO -> "Echivalent ~${steps * 7} pași/săpt. • ${if (period == com.example.model.WorkoutGoalPeriod.MONTHLY) "$workouts antrenamente/lună" else "$workouts antrenamente/săpt."}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                    }
                }

                // 3. Workouts target slider & numeric input
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val countLabel = if (period == com.example.model.WorkoutGoalPeriod.MONTHLY) {
                            when (strings.language) {
                                com.example.model.AppLanguage.HU -> "Havi edzésszám: $workouts edzés/hónap"
                                com.example.model.AppLanguage.EN -> "Monthly workouts: $workouts/mo"
                                com.example.model.AppLanguage.DE -> "Monatliche Trainings: $workouts/Monat"
                                com.example.model.AppLanguage.RO -> "Antrenamente lunare: $workouts/lună"
                            }
                        } else {
                            when (strings.language) {
                                com.example.model.AppLanguage.HU -> "Heti edzésszám: $workouts edzés/hét"
                                com.example.model.AppLanguage.EN -> "Weekly workouts: $workouts/wk"
                                com.example.model.AppLanguage.DE -> "Wöchentliche Trainings: $workouts/Woche"
                                com.example.model.AppLanguage.RO -> "Antrenamente săpt.: $workouts/săpt."
                            }
                        }
                        Text(
                            text = countLabel,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        com.example.ui.components.TrainPilotTextField(
                            value = workoutsInputText,
                            onValueChange = { input ->
                                workoutsInputText = input.filter { it.isDigit() }.take(2)
                                input.toIntOrNull()?.let { num ->
                                    workouts = num.coerceIn(minWorkouts, maxWorkouts)
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.width(68.dp).heightIn(min = 44.dp).testTag("workout_count_numeric_input"),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontWeight = FontWeight.Bold),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                        )
                    }
                    Slider(
                        value = workouts.toFloat(),
                        onValueChange = {
                            workouts = it.toInt()
                            workoutsInputText = workouts.toString()
                        },
                        valueRange = minWorkouts.toFloat()..maxWorkouts.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = palette.primary,
                            activeTrackColor = palette.primary
                        ),
                        modifier = Modifier.testTag("workout_goal_slider")
                    )
                }

                // 4. Daily steps slider & numeric input
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val stepsLabel = when (strings.language) {
                            com.example.model.AppLanguage.HU -> "Napi lépéscél: $steps lépés/nap"
                            com.example.model.AppLanguage.EN -> "Daily steps: $steps steps/day"
                            com.example.model.AppLanguage.DE -> "Tägliche Schritte: $steps/Tag"
                            com.example.model.AppLanguage.RO -> "Pași zilnici: $steps pași/zi"
                        }
                        Text(
                            text = stepsLabel,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        com.example.ui.components.TrainPilotTextField(
                            value = stepsInputText,
                            onValueChange = { input ->
                                stepsInputText = input.filter { it.isDigit() }.take(5)
                                input.toIntOrNull()?.let { num ->
                                    steps = num.coerceIn(2000, 30000)
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.width(88.dp).heightIn(min = 44.dp).testTag("steps_count_numeric_input"),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontWeight = FontWeight.Bold),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                        )
                    }
                    Slider(
                        value = steps.toFloat(),
                        onValueChange = {
                            steps = (it.toInt() / 500) * 500
                            stepsInputText = steps.toString()
                        },
                        valueRange = 2000f..30000f,
                        colors = SliderDefaults.colors(
                            thumbColor = palette.stepsAccent,
                            activeTrackColor = palette.stepsAccent
                        ),
                        modifier = Modifier.testTag("steps_goal_slider")
                    )
                }
            }
        },
        confirmButton = {
            com.repforge.app.ui.theme.TrainPilotSaveButton(
                onClick = { onSave(workouts, period, steps) },
                modifier = Modifier.heightIn(min = 48.dp)) {
                Text(strings.saveBtn, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TrainPilotOutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(strings.cancelBtn, color = palette.textPrimary)
            }
        }
    )
}

@Composable
fun GoalsEditDialog(
    currentWeeklyWorkouts: Int,
    currentStepsGoal: Int,
    onSave: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    GoalsEditDialog(
        currentWorkouts = currentWeeklyWorkouts,
        currentPeriod = com.example.model.WorkoutGoalPeriod.WEEKLY,
        currentStepsGoal = currentStepsGoal,
        onSave = { workouts, _, steps -> onSave(workouts, steps) },
        onDismiss = onDismiss
    )
}

typealias PersonalPlannerSave = (
        heightCm: Int,
        weightKg: Float,
        goalHu: String,
        experienceHu: String,
        locationHu: String,
        equipmentHu: String,
        daysPerWeek: Int,
        durationMin: Int,
        excludedMuscles: Set<String>,
        excludedExercises: Set<String>
    ) -> Unit
