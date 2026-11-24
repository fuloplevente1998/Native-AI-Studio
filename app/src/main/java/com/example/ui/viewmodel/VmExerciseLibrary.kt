package com.example.ui.viewmodel

import com.example.model.ExerciseCategory
import com.example.model.ExerciseItem
import com.example.model.SubScreen
import kotlinx.coroutines.flow.update
import com.repforge.app.NativeDataReplacement
import kotlinx.coroutines.flow.map
import com.repforge.app.data.db.ExerciseLibraryEntity
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.UUID
import com.example.ui.viewmodel.TrainPilotViewModel.CustomExerciseProgramTarget

fun TrainPilotViewModel.deleteStudioCustomExercise(exerciseId: String) {
    if (_uiState.value.exercises.none { it.id == exerciseId && it.isCustom }) return
    mutateStudioLibrary(
        tr("Gyakorlat törölve.", "Exercise deleted.", "Übung gelöscht.", "Exercițiu șters."),
        onCommitted = { it.withoutDeletedCustomExercise(exerciseId) }
    ) { context, db, _ ->
        com.repforge.app.NativeCustomExerciseEditor.delete(context, db, exerciseId)
        null
    }
}

internal fun TrainPilotViewModel.onStudioCustomExerciseDeleted(exerciseId: String) {
    _uiState.update { it.withoutDeletedCustomExercise(exerciseId) }
}

internal fun TrainPilotUiState.withoutDeletedCustomExercise(exerciseId: String) = copy(
    exercises = exercises.filterNot { it.id == exerciseId },
    favoriteExerciseIds = favoriteExerciseIds - exerciseId,
    quickWorkoutQueue = quickWorkoutQueue.filterNot { it.id == exerciseId }
)

internal fun TrainPilotViewModel.customExerciseProgramTargetLabel(): String? = customExerciseProgramTarget?.label

fun TrainPilotViewModel.toggleAddCustomExerciseDialog(show: Boolean, fromProgramEditor: Boolean = false) {
    val state = _uiState.value
    val editorOpen = state.currentSubScreen == SubScreen.PROGRAM_EDITOR ||
        state.inlineEditorProgramId == state.editingProgramId
    val program = if (show && fromProgramEditor && editorOpen) {
        state.programs.firstOrNull { it.id == state.editingProgramId && it.isCustom }
    } else null
    val day = program?.days?.getOrNull(state.editingDayIndex)
    val dataset = programEditorContinuation
    customExerciseProgramTarget = if (program != null && day != null && dataset != null) {
        CustomExerciseProgramTarget(program.id, day.id,
            "${program.localizedName(state.language)} · ${day.localizedDayCode(state.language)}", dataset)
    } else null
    _uiState.update { it.copy(showAddCustomExerciseDialog = show) }
}

