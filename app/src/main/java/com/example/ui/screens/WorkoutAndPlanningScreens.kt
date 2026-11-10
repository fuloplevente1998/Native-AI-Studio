package com.example.ui.screens

import com.example.ui.components.TrainPilotAlertDialog as AlertDialog

import com.repforge.app.ui.theme.TrainPilotCardSpacing

import com.repforge.app.ui.theme.TrainPilotOutlinedButton

import com.repforge.app.ui.theme.TrainPilotFrames

import com.repforge.app.ui.theme.trainPilotBorder

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.IconToggleButton
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CalendarEventStatus
import com.example.model.ExerciseCategory
import com.example.model.ExerciseItem
import com.example.model.SubScreen
import com.example.ui.components.AnatomicalMuscleHeatmapCard
import com.example.ui.components.ExercisePostureMiniIllustration
import com.example.ui.components.LocalOpenExerciseVideo
import com.example.ui.components.ProgramTypeEmblemIcon
import com.example.ui.components.SmallExerciseVideoButton
import com.example.ui.components.SubScreenTopBar
import com.example.ui.components.programEquipmentSummary
import com.example.ui.components.programEquipmentCategoryLabel
import com.example.localization.LocalTrainPilotStrings
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.viewmodel.TrainPilotUiState

private fun trW(lang: com.example.model.AppLanguage, hu: String, en: String, de: String, ro: String): String = when (lang) {
    com.example.model.AppLanguage.HU -> hu
    com.example.model.AppLanguage.EN -> en
    com.example.model.AppLanguage.DE -> de
    com.example.model.AppLanguage.RO -> ro
}

@Composable
fun WorkoutTabScreen(
    state: TrainPilotUiState,
    onOpenSettings: () -> Unit,
    onOpenQuickWorkout: () -> Unit,
    onOpenExerciseLibrary: () -> Unit,
    onOpenOutdoorWorkout: () -> Unit,
    onOpenProgramsList: () -> Unit,
    onOpenProgramEditor: (String, Int) -> Unit,
    onStartWorkoutDay: (Int) -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenCoach: () -> Unit,
    onOpenProgressionModal: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val strings = LocalTrainPilotStrings.current
    val activeProgram = state.programs.firstOrNull { it.isActive } ?: state.programs.firstOrNull()

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
                .padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(TrainPilotCardSpacing.CardGap)
        ) {
        // 1. Gyors edzés Card (Mockup 1/4 #2)
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = palette.surface,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(18.dp))
                .clickable { onOpenQuickWorkout() }
                .testTag("workout_tab_quick_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                    contentDescription = null,
                    tint = palette.primary,
                    modifier = Modifier.size(30.dp)
                )
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

        // 2. Two-column cards: Gyakorlat választása (with posture mini-graphic) & Szabadtéri edzés (with mini GPS map & live metrics preview)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.surface,
                modifier = Modifier
                    .weight(1f)
                    .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(18.dp))
                    .clickable { onOpenExerciseLibrary() }
                    .testTag("workout_tab_exercise_picker")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = palette.primary.copy(alpha = 0.16f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    tint = palette.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = palette.primary.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = trW(state.language, "${state.exercises.size} gyak.", "${state.exercises.size} ex.", "${state.exercises.size} Üb.", "${state.exercises.size} ex."),
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Text(
                        text = strings.exercisePickerTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.exercisePickerSub,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = palette.textSecondary
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.surface,
                modifier = Modifier
                    .weight(1f)
                    .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(18.dp))
                    .clickable { onOpenOutdoorWorkout() }
                    .testTag("workout_tab_outdoor_card")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mini outdoor GPS map preview box
                        Box(
                            modifier = Modifier
                                .width(68.dp)
                                .height(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF14261C))
                                .border(1.dp, palette.success.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height
                                // River stripe
                                drawLine(
                                    color = Color(0xFF1E3A5F),
                                    start = Offset(0f, h * 0.2f),
                                    end = Offset(w * 0.35f, h),
                                    strokeWidth = 8.dp.toPx()
                                )
                                // GPS loop
                                drawLine(
                                    color = Color(0xFFF59E0B),
                                    start = Offset(w * 0.28f, h * 0.78f),
                                    end = Offset(w * 0.55f, h * 0.24f),
                                    strokeWidth = 2.5.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                                drawLine(
                                    color = Color(0xFFF59E0B),
                                    start = Offset(w * 0.55f, h * 0.24f),
                                    end = Offset(w * 0.82f, h * 0.65f),
                                    strokeWidth = 2.5.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                                drawCircle(
                                    color = Color(0xFF22C55E),
                                    radius = 3.2.dp.toPx(),
                                    center = Offset(w * 0.28f, h * 0.78f)
                                )
                                drawCircle(
                                    color = Color(0xFFF59E0B),
                                    radius = 3.2.dp.toPx(),
                                    center = Offset(w * 0.82f, h * 0.65f)
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = palette.success.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = trW(state.language, "GPS élő", "GPS live", "GPS live", "GPS live"),
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.success,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Text(
                        text = strings.outdoorWorkoutTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.outdoorWorkoutSub,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = palette.textSecondary
                        )
                    }
                }
            }
        }

        // 3. Aktuális program Header + Összes megtekintése >
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.currentProgramTitle,
                style = MaterialTheme.typography.titleLarge,
                color = palette.textPrimary,
                modifier = Modifier.weight(1f)
            )
            Row(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenProgramsList() }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.viewAllAction,
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = palette.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        if (activeProgram != null) {
            // Side-by-side A nap / B nap cards (Mockup 1/4 #2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                activeProgram.days.take(2).forEachIndexed { index, day ->
                    val isPrimaryDay = index == 0
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = palette.surface,
                        modifier = Modifier
                            .weight(1f)
                            .trainPilotBorder(
                                width = if (isPrimaryDay) 1.5.dp else 1.dp,
                                color = if (isPrimaryDay) palette.primary else palette.cardBorder,
                                shape = RoundedCornerShape(18.dp)
                            )
                            .clickable { onOpenProgramEditor(activeProgram.id, index) }
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = day.localizedDayCode(state.language),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isPrimaryDay) palette.primary else palette.textSecondary
                                )
                                ProgramTypeEmblemIcon(
                                    programTitle = "${activeProgram.nameHu} ${day.titleHu}",
                                    isHighlighted = isPrimaryDay,
                                    size = 34.dp
                                )
                            }
                            com.example.ui.components.secondaryWorkoutDayTitle(
                                day.localizedDayCode(state.language), day.localizedTitle(state.language))?.let { distinctTitle ->
                            Text(
                                text = distinctTitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = palette.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = palette.surfaceElevated
                            ) {
                                Text(
                                    text = programEquipmentSummary(day.exercises.ifEmpty { activeProgram.days.flatMap { it.exercises } }, activeProgram.tagsHu.firstOrNull().orEmpty(), state.language),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = palette.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                            Text(
                                text = trW(
                                    state.language,
                                    "📋 ${day.exercises.size} gyakorlat • ⏱ kb. ${day.durationMinutes} perc",
                                    "📋 ${day.exercises.size} exercises • ⏱ ~${day.durationMinutes} min",
                                    "📋 ${day.exercises.size} Übungen • ⏱ ca. ${day.durationMinutes} Min",
                                    "📋 ${day.exercises.size} exerciții • ⏱ ~${day.durationMinutes} min"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Button(
                                onClick = { onStartWorkoutDay(index) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPrimaryDay) palette.primary else palette.surfaceElevated,
                                    contentColor = if (isPrimaryDay) palette.onPrimary else palette.textPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = strings.startAction,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }

            // Edzés beállításai (from main Edzés screen)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = palette.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
                    .clickable { onOpenProgressionModal() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "⚙ ${trW(state.language, "Edzés beállításai", "Workout settings", "Trainingseinstellungen", "Setări antrenament")} • ${activeProgram.localizedName(state.language)}",
                            style = MaterialTheme.typography.titleSmall,
                            color = palette.textPrimary
                        )
                        val localizedStyle = when {
                            state.trainingStyleHu.contains("Erő-alap", ignoreCase = true) -> trW(state.language, state.trainingStyleHu, "Strength-focused (lower reps, higher weight)", "Kraftfokus (weniger Wdh., höheres Gewicht)", "Focus pe forță (repetări mai puține, greutate mai mare)")
                            state.trainingStyleHu.contains("Állóképesség", ignoreCase = true) -> trW(state.language, state.trainingStyleHu, "Endurance / Toning (12–15+ reps)", "Ausdauer / Definition (12–15+ Wdh.)", "Anduranță / Tonifiere (12–15+ repetări)")
                            else -> trW(state.language, state.trainingStyleHu, "Balanced (strength + muscle, 8–12 reps)", "Ausgewogen (Kraft + Muskel, 8–12 Wdh.)", "Echilibrat (forță + masă, 8–12 repetări)")
                        }
                        val localizedRule = when {
                            state.progressionRuleHu.contains("Óvatos", ignoreCase = true) -> trW(state.language, state.progressionRuleHu, "Conservative progression (first +reps, then weight)", "Vorsichtige Progression (zuerst +Wdh., dann Gewicht)", "Progresie prudentă (întâi +repetări, apoi greutate)")
                            state.progressionRuleHu.contains("Kézi", ignoreCase = true) -> trW(state.language, state.progressionRuleHu, "Manual control (recommendation only)", "Manuelle Steuerung (nur Empfehlung)", "Control manual (doar recomandare)")
                            else -> trW(state.language, state.progressionRuleHu, "Standard progression (+1–2 reps or +2 kg)", "Standard-Progression (+1–2 Wdh. oder +2 kg)", "Progresie standard (+1–2 repetări sau +2 kg)")
                        }
                        Text(
                            text = "$localizedStyle • $localizedRule",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = palette.primary
                    )
                }
            }
        } else {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(18.dp))
                    .padding(20.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = trW(state.language, "Nincs aktív program", "No active program", "Kein aktives Programm", "Niciun program activ"),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = palette.textPrimary
                    )
                    Text(
                        text = trW(
                            state.language,
                            "Válassz egy edzésprogramot a programlistából vagy hozz létre egy újat.",
                            "Select a workout program from the list or create a new one.",
                            "Wähle ein Trainingsprogramm aus der Liste oder erstelle ein neues.",
                            "Selectează un program din listă sau creează unul nou."
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = onOpenProgramsList,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.primary,
                            contentColor = palette.onPrimary
                        )
                    ) {
                        Text(strings.programsShortcutTitle)
                    }
                }
            }
        }

    }
}
}

@Composable
private fun ShortcutSquareCard(
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
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = palette.textPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = palette.textPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary
            )
        }
    }
}

