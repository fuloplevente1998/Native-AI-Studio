package com.example.ui.screens

import com.example.ui.theme.trainPilotGlassRim
import com.example.ui.theme.trainPilotGlassMaterial
import com.example.ui.theme.glassTile
import com.example.ui.theme.glassCardFill
import androidx.compose.runtime.key

import com.example.ui.components.trainPilotAnimatedExpansion
import com.repforge.app.ui.theme.trainPilotClickable
import com.example.ui.components.trainPilotReorderItem

import com.repforge.app.ui.theme.TrainPilotCardSpacing

import com.repforge.app.ui.theme.TrainPilotOutlinedButton

import com.repforge.app.ui.theme.trainPilotBorder

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.SmallExerciseVideoButton
import com.example.ui.components.SubScreenTopBar
import com.example.localization.LocalTrainPilotStrings
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.viewmodel.TrainPilotUiState

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
    val pendingPrescriptions = remember(program.id) {
        mutableStateMapOf<Pair<String, String>, com.example.model.StudioProgramPrescriptionDraft>()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Matches Mockup 4/4 #6 "Program szerkesztése"
        Box(Modifier.padding(horizontal = TrainPilotCardSpacing.ScreenGutter, vertical = 8.dp)) {
            SubScreenTopBar(
                title = strings.editProgramTitle,
                onBack = onBack,
                onOpenSettings = onOpenSettings
            )
        }

        Column(
            modifier = Modifier.weight(1f).testTag("program_editor_body").verticalScroll(rememberScrollState())
                .padding(horizontal = TrainPilotCardSpacing.ScreenGutter)
                .padding(bottom = 24.dp)
        ) {
            ProgramEditorContent(
                state = state,
                program = program,
                pendingPrescriptions = pendingPrescriptions,
                showNameField = true,
                onSelectDayIndex = onSelectDayIndex,
                onToggleExpandExercise = onToggleExpandExercise,
                onSaveExercisePrescription = onSaveExercisePrescription,
                onOpenExerciseDetail = onOpenExerciseDetail,
                onOpenExerciseLibrary = onOpenExerciseLibrary,
                onOpenAddCustomExercise = onOpenAddCustomExercise,
                onMoveExercise = onMoveExercise,
                onCopyProgram = onCopyProgram,
                onDeleteProgram = onDeleteProgram,
                onAddDay = onAddDay,
                onDeleteDay = onDeleteDay,
                onRemoveExercise = onRemoveExercise,
                onRenameProgram = onRenameProgram,
                onSaveProgram = onSaveProgram
            )
        }
    }
}

/**
 * Program editor body shared by the full editor route and the inline (card) editor.
 * Same callbacks, drafts and persistence path; only the surrounding chrome differs.
 */