fun TrainPilotViewModel.addCustomExercise(
    nameHu: String,
    category: ExerciseCategory,
    equipmentHu: String,
    descriptionHu: String,
    videoUrl: String = "",
    isTimed: Boolean = false,
    addToCurrentProgramDay: Boolean = false
) {
    if (_uiState.value.libraryMutationBusy) return
    val cleanName = nameHu.trim().ifBlank { "Egyedi gyakorlat" }
    val cleanEquip = equipmentHu.trim().ifBlank { "Saját testsúly / Egyedi" }
    val cleanDesc = descriptionHu.trim().ifBlank { "Egyedi felhasználói gyakorlat saját leírással és végrehajtási jegyzettel." }
    val cleanVideoUrl = videoUrl.trim()
    val parsedSteps = cleanDesc
        .split("\n", ".", ";")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .ifEmpty { listOf(cleanDesc) }

    val resolvedCategory = if (category == ExerciseCategory.ALL) ExerciseCategory.CORE else category
    val uniqueSuffix = java.util.UUID.randomUUID().toString().replace("-", "").take(6)
    val lowEquip = cleanEquip.lowercase()
    val isBodyweightOrBand = lowEquip.contains("testsúly") || lowEquip.contains("bodyweight") ||
        lowEquip.contains("gumiszalag") || lowEquip.contains("band") ||
        lowEquip.contains("kültér") || lowEquip.contains("szabadtér") ||
        lowEquip.contains("futócipő") || cleanName.contains("futás", ignoreCase = true)
    val defaultWeight = if (isTimed || isBodyweightOrBand) 0f else 20f
    val resolvedLoadType = when {
        lowEquip.contains("gumiszalag") || lowEquip.contains("band") -> "band"
        isTimed || isBodyweightOrBand -> "bodyweight"
        lowEquip.contains("1 kézisúlyzó") || lowEquip.contains("egykezes") -> "single_dumbbell"
        lowEquip.contains("kézisúlyzó") || lowEquip.contains("kézi súlyzó") || lowEquip.contains("dumbbell") -> "per_hand"
        lowEquip.contains("kétkezes rúd") || lowEquip.contains("barbell") -> "barbell"
        lowEquip.contains("kettlebell") -> "kettlebell"
        lowEquip.contains("csiga") || lowEquip.contains("cable") -> "cable"
        lowEquip.contains("gép") || lowEquip.contains("machine") -> "machine"
        else -> "weight"
    }
    val resolvedRepUnit = if (isTimed) "mp" else "ism."
    val resolvedMeasurementType = if (isTimed) "time" else "reps"
    val defaultRepsCount = if (isTimed) 60 else 12
    val resolvedTargetRepsText = if (isTimed) "${defaultRepsCount} mp" else defaultRepsCount.toString()
    val newExercise = ExerciseItem(
        id = "custom_ex_${System.currentTimeMillis()}_$uniqueSuffix",
        nameHu = cleanName,
        nameEn = cleanName,
        nameDe = cleanName,
        nameRo = cleanName,
        category = resolvedCategory,
        equipmentHu = cleanEquip,
        primaryMusclesHu = resolvedCategory.labelHu,
        secondaryMusclesHu = if (cleanVideoUrl.isNotBlank()) "Videó: $cleanVideoUrl" else "Stabilizáló izmok",
        defaultSets = 3,
        defaultReps = defaultRepsCount,
        defaultWeightKg = defaultWeight,
        defaultRestSec = 90,
        isTimed = isTimed,
        targetSeconds = 60,
        stepsHu = parsedSteps,
        stepsEn = parsedSteps,
        stepsDe = parsedSteps,
        stepsRo = parsedSteps,
        coachStatusHu = "Egyedi gyakorlat",
        coachHintHu = cleanDesc,
        videoUrl = cleanVideoUrl,
        isCustom = true,
        loadType = resolvedLoadType,
        repUnit = resolvedRepUnit,
        measurementType = resolvedMeasurementType,
        targetRepsText = resolvedTargetRepsText
    )

    val origin = _uiState.value
    val target = customExerciseProgramTarget.takeIf { addToCurrentProgramDay }
    val targetProgram = target?.programId
    val targetDay = target?.dayId

    val primaryKey = when (resolvedCategory) {
        ExerciseCategory.CHEST -> "chest"
        ExerciseCategory.BACK -> "back"
        ExerciseCategory.SHOULDERS -> "shoulders"
        ExerciseCategory.ARMS -> "arms"
        ExerciseCategory.LEGS -> "legs"
        ExerciseCategory.CORE, ExerciseCategory.ALL -> "core"
    }
    val rawJson = JSONObject().apply {
        put("id", newExercise.id)
        put("nameHu", cleanName)
        put("equipment", cleanEquip)
        put("target", resolvedCategory.labelHu)
        put("videoUrl", cleanVideoUrl)
        put("custom", true)
    }
    val entity = ExerciseLibraryEntity(
        id = newExercise.id,
        nameHu = cleanName,
        nameEn = cleanName,
        nameDe = cleanName,
        nameRo = cleanName,
        loadType = resolvedLoadType,
        repUnit = resolvedRepUnit,
        measurementType = resolvedMeasurementType,
        defaultSets = newExercise.defaultSets,
        defaultReps = resolvedTargetRepsText,
        defaultWeight = newExercise.defaultWeightKg.toDouble(),
        defaultRestSeconds = newExercise.defaultRestSec,
        primaryMuscle = primaryKey,
        muscleGroupsJson = JSONArray().put(primaryKey).toString(),
        notesHu = cleanDesc,
        notesEn = cleanDesc,
        notesDe = cleanDesc,
        notesRo = cleanDesc,
        videoUrl = cleanVideoUrl.ifBlank { null },
        videoProvider = if (cleanVideoUrl.isNotBlank()) "custom" else null,
        custom = true,
        setRir229 = true,
        rawJson = rawJson.toString()
    )
    mutateStudioLibrary(tr(
        if (addToCurrentProgramDay) "Új gyakorlat elmentve és hozzáadva a programhoz: $cleanName" else "Új gyakorlat elmentve: $cleanName",
        if (addToCurrentProgramDay) "Exercise saved and added to program: $cleanName" else "Exercise saved: $cleanName",
        if (addToCurrentProgramDay) "Übung gespeichert und zum Programm hinzugefügt: $cleanName" else "Übung gespeichert: $cleanName",
        if (addToCurrentProgramDay) "Exercițiu salvat și adăugat în program: $cleanName" else "Exercițiu salvat: $cleanName"),
        onCommitted = { updated ->
            customExerciseProgramTarget = null
            updated.copy(showAddCustomExerciseDialog = false,
                selectedExerciseId = if (updated.isWorkoutSessionActive) updated.selectedExerciseId else entity.id)
        }
    ) { context, db, _ ->
        check(!addToCurrentProgramDay || (!targetProgram.isNullOrBlank() && targetDay != null)) {
            "Program day changed; reopen editor"
        }
        target?.dataset?.requireCurrent(context)
        com.repforge.app.NativeCustomLibraryMutation.addExercise(context, db, entity,
            targetProgram, targetDay, origin.language.name.lowercase(Locale.ROOT),
            com.repforge.app.NativeDataReplacement.continuationForCurrentData(context))
    }
}

// Exercise Library Actions
fun TrainPilotViewModel.updateExerciseSearch(query: String) {
    _uiState.update { it.copy(exerciseSearchQuery = query) }
}

fun TrainPilotViewModel.selectExerciseCategory(cat: ExerciseCategory) {
    _uiState.update { it.copy(selectedExerciseCategory = cat) }
}

fun TrainPilotViewModel.openExerciseDetail(exerciseId: String) {
    if (_uiState.value.currentSubScreen == SubScreen.EXERCISE_DETAIL &&
        _uiState.value.selectedExerciseId == exerciseId) return
    _uiState.update {
        val back = (it.backStack + it.navigationRoute()).takeLast(16)
        it.copy(
            selectedExerciseId = exerciseId,
            currentSubScreen = SubScreen.EXERCISE_DETAIL,
            backStack = back
        )
    }
}
