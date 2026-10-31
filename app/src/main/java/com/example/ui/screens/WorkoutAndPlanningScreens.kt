package com.example.ui.screens

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
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.ui.components.programEquipmentCategoryLabel
import com.example.localization.LocalTrainPilotStrings
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.viewmodel.TrainPilotUiState

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Gyors edzés Card (Mockup 1/4 #2)
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = palette.surface,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
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
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
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
                        ExercisePostureMiniIllustration(
                            exerciseName = "Goblet guggolás",
                            size = 42.dp
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = palette.primary.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = "106 gyak.",
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
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
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
                                text = "GPS élő",
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
                            .border(
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
                                    text = day.dayCode,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isPrimaryDay) palette.primary else palette.textSecondary
                                )
                                ProgramTypeEmblemIcon(
                                    programTitle = "${activeProgram.nameHu} ${day.titleHu}",
                                    isHighlighted = isPrimaryDay,
                                    size = 34.dp
                                )
                            }
                            Text(
                                text = day.localizedTitle(state.language),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = palette.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = palette.surfaceElevated
                            ) {
                                Text(
                                    text = programEquipmentCategoryLabel(activeProgram.nameHu),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = palette.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                            Text(
                                text = "📋 ${day.exercises.size} gyakorlat • ⏱ kb. ${day.durationMinutes} perc",
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
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
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
                            text = "⚙ Edzés beállításai • ${activeProgram.localizedName(state.language)}",
                            style = MaterialTheme.typography.titleSmall,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "${state.trainingStyleHu} • ${state.progressionRuleHu}",
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
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
                    .padding(20.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Nincs aktív program",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = palette.textPrimary
                    )
                    Text(
                        text = "Válassz egy edzésprogramot a programlistából vagy hozz létre egy újat.",
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
            .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
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
        var customProgSub by remember { mutableStateOf("Saját egyedi összeállítású edzésterv") }
        AlertDialog(
            onDismissRequest = { showCreateCustomProgramDialog = false },
            containerColor = palette.surfaceCard,
            titleContentColor = palette.textPrimary,
            textContentColor = palette.textSecondary,
            title = {
                Text("Új egyedi program létrehozása", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Adj nevet és leírást az új egyedi programodnak (pl. Kör futás & Core, Saját A/B terv):",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                    OutlinedTextField(
                        value = customProgName,
                        onValueChange = { customProgName = it },
                        label = { Text("Program neve (pl. Kör futás & Erő)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customProgSub,
                        onValueChange = { customProgSub = it },
                        label = { Text("Rövid leírás / megjegyzés") },
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
                    Text("Létrehozás és szerkesztés", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCreateCustomProgramDialog = false }) {
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
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Segmented Pill: Alap programok (Left) | Saját programok (Right)
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = palette.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(24.dp))
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    val builtinCount = state.programs.count { !it.isCustom }
                    val customCount = state.programs.count { it.isCustom }
                    val tabs = listOf(
                        false to "Alap programok ($builtinCount)",
                        true to "Saját programok ($customCount)"
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
                    shape = RoundedCornerShape(18.dp),
                    color = palette.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, palette.primary, RoundedCornerShape(18.dp))
                        .clickable { onOpenProgramEditor(activeProgram.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProgramTypeEmblemIcon(
                            programTitle = activeProgram.nameHu,
                            isHighlighted = true,
                            size = 54.dp
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeProgram.localizedName(state.language),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = palette.textPrimary
                            )
                            Text(
                                text = activeProgram.localizedSubtitle(state.language),
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = palette.primary.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = programEquipmentCategoryLabel(activeProgram.nameHu),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${activeProgram.days.size} edzésnap (A/B)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.textSecondary
                                )
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = palette.primary
                                ) {
                                    Text(
                                        text = strings.activeBadgeLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = palette.onPrimary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = palette.textSecondary
                        )
                    }
                }
            }

            Text(
                text = if (state.programsTabCustomOnly) "Saját készítésű és generált tervek" else strings.moreProgramsTitle,
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary
            )

            if (filteredPrograms.isEmpty() && state.programsTabCustomOnly) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Még nincs saját generált programod",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = palette.textPrimary
                        )
                        Text(
                            text = "Készíts személyre szabott edzéstervet a Kezdőprofil (A01) varázslóval vagy hozz létre saját programot!",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(
                            onClick = onOpenPersonalPlanner,
                            colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Kezdőprofil és egyéni terv (A01)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            filteredPrograms.forEachIndexed { index, program ->
                var dragAccumY by remember(program.id, index) { mutableFloatStateOf(0f) }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
                        .clickable { onOpenProgramEditor(program.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Working Reorder Handle (= + ▲ / ▼)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier
                                .padding(end = 6.dp)
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
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                            Icon(
                                imageVector = Icons.Default.DragHandle,
                                contentDescription = "Sorrend módosítása",
                                tint = palette.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "▼",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (index < filteredPrograms.lastIndex) palette.primary else palette.textMuted.copy(alpha = 0.35f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable(enabled = index < filteredPrograms.lastIndex) { onMoveProgramOrder(index, index + 1) }
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }

                        ProgramTypeEmblemIcon(
                            programTitle = program.nameHu,
                            isHighlighted = program.isActive,
                            size = 46.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = program.localizedName(state.language),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = palette.textPrimary
                            )
                            Text(
                                text = program.localizedSubtitle(state.language),
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = palette.surfaceElevated
                            ) {
                                Text(
                                    text = programEquipmentCategoryLabel(program.nameHu),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = palette.primary,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        if (!program.isActive) {
                            Button(
                                onClick = { onActivateProgram(program.id) },
                                modifier = Modifier.heightIn(min = 44.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.primary,
                                    contentColor = palette.onPrimary
                                )
                            ) {
                                Text(strings.activateAction, style = MaterialTheme.typography.labelMedium)
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
                    Text("Új egyedi program", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onOpenAddCustomExercise,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 50.dp)
                        .testTag("programs_create_custom_exercise_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = palette.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Új gyakorlat", style = MaterialTheme.typography.labelLarge, color = palette.primary, fontWeight = FontWeight.Bold)
                }
            }

            OutlinedButton(
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

            OutlinedButton(
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
    onOpenAddCustomExercise: () -> Unit = {}
) {
    val palette = LocalTrainPilotPalette.current
    val strings = LocalTrainPilotStrings.current
    val program = state.programs.firstOrNull { it.id == state.editingProgramId }
        ?: state.programs.firstOrNull()
        ?: com.example.model.TrainingProgramModel(id = "custom_program", nameHu = "Egyéni program", nameEn = "Custom Program", subtitleHu = "Saját program", subtitleEn = "Custom Program", tagsHu = emptyList(), isCustom = true, isActive = false, days = emptyList())
    val dayIndex = state.editingDayIndex.coerceIn(0, (program.days.size - 1).coerceAtLeast(0))
    val currentDay = program.days.getOrNull(dayIndex)
    var programName by remember(program.id) { mutableStateOf(program.nameHu) }

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
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = strings.editProgramTitle,
                style = MaterialTheme.typography.labelLarge,
                color = palette.textSecondary
            )
            OutlinedTextField(
                value = programName,
                onValueChange = { programName = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Segmented A nap | B nap selector (Mockup 4/4 #6)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                program.days.forEachIndexed { idx, d ->
                    val selected = idx == dayIndex
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) palette.primary else palette.surface,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .border(
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
                                text = d.dayCode,
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
                var sets by remember(ex.id, ex.defaultSets) { mutableIntStateOf(ex.defaultSets) }
                var reps by remember(ex.id, ex.defaultReps) { mutableIntStateOf(ex.defaultReps) }
                var weight by remember(ex.id, ex.defaultWeightKg) { mutableFloatStateOf(ex.defaultWeightKg) }
                var restSec by remember(ex.id, ex.defaultRestSec) { mutableIntStateOf(ex.defaultRestSec) }
                var dragAccumY by remember(ex.id, index) { mutableFloatStateOf(0f) }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
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
                            ExercisePostureMiniIllustration(
                                exerciseName = ex.nameHu,
                                size = 42.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
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
                            SmallExerciseVideoButton(exerciseName = ex.localizedName(state.language))
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
                                        modifier = Modifier.weight(1f),
                                        label = strings.setsLabel,
                                        valueText = "$sets",
                                        onMinus = { sets = (sets - 1).coerceAtLeast(1) },
                                        onPlus = { sets = (sets + 1).coerceAtMost(10) }
                                    )
                                    EditorStepperBox(
                                        modifier = Modifier.weight(1f),
                                        label = strings.repsLabel,
                                        valueText = "$reps",
                                        onMinus = { reps = (reps - 1).coerceAtLeast(1) },
                                        onPlus = { reps = (reps + 1).coerceAtMost(50) }
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
                                        onMinus = { weight = (weight - 2f).coerceAtLeast(0f) },
                                        onPlus = { weight += 2f }
                                    )
                                    EditorStepperBox(
                                        modifier = Modifier.weight(1f),
                                        label = "${strings.restLabel} (s)",
                                        valueText = "$restSec",
                                        onMinus = { restSec = (restSec - 15).coerceAtLeast(15) },
                                        onPlus = { restSec = (restSec + 15).coerceAtMost(300) }
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = palette.surfaceElevated,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = ex.equipmentHu,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.textPrimary,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { onToggleExpandExercise(ex.id) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(strings.cancelBtn, color = palette.textPrimary)
                                    }
                                    Button(
                                        onClick = {
                                            onSaveExercisePrescription(ex.id, sets, reps, weight, restSec)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = palette.primary,
                                            contentColor = palette.onPrimary
                                        )
                                    ) {
                                        Text(strings.saveBtn, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // + Gyakorlat hozzáadása a könyvtárból & + Új egyedi gyakorlat létrehozása
            OutlinedButton(
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

            OutlinedButton(
                onClick = onOpenAddCustomExercise,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("editor_add_custom_exercise_button")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = palette.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Új egyedi gyakorlat létrehozása (pl. Kör futás)", color = palette.primary, fontWeight = FontWeight.Bold)
            }

            // Program törlése (DEMO)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = palette.danger.copy(alpha = 0.12f),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .border(1.dp, palette.danger.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                    .clickable { onBack() }
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = null,
                        tint = palette.danger
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.deleteProgramDemoAction,
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.danger
                    )
                }
            }
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
                .border(1.dp, palette.cardBorder, RoundedCornerShape(10.dp))
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
                IconButton(onClick = onPlus, modifier = Modifier.size(48.dp)) {
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
    onActivateProgram: (String) -> Unit = {}
) {
    val palette = LocalTrainPilotPalette.current
    val strings = LocalTrainPilotStrings.current
    val today = remember { java.time.LocalDate.now() }
    var displayedYearMonth by remember { mutableStateOf(java.time.YearMonth.from(today)) }

    val huMonthNames = listOf(
        "Január", "Február", "Március", "Április", "Május", "Június",
        "Július", "Augusztus", "Szeptember", "Október", "November", "December"
    )
    val monthHeaderText = "${displayedYearMonth.year}. ${huMonthNames[displayedYearMonth.monthValue - 1]} 01."
    val dayEvents = state.calendarEvents.filter { it.dayOfMonth == state.selectedCalendarDay }
    val activeProgram = state.programs.firstOrNull { it.isActive } ?: state.programs.firstOrNull()
    val activeProgramTitle = activeProgram?.nameHu ?: "Otthoni A/B – Alap (személyre szabott)"

    var editingEvent by remember { mutableStateOf<com.example.model.CalendarDayEvent?>(null) }
    var movingEvent by remember { mutableStateOf<com.example.model.CalendarDayEvent?>(null) }
    var activeFieldDialog by remember { mutableStateOf<String?>(null) }

    // 1. Modify Event Dialog
    editingEvent?.let { ev ->
        var splitSel by remember(ev.id) { mutableStateOf(ev.splitBadge) }
        var timeSel by remember(ev.id) { mutableStateOf(ev.timeLabel) }
        var durSel by remember(ev.id) { mutableIntStateOf(ev.durationMinutes) }
        AlertDialog(
            onDismissRequest = { editingEvent = null },
            containerColor = palette.surface,
            titleContentColor = palette.textPrimary,
            title = { Text("Edzésnap módosítása (${displayedYearMonth.year}. ${displayedYearMonth.monthValue.toString().padStart(2, '0')}. ${ev.dayOfMonth}.)", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Edzésnap típusa (A / B):", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
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
                                    Text("Alap $sp", fontWeight = FontWeight.Bold, color = if (isSel) palette.onPrimary else palette.textPrimary)
                                }
                            }
                        }
                    }
                    Text("Időpont:", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
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
                    Text("Időtartam (perc):", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
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
                                    Text("$dr p", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (isSel) palette.onPrimary else palette.textPrimary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onModifyCalendarEvent(ev.id, "$activeProgramTitle • Alap $splitSel", splitSel, timeSel, durSel)
                        editingEvent = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary)
                ) {
                    Text("Mentés", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { editingEvent = null }) {
                    Text("Mégse", color = palette.textPrimary)
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
            title = { Text("Edzés áthelyezése (${ev.dayOfMonth}. napról)", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Válaszd ki az új napot ebben a hónapban:", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                    EditorStepperBox(
                        label = "Új naptári nap (1–${displayedYearMonth.lengthOfMonth()})",
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
                                text = "Következő tervezett edzések automatikus eltolása",
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
                    Text("Áthelyezés", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { movingEvent = null }) {
                    Text("Mégse", color = palette.textPrimary)
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
                        "cadence" -> "Tervezési ritmus"
                        "firstDay" -> "Első edzésnap"
                        "startDate" -> "Kezdőnap kiválasztása"
                        "time" -> "Edzés időpontja"
                        "duration" -> "Időtartam (perc)"
                        else -> "Hetek száma"
                    },
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                val options: List<String> = when (fieldKey) {
                    "cadence" -> listOf("Minden második nap", "Heti 3 nap (H-Sze-P)", "Heti 4 nap", "Minden nap")
                    "firstDay" -> listOf("Alap A", "Alap B")
                    "startDate" -> (1..displayedYearMonth.lengthOfMonth()).map { d ->
                        "${displayedYearMonth.year}. ${displayedYearMonth.monthValue.toString().padStart(2, '0')}. ${d.toString().padStart(2, '0')}."
                    }
                    "time" -> listOf("07:00", "08:30", "10:00", "16:00", "17:30", "18:00", "19:30")
                    "duration" -> listOf("30", "45", "60", "75", "90")
                    else -> listOf("1", "2", "4", "6", "8", "12")
                }
                Column(
                    modifier = Modifier
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    options.forEach { opt ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = palette.surfaceElevated,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    when (fieldKey) {
                                        "cadence" -> onUpdatePlanningParams(opt, state.planningFirstDayHu, state.planningStartDate, state.planningTime, state.planningDurationMin, state.planningWeeksCount)
                                        "firstDay" -> onUpdatePlanningParams(state.planningCadenceHu, opt, state.planningStartDate, state.planningTime, state.planningDurationMin, state.planningWeeksCount)
                                        "startDate" -> onUpdatePlanningParams(state.planningCadenceHu, state.planningFirstDayHu, opt, state.planningTime, state.planningDurationMin, state.planningWeeksCount)
                                        "time" -> onUpdatePlanningParams(state.planningCadenceHu, state.planningFirstDayHu, state.planningStartDate, opt, state.planningDurationMin, state.planningWeeksCount)
                                        "duration" -> onUpdatePlanningParams(state.planningCadenceHu, state.planningFirstDayHu, state.planningStartDate, state.planningTime, opt.toIntOrNull() ?: 60, state.planningWeeksCount)
                                        "weeks" -> onUpdatePlanningParams(state.planningCadenceHu, state.planningFirstDayHu, state.planningStartDate, state.planningTime, state.planningDurationMin, opt.toIntOrNull() ?: 4)
                                    }
                                    activeFieldDialog = null
                                }
                        ) {
                            Text(
                                text = opt,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = palette.textPrimary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                OutlinedButton(onClick = { activeFieldDialog = null }) {
                    Text("Bezárás", color = palette.textPrimary)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 8.dp, bottom = 24.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. EXACT SCREENSHOT 7 CALENDAR CARD
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF0E131A),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(22.dp))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header row: [ < ]   2026. Október 01.   [ > ]   [ X ]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF171E27),
                            modifier = Modifier
                                .size(44.dp)
                                .border(1.dp, Color(0xFF252E3B), RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { displayedYearMonth = displayedYearMonth.minusMonths(1) }
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
                                color = Color(0xFF171E27),
                                modifier = Modifier
                                    .size(44.dp)
                                    .border(1.dp, Color(0xFF252E3B), RoundedCornerShape(12.dp))
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { displayedYearMonth = displayedYearMonth.plusMonths(1) }
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
                                    color = Color(0xFF421C22),
                                    modifier = Modifier
                                        .size(42.dp)
                                        .border(1.dp, Color(0xFF6E2B35), RoundedCornerShape(12.dp))
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onBack() }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "✕",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Weekday headers: H   K   Sze   Cs   P   Szo   V
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("H", "K", "Sze", "Cs", "P", "Szo", "V").forEach { d ->
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

                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        for (weekIdx in 0 until (totalCells / 7)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                for (dow in 0..6) {
                                    val cellIndex = weekIdx * 7 + dow
                                    val dayNumber = cellIndex - firstDayOffset + 1
                                    if (dayNumber !in 1..daysInMonth) {
                                        Spacer(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(42.dp)
                                        )
                                    } else {
                                        val isSelected = dayNumber == state.selectedCalendarDay
                                        val evs = state.calendarEvents.filter { it.dayOfMonth == dayNumber }
                                        val completedEv = evs.firstOrNull { it.status == CalendarEventStatus.COMPLETED }
                                        val plannedEv = evs.firstOrNull { it.status == CalendarEventStatus.PLANNED }

                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = when {
                                                plannedEv != null -> Color(0xFF1F2733)
                                                else -> Color(0xFF131922)
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(42.dp)
                                                .border(
                                                    width = if (isSelected) 1.8.dp else 1.dp,
                                                    color = if (isSelected) palette.primary else Color(0xFF222B38),
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { onSelectDay(dayNumber) }
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
                                                    color = Color.White
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

            // 2. INTERACTIVE DAY ACTIONS CARD (Módosítás, Áthelyezés, Törlés, Visszavonás)
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF121820),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${displayedYearMonth.year}. ${displayedYearMonth.monthValue.toString().padStart(2, '0')}. ${state.selectedCalendarDay.toString().padStart(2, '0')}. • Napi műveletek",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = palette.textPrimary
                        )
                        if (state.calendarUndoStack.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = palette.primary.copy(alpha = 0.16f),
                                modifier = Modifier
                                    .border(1.dp, palette.primary, RoundedCornerShape(8.dp))
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onUndoCalendarAction() }
                            ) {
                                Text(
                                    text = "↶ Visszavonás (${state.calendarUndoStack.size})",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = palette.primary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    if (dayEvents.isEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Nincs tervezett edzés ezen a napon.",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary,
                                modifier = Modifier.weight(1f)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("A", "B").forEach { sp ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = palette.surfaceElevated,
                                        modifier = Modifier
                                            .border(1.dp, palette.primary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                onAddPlannedWorkoutOnDay(
                                                    state.selectedCalendarDay,
                                                    sp,
                                                    state.planningTime,
                                                    state.planningDurationMin
                                                )
                                            }
                                    ) {
                                        Text(
                                            text = "+ Alap $sp",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = palette.primary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                        )
                                    }
                                    if (state.calendarUndoStack.isNotEmpty()) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = palette.primary.copy(alpha = 0.14f),
                                            modifier = Modifier
                                                .border(1.dp, palette.primary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { onUndoCalendarAction() }
                                        ) {
                                            Text(
                                                text = "↶ Visszavonás",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = palette.primary,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                            )
                                        }
                                    }
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
                                    .background(Color(0xFF171F2A))
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
                                            text = ev.titleHu,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = palette.textPrimary
                                        )
                                        Text(
                                            text = "${ev.timeLabel} • ~${ev.durationMinutes} perc • ${if (isDone) "Teljesítve ✓" else "Tervezett (${ev.splitBadge} nap)"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isDone) palette.success else palette.primary
                                        )
                                    }
                                    if (isDone) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = palette.primary.copy(alpha = 0.16f),
                                            modifier = Modifier.clickable { onOpenWorkoutDetail() }
                                        ) {
                                            Text(
                                                text = "Napló >",
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
                                        label = "Módosítás",
                                        color = palette.primary,
                                        modifier = Modifier.weight(1f),
                                        onClick = { editingEvent = ev }
                                    )
                                    CalendarActionChip(
                                        label = "Áthelyezés",
                                        color = palette.textPrimary,
                                        modifier = Modifier.weight(1f),
                                        onClick = { movingEvent = ev }
                                    )
                                    CalendarActionChip(
                                        label = "Törlés",
                                        color = palette.danger,
                                        modifier = Modifier.weight(1f),
                                        onClick = { onDeleteCalendarEvent(ev.id) }
                                    )
                                    CalendarActionChip(
                                        label = "Visszavonás",
                                        color = if (state.calendarUndoStack.isNotEmpty()) palette.secondary else palette.textSecondary,
                                        modifier = Modifier.weight(1f),
                                        onClick = { onUndoCalendarAction() }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. EXACT SCREENSHOT 7 MONTHLY PLANNER & REPLAN CARD
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF0E131A),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(22.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = activeProgramTitle,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = palette.textPrimary,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 10.dp)
                        )
                        Button(
                            onClick = { onOpenReplanModal(true) },
                            modifier = Modifier.heightIn(min = 46.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.primary,
                                contentColor = Color(0xFF14181F)
                            )
                        ) {
                            Text(
                                text = "Újratervezés",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold)
                            )
                        }
                    }

                    val monthDates = remember(displayedYearMonth) {
                        (1..displayedYearMonth.lengthOfMonth()).map { d ->
                            "${displayedYearMonth.year}. ${displayedYearMonth.monthValue.toString().padStart(2, '0')}. ${d.toString().padStart(2, '0')}."
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PlannerFieldTile(
                            modifier = Modifier.weight(1f),
                            label = "Tervezési ritmus",
                            value = state.planningCadenceHu,
                            showChevron = true,
                            dropdownOptions = listOf("Minden második nap", "Heti 3 nap (H-Sze-P)", "Heti 4 nap", "Minden nap"),
                            onSelectOption = { opt ->
                                onUpdatePlanningParams(opt, state.planningFirstDayHu, state.planningStartDate, state.planningTime, state.planningDurationMin, state.planningWeeksCount)
                            }
                        )
                        PlannerFieldTile(
                            modifier = Modifier.weight(1f),
                            label = "Első edzésnap",
                            value = state.planningFirstDayHu,
                            showChevron = true,
                            dropdownOptions = listOf("Alap A", "Alap B"),
                            onSelectOption = { opt ->
                                onUpdatePlanningParams(state.planningCadenceHu, opt, state.planningStartDate, state.planningTime, state.planningDurationMin, state.planningWeeksCount)
                            }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PlannerFieldTile(
                            modifier = Modifier.weight(1f),
                            label = "Kezdőnap",
                            value = state.planningStartDate,
                            showCalendarIcon = true,
                            dropdownOptions = monthDates,
                            onSelectOption = { opt ->
                                onUpdatePlanningParams(state.planningCadenceHu, state.planningFirstDayHu, opt, state.planningTime, state.planningDurationMin, state.planningWeeksCount)
                            }
                        )
                        PlannerFieldTile(
                            modifier = Modifier.weight(1f),
                            label = "Időpont",
                            value = state.planningTime,
                            dropdownOptions = listOf("07:00", "08:30", "10:00", "16:00", "17:30", "18:00", "19:30"),
                            onSelectOption = { opt ->
                                onUpdatePlanningParams(state.planningCadenceHu, state.planningFirstDayHu, state.planningStartDate, opt, state.planningDurationMin, state.planningWeeksCount)
                            }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PlannerFieldTile(
                            modifier = Modifier.weight(1f),
                            label = "Időtartam (perc)",
                            value = "${state.planningDurationMin}",
                            dropdownOptions = listOf("30", "45", "60", "75", "90"),
                            onSelectOption = { opt ->
                                onUpdatePlanningParams(state.planningCadenceHu, state.planningFirstDayHu, state.planningStartDate, state.planningTime, opt.toIntOrNull() ?: 60, state.planningWeeksCount)
                            }
                        )
                        PlannerFieldTile(
                            modifier = Modifier.weight(1f),
                            label = "Hetek száma",
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
                            contentColor = Color(0xFF14181F)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = "Edzések megtervezése",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                        )
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
        val selectedProgName = selectedProgObj?.nameHu ?: activeProgramTitle
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
                color = Color(0xFF0E131A),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 20.dp)
                    .border(1.dp, Color(0xFF242E3D), RoundedCornerShape(24.dp))
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
                            text = "Edzések újratervezése",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = Color.White
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF421C22),
                            modifier = Modifier
                                .size(42.dp)
                                .border(1.dp, Color(0xFF6E2B35), RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onOpenReplanModal(false) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "✕",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Text(
                        text = "Bármelyik edzésnappal újratervezhetsz. Programváltáskor a hátralévő tervet az új program váltja fel. A naplózott és aktív edzések megmaradnak.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )

                    // Program selector (floating dropdown)
                    PlannerFieldTile(
                        label = "Program",
                        value = selectedProgName,
                        showChevron = true,
                        dropdownOptions = state.programs.map { it.nameHu },
                        onSelectOption = { chosenName ->
                            state.programs.firstOrNull { it.nameHu == chosenName }?.let { chosen ->
                                selectedProgId = chosen.id
                                onActivateProgram(chosen.id)
                            }
                        }
                    )

                    // Első edzésnap selector (floating dropdown)
                    val firstDayOptions = listOf("Automatikus", "Alap A", "Alap B")
                    PlannerFieldTile(
                        label = "Első edzésnap",
                        value = selectedFirstDay,
                        showChevron = true,
                        dropdownOptions = firstDayOptions,
                        onSelectOption = { selectedFirstDay = it }
                    )

                    // Új kezdőnap selector (floating dropdown)
                    PlannerFieldTile(
                        label = "Új kezdőnap",
                        value = selectedNewStartDate,
                        showCalendarIcon = true,
                        showChevron = true,
                        dropdownOptions = monthDatesForReplan,
                        onSelectOption = { selectedNewStartDate = it }
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Program: $selectedProgName",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color.White
                    )

                    // Shifted sessions preview list with dividers matching Screenshot 8
                    val startD = selectedNewStartDate
                        .split(".")
                        .mapNotNull { it.trim().toIntOrNull() }
                        .lastOrNull() ?: today.dayOfMonth
                    val previewItems = listOf(
                        "Alap B" to "${displayedYearMonth.year}. ${displayedYearMonth.monthValue.toString().padStart(2, '0')}. ${(startD - 2).coerceAtLeast(1).toString().padStart(2, '0')}. ${state.planningTime} → $selectedNewStartDate ${state.planningTime}",
                        "Alap A" to "$selectedNewStartDate ${state.planningTime} → ${displayedYearMonth.year}. ${displayedYearMonth.monthValue.toString().padStart(2, '0')}. ${(startD + 2).coerceAtMost(31).toString().padStart(2, '0')}. ${state.planningTime}",
                        "Alap B" to "${displayedYearMonth.year}. ${displayedYearMonth.monthValue.toString().padStart(2, '0')}. ${(startD + 2).coerceAtMost(31).toString().padStart(2, '0')}. ${state.planningTime} → ${displayedYearMonth.year}. ${displayedYearMonth.monthValue.toString().padStart(2, '0')}. ${(startD + 4).coerceAtMost(31).toString().padStart(2, '0')}. ${state.planningTime}"
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
                                color = Color.White
                            )
                            Text(
                                text = shiftText,
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider(color = Color(0xFF202936))
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = { onApplyReplan(selectedNewStartDate, selectedFirstDay) },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.primary,
                            contentColor = Color(0xFF14181F)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Text(
                            text = "Terv frissítése",
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
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.14f),
        modifier = modifier
            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
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
                color = color,
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
    showChevron: Boolean = false,
    showCalendarIcon: Boolean = false,
    dropdownOptions: List<String>? = null,
    onSelectOption: ((String) -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    val palette = LocalTrainPilotPalette.current
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = palette.textSecondary
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF141B24),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (expanded) palette.primary else Color(0xFF242E3D),
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
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
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
                    containerColor = Color(0xFF0E131A),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, palette.primary.copy(alpha = 0.7f)),
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

@Composable
fun ExerciseLibraryScreen(
    state: TrainPilotUiState,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectCategory: (ExerciseCategory) -> Unit,
    onSelectExercise: (String) -> Unit,
    onOpenAddCustomExercise: () -> Unit = {}
) {
    val palette = LocalTrainPilotPalette.current
    val strings = LocalTrainPilotStrings.current
    val filtered = state.exercises.filter { ex ->
        val matchesCategory = state.selectedExerciseCategory == ExerciseCategory.ALL || ex.category == state.selectedExerciseCategory
        val matchesSearch = state.exerciseSearchQuery.isBlank() ||
            ex.nameHu.contains(state.exerciseSearchQuery, ignoreCase = true) ||
            ex.equipmentHu.contains(state.exerciseSearchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        // Matches Mockup 1/4 #5 "Gyakorlatok"
        SubScreenTopBar(
            title = strings.exercisePickerTitle,
            onBack = onBack,
            onOpenSettings = onOpenSettings
        )

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = state.exerciseSearchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text(strings.logSearchHint, color = palette.textSecondary) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = palette.textSecondary
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Horizontal category chips: Összes | Láb | Mell | Hát | Váll | Kar | Core
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExerciseCategory.entries.forEach { cat ->
                    val selected = state.selectedExerciseCategory == cat
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (selected) palette.primary else palette.surface,
                        modifier = Modifier
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .border(
                                1.dp,
                                if (selected) palette.primary else palette.cardBorder,
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { onSelectCategory(cat) }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = cat.localized(state.language),
                                style = MaterialTheme.typography.labelLarge,
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
                    text = "${state.exercises.size} (${filtered.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.textPrimary
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = palette.primary.copy(alpha = 0.16f),
                    modifier = Modifier
                        .border(1.dp, palette.primary, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onOpenAddCustomExercise() }
                        .testTag("exercise_library_add_custom_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Új egyedi gyakorlat (pl. Kör futás)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.primary
                        )
                    }
                }
            }

            filtered.forEach { ex ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = palette.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
                        .clickable { onSelectExercise(ex.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ExercisePostureMiniIllustration(
                            exerciseName = ex.nameHu,
                            size = 56.dp
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = ex.localizedName(state.language),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.textPrimary
                                )
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = palette.primary.copy(alpha = 0.18f),
                                    modifier = Modifier.border(
                                        1.dp,
                                        palette.primary.copy(alpha = 0.5f),
                                        RoundedCornerShape(10.dp)
                                    )
                                ) {
                                    Text(
                                        text = ex.category.localized(state.language),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = ex.equipmentHu,
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                        SmallExerciseVideoButton(exerciseName = ex.localizedName(state.language))
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = palette.textSecondary
                        )
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
    onStartDemoWithExercise: () -> Unit
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
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = exercise.localizedName(state.language),
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SmallExerciseVideoButton(exerciseName = exercise.localizedName(state.language))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = palette.primary.copy(alpha = 0.18f),
                        modifier = Modifier.border(1.dp, palette.primary, RoundedCornerShape(12.dp))
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

            val (youtubeVideoId, _) = remember(exercise.id, exercise.nameHu) {
                com.example.ui.components.resolveCatalogExerciseVideoAndNotes(
                    exercise.id.ifBlank { exercise.nameHu }
                )
            }

            // Anatomical & Posture Illustration Card (Embedded video is NOT auto-opened inline; clicking the video button opens the popup dialog!)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = palette.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 36.dp, start = 16.dp, end = 16.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ExercisePostureMiniIllustration(
                            exerciseName = exercise.nameHu,
                            size = 112.dp
                        )
                        Canvas(
                            modifier = Modifier
                                .weight(1f)
                                .height(130.dp)
                        ) {
                            val cx1 = size.width * 0.35f
                            val cx2 = size.width * 0.72f
                            val cy = size.height * 0.5f
                            drawCircle(color = Color(0xFF94A3B8), radius = 14f, center = Offset(cx1, cy - 45f))
                            drawLine(
                                color = Color(0xFFCBD5E1),
                                start = Offset(cx1, cy - 28f),
                                end = Offset(cx1, cy + 12f),
                                strokeWidth = 15f,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = Color(0xFFF97316),
                                start = Offset(cx1 - 7f, cy + 12f),
                                end = Offset(cx1 - 14f, cy + 50f),
                                strokeWidth = 12f,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = Color(0xFFF97316),
                                start = Offset(cx1 + 7f, cy + 12f),
                                end = Offset(cx1 + 14f, cy + 50f),
                                strokeWidth = 12f,
                                cap = StrokeCap.Round
                            )
                            drawCircle(color = Color(0xFF94A3B8), radius = 14f, center = Offset(cx2, cy - 20f))
                            drawLine(
                                color = Color(0xFFCBD5E1),
                                start = Offset(cx2, cy - 4f),
                                end = Offset(cx2 - 5f, cy + 26f),
                                strokeWidth = 15f,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = Color(0xFFF97316),
                                start = Offset(cx2 - 5f, cy + 26f),
                                end = Offset(cx2 - 22f, cy + 22f),
                                strokeWidth = 12f,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = Color(0xFFF97316),
                                start = Offset(cx2 - 22f, cy + 22f),
                                end = Offset(cx2 - 15f, cy + 52f),
                                strokeWidth = 10f,
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.45f))
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                            .align(Alignment.TopCenter),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Anatómiai kivitelezés • Videó ID: $youtubeVideoId",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = palette.primary.copy(alpha = 0.22f),
                            modifier = Modifier
                                .border(1.dp, palette.primary, RoundedCornerShape(8.dp))
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { openVideoModal(exercise.localizedName(state.language)) }
                                .testTag("exercise_detail_popup_video_chip")
                        ) {
                            Text(
                                text = "▶ Videó megnyitása",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = palette.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // Metadata table: Eszköz, Fő izmok, Másodlagos izmok
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = palette.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ExerciseMetaRow(label = "Eszköz", value = exercise.equipmentHu)
                    ExerciseMetaRow(label = "Fő izmok", value = exercise.primaryMusclesHu)
                    ExerciseMetaRow(label = "Másodlagos izmok", value = exercise.secondaryMusclesHu)
                }
            }

            // Dual Front & Back Anatomical Muscle Map highlighting the target muscle of this exercise
            val mappedMuscleId = when (exercise.category) {
                ExerciseCategory.LEGS -> "legs"
                ExerciseCategory.CHEST -> "chest"
                ExerciseCategory.BACK -> "back"
                ExerciseCategory.SHOULDERS -> "shoulders"
                ExerciseCategory.ARMS -> "arms"
                ExerciseCategory.CORE -> "core"
                ExerciseCategory.ALL -> "legs"
            }
            AnatomicalMuscleHeatmapCard(
                title = "${strings.muscleLoadTitle} (${exercise.localizedName(state.language)})",
                subtitle = "${exercise.primaryMusclesHu} • ${exercise.secondaryMusclesHu}",
                highlightedMuscleId = mappedMuscleId
            )

            Text(
                text = strings.executionTitle,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = palette.textPrimary
            )

            val steps = exercise.stepsHu.ifEmpty {
                listOf(
                    "Vedd fel a stabil kiinduló testhelyzetet.",
                    "Hajtsd végre a mozdulatot kontrollált tempóban.",
                    "Tartsd meg a feszülést, majd térj vissza a kiinduló helyzetbe."
                )
            }
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
                OutlinedButton(
                    onClick = { openVideoModal(exercise.localizedName(state.language)) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.heightIn(min = 50.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = palette.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("YouTube ${strings.videoLabel}", color = palette.textPrimary)
                }
                Button(
                    onClick = { onAddExerciseToWorkout(exercise) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primary,
                        contentColor = palette.onPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 50.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.addToWorkoutAction, fontWeight = FontWeight.Bold)
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
    var nameHu by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ExerciseCategory.LEGS) }
    var categoryDropdownOpen by remember { mutableStateOf(false) }

    val equipmentOptions = listOf(
        "Saját testsúly / Kültéri (pl. Kör futás)",
        "Kézi súlyzó (Dumbbell)",
        "Rúd és tárcsa (Barbell)",
        "Kondigép / Kábel",
        "Gumiszalag / Kettlebell"
    )
    var selectedEquipment by remember { mutableStateOf(equipmentOptions.first()) }
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
                text = "Új egyedi gyakorlat hozzáadása",
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
                    text = "Adj meg saját gyakorlatot (pl. Kör futás, Saját köredzés), válassz kategóriát és írd le a végrehajtás lépéseit:",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )

                OutlinedTextField(
                    value = nameHu,
                    onValueChange = { nameHu = it },
                    label = { Text("Gyakorlat neve (pl. Kör futás)") },
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
                            .border(1.dp, palette.cardBorder, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { categoryDropdownOpen = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Izomcsoport / Kategória", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                                Text(selectedCategory.labelHu, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                            }
                            Text("▼", color = palette.primary, fontWeight = FontWeight.Bold)
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
                                        text = cat.labelHu,
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
                            .border(1.dp, palette.cardBorder, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { equipmentDropdownOpen = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Eszköz / Mérés jellege", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                                Text(selectedEquipment, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                            }
                            Text("▼", color = palette.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                    DropdownMenu(
                        expanded = equipmentDropdownOpen,
                        onDismissRequest = { equipmentDropdownOpen = false },
                        modifier = Modifier.background(palette.surfaceElevated)
                    ) {
                        equipmentOptions.forEach { eq ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = eq,
                                        color = if (eq == selectedEquipment) palette.primary else palette.textPrimary,
                                        fontWeight = if (eq == selectedEquipment) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    selectedEquipment = eq
                                    equipmentDropdownOpen = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = descriptionHu,
                    onValueChange = { descriptionHu = it },
                    label = { Text("Leírás / Végrehajtási lépések") },
                    placeholder = { Text("Pl. 400m egyenletes tempójú kör futás, majd 60 mp séta pihenő...") },
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_exercise_description_input")
                )

                OutlinedTextField(
                    value = videoUrl,
                    onValueChange = { videoUrl = it },
                    label = { Text("Videó link vagy YouTube azonosító (opcionális)") },
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
                    Text("Időalapú gyakorlat (másodperc / perc mérés)", style = MaterialTheme.typography.bodySmall, color = palette.textPrimary)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { addToCurrentProgramDay = !addToCurrentProgramDay }
                ) {
                    Checkbox(checked = addToCurrentProgramDay, onCheckedChange = { addToCurrentProgramDay = it })
                    Text("Hozzáadás az aktuálisan szerkesztett programnaphoz", style = MaterialTheme.typography.bodySmall, color = palette.textPrimary)
                }
            }
        },
        confirmButton = {
            Button(
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.primary,
                    contentColor = palette.onPrimary
                ),
                modifier = Modifier.testTag("custom_exercise_save_button")
            ) {
                Text("Gyakorlat mentése", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Mégse", color = palette.textPrimary)
            }
        }
    )
}