@Composable
internal fun ProgramEditorContent(
    state: TrainPilotUiState,
    program: com.example.model.TrainingProgramModel,
    pendingPrescriptions: androidx.compose.runtime.snapshots.SnapshotStateMap<Pair<String, String>, com.example.model.StudioProgramPrescriptionDraft>,
    showNameField: Boolean,
    onSelectDayIndex: (Int) -> Unit,
    onToggleExpandExercise: (String) -> Unit,
    onSaveExercisePrescription: (String, Int, Int, Float, Int) -> Unit,
    onOpenExerciseDetail: (String) -> Unit,
    onOpenExerciseLibrary: () -> Unit,
    onMoveExercise: (Int, Int) -> Unit,
    onCopyProgram: () -> Unit,
    onDeleteProgram: () -> Unit,
    onAddDay: (String) -> Unit,
    onDeleteDay: () -> Unit,
    onRemoveExercise: (String) -> Unit,
    onRenameProgram: (String) -> Unit,
    onSaveProgram: (String, List<com.example.model.StudioProgramPrescriptionDraft>) -> Unit,
    modifier: Modifier = Modifier,
    onOpenAddCustomExercise: (() -> Unit)? = null
) {
    val palette = LocalTrainPilotPalette.current
    val strings = LocalTrainPilotStrings.current
    val dayIndex = state.editingDayIndex.coerceIn(0, (program.days.size - 1).coerceAtLeast(0))
    val currentDay = program.days.getOrNull(dayIndex)
    val initialProgramName = program.localizedName(state.language)
    var programName by remember(program.id, initialProgramName) { mutableStateOf(initialProgramName) }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(TrainPilotCardSpacing.CardGap)
    ) {
        if (showNameField) com.example.ui.components.TrainPilotTextField(
            value = programName,
            onValueChange = { programName = it },
            modifier = Modifier.fillMaxWidth().testTag("studio_program_name"),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        if (showNameField && programName.trim() != initialProgramName) com.repforge.app.ui.theme.TrainPilotSaveButton(
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
                        .trainPilotClickable(RoundedCornerShape(12.dp)) { onSelectDayIndex(idx) }
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
        val exerciseReorder = com.example.ui.components.rememberTrainPilotReorderState()
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
                color = palette.glassTile(palette.surface),
                modifier = Modifier.trainPilotGlassRim(palette, RoundedCornerShape(16.dp))
                    .fillMaxWidth()
                    .trainPilotReorderItem(exerciseReorder, ex.id, index, layerWhenIdle = false)
                    .trainPilotAnimatedExpansion()
                    .trainPilotBorder(
                        width = if (isExpanded) 1.5.dp else 1.dp,
                        color = if (isExpanded) palette.primary else palette.cardBorder,
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                // Compact row like the program cards: 48 dp touch height, less padding.
                Column(modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 2.dp, bottom = 2.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .trainPilotClickable(RoundedCornerShape(16.dp)) { onToggleExpandExercise(ex.id) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Only reorder control: long press the handle, drag, release (saved once).
                        com.example.ui.components.TrainPilotReorderHandle(
                            state = exerciseReorder,
                            key = ex.id,
                            index = index,
                            lastIndex = totalExercisesInDay - 1,
                            gap = TrainPilotCardSpacing.CardGap,
                            label = trW(state.language, "Sorrend: tartsd nyomva és húzd", "Order: press and hold, then drag", "Reihenfolge: gedrückt halten und ziehen", "Ordine: ține apăsat și trage"),
                            moveUpLabel = trW(state.language, "Feljebb mozgatás", "Move up", "Nach oben", "Mută mai sus"),
                            moveDownLabel = trW(state.language, "Lejjebb mozgatás", "Move down", "Nach unten", "Mută mai jos"),
                            testTag = "editor_exercise_drag_handle_$index",
                            onMove = onMoveExercise,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${index + 1}. ${ex.localizedName(state.language)}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, lineHeight = 18.sp),
                                color = palette.textPrimary,
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            val weightLabel = when {
                                ex.defaultWeightKg > 0f -> "${ex.defaultWeightKg.toString().removeSuffix(".0")} kg"
                                ex.loadType == "bodyweight" -> trW(state.language, "Testsúly", "Bodyweight", "Körpergewicht", "Greutatea corpului")
                                else -> trW(state.language, "Súly nincs megadva", "Load not set", "Gewicht offen", "Greutate nesetată")
                            }
                            val reps = ex.targetRepsText.ifBlank { ex.defaultReps.toString() }
                            Text(
                                text = "${ex.defaultSets} × $reps • $weightLabel • ${ex.defaultRestSec} s",
                                style = MaterialTheme.typography.labelSmall.copy(lineHeight = 14.sp),
                                color = palette.textSecondary,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                        SmallExerciseVideoButton(exerciseName = ex.localizedName(state.language), exerciseId = ex.id)
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                .trainPilotClickable(RoundedCornerShape(8.dp)) { onOpenExerciseDetail(ex.id) }
                                .semantics { contentDescription = "${strings.detailsAction}: ${ex.localizedName(state.language)}" }
                                .testTag("editor_exercise_details_${ex.id}")
                        ) {
                            Icon(Icons.Outlined.Info, contentDescription = null, tint = palette.primary, modifier = Modifier.size(20.dp))
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
                                color = palette.glassTile(palette.surfaceElevated, elevated = true),
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

        if (program.isCustom && currentDay != null && onOpenAddCustomExercise != null) {
            TrainPilotOutlinedButton(
                onClick = onOpenAddCustomExercise,
                enabled = !state.libraryMutationBusy,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .testTag("program_editor_create_custom_exercise")
            ) {
                Text(trW(state.language, "Új saját gyakorlat", "New custom exercise", "Neue eigene Übung", "Exercițiu propriu nou"))
            }
        }

        com.example.ui.components.StudioProgramManagementControls(
            program, state.libraryMutationBusy, currentDay != null,
            onCopyProgram, onDeleteProgram, onAddDay, onDeleteDay,
            onSave = { onSaveProgram(programName, pendingPrescriptions.values.toList()) }
        )

    }
}

@Composable
internal fun EditorStepperBox(
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
            color = palette.glassTile(palette.surfaceElevated, elevated = true),
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