@Composable
fun ProgramsListScreen(
    state: TrainPilotUiState,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleCustomTab: (Boolean) -> Unit,
    onActivateProgram: (String) -> Unit,
    onOpenProgramEditor: (String) -> Unit,
    onOpenExerciseLibrary: () -> Unit,
    onOpenPersonalPlanner: () -> Unit = {},
    onMoveProgramOrder: (Int, Int) -> Unit = { _, _ -> },
    onCreateCustomProgram: (String, String) -> Unit = { _, _ -> },
    onOpenAddCustomExercise: () -> Unit = {}
) {
    val palette = LocalTrainPilotPalette.current
    val strings = LocalTrainPilotStrings.current
    val activeProgram = state.programs.firstOrNull { it.isActive } ?: state.programs.firstOrNull()
    val filteredPrograms = remember(state.programs, state.programsTabCustomOnly, activeProgram?.id) {
        if (state.programsTabCustomOnly) {
            state.programs.filter { it.isCustom }
        } else {
            state.programs.filter { !it.isCustom && it.id != activeProgram?.id }
        }
    }
    var showCreateCustomProgramDialog by remember { mutableStateOf(false) }

    if (showCreateCustomProgramDialog) {
        var customProgName by remember { mutableStateOf("") }
        var customProgSub by remember { mutableStateOf(trW(state.language, "Saját egyedi összeállítású edzésterv", "Custom workout plan", "Eigener Trainingsplan", "Plan de antrenament personalizat")) }
        AlertDialog(
            onDismissRequest = { showCreateCustomProgramDialog = false },
            containerColor = palette.surfaceCard,
            titleContentColor = palette.textPrimary,
            textContentColor = palette.textSecondary,
            title = {
                Text(trW(state.language, "Új egyedi program létrehozása", "Create new custom program", "Neues eigenes Programm erstellen", "Creează program personalizat nou"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = trW(
                            state.language,
                            "Adj nevet és leírást az új egyedi programodnak (pl. Kör futás & Core, Saját A/B terv):",
                            "Enter a name and description for your new custom program:",
                            "Gib einen Namen und eine Beschreibung für dein eigenes Programm ein:",
                            "Introdu un nume și o descriere pentru noul tău program personalizat:"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                    OutlinedTextField(
                        value = customProgName,
                        onValueChange = { customProgName = it },
                        label = { Text(trW(state.language, "Program neve (pl. Kör futás & Erő)", "Program name", "Programmname", "Numele programului")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customProgSub,
                        onValueChange = { customProgSub = it },
                        label = { Text(trW(state.language, "Rövid leírás / megjegyzés", "Short description / note", "Kurzbeschreibung / Notiz", "Descriere scurtă / notiță")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCreateCustomProgramDialog = false
                        onCreateCustomProgram(customProgName, customProgSub)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primary,
                        contentColor = palette.onPrimary
                    )
                ) {
                    Text(trW(state.language, "Létrehozás és szerkesztés", "Create and edit", "Erstellen und bearbeiten", "Creează și editează"), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TrainPilotOutlinedButton(onClick = { showCreateCustomProgramDialog = false }) {
                    Text(strings.cancelBtn, color = palette.textPrimary)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        if (state.currentSubScreen == SubScreen.PROGRAMS_LIST) {
            SubScreenTopBar(
                title = strings.programsShortcutTitle,
                onBack = onBack,
                onOpenSettings = onOpenSettings,
                rightExtraContent = {
                    IconButton(
                        onClick = { onOpenProgramEditor(activeProgram?.id ?: "") },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = strings.createOrEditProgramAction,
                            tint = palette.textPrimary
                        )
                    }
                }
            )
        } else {
            Spacer(modifier = Modifier.height(8.dp))
        }

        Column(
            modifier = Modifier.padding(horizontal = TrainPilotCardSpacing.ScreenGutter),
            verticalArrangement = Arrangement.spacedBy(TrainPilotCardSpacing.CardGap)
        ) {
            // Segmented Pill: Alap programok (Left) | Saját programok (Right)
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = palette.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(24.dp))
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    val builtinCount = state.programs.count { !it.isCustom }
                    val customCount = state.programs.count { it.isCustom }
                    val tabs = listOf(
                        false to trW(state.language, "Alap programok ($builtinCount)", "Built-in programs ($builtinCount)", "Standardprogramme ($builtinCount)", "Programe de bază ($builtinCount)"),
                        true to trW(state.language, "Saját programok ($customCount)", "Custom programs ($customCount)", "Eigene Programme ($customCount)", "Programe proprii ($customCount)")
                    )
                    tabs.forEach { (isCustom, label) ->
                        val selected = state.programsTabCustomOnly == isCustom
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (selected) palette.primary else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .clickable { onToggleCustomTab(isCustom) }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = if (selected) palette.onPrimary else palette.textSecondary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            if (activeProgram != null && (!state.programsTabCustomOnly || activeProgram.isCustom)) {
                Text(
                    text = strings.currentProgramTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .trainPilotBorder(1.5.dp, palette.primary, RoundedCornerShape(16.dp))
                        .clickable { onOpenProgramEditor(activeProgram.id) }
                        .testTag("programs_active_program_card")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProgramTypeEmblemIcon(
                            programTitle = activeProgram.nameHu,
                            isHighlighted = true,
                            size = 44.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = activeProgram.localizedName(state.language),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = palette.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = palette.primary
                                ) {
                                    Text(
                                        text = strings.activeBadgeLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = palette.onPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${activeProgram.localizedSubtitle(state.language)} • ${trW(state.language, "${activeProgram.days.size} edzésnap", "${activeProgram.days.size} workout days", "${activeProgram.days.size} Trainingstage", "${activeProgram.days.size} zile de antrenament")}",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = palette.primary.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = programEquipmentSummary(activeProgram, state.language),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, lineHeight = 14.sp),
                                    color = palette.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = palette.textSecondary
                        )
                    }
                }
            }

            Text(
                text = if (state.programsTabCustomOnly) trW(state.language, "Saját készítésű és generált tervek", "Custom and generated plans", "Eigene und generierte Pläne", "Planuri proprii și generate") else strings.moreProgramsTitle,
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary
            )

            if (filteredPrograms.isEmpty() && state.programsTabCustomOnly) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = trW(state.language, "Még nincs saját generált programod", "No custom generated programs yet", "Noch keine eigenen Programme", "Nu ai încă programe personalizate"),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = palette.textPrimary
                        )
                        Text(
                            text = trW(
                                state.language,
                                "Készíts személyre szabott edzéstervet a Kezdőprofil (A01) varázslóval vagy hozz létre saját programot!",
                                "Build a personalized workout plan with the Starter Profile (A01) wizard or create a custom program!",
                                "Erstelle einen personalisierten Trainingsplan mit dem Startprofil (A01) oder lege ein eigenes Programm an!",
                                "Creează un plan personalizat cu profilul de bază (A01) sau adaugă un program propriu!"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(
                            onClick = onOpenPersonalPlanner,
                            colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(trW(state.language, "Kezdőprofil és egyéni terv (A01)", "Starter profile & custom plan (A01)", "Startprofil & eigener Plan (A01)", "Profil de bază și plan propriu (A01)"), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            filteredPrograms.forEachIndexed { index, program ->
                var dragAccumY by remember(program.id, index) { mutableFloatStateOf(0f) }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
                        .clickable { onOpenProgramEditor(program.id) }
                        .testTag("program_card_${program.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Working Reorder Handle (= + ▲ / ▼)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(0.dp),
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .pointerInput(index, filteredPrograms.size) {
                                    detectVerticalDragGestures(
                                        onDragStart = { dragAccumY = 0f },
                                        onVerticalDrag = { change, dragAmount ->
                                            change.consume()
                                            dragAccumY += dragAmount
                                            if (dragAccumY < -44f && index > 0) {
                                                dragAccumY = 0f
                                                onMoveProgramOrder(index, index - 1)
                                            } else if (dragAccumY > 44f && index < filteredPrograms.lastIndex) {
                                                dragAccumY = 0f
                                                onMoveProgramOrder(index, index + 1)
                                            }
                                        }
                                    )
                                }
                        ) {
                            Text(
                                text = "▲",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (index > 0) palette.primary else palette.textMuted.copy(alpha = 0.35f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable(enabled = index > 0) { onMoveProgramOrder(index, index - 1) }
                                    .padding(horizontal = 3.dp, vertical = 0.dp)
                            )
                            Icon(
                                imageVector = Icons.Default.DragHandle,
                                contentDescription = "Sorrend módosítása",
                                tint = palette.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "▼",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (index < filteredPrograms.lastIndex) palette.primary else palette.textMuted.copy(alpha = 0.35f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable(enabled = index < filteredPrograms.lastIndex) { onMoveProgramOrder(index, index + 1) }
                                    .padding(horizontal = 3.dp, vertical = 0.dp)
                            )
                        }

                        ProgramTypeEmblemIcon(
                            programTitle = program.nameHu,
                            isHighlighted = program.isActive,
                            size = 38.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = program.localizedName(state.language),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, lineHeight = 18.sp),
                                color = palette.textPrimary
                            )
                            Text(
                                text = program.localizedSubtitle(state.language),
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = palette.surfaceElevated
                            ) {
                                Text(
                                    text = programEquipmentSummary(program, state.language),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 13.sp),
                                    color = palette.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        if (!program.isActive) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onActivateProgram(program.id) }
                                    .testTag("program_activate_button_${program.id}")
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(9.dp),
                                    color = palette.primary,
                                    modifier = Modifier.testTag("program_activate_visual_${program.id}")
                                ) {
                                    Text(
                                        text = strings.activateAction,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = palette.onPrimary,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showCreateCustomProgramDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primary,
                        contentColor = palette.onPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 50.dp)
                        .testTag("programs_create_custom_program_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(trW(state.language, "Új egyedi program", "New custom program", "Neues eigenes Programm", "Program personalizat nou"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }

                TrainPilotOutlinedButton(
                    onClick = onOpenAddCustomExercise,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 50.dp)
                        .testTag("programs_create_custom_exercise_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = palette.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(trW(state.language, "Új gyakorlat", "New exercise", "Neue Übung", "Exercițiu nou"), style = MaterialTheme.typography.labelLarge, color = palette.primary, fontWeight = FontWeight.Bold)
                }
            }

            TrainPilotOutlinedButton(
                onClick = { onOpenProgramEditor(activeProgram?.id ?: "") },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = palette.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.createOrEditProgramAction,
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.primary
                )
            }

            TrainPilotOutlinedButton(
                onClick = onOpenExerciseLibrary,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text(
                    text = "${strings.exercisePickerTitle} (${state.exercises.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.textPrimary
                )
            }
        }
    }
}

@Composable
fun ProgramEditorScreen(
    state: TrainPilotUiState,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectDayIndex: (Int) -> Unit,
    onToggleExpandExercise: (String) -> Unit,
    onSaveExercisePrescription: (String, Int, Int, Float, Int) -> Unit,
    onOpenExerciseDetail: (String) -> Unit,
    onOpenExerciseLibrary: () -> Unit,
    onMoveExercise: (Int, Int) -> Unit = { _, _ -> },
    onOpenAddCustomExercise: () -> Unit = {},
    onCopyProgram: () -> Unit = {},
    onDeleteProgram: () -> Unit = {},
    onAddDay: (String) -> Unit = {},
    onDeleteDay: () -> Unit = {},
    onRemoveExercise: (String) -> Unit = {},
    onRenameProgram: (String) -> Unit = {},
    onSaveProgram: (String, List<com.example.model.StudioProgramPrescriptionDraft>) -> Unit = { _, _ -> }
) {
    val palette = LocalTrainPilotPalette.current
    val strings = LocalTrainPilotStrings.current
    val program = state.programs.firstOrNull { it.id == state.editingProgramId }
        ?: state.programs.firstOrNull()
        ?: com.example.model.TrainingProgramModel(id = "custom_program", nameHu = "Egyéni program", nameEn = "Custom Program", subtitleHu = "Saját program", subtitleEn = "Custom Program", tagsHu = emptyList(), isCustom = true, isActive = false, days = emptyList())
    val dayIndex = state.editingDayIndex.coerceIn(0, (program.days.size - 1).coerceAtLeast(0))
    val currentDay = program.days.getOrNull(dayIndex)
    val initialProgramName = program.localizedName(state.language)
    var programName by remember(program.id, initialProgramName) { mutableStateOf(initialProgramName) }
    val pendingPrescriptions = remember(program.id) {
        mutableStateMapOf<Pair<String, String>, com.example.model.StudioProgramPrescriptionDraft>()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        // Matches Mockup 4/4 #6 "Program szerkesztése"
        SubScreenTopBar(
            title = strings.editProgramTitle,
            onBack = onBack,
            onOpenSettings = onOpenSettings
        )

        Column(
            modifier = Modifier.padding(horizontal = TrainPilotCardSpacing.ScreenGutter),
            verticalArrangement = Arrangement.spacedBy(TrainPilotCardSpacing.CardGap)
        ) {
            OutlinedTextField(
                value = programName,
                onValueChange = { programName = it },
                modifier = Modifier.fillMaxWidth().testTag("studio_program_name"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            if (programName.trim() != initialProgramName) com.repforge.app.ui.theme.TrainPilotSaveButton(
                onClick = { onRenameProgram(programName) }, enabled = !state.libraryMutationBusy && programName.isNotBlank(),
                modifier = Modifier.fillMaxWidth().testTag("studio_program_save_name")) {
                Text(trW(state.language, "Programnév mentése", "Save program name", "Programmname speichern", "Salvează numele programului"))
            }

            // Segmented A nap | B nap selector (Mockup 4/4 #6)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                program.days.forEachIndexed { idx, d ->
                    val selected = idx == dayIndex
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) palette.primary else palette.surface,
                        modifier = Modifier
                            .widthIn(min = 76.dp)
                            .heightIn(min = 48.dp)
                            .trainPilotBorder(
                                1.dp,
                                if (selected) palette.primary else palette.cardBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectDayIndex(idx) }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp)
                        ) {
                            Text(
                                text = d.localizedDayCode(state.language),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (selected) palette.onPrimary else palette.textPrimary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            val totalExercisesInDay = currentDay?.exercises?.size ?: 0
            currentDay?.exercises?.forEachIndexed { index, ex ->
                val isExpanded = state.expandedEditorExerciseId == ex.id
                val draftKey = currentDay.id to ex.id
                val fields = pendingPrescriptions[draftKey] ?: com.example.model.StudioProgramPrescriptionDraft(
                    currentDay.id, ex.id, ex.defaultSets, ex.defaultReps, ex.defaultWeightKg, ex.defaultRestSec)
                val sets = fields.sets
                val reps = fields.reps
                val weight = fields.weightKg
                val restSec = fields.restSec
                fun editPrescription(change: (com.example.model.StudioProgramPrescriptionDraft) -> com.example.model.StudioProgramPrescriptionDraft) {
                    pendingPrescriptions[draftKey] = change(pendingPrescriptions[draftKey] ?: fields)
                }
                var dragAccumY by remember(ex.id, index) { mutableFloatStateOf(0f) }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .trainPilotBorder(
                            width = if (isExpanded) 1.5.dp else 1.dp,
                            color = if (isExpanded) palette.primary else palette.cardBorder,
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable { onToggleExpandExercise(ex.id) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Working Reorder Handle (= with vertical drag + ▲ / ▼ buttons)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .pointerInput(index, totalExercisesInDay) {
                                        detectVerticalDragGestures(
                                            onDragStart = { dragAccumY = 0f },
                                            onVerticalDrag = { change, dragAmount ->
                                                change.consume()
                                                dragAccumY += dragAmount
                                                if (dragAccumY < -40f && index > 0) {
                                                    dragAccumY = 0f
                                                    onMoveExercise(index, index - 1)
                                                } else if (dragAccumY > 40f && index < totalExercisesInDay - 1) {
                                                    dragAccumY = 0f
                                                    onMoveExercise(index, index + 1)
                                                }
                                            }
                                        )
                                    }
                                    .testTag("editor_exercise_drag_handle_$index")
                            ) {
                                Text(
                                    text = "▲",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (index > 0) palette.primary else palette.textMuted.copy(alpha = 0.35f),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable(enabled = index > 0) { onMoveExercise(index, index - 1) }
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                                Icon(
                                    imageVector = Icons.Default.DragHandle,
                                    contentDescription = "Sorrend mozgatása",
                                    tint = palette.textSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "▼",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (index < totalExercisesInDay - 1) palette.primary else palette.textMuted.copy(alpha = 0.35f),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable(enabled = index < totalExercisesInDay - 1) { onMoveExercise(index, index + 1) }
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${index + 1}.  ${ex.localizedName(state.language)}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = palette.textPrimary
                                )
                                val weightLabel = if (ex.defaultWeightKg > 0f) "${ex.defaultWeightKg.toInt()} kg" else "BW"
                                Text(
                                    text = "${ex.defaultSets} × ${ex.defaultReps}  •  $weightLabel  •  ${ex.defaultRestSec} s",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSecondary
                                )
                            }
                            SmallExerciseVideoButton(exerciseName = ex.localizedName(state.language), exerciseId = ex.id)
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onOpenExerciseDetail(ex.id) }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = strings.detailsAction,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.primary
                                )
                            }
                        }

                        AnimatedVisibility(visible = isExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    EditorStepperBox(
                                        modifier = Modifier.weight(1f).testTag("editor_sets_${ex.id}"),
                                        label = strings.setsLabel,
                                        valueText = "$sets",
                                        onMinus = { editPrescription { it.copy(sets = (it.sets - 1).coerceAtLeast(1)) } },
                                        onPlus = { editPrescription { it.copy(sets = (it.sets + 1).coerceAtMost(10)) } }
                                    )
                                    EditorStepperBox(
                                        modifier = Modifier.weight(1f),
                                        label = strings.repsLabel,
                                        valueText = "$reps",
                                        onMinus = { editPrescription { it.copy(reps = (it.reps - 1).coerceAtLeast(1)) } },
                                        onPlus = { editPrescription { it.copy(reps = (it.reps + 1).coerceAtMost(50)) } }
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    EditorStepperBox(
                                        modifier = Modifier.weight(1f),
                                        label = "${strings.weightLabel} (kg)",
                                        valueText = "${weight.toInt()}",
                                        onMinus = { editPrescription { it.copy(weightKg = (it.weightKg - 2f).coerceAtLeast(0f)) } },
                                        onPlus = { editPrescription { it.copy(weightKg = it.weightKg + 2f) } }
                                    )
                                    EditorStepperBox(
                                        modifier = Modifier.weight(1f),
                                        label = "${strings.restLabel} (s)",
                                        valueText = "$restSec",
                                        onMinus = { editPrescription { it.copy(restSec = (it.restSec - 15).coerceAtLeast(15)) } },
                                        onPlus = { editPrescription { it.copy(restSec = (it.restSec + 15).coerceAtMost(300)) } }
                                    )
                                }

                                com.example.ui.components.StudioRemoveProgramExerciseButton(
                                    ex.id, ex.localizedName(state.language), state.libraryMutationBusy, onRemoveExercise)

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = palette.surfaceElevated,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = ex.localizedEquipment(state.language),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.textPrimary,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    TrainPilotOutlinedButton(
                                        onClick = { pendingPrescriptions.remove(draftKey); onToggleExpandExercise(ex.id) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(strings.cancelBtn, color = palette.textPrimary)
                                    }
                                    com.repforge.app.ui.theme.TrainPilotSaveButton(
                                        onClick = {
                                            onSaveExercisePrescription(ex.id, sets, reps, weight, restSec)
                                            pendingPrescriptions.remove(draftKey)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp),
                                        shape = RoundedCornerShape(10.dp)) {
                                        Text(strings.saveBtn, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // + Gyakorlat hozzáadása a könyvtárból & + Új egyedi gyakorlat létrehozása
            TrainPilotOutlinedButton(
                onClick = onOpenExerciseLibrary,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = palette.textPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.addExerciseAction, color = palette.textPrimary)
            }

            com.example.ui.components.StudioProgramManagementControls(
                program, state.libraryMutationBusy, currentDay != null,
                onCopyProgram, onDeleteProgram, onAddDay, onDeleteDay,
                onSave = { onSaveProgram(programName, pendingPrescriptions.values.toList()) }
            )
        }
    }
}

@Composable
private fun EditorStepperBox(
    modifier: Modifier = Modifier,
    label: String,
    valueText: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val strings = LocalTrainPilotStrings.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = palette.surfaceElevated,
            modifier = Modifier
                .fillMaxWidth()
                .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(10.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onMinus, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = strings.decreaseValueDesc,
                        tint = palette.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = palette.textPrimary
                )
                IconButton(onClick = onPlus, modifier = Modifier.size(48.dp).testTag("editor_stepper_plus")) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = strings.increaseValueDesc,
                        tint = palette.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CalendarPlannerScreen(
    state: TrainPilotUiState,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectDay: (Int) -> Unit,
    onOpenReplanModal: (Boolean) -> Unit,
    onApplyReplan: (String, String) -> Unit,
    onOpenWorkoutDetail: () -> Unit,
    onUpdatePlanningParams: (String, String, String, String, Int, Int) -> Unit = { _, _, _, _, _, _ -> },
    onPlanMonthlyWorkouts: () -> Unit = {},
    onModifyCalendarEvent: (String, String, String, String, Int) -> Unit = { _, _, _, _, _ -> },
    onMoveCalendarEvent: (String, Int, Boolean) -> Unit = { _, _, _ -> },
    onDeleteCalendarEvent: (String) -> Unit = {},
    onAddPlannedWorkoutOnDay: (Int, String, String, Int) -> Unit = { _, _, _, _ -> },
    onUndoCalendarAction: () -> Unit = {},
    onSelectMonth: (String) -> Unit = {},
    onActivateProgram: (String) -> Unit = {}
) {
    val palette = LocalTrainPilotPalette.current
    val strings = LocalTrainPilotStrings.current
    val today = remember { java.time.LocalDate.now() }
    val displayedYearMonth = java.time.YearMonth.parse(state.calendarYearMonth)

    val huMonthNames = when (state.language) {
        com.example.model.AppLanguage.HU -> listOf("Január", "Február", "Március", "Április", "Május", "Június", "Július", "Augusztus", "Szeptember", "Október", "November", "December")
        com.example.model.AppLanguage.EN -> listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        com.example.model.AppLanguage.DE -> listOf("Januar", "Februar", "März", "April", "Mai", "Juni", "Juli", "August", "September", "Oktober", "November", "Dezember")
        com.example.model.AppLanguage.RO -> listOf("Ianuarie", "Februarie", "Martie", "Aprilie", "Mai", "Iunie", "Iulie", "August", "Septembrie", "Octombrie", "Noiembrie", "Decembrie")
    }
    val monthHeaderText = "${displayedYearMonth.year}. ${huMonthNames[displayedYearMonth.monthValue - 1]}"
    var openedDay by androidx.compose.runtime.saveable.rememberSaveable(state.calendarYearMonth) { mutableStateOf<Int?>(null) }
    val selectedDay = openedDay ?: state.selectedCalendarDay
    val dayEvents = state.calendarEvents.filter { it.dayOfMonth == selectedDay && it.dateIso.startsWith(displayedYearMonth.toString()) }
    val activeProgram = state.programs.firstOrNull { it.isActive } ?: state.programs.firstOrNull()
    val activeProgramTitle = activeProgram?.localizedName(state.language) ?: trW(state.language, "Otthoni A/B (személyre szabott)", "Home A/B (personalized)", "Zuhause A/B (personalisiert)", "Acasă A/B (personalizat)")

    var editingEvent by remember { mutableStateOf<com.example.model.CalendarDayEvent?>(null) }
    var movingEvent by remember { mutableStateOf<com.example.model.CalendarDayEvent?>(null) }
    var activeFieldDialog by remember { mutableStateOf<String?>(null) }

    val localizeCadenceLabel: (String) -> String = { raw ->
        when {
            raw.contains("Minden második", ignoreCase = true) -> trW(state.language, "Minden második nap", "Every second day", "Jeden zweiten Tag", "La fiecare două zile")
            raw.contains("Heti 3", ignoreCase = true) -> trW(state.language, "Heti 3 nap (H-Sze-P)", "3 days/week (M-W-F)", "3 Tage/Woche (Mo-Mi-Fr)", "3 zile/săpt. (L-Mi-V)")
            raw.contains("Heti 4", ignoreCase = true) -> trW(state.language, "Heti 4 nap", "4 days/week", "4 Tage/Woche", "4 zile/săpt.")
            raw.contains("Minden nap", ignoreCase = true) -> trW(state.language, "Minden nap", "Every day", "Jeden Tag", "În fiecare zi")
            else -> raw
        }
    }
    val localizeFirstDayLabel: (String) -> String = { raw ->
        when {
            raw.contains("Automatikus", ignoreCase = true) -> trW(state.language, "Automatikus", "Automatic", "Automatisch", "Automat")
            raw.contains("B", ignoreCase = true) -> trW(state.language, "Alap B", "Day B", "Tag B", "Ziua B")
            else -> trW(state.language, "Alap A", "Day A", "Tag A", "Ziua A")
        }
    }

    // 1. Modify Event Dialog
    editingEvent?.let { ev ->
        var splitSel by remember(ev.id) { mutableStateOf(ev.splitBadge) }
        var timeSel by remember(ev.id) { mutableStateOf(ev.timeLabel) }
        var durSel by remember(ev.id) { mutableIntStateOf(ev.durationMinutes) }
        AlertDialog(
            onDismissRequest = { editingEvent = null },
            containerColor = palette.surface,
            titleContentColor = palette.textPrimary,
            title = {
                Text(
                    text = trW(
                        state.language,
                        "Edzésnap módosítása (${displayedYearMonth.year}. ${displayedYearMonth.monthValue.toString().padStart(2, '0')}. ${ev.dayOfMonth}.)",
                        "Edit workout day (${displayedYearMonth.year}-${displayedYearMonth.monthValue.toString().padStart(2, '0')}-${ev.dayOfMonth.toString().padStart(2, '0')})",
                        "Trainingstag bearbeiten (${ev.dayOfMonth.toString().padStart(2, '0')}.${displayedYearMonth.monthValue.toString().padStart(2, '0')}.${displayedYearMonth.year})",
                        "Modifică ziua de antrenament (${ev.dayOfMonth.toString().padStart(2, '0')}.${displayedYearMonth.monthValue.toString().padStart(2, '0')}.${displayedYearMonth.year})"
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(trW(state.language, "Edzésnap típusa (A / B):", "Workout day type (A / B):", "Trainingstag-Typ (A / B):", "Tipul zilei de antrenament (A / B):"), style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("A", "B").forEach { sp ->
                            val isSel = splitSel == sp
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) palette.primary else palette.surfaceElevated,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { splitSel = sp }
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
                                    Text(trW(state.language, "Alap $sp", "Day $sp", "Tag $sp", "Ziua $sp"), fontWeight = FontWeight.Bold, color = if (isSel) palette.onPrimary else palette.textPrimary)
                                }
                            }
                        }
                    }
                    Text(trW(state.language, "Időpont:", "Time:", "Uhrzeit:", "Ora:"), style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("08:00", "10:00", "16:00", "18:00").forEach { tm ->
                            val isSel = timeSel == tm
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) palette.primary else palette.surfaceElevated,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { timeSel = tm }
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                                    Text(tm, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (isSel) palette.onPrimary else palette.textPrimary)
                                }
                            }
                        }
                    }
                    Text(trW(state.language, "Időtartam (perc):", "Duration (min):", "Dauer (Min.):", "Durată (min):"), style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(45, 60, 75, 90).forEach { dr ->
                            val isSel = durSel == dr
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) palette.primary else palette.surfaceElevated,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { durSel = dr }
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                                    Text("$dr ${trW(state.language, "p", "m", "M", "m")}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (isSel) palette.onPrimary else palette.textPrimary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                com.repforge.app.ui.theme.TrainPilotSaveButton(
                    onClick = {
                        onModifyCalendarEvent(ev.id, "$activeProgramTitle • Alap $splitSel", splitSel, timeSel, durSel)
                        editingEvent = null
                    }) {
                    Text(strings.saveBtn, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TrainPilotOutlinedButton(onClick = { editingEvent = null }) {
                    Text(strings.cancelBtn, color = palette.textPrimary)
                }
            }
        )
    }

    // 2. Move / Reschedule Day Event Dialog
    movingEvent?.let { ev ->
        var targetDay by remember(ev.id) { mutableIntStateOf((ev.dayOfMonth + 1).coerceAtMost(displayedYearMonth.lengthOfMonth())) }
        var shiftAllFollowing by remember(ev.id) { mutableStateOf(true) }
        AlertDialog(
            onDismissRequest = { movingEvent = null },
            containerColor = palette.surface,
            titleContentColor = palette.textPrimary,
            title = {
                Text(
                    text = trW(
                        state.language,
                        "Edzés áthelyezése (${ev.dayOfMonth}. napról)",
                        "Move workout (from day ${ev.dayOfMonth})",
                        "Training verschieben (von Tag ${ev.dayOfMonth})",
                        "Mută antrenamentul (din ziua ${ev.dayOfMonth})"
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(trW(state.language, "Válaszd ki az új napot ebben a hónapban:", "Select the new day in this month:", "Wähle den neuen Tag in diesem Monat:", "Selectează noua zi din această lună:"), style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                    EditorStepperBox(
                        label = trW(state.language, "Új naptári nap (1–${displayedYearMonth.lengthOfMonth()})", "New calendar day (1–${displayedYearMonth.lengthOfMonth()})", "Neuer Kalendertag (1–${displayedYearMonth.lengthOfMonth()})", "Zi calendaristică nouă (1–${displayedYearMonth.lengthOfMonth()})"),
                        valueText = "${displayedYearMonth.year}. ${displayedYearMonth.monthValue.toString().padStart(2, '0')}. ${targetDay.toString().padStart(2, '0')}.",
                        onMinus = { targetDay = (targetDay - 1).coerceAtLeast(1) },
                        onPlus = { targetDay = (targetDay + 1).coerceAtMost(displayedYearMonth.lengthOfMonth()) }
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = palette.surfaceElevated,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { shiftAllFollowing = !shiftAllFollowing }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (shiftAllFollowing) Icons.Default.Check else Icons.Default.Add,
                                contentDescription = null,
                                tint = palette.primary
                            )
                            Text(
                                text = trW(
                                    state.language,
                                    "Következő tervezett edzések automatikus eltolása",
                                    "Automatically shift following planned workouts",
                                    "Folgende geplante Trainings automatisch verschieben",
                                    "Decalează automat următoarele antrenamente planificate"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textPrimary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onMoveCalendarEvent(ev.id, targetDay, shiftAllFollowing)
                        movingEvent = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary)
                ) {
                    Text(trW(state.language, "Áthelyezés", "Move", "Verschieben", "Mută"), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TrainPilotOutlinedButton(onClick = { movingEvent = null }) {
                    Text(strings.cancelBtn, color = palette.textPrimary)
                }
            }
        )
    }

    // 3. Planner Field Picker Dialog
    activeFieldDialog?.let { fieldKey ->
        AlertDialog(
            onDismissRequest = { activeFieldDialog = null },
            containerColor = palette.surface,
            titleContentColor = palette.textPrimary,
            title = {
                Text(
                    text = when (fieldKey) {
                        "cadence" -> trW(state.language, "Tervezési ritmus", "Planning cadence", "Planungsrhythmus", "Ritm de planificare")
                        "firstDay" -> trW(state.language, "Első edzésnap", "First workout day", "Erster Trainingstag", "Prima zi de antrenament")
                        "startDate" -> trW(state.language, "Kezdőnap kiválasztása", "Select start date", "Startdatum wählen", "Selectează data de început")
                        "time" -> trW(state.language, "Edzés időpontja", "Workout time", "Trainingszeit", "Ora antrenamentului")
                        "duration" -> trW(state.language, "Időtartam (perc)", "Duration (min)", "Dauer (Min.)", "Durată (min)")
                        else -> trW(state.language, "Hetek száma", "Number of weeks", "Anzahl der Wochen", "Număr de săptămâni")
                    },
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold
                )
            },
            text = {
                val options: List<Pair<String, String>> = when (fieldKey) {
                    "cadence" -> listOf("Minden második nap", "Heti 3 nap (H-Sze-P)", "Heti 4 nap", "Minden nap").map { it to localizeCadenceLabel(it) }
                    "firstDay" -> listOf("Alap A", "Alap B").map { it to localizeFirstDayLabel(it) }
                    "startDate" -> (1..displayedYearMonth.lengthOfMonth()).map { d ->
                        val dateStr = "${displayedYearMonth.year}. ${displayedYearMonth.monthValue.toString().padStart(2, '0')}. ${d.toString().padStart(2, '0')}."
                        dateStr to dateStr
                    }
                    "time" -> listOf("07:00", "08:30", "10:00", "16:00", "17:30", "18:00", "19:30").map { it to it }
                    "duration" -> listOf("30", "45", "60", "75", "90").map { it to it }
                    else -> listOf("1", "2", "4", "6", "8", "12").map { it to it }
                }
                Column(
                    modifier = Modifier
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    options.forEach { (canonicalVal, displayLabel) ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = palette.surfaceElevated,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    when (fieldKey) {
                                        "cadence" -> onUpdatePlanningParams(canonicalVal, state.planningFirstDayHu, state.planningStartDate, state.planningTime, state.planningDurationMin, state.planningWeeksCount)
                                        "firstDay" -> onUpdatePlanningParams(state.planningCadenceHu, canonicalVal, state.planningStartDate, state.planningTime, state.planningDurationMin, state.planningWeeksCount)
                                        "startDate" -> onUpdatePlanningParams(state.planningCadenceHu, state.planningFirstDayHu, canonicalVal, state.planningTime, state.planningDurationMin, state.planningWeeksCount)
                                        "time" -> onUpdatePlanningParams(state.planningCadenceHu, state.planningFirstDayHu, state.planningStartDate, canonicalVal, state.planningDurationMin, state.planningWeeksCount)
                                        "duration" -> onUpdatePlanningParams(state.planningCadenceHu, state.planningFirstDayHu, state.planningStartDate, state.planningTime, canonicalVal.toIntOrNull() ?: 60, state.planningWeeksCount)
                                        "weeks" -> onUpdatePlanningParams(state.planningCadenceHu, state.planningFirstDayHu, state.planningStartDate, state.planningTime, state.planningDurationMin, canonicalVal.toIntOrNull() ?: 4)
                                    }
                                    activeFieldDialog = null
                                }
                        ) {
                            Text(
                                text = displayLabel,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = palette.textPrimary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TrainPilotOutlinedButton(onClick = { activeFieldDialog = null }) {
                    Text(trW(strings.language, "Bezárás", "Close", "Schließen", "Închide"), color = palette.textPrimary)
                }
            }
        )
    }

    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
        val calendarWeeks = ((displayedYearMonth.atDay(1).dayOfWeek.value - 1 + displayedYearMonth.lengthOfMonth() + 6) / 7)
        // Reserve space for the complete three-row planner, its actions and month header.
        // Never shrink touch targets: short windows and large fonts use the page scroll.
        val calendarCellHeight = ((maxHeight - 424.dp) / calendarWeeks.toFloat()).coerceIn(48.dp, 52.dp)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = TrainPilotCardSpacing.ScreenGutter, vertical = 4.dp)
                .testTag("calendar_compact_page"),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 1. EXACT SCREENSHOT 7 CALENDAR CARD
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth().testTag("calendar_month_panel")
                    .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(22.dp))
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Header row: [ < ]   2026. Október 01.   [ > ]   [ X ]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = palette.surfaceElevated,
                            modifier = Modifier
                                .size(48.dp)
                                .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { openedDay = null; onSelectMonth(displayedYearMonth.minusMonths(1).toString()) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "Előző hónap",
                                    tint = palette.textPrimary
                                )
                            }
                        }

                        Text(
                            text = monthHeaderText,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = palette.textPrimary
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = palette.surfaceElevated,
                                modifier = Modifier
                                    .size(48.dp)
                                    .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(12.dp))
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { openedDay = null; onSelectMonth(displayedYearMonth.plusMonths(1).toString()) }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "Következő hónap",
                                        tint = palette.textPrimary
                                    )
                                }
                            }

                            if (state.currentSubScreen == SubScreen.CALENDAR_PLANNER) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = palette.danger.copy(alpha = 0.12f),
                                    modifier = Modifier
                                        .size(42.dp)
                                        .border(1.dp, palette.danger.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onBack() }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "✕",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                            color = palette.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Weekday headers
                    val weekdayShortHeaders = when (state.language) {
                        com.example.model.AppLanguage.HU -> listOf("H", "K", "Sze", "Cs", "P", "Szo", "V")
                        com.example.model.AppLanguage.EN -> listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                        com.example.model.AppLanguage.DE -> listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
                        com.example.model.AppLanguage.RO -> listOf("Lun", "Mar", "Mie", "Joi", "Vin", "Sâm", "Dum")
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        weekdayShortHeaders.forEach { d ->
                            Text(
                                text = d,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = palette.textSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Dynamic month grid with rounded dark cells matching Screenshot 7
                    val daysInMonth = displayedYearMonth.lengthOfMonth()
                    val firstDayOffset = displayedYearMonth.atDay(1).dayOfWeek.value - 1 // Monday=0 .. Sunday=6
                    val totalCells = ((firstDayOffset + daysInMonth + 6) / 7) * 7

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (weekIdx in 0 until (totalCells / 7)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                for (dow in 0..6) {
                                    val cellIndex = weekIdx * 7 + dow
                                    val dayNumber = cellIndex - firstDayOffset + 1
                                    if (dayNumber !in 1..daysInMonth) {
                                        Spacer(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(calendarCellHeight)
                                        )
                                    } else {
                                        val isSelected = dayNumber == state.selectedCalendarDay
                                        val evs = state.calendarEvents.filter { it.dayOfMonth == dayNumber && it.dateIso.startsWith(displayedYearMonth.toString()) }
                                        val completedEv = evs.firstOrNull { it.status == CalendarEventStatus.COMPLETED }
                                        val plannedEv = evs.firstOrNull { it.status == CalendarEventStatus.PLANNED }

                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = when {
                                                plannedEv != null -> palette.primary.copy(alpha = 0.12f)
                                                else -> palette.surface
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(calendarCellHeight)
                                                .trainPilotBorder(
                                                    width = if (isSelected) 1.8.dp else 1.dp,
                                                    color = if (isSelected) palette.primary else palette.cardBorder,
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .clip(RoundedCornerShape(10.dp))
                                                .testTag("calendar_day_$dayNumber")
                                                .clickable { onSelectDay(dayNumber); openedDay = dayNumber }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "$dayNumber",
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.ExtraBold
                                                    ),
                                                    color = palette.textPrimary
                                                )
                                                when {
                                                    completedEv != null -> {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = "Teljesítve",
                                                            tint = palette.primary,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                    plannedEv != null -> {
                                                        Text(
                                                            text = plannedEv.splitBadge,
                                                            style = MaterialTheme.typography.labelMedium.copy(
                                                                fontWeight = FontWeight.ExtraBold
                                                            ),
                                                            color = palette.primary
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
                }
            }
            // 3. Calendar planning stays visible below the month grid.
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calendar_planning_block")
                    .trainPilotBorder(TrainPilotFrames.panelWidth, palette.cardBorder, RoundedCornerShape(22.dp))
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(activeProgramTitle, color = palette.textPrimary,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${localizeCadenceLabel(state.planningCadenceHu)} • ${state.planningTime}",
                                color = palette.textSecondary, style = MaterialTheme.typography.labelSmall,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (state.calendarUndoStack.isNotEmpty()) com.repforge.app.ui.theme.TrainPilotNegativeIconButton(onClick = onUndoCalendarAction,
                            modifier = Modifier.size(48.dp).testTag("calendar_undo_action")) {
                            Text("↶", color = androidx.compose.material3.LocalContentColor.current, style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.semantics { contentDescription = trW(state.language, "Visszavonás (${state.calendarUndoStack.size})", "Undo (${state.calendarUndoStack.size})", "Rückgängig (${state.calendarUndoStack.size})", "Anulează (${state.calendarUndoStack.size})") })
                        }
                        Button(
                            onClick = { onOpenReplanModal(true) },
                            modifier = Modifier.heightIn(min = 48.dp).testTag("calendar_replan_action"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.primary,
                                contentColor = palette.onPrimary
                            )
                        ) {
                            Text(
                                text = trW(state.language, "Újratervezés", "Replan", "Neu planen", "Replanificare"),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold)
                            )
                        }
                    }

                    val monthDates = remember(displayedYearMonth) {
                        (1..displayedYearMonth.lengthOfMonth()).map { d ->
                            "${displayedYearMonth.year}. ${displayedYearMonth.monthValue.toString().padStart(2, '0')}. ${d.toString().padStart(2, '0')}."
                        }
                    }

                    val cadenceMap = listOf("Minden második nap", "Heti 3 nap (H-Sze-P)", "Heti 4 nap", "Minden nap").associateBy { localizeCadenceLabel(it) }
                    val firstDayMap = listOf("Alap A", "Alap B").associateBy { localizeFirstDayLabel(it) }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PlannerFieldTile(
                            modifier = Modifier.weight(1f).testTag("calendar_field_cadence"),
                            compact = true,
                            label = trW(state.language, "Tervezési ritmus", "Planning cadence", "Planungsrhythmus", "Ritm de planificare"),
                            value = localizeCadenceLabel(state.planningCadenceHu),
                            showChevron = true,
                            dropdownOptions = cadenceMap.keys.toList(),
                            onSelectOption = { opt ->
                                val canonical = cadenceMap[opt] ?: opt
                                onUpdatePlanningParams(canonical, state.planningFirstDayHu, state.planningStartDate, state.planningTime, state.planningDurationMin, state.planningWeeksCount)
                            }
                        )
                        PlannerFieldTile(
                            modifier = Modifier.weight(1f).testTag("calendar_field_first_day"),
                            compact = true,
                            label = trW(state.language, "Első edzésnap", "First workout day", "Erster Trainingstag", "Prima zi de antrenament"),
                            value = localizeFirstDayLabel(state.planningFirstDayHu),
                            showChevron = true,
                            dropdownOptions = firstDayMap.keys.toList(),
                            onSelectOption = { opt ->
                                val canonical = firstDayMap[opt] ?: opt
                                onUpdatePlanningParams(state.planningCadenceHu, canonical, state.planningStartDate, state.planningTime, state.planningDurationMin, state.planningWeeksCount)
                            }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PlannerFieldTile(
                            modifier = Modifier.weight(1f).testTag("calendar_field_start_date"),
                            compact = true,
                            label = trW(state.language, "Kezdőnap", "Start date", "Startdatum", "Data de început"),
                            value = state.planningStartDate,
                            showCalendarIcon = true,
                            dropdownOptions = monthDates,
                            onSelectOption = { opt ->
                                onUpdatePlanningParams(state.planningCadenceHu, state.planningFirstDayHu, opt, state.planningTime, state.planningDurationMin, state.planningWeeksCount)
                            }
                        )
                        PlannerFieldTile(
                            modifier = Modifier.weight(1f).testTag("calendar_field_time"),
                            compact = true,
                            label = trW(state.language, "Időpont", "Time", "Uhrzeit", "Ora"),
                            value = state.planningTime,
                            dropdownOptions = listOf("07:00", "08:30", "10:00", "16:00", "17:30", "18:00", "19:30"),
                            onSelectOption = { opt ->
                                onUpdatePlanningParams(state.planningCadenceHu, state.planningFirstDayHu, state.planningStartDate, opt, state.planningDurationMin, state.planningWeeksCount)
                            }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PlannerFieldTile(
                            modifier = Modifier.weight(1f).testTag("calendar_field_duration"),
                            compact = true,
                            label = trW(state.language, "Időtartam (perc)", "Duration (min)", "Dauer (Min.)", "Durată (min)"),
                            value = "${state.planningDurationMin}",
                            dropdownOptions = listOf("30", "45", "60", "75", "90"),
                            onSelectOption = { opt ->
                                onUpdatePlanningParams(state.planningCadenceHu, state.planningFirstDayHu, state.planningStartDate, state.planningTime, opt.toIntOrNull() ?: 60, state.planningWeeksCount)
                            }
                        )
                        PlannerFieldTile(
                            modifier = Modifier.weight(1f).testTag("calendar_field_weeks"),
                            compact = true,
                            label = trW(state.language, "Hetek száma", "Weeks count", "Anzahl Wochen", "Număr săptămâni"),
                            value = "${state.planningWeeksCount}",
                            dropdownOptions = listOf("1", "2", "4", "6", "8", "12"),
                            onSelectOption = { opt ->
                                onUpdatePlanningParams(state.planningCadenceHu, state.planningFirstDayHu, state.planningStartDate, state.planningTime, state.planningDurationMin, opt.toIntOrNull() ?: 4)
                            }
                        )
                    }

                    Button(
                        onClick = onPlanMonthlyWorkouts,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.primary,
                            contentColor = palette.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp).testTag("calendar_plan_action")
                    ) {
                        Text(
                            text = trW(state.language, "Edzések megtervezése", "Plan workouts", "Trainings planen", "Planifică antrenamentele"),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                        )
                    }
                }
            }
        }
    }
    if (openedDay != null) {
        com.example.ui.components.CompactCalendarDialog(
            title = "${displayedYearMonth.year}. ${displayedYearMonth.monthValue.toString().padStart(2, '0')}. ${selectedDay.toString().padStart(2, '0')}.",
            tag = "calendar_day_dialog", onClose = { openedDay = null }
        ) {
            // 2. INTERACTIVE DAY ACTIONS CARD (Módosítás, Áthelyezés, Törlés, Visszavonás)
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth().testTag("calendar_day_actions")
                    .trainPilotBorder(TrainPilotFrames.panelWidth, palette.cardBorder, RoundedCornerShape(18.dp))
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (dayEvents.isEmpty()) {
                        Text(trW(state.language, "Nincs tervezett edzés ezen a napon.", "No planned workout on this day.", "Kein geplantes Training an diesem Tag.", "Niciun antrenament planificat în această zi."), style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary, modifier = Modifier.fillMaxWidth().testTag("calendar_empty_day"))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (split in listOf("A", "B")) {
                                TrainPilotOutlinedButton(onClick = {
                                    onAddPlannedWorkoutOnDay(selectedDay, split, state.planningTime, state.planningDurationMin)
                                }, modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("calendar_add_$split")) {
                                    Text(trW(state.language, "+ Alap $split", "+ Day $split", "+ Tag $split", "+ Ziua $split"))
                                }
                            }
                        }
                    } else {
                        dayEvents.forEach { ev ->
                            val isDone = ev.status == CalendarEventStatus.COMPLETED
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(palette.surfaceElevated)
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = ev.localizedTitle(state.language, state.programs),
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = palette.textPrimary
                                        )
                                        Text(
                                            text = "${ev.timeLabel} • ~${ev.durationMinutes} ${trW(state.language, "perc", "min", "Min.", "min")} • ${if (isDone) trW(state.language, "Teljesítve ✓", "Completed ✓", "Abgeschlossen ✓", "Finalizat ✓") else trW(state.language, "Tervezett (${ev.splitBadge} nap)", "Planned (Day ${ev.splitBadge})", "Geplant (Tag ${ev.splitBadge})", "Planificat (Ziua ${ev.splitBadge})")}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isDone) palette.success else palette.primary
                                        )
                                    }
                                    if (isDone) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = palette.primary.copy(alpha = 0.16f),
                                            modifier = Modifier.clickable { openedDay = null; onOpenWorkoutDetail() }
                                        ) {
                                            Text(
                                                text = "${strings.navLog} >",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = palette.primary,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }

                                // 4-button Action row: Módosítás | Áthelyezés | Törlés | Visszavonás
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    CalendarActionChip(
                                        label = trW(state.language, "Módosítás", "Modify", "Ändern", "Modifică"),
                                        color = palette.primary,
                                        modifier = Modifier.weight(1f),
                                        onClick = { openedDay = null; editingEvent = ev }
                                    )
                                    CalendarActionChip(
                                        label = trW(state.language, "Áthelyezés", "Move", "Verschieben", "Mută"),
                                        color = palette.textPrimary,
                                        modifier = Modifier.weight(1f),
                                        onClick = { openedDay = null; movingEvent = ev }
                                    )
                                    CalendarActionChip(
                                        label = trW(state.language, "Törlés", "Delete", "Löschen", "Șterge"),
                                        color = palette.danger,
                                        modifier = Modifier.weight(1f),
                                        onClick = { onDeleteCalendarEvent(ev.id) }, negative = true
                                    )
                                    CalendarActionChip(
                                        label = trW(state.language, "Visszavonás", "Undo", "Rückgängig", "Anulează"),
                                        color = if (state.calendarUndoStack.isNotEmpty()) palette.secondary else palette.textSecondary,
                                        modifier = Modifier.weight(1f),
                                        onClick = { onUndoCalendarAction() }, negative = true
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    // 4. EXACT SCREENSHOT 8 "Edzések újratervezése" MODAL DIALOG
    if (state.showReplanModal) {
        var selectedProgId by remember(activeProgram?.id) { mutableStateOf(activeProgram?.id ?: "") }
        var selectedFirstDay by remember { mutableStateOf("Automatikus") }
        val defaultTomorrow = remember {
            today.plusDays(1).format(java.time.format.DateTimeFormatter.ofPattern("yyyy. MM. dd."))
        }
        var selectedNewStartDate by remember { mutableStateOf(defaultTomorrow) }

        val selectedProgObj = state.programs.firstOrNull { it.id == selectedProgId } ?: activeProgram
        val selectedProgName = selectedProgObj?.localizedName(state.language) ?: activeProgramTitle
        val monthDatesForReplan = remember(displayedYearMonth) {
            (1..displayedYearMonth.lengthOfMonth()).map { d ->
                "${displayedYearMonth.year}. ${displayedYearMonth.monthValue.toString().padStart(2, '0')}. ${d.toString().padStart(2, '0')}."
            }
        }

        androidx.compose.ui.window.Dialog(
            onDismissRequest = { onOpenReplanModal(false) },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 20.dp)
                    .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header: Edzések újratervezése + red X close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = trW(state.language, "Edzések újratervezése", "Replan workouts", "Trainings neu planen", "Replanificare antrenamente"),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = palette.textPrimary
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = palette.danger.copy(alpha = 0.12f),
                            modifier = Modifier
                                .size(42.dp)
                                .border(1.dp, palette.danger.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onOpenReplanModal(false) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "✕",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = palette.textPrimary
                                )
                            }
                        }
                    }

                    Text(
                        text = trW(
                            state.language,
                            "Bármelyik edzésnappal újratervezhetsz. Programváltáskor a hátralévő tervet az új program váltja fel. A naplózott és aktív edzések megmaradnak.",
                            "Replan starting with any workout day. Switching programs replaces the remaining plan while keeping logged and active workouts.",
                            "Du kannst mit jedem Trainingstag neu planen. Beim Programmwechsel wird der restliche Plan ersetzt, protokollierte und aktive Trainings bleiben erhalten.",
                            "Poți replanifica începând cu orice zi de antrenament. La schimbarea programului, restul planului este înlocuit, iar antrenamentele salvate și active rămân."
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )

                    // Program selector (floating dropdown)
                    PlannerFieldTile(
                        label = trW(state.language, "Program", "Program", "Programm", "Program"),
                        value = selectedProgName,
                        showChevron = true,
                        dropdownOptions = state.programs.map { it.localizedName(state.language) },
                        onSelectOption = { chosenName ->
                            state.programs.firstOrNull { it.localizedName(state.language) == chosenName || it.nameHu == chosenName }?.let { chosen ->
                                selectedProgId = chosen.id
                                onActivateProgram(chosen.id)
                            }
                        }
                    )

                    // Első edzésnap selector (floating dropdown)
                    val firstDayOptionsMap = listOf("Automatikus", "Alap A", "Alap B").associateBy { localizeFirstDayLabel(it) }
                    PlannerFieldTile(
                        label = trW(state.language, "Első edzésnap", "First workout day", "Erster Trainingstag", "Prima zi de antrenament"),
                        value = localizeFirstDayLabel(selectedFirstDay),
                        showChevron = true,
                        dropdownOptions = firstDayOptionsMap.keys.toList(),
                        onSelectOption = { selectedFirstDay = firstDayOptionsMap[it] ?: it }
                    )

                    // Új kezdőnap selector (floating dropdown)
                    PlannerFieldTile(
                        label = trW(state.language, "Új kezdőnap", "New start date", "Neues Startdatum", "Noua dată de început"),
                        value = selectedNewStartDate,
                        showCalendarIcon = true,
                        showChevron = true,
                        dropdownOptions = monthDatesForReplan,
                        onSelectOption = { selectedNewStartDate = it }
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${trW(state.language, "Program", "Program", "Programm", "Program")}: $selectedProgName",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = palette.textPrimary
                    )

                    // Shifted sessions preview list with dividers matching Screenshot 8
                    val startD = selectedNewStartDate
                        .split(".")
                        .mapNotNull { it.trim().toIntOrNull() }
                        .lastOrNull() ?: today.dayOfMonth
                    val previewItems = listOf(
                        localizeFirstDayLabel("Alap B") to "${displayedYearMonth.year}. ${displayedYearMonth.monthValue.toString().padStart(2, '0')}. ${(startD - 2).coerceAtLeast(1).toString().padStart(2, '0')}. ${state.planningTime} → $selectedNewStartDate ${state.planningTime}",
                        localizeFirstDayLabel("Alap A") to "$selectedNewStartDate ${state.planningTime} → ${displayedYearMonth.year}. ${displayedYearMonth.monthValue.toString().padStart(2, '0')}. ${(startD + 2).coerceAtMost(31).toString().padStart(2, '0')}. ${state.planningTime}",
                        localizeFirstDayLabel("Alap B") to "${displayedYearMonth.year}. ${displayedYearMonth.monthValue.toString().padStart(2, '0')}. ${(startD + 2).coerceAtMost(31).toString().padStart(2, '0')}. ${state.planningTime} → ${displayedYearMonth.year}. ${displayedYearMonth.monthValue.toString().padStart(2, '0')}. ${(startD + 4).coerceAtMost(31).toString().padStart(2, '0')}. ${state.planningTime}"
                    )
                    previewItems.forEach { (splitTitle, shiftText) ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = splitTitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = palette.textPrimary
                            )
                            Text(
                                text = shiftText,
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider(color = palette.cardBorder)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = { onApplyReplan(selectedNewStartDate, selectedFirstDay) },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.primary,
                            contentColor = palette.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Text(
                            text = trW(state.language, "Terv frissítése", "Update plan", "Plan aktualisieren", "Actualizează planul"),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarActionChip(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    negative: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (negative) com.repforge.app.ui.theme.TrainPilotActionAppearance.negativeBackground else color.copy(alpha = 0.14f),
        modifier = modifier
            .heightIn(min = 48.dp)
            .border(1.dp, if (negative) com.repforge.app.ui.theme.TrainPilotActionAppearance.negativeOutline.copy(alpha = 0.7f) else color.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (negative) com.repforge.app.ui.theme.TrainPilotActionAppearance.negativeForeground else color,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PlannerFieldTile(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    compact: Boolean = false,
    showChevron: Boolean = false,
    showCalendarIcon: Boolean = false,
    dropdownOptions: List<String>? = null,
    onSelectOption: ((String) -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    val palette = LocalTrainPilotPalette.current
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        if (!compact) Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = palette.textSecondary
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = palette.surfaceElevated,
                modifier = Modifier
                    .fillMaxWidth()
                    .trainPilotBorder(
                        1.dp,
                        if (expanded) palette.primary else palette.cardBorder,
                        RoundedCornerShape(14.dp)
                    )
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        if (!dropdownOptions.isNullOrEmpty() && onSelectOption != null) {
                            expanded = !expanded
                        } else {
                            onClick()
                        }
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .padding(horizontal = if (compact) 8.dp else 14.dp, vertical = if (compact) 4.dp else 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        if (compact) Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = palette.textSecondary,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = value,
                            style = (if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge)
                                .copy(fontWeight = FontWeight.ExtraBold),
                            color = palette.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (showCalendarIcon) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "📅", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (showChevron || !dropdownOptions.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (!dropdownOptions.isNullOrEmpty() && onSelectOption != null) {
                androidx.compose.material3.DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    containerColor = palette.surfaceCard,
                    shape = RoundedCornerShape(12.dp),
                    border = TrainPilotFrames.stroke(1.dp, palette.primary.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .heightIn(max = 260.dp)
                ) {
                    dropdownOptions.forEach { opt ->
                        val isSelected = opt == value
                        androidx.compose.material3.DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = opt,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) palette.primary else palette.textPrimary
                                    )
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "✓",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.primary
                                        )
                                    }
                                }
                            },
                            onClick = {
                                onSelectOption(opt)
                                expanded = false
                            },
                            modifier = Modifier.background(
                                if (isSelected) palette.primary.copy(alpha = 0.16f)
                                else Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseLibraryScreen(
    state: TrainPilotUiState,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectCategory: (ExerciseCategory) -> Unit,
    onSelectExercise: (String) -> Unit,
    onOpenAddCustomExercise: () -> Unit = {},
    onStartExerciseNow: (ExerciseItem) -> Unit = {},
    onAddExerciseToQueue: (ExerciseItem) -> Unit = {},
    onStartQueuedWorkout: () -> Unit = {},
    onToggleFavoritesOnly: () -> Unit = {},
    onToggleFavorite: (String) -> Unit = {}
) {
    val palette = LocalTrainPilotPalette.current
    val strings = LocalTrainPilotStrings.current
    val filtered = state.exercises.filter { ex ->
        val matchesCategory = state.selectedExerciseCategory == ExerciseCategory.ALL || ex.category == state.selectedExerciseCategory
        val matchesSearch = state.exerciseSearchQuery.isBlank() ||
            ex.nameHu.contains(state.exerciseSearchQuery, ignoreCase = true) ||
            ex.nameEn.contains(state.exerciseSearchQuery, ignoreCase = true) ||
            ex.nameDe.contains(state.exerciseSearchQuery, ignoreCase = true) ||
            ex.nameRo.contains(state.exerciseSearchQuery, ignoreCase = true) ||
            ex.localizedPrimaryMuscles(state.language).contains(state.exerciseSearchQuery, ignoreCase = true) ||
            ex.localizedEquipment(state.language).contains(state.exerciseSearchQuery, ignoreCase = true) ||
            ex.primaryMusclesHu.contains(state.exerciseSearchQuery, ignoreCase = true) ||
            ex.equipmentHu.contains(state.exerciseSearchQuery, ignoreCase = true)
        matchesCategory && matchesSearch && (!state.favoriteExercisesOnly || ex.id in state.favoriteExerciseIds)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = TrainPilotCardSpacing.ScreenGutter, end = TrainPilotCardSpacing.ScreenGutter, top = 4.dp, bottom = 24.dp)
    ) {
        // Matches Mockup 1/4 #5 "Gyakorlatok" (106-os gyakorlat-adatbázis kategóriák szerint)
        SubScreenTopBar(
            title = "${strings.exercisePickerTitle} (${state.exercises.size})",
            onBack = onBack,
            onOpenSettings = onOpenSettings
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CompactJournalSearchField(state.exerciseSearchQuery, onSearchQueryChange, strings.logSearchHint)
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = state.favoriteExercisesOnly, onClick = onToggleFavoritesOnly,
                label = { Text(trW(state.language, "Kedvencek (${state.favoriteExerciseIds.size})", "Favorites (${state.favoriteExerciseIds.size})", "Favoriten (${state.favoriteExerciseIds.size})", "Favorite (${state.favoriteExerciseIds.size})")) },
                modifier = Modifier.testTag("studio_library_favorites_only"))
                Button(onClick = onOpenAddCustomExercise,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("exercise_library_add_custom_button"),
                    shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Add, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(trW(state.language, "Új egyedi gyakorlat", "New custom exercise", "Neue eigene Übung", "Exercițiu personalizat nou"),
                        fontWeight = FontWeight.Bold)
                }
            }

            // Compact Horizontal Category Chips with exact counts: Összes (106) | Láb | Mell | Hát | Váll | Kar | Core
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ExerciseCategory.entries.forEach { cat ->
                    val selected = state.selectedExerciseCategory == cat
                    val catCount = if (cat == ExerciseCategory.ALL) {
                        state.exercises.size
                    } else {
                        state.exercises.count { it.category == cat }
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (selected) palette.primary else palette.surface,
                        modifier = Modifier
                            .heightIn(min = 36.dp)
                            .trainPilotBorder(
                                1.dp,
                                if (selected) palette.primary else palette.cardBorder,
                                RoundedCornerShape(16.dp)
                            )
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelectCategory(cat) }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${cat.localized(state.language)} ($catCount)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) palette.onPrimary else palette.textPrimary
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = trW(state.language, "${filtered.size} / ${state.exercises.size} gyakorlat", "${filtered.size} / ${state.exercises.size} exercises", "${filtered.size} / ${state.exercises.size} Übungen", "${filtered.size} / ${state.exercises.size} exerciții"),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textSecondary
                )

            }

            if (state.quickWorkoutQueue.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.primary.copy(alpha = 0.14f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .trainPilotBorder(1.5.dp, palette.primary, RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = trW(state.language, "Edzésre összeállítva (${state.quickWorkoutQueue.size} gyakorlat)", "Queued for workout (${state.quickWorkoutQueue.size} exercises)", "Für Training zusammengestellt (${state.quickWorkoutQueue.size} Übungen)", "Pregătit pentru antrenament (${state.quickWorkoutQueue.size} exerciții)"),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = palette.primary
                            )
                            Text(
                                text = state.quickWorkoutQueue.joinToString(", ") { it.localizedName(state.language) },
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onStartQueuedWorkout,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.primary,
                                contentColor = palette.onPrimary
                            ),
                            modifier = Modifier
                                .heightIn(min = 36.dp)
                                .testTag("exercise_library_start_queued_workout_button")
                        ) {
                            Text("▶ ${strings.startAction}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }

            // Compact Exercise Cards (single-row layout, clean Hungarian labels, popup video button)
            filtered.forEach { ex ->
                val inQueue = state.quickWorkoutQueue.any { it.id == ex.id }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .trainPilotBorder(
                            if (inQueue) 1.5.dp else 1.dp,
                            if (inQueue) palette.primary else palette.cardBorder,
                            RoundedCornerShape(14.dp)
                        )
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectExercise(ex.id) }
                        .testTag("exercise_library_row_${ex.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = ex.localizedName(state.language),
                                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 18.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .testTag("exercise_library_name_${ex.id}")
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = palette.primary.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = ex.category.localized(state.language),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        fontWeight = FontWeight.SemiBold,
                                        color = palette.primary,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${ex.localizedPrimaryMuscles(state.language)} • ${ex.localizedEquipment(state.language)} • ${ex.defaultSets}×${ex.defaultReps}",
                                style = MaterialTheme.typography.labelSmall.copy(lineHeight = 14.sp),
                                color = palette.textSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("exercise_library_meta_${ex.id}")
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        val favorite = ex.id in state.favoriteExerciseIds
                        IconToggleButton(checked = favorite, onCheckedChange = { onToggleFavorite(ex.id) },
                            enabled = !state.libraryMutationBusy,
                            modifier = Modifier.sizeIn(minWidth = 40.dp, minHeight = 48.dp).testTag("exercise_library_favorite_${ex.id}")) {
                            Icon(if (favorite) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = if (favorite) trW(state.language, "Eltávolítás a kedvencekből", "Remove from favorites", "Aus Favoriten entfernen", "Elimină din favorite")
                                else trW(state.language, "Hozzáadás a kedvencekhez", "Add to favorites", "Zu Favoriten hinzufügen", "Adaugă la favorite"),
                                tint = if (favorite) palette.primary else palette.textSecondary,
                                modifier = Modifier.size(20.dp))
                        }
                        // Video popup button (opens modal dialog ONLY when clicked)
                        SmallExerciseVideoButton(
                            exerciseName = ex.localizedName(state.language),
                            compactIconOnly = true,
                            exerciseId = ex.id
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        // Compact Add to workout queue button (+)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (inQueue) palette.primary else palette.surfaceElevated,
                            modifier = Modifier
                                .sizeIn(minWidth = 34.dp, minHeight = 36.dp)
                                .trainPilotBorder(
                                    1.dp,
                                    if (inQueue) palette.primary else palette.cardBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onAddExerciseToQueue(ex) }
                                .testTag("exercise_library_add_to_queue_${ex.id}")
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                                Text(
                                    text = if (inQueue) "✓" else "+",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (inQueue) palette.onPrimary else palette.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        // Compact Immediate Start button (▶)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = palette.primary,
                            modifier = Modifier
                                .sizeIn(minWidth = 34.dp, minHeight = 36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onStartExerciseNow(ex) }
                                .testTag("exercise_library_start_now_${ex.id}")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "▶",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = palette.onPrimary
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
fun ExerciseDetailScreen(
    state: TrainPilotUiState,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onAddExerciseToWorkout: (ExerciseItem) -> Unit,
    onStartDemoWithExercise: () -> Unit,
    onToggleFavorite: (String) -> Unit = {},
    customExerciseActions: @Composable () -> Unit = {}
) {
    val palette = LocalTrainPilotPalette.current
    val strings = LocalTrainPilotStrings.current
    val openVideoModal = LocalOpenExerciseVideo.current
    val exercise = state.exercises.firstOrNull { it.id == state.selectedExerciseId }
        ?: state.exercises.firstOrNull()
        ?: ExerciseItem(
            id = "default_ex",
            nameHu = "Gyakorlat",
            nameEn = "Exercise",
            nameDe = "Übung",
            nameRo = "Exercițiu",
            category = ExerciseCategory.CHEST,
            equipmentHu = "Súlyzó",
            primaryMusclesHu = "Mellkas",
            secondaryMusclesHu = "Tricepsz",
            defaultSets = 3,
            defaultReps = 10,
            defaultWeightKg = 20f,
            defaultRestSec = 90
        )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        // Matches Mockup 1/4 #6 "Gyakorlat részletei"
        SubScreenTopBar(
            title = strings.detailsAction,
            onBack = onBack,
            onOpenSettings = onOpenSettings
        )

        Column(
            modifier = Modifier.padding(horizontal = TrainPilotCardSpacing.ScreenGutter),
            verticalArrangement = Arrangement.spacedBy(TrainPilotCardSpacing.CardGap)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = exercise.localizedName(state.language),
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SmallExerciseVideoButton(exerciseName = exercise.localizedName(state.language), exerciseId = exercise.id)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = palette.primary.copy(alpha = 0.18f),
                        modifier = Modifier.trainPilotBorder(1.dp, palette.primary, RoundedCornerShape(12.dp))
                    ) {
                        Text(
                            text = exercise.category.localized(state.language),
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            TrainPilotOutlinedButton(onClick = { onToggleFavorite(exercise.id) },
                enabled = !state.libraryMutationBusy, modifier = Modifier.fillMaxWidth().testTag("studio_exercise_favorite")) {
                Text(if (exercise.id in state.favoriteExerciseIds)
                    trW(state.language, "★ Eltávolítás a kedvencekből", "★ Remove from favorites", "★ Aus Favoriten entfernen", "★ Elimină din favorite")
                else
                    trW(state.language, "☆ Hozzáadás a kedvencekhez", "☆ Add to favorites", "☆ Zu Favoriten hinzufügen", "☆ Adaugă la favorite"))
            }
            customExerciseActions()

            // Metadata table: Eszköz, Fő izmok, Másodlagos izmok
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = palette.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
                    .testTag("exercise_detail_metadata_card")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ExerciseMetaRow(label = trW(state.language, "Eszköz", "Equipment", "Gerät", "Echipament"), value = exercise.localizedEquipment(state.language))
                    ExerciseMetaRow(label = trW(state.language, "Fő izmok", "Primary muscles", "Hauptmuskeln", "Mușchi principali"), value = exercise.localizedPrimaryMuscles(state.language))
                    ExerciseMetaRow(label = trW(state.language, "Másodlagos izmok", "Secondary muscles", "Sekundärmuskeln", "Mușchi secundari"), value = exercise.localizedSecondaryMuscles(state.language))
                }
            }

            // Dual Front & Back Anatomical Muscle Map highlighting the target & secondary muscles of this exercise
            val mappedMuscleId = when (exercise.category) {
                ExerciseCategory.LEGS -> "legs"
                ExerciseCategory.CHEST -> "chest"
                ExerciseCategory.BACK -> "back"
                ExerciseCategory.SHOULDERS -> "shoulders"
                ExerciseCategory.ARMS -> "arms"
                ExerciseCategory.CORE -> "core"
                ExerciseCategory.ALL -> "legs"
            }
            val secondaryLower = exercise.secondaryMusclesHu.lowercase()
            val secondaryMuscleIds = buildSet {
                if (secondaryLower.contains("mell")) add("chest")
                if (secondaryLower.contains("hát") || secondaryLower.contains("trapéz") || secondaryLower.contains("rombusz")) add("back")
                if (secondaryLower.contains("váll") || secondaryLower.contains("delta")) add("shoulders")
                if (secondaryLower.contains("láb") || secondaryLower.contains("comb") || secondaryLower.contains("far") || secondaryLower.contains("vádli") || secondaryLower.contains("hajlító")) add("legs")
                if (secondaryLower.contains("tricepsz") || secondaryLower.contains("bicepsz") || secondaryLower.contains("alkar") || secondaryLower.contains("kar")) add("arms")
                if (secondaryLower.contains("törzs") || secondaryLower.contains("has") || secondaryLower.contains("core") || secondaryLower.contains("gerinc")) add("core")
                remove(mappedMuscleId)
            }
            val defaultSets = exercise.defaultSets.coerceAtLeast(1)
            val secondarySets = (defaultSets / 2).coerceAtLeast(1)
            val exerciseMuscleLoads = remember(exercise.id, mappedMuscleId, secondaryMuscleIds, defaultSets) {
                listOf(
                    "chest" to "Mell",
                    "back" to "Hát",
                    "shoulders" to "Váll",
                    "legs" to "Láb",
                    "arms" to "Kar",
                    "core" to "Törzs"
                ).map { (id, labelHu) ->
                    when {
                        id == mappedMuscleId -> com.example.ui.components.MuscleLoadItem(
                            id = id,
                            nameHu = labelHu,
                            setsText = "$defaultSets sorozat",
                            percentage = 100,
                            ratio = 1f,
                            levelLabelHu = "Fő célizom",
                            topExercisesHu = listOf(exercise.nameHu to "$defaultSets sorozat"),
                            exerciseIdToOpen = exercise.id,
                            setsCount = defaultSets
                        )
                        id in secondaryMuscleIds -> com.example.ui.components.MuscleLoadItem(
                            id = id,
                            nameHu = labelHu,
                            setsText = "$secondarySets sorozat",
                            percentage = 50,
                            ratio = 0.5f,
                            levelLabelHu = "Másodlagos izom",
                            topExercisesHu = listOf(exercise.nameHu to "$secondarySets sorozat"),
                            exerciseIdToOpen = exercise.id,
                            setsCount = secondarySets
                        )
                        else -> com.example.ui.components.MuscleLoadItem(
                            id = id,
                            nameHu = labelHu,
                            setsText = "0 sorozat",
                            percentage = 0,
                            ratio = 0f,
                            levelLabelHu = "Nincs terhelés",
                            topExercisesHu = emptyList(),
                            exerciseIdToOpen = "",
                            setsCount = 0
                        )
                    }
                }
            }
            AnatomicalMuscleHeatmapCard(
                title = "${strings.muscleLoadTitle} (${exercise.localizedName(state.language)})",
                subtitle = "${exercise.localizedPrimaryMuscles(state.language)} • ${exercise.localizedSecondaryMuscles(state.language)}",
                highlightedMuscleId = mappedMuscleId,
                realMuscleLoads = exerciseMuscleLoads,
                periodDays = null
            )

            Text(
                text = strings.executionTitle,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = palette.textPrimary
            )

            val steps = exercise.localizedSteps(state.language)
            steps.forEachIndexed { idx, stepText ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = palette.surfaceElevated,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${idx + 1}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = palette.primary
                            )
                        }
                    }
                    Text(
                        text = stepText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TrainPilotOutlinedButton(
                    onClick = { onAddExerciseToWorkout(exercise) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 50.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = palette.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.addToWorkoutAction, color = palette.textPrimary, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { onStartDemoWithExercise() },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primary,
                        contentColor = palette.onPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 50.dp)
                        .testTag("exercise_detail_start_now_button")
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(trW(state.language, "▶ Edzés indítása ezzel", "▶ Start workout with this", "▶ Training damit starten", "▶ Începe antrenamentul cu acesta"), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ExerciseMetaRow(label: String, value: String) {
    val palette = LocalTrainPilotPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary,
            modifier = Modifier.weight(0.42f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = palette.textPrimary,
            modifier = Modifier.weight(0.58f)
        )
    }
}

@Composable
fun AddCustomExerciseModalDialog(
    onDismiss: () -> Unit,
    onSaveCustomExercise: (
        nameHu: String,
        category: ExerciseCategory,
        equipmentHu: String,
        descriptionHu: String,
        videoUrl: String,
        isTimed: Boolean,
        addToCurrentProgramDay: Boolean
    ) -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val strings = LocalTrainPilotStrings.current
    val lang = strings.language
    var nameHu by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ExerciseCategory.LEGS) }
    var categoryDropdownOpen by remember { mutableStateOf(false) }

    val equipmentOptions = listOf(
        "Saját testsúly / Kültéri (pl. Kör futás)" to trW(lang, "Saját testsúly / Kültéri (pl. Kör futás)", "Bodyweight / Outdoor (e.g. Track run)", "Eigengewicht / Outdoor (z. B. Bahnlauf)", "Greutate corporală / Exterior (ex. Alergare pe pistă)"),
        "Kézi súlyzó (Dumbbell)" to trW(lang, "Kézi súlyzó (Dumbbell)", "Dumbbell", "Kurzhantel (Dumbbell)", "Ganteră (Dumbbell)"),
        "Rúd és tárcsa (Barbell)" to trW(lang, "Rúd és tárcsa (Barbell)", "Barbell & plates", "Langhantel & Scheiben (Barbell)", "Haltere și discuri (Barbell)"),
        "Kondigép / Kábel" to trW(lang, "Kondigép / Kábel", "Gym machine / Cable", "Fitnessgerät / Kabelzug", "Aparat sală / Cablu"),
        "Gumiszalag / Kettlebell" to trW(lang, "Gumiszalag / Kettlebell", "Resistance band / Kettlebell", "Widerstandsband / Kettlebell", "Bandă elastică / Kettlebell")
    )
    var selectedEquipment by remember { mutableStateOf(equipmentOptions.first().first) }
    var equipmentDropdownOpen by remember { mutableStateOf(false) }

    var descriptionHu by remember { mutableStateOf("") }
    var videoUrl by remember { mutableStateOf("") }
    var isTimed by remember { mutableStateOf(false) }
    var addToCurrentProgramDay by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surfaceCard,
        titleContentColor = palette.textPrimary,
        textContentColor = palette.textSecondary,
        title = {
            Text(
                text = trW(lang, "Új egyedi gyakorlat hozzáadása", "Add new custom exercise", "Neue eigene Übung hinzufügen", "Adaugă exercițiu personalizat nou"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = palette.textPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = trW(
                        lang,
                        "Adj meg saját gyakorlatot (pl. Kör futás, Saját köredzés), válassz kategóriát és írd le a végrehajtás lépéseit:",
                        "Enter your custom exercise, choose a category, and describe the execution steps:",
                        "Gib deine eigene Übung ein, wähle eine Kategorie und beschreibe die Ausführungsschritte:",
                        "Introdu propriul exercițiu, alege o categorie și descrie pașii de execuție:"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )

                OutlinedTextField(
                    value = nameHu,
                    onValueChange = { nameHu = it },
                    label = { Text(trW(lang, "Gyakorlat neve (pl. Kör futás)", "Exercise name (e.g. Track run)", "Übungsname (z. B. Bahnlauf)", "Numele exercițiului (ex. Alergare pe pistă)")) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_exercise_name_input")
                )

                // Floating Dropdown: Izomcsoport / Kategória
                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = palette.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { categoryDropdownOpen = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(trW(lang, "Izomcsoport / Kategória", "Muscle group / Category", "Muskelgruppe / Kategorie", "Grupă musculară / Categorie"), style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                                Text(selectedCategory.localized(lang), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                            }
                            Icon(
                                imageVector = if (categoryDropdownOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = palette.primary
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = categoryDropdownOpen,
                        onDismissRequest = { categoryDropdownOpen = false },
                        modifier = Modifier.background(palette.surfaceElevated)
                    ) {
                        ExerciseCategory.entries.filter { it != ExerciseCategory.ALL }.forEach { cat ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = cat.localized(lang),
                                        color = if (cat == selectedCategory) palette.primary else palette.textPrimary,
                                        fontWeight = if (cat == selectedCategory) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    selectedCategory = cat
                                    categoryDropdownOpen = false
                                }
                            )
                        }
                    }
                }

                // Floating Dropdown: Eszköz / Típus
                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = palette.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { equipmentDropdownOpen = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(trW(lang, "Eszköz / Mérés jellege", "Equipment / Measurement type", "Gerät / Messart", "Echipament / Tip măsurare"), style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                                val displayEq = equipmentOptions.firstOrNull { it.first == selectedEquipment }?.second ?: selectedEquipment
                                Text(displayEq, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                            }
                            Icon(
                                imageVector = if (equipmentDropdownOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = palette.primary
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = equipmentDropdownOpen,
                        onDismissRequest = { equipmentDropdownOpen = false },
                        modifier = Modifier.background(palette.surfaceElevated)
                    ) {
                        equipmentOptions.forEach { (canonicalEq, displayEq) ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = displayEq,
                                        color = if (canonicalEq == selectedEquipment) palette.primary else palette.textPrimary,
                                        fontWeight = if (canonicalEq == selectedEquipment) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    selectedEquipment = canonicalEq
                                    equipmentDropdownOpen = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = descriptionHu,
                    onValueChange = { descriptionHu = it },
                    label = { Text(trW(lang, "Leírás / Végrehajtási lépések", "Description / Execution steps", "Beschreibung / Ausführungsschritte", "Descriere / Pași de execuție")) },
                    placeholder = { Text(trW(lang, "Pl. 400m egyenletes tempójú kör futás, majd 60 mp séta pihenő...", "E.g. 400m steady pace run, then 60 s walking rest...", "Z. B. 400 m gleichmäßiger Lauf, dann 60 Sek. Gehpause...", "Ex. alergare 400 m ritm constant, apoi 60 sec pauză de mers...")) },
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_exercise_description_input")
                )

                OutlinedTextField(
                    value = videoUrl,
                    onValueChange = { videoUrl = it },
                    label = { Text(trW(lang, "Videó link vagy YouTube azonosító (opcionális)", "Video URL or YouTube ID (optional)", "Video-Link oder YouTube-ID (optional)", "Link video sau ID YouTube (opțional)")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { isTimed = !isTimed }
                ) {
                    Checkbox(checked = isTimed, onCheckedChange = { isTimed = it })
                    Text(trW(lang, "Időalapú gyakorlat (másodperc / perc mérés)", "Timed exercise (seconds / minutes measurement)", "Zeitbasierte Übung (Sekunden-/Minutenmessung)", "Exercițiu cronometrat (măsurare în secunde / minute)"), style = MaterialTheme.typography.bodySmall, color = palette.textPrimary)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { addToCurrentProgramDay = !addToCurrentProgramDay }
                ) {
                    Checkbox(checked = addToCurrentProgramDay, onCheckedChange = { addToCurrentProgramDay = it })
                    Text(trW(lang, "Hozzáadás az aktuálisan szerkesztett programnaphoz", "Add to currently edited program day", "Zum aktuell bearbeiteten Programmtag hinzufügen", "Adaugă la ziua de program editată curent"), style = MaterialTheme.typography.bodySmall, color = palette.textPrimary)
                }
            }
        },
        confirmButton = {
            com.repforge.app.ui.theme.TrainPilotSaveButton(
                onClick = {
                    onSaveCustomExercise(
                        nameHu,
                        selectedCategory,
                        selectedEquipment,
                        descriptionHu,
                        videoUrl,
                        isTimed,
                        addToCurrentProgramDay
                    )
                },
                modifier = Modifier.testTag("custom_exercise_save_button")
            ) {
                Text(strings.saveBtn, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TrainPilotOutlinedButton(onClick = onDismiss) {
                Text(strings.cancelBtn, color = palette.textPrimary)
            }
        }
    )
}

