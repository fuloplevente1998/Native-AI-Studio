package com.example.ui.viewmodel

import com.example.model.AppLanguage
import com.example.model.TrainingProgramModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

internal fun TrainPilotViewModel.parseEquipmentSelectionToGearKeys(equipmentHu: String): Pair<Set<String>, Boolean> {
    val tokens = equipmentHu.split(',').map { it.trim().lowercase() }.filter { it.isNotBlank() }
    val gear = linkedSetOf<String>()
    var includeBodyweight = tokens.isEmpty()
    tokens.forEach { t ->
        if (t.contains("saját testsúly") || t == "bodyweight") {
            includeBodyweight = true
        }
        if (t.contains("egykezes") || t.contains("1 db") || t.contains("1 kézisúlyzó") || t == "dumbbell") {
            gear.add("dumbbell")
        }
        if (t.contains("kézisúlyzó") || t.contains("kézi súlyzó") || t.contains("2 db") || t == "dumbbells") {
            gear.add("dumbbell")
            if (!t.contains("egykezes") && !t.contains("1 db") && !t.contains("1 kézisúlyzó")) {
                gear.add("dumbbells")
            }
        }
        if (t.contains("kétkezes rúd") || t.contains("súlyzórúd") || t == "barbell") {
            gear.add("barbell")
        }
        if (t == "pad / szék / kanapé" || t == "bench") {
            gear.add("bench")
            gear.add("support")
        }
        if (t.contains("stabil") || t.contains("támasz") || t.contains("fal") || t == "support") {
            gear.add("support")
        }
        if (t == "fekvenyomó pad" || t == "lifting-bench") gear.addAll(listOf("bench", "lifting-bench"))
        if (t == "állítható fekvenyomó pad" || t == "incline-bench") {
            gear.addAll(listOf("bench", "lifting-bench", "incline-bench"))
        }
        if (t == "biztonsági állvány" || t == "rack") gear.add("rack")
        if (t == "alacsony, stabil rúd" || t == "low-bar") gear.add("low-bar")
        if (t == "biztonságos szalagrögzítő" || t == "band-anchor") gear.add("band-anchor")
        if (t.contains("fellépő") || t.contains("doboz") || t.contains("lépcső") || t == "step") {
            gear.add("step")
        }
        if (t.contains("húzódzkodó") || t == "pullupbar") {
            gear.add("pullupbar")
        }
        if (t.contains("tolódzkodó") || t == "dipbars") {
            gear.add("dipbars")
        }
        if (t.contains("gumiszalag") || t.contains("ellenállás") || t == "bands") {
            gear.add("bands")
        }
        if (t.contains("kettlebell")) {
            gear.add("kettlebell")
        }
        if (t.contains("edzőtermi") || t.contains("csigás") || t.contains("gépek") || t == "machines") {
            gear.add("machines")
        }
        if (t.contains("teljes edzőterem")) {
            includeBodyweight = true
            gear.addAll(listOf("dumbbell", "dumbbells", "barbell", "bench", "support", "step", "pullupbar", "dipbars", "bands", "kettlebell", "machines"))
        }
        if (t.contains("otthoni alap")) {
            includeBodyweight = true
            gear.addAll(listOf("dumbbell", "dumbbells", "bands", "pullupbar", "support", "bench"))
        }
    }
    return gear to includeBodyweight
}

internal fun TrainPilotViewModel.starterPlan(equipment: String, location: String, experience: String, duration: Int,
    muscles: Set<String>, exercises: Set<String>, gentlerStart: Boolean = false): com.example.model.StudioStarterPlan {
    val (gear, bodyweight) = parseEquipmentSelectionToGearKeys(equipment)
    return com.example.model.StudioStarterPlan(gear, bodyweight, location, experience, duration, muscles, exercises, gentlerStart)
}

internal fun TrainPilotViewModel.recommendStarterProgram(equipment: String, location: String, experience: String,
    goal: String, daysPerWeek: Int, duration: Int, muscles: Set<String>, exercises: Set<String>): String? {
    val state = _uiState.value
    return starterPlan(equipment, location, experience, duration, muscles, exercises)
        .recommend(state.programs, goal, daysPerWeek)?.localizedName(state.language)
}

fun TrainPilotViewModel.saveInitialProfileAndPlan(
    heightCm: Int,
    weightKg: Float,
    goalHu: String,
    experienceHu: String,
    locationHu: String,
    equipmentHu: String,
    daysPerWeek: Int,
    durationMin: Int,
    excludedMuscles: Set<String>,
    excludedExercises: Set<String>,
    completeOnboarding: Boolean = false,
    useRecommendedProgram: Boolean = false,
    trainingStyle: String? = null,
    progressionMode: String? = null,
    onSaveResult: ((Boolean) -> Unit)? = null
) {
    val state = _uiState.value
    val planner = starterPlan(equipmentHu, locationHu, experienceHu, durationMin, excludedMuscles, excludedExercises,
        gentlerStart = com.example.model.suggestGentlerStarterPlan(state.todayRecoveryReading))
    val recommended = if (useRecommendedProgram) planner.recommend(state.programs, goalHu, daysPerWeek) else null
    val draft = if (useRecommendedProgram) null
        else planner.buildFromTemplates(state.exercises, state.programs, goalHu, daysPerWeek)
    val newProgram = if (useRecommendedProgram) {
        recommended ?: run { onSaveResult?.invoke(false); return }
    } else {
        val planned = draft ?: run { onSaveResult?.invoke(false); return }
        val source = state.programs.firstOrNull { it.id == planned.sourceId }
        val generatedProgramId = "custom_plan_" + java.util.UUID.randomUUID()
        val gearSummaryTokens = equipmentHu.split(',').map { it.trim() }.filter { it.isNotBlank() }
        val shortGearTag = when {
            gearSummaryTokens.size > 2 -> "${gearSummaryTokens.take(2).joinToString(" + ")} (+${gearSummaryTokens.size - 2})"
            gearSummaryTokens.isNotEmpty() -> gearSummaryTokens.joinToString(" + ")
            else -> locationHu
        }

        val goalEn = com.example.localization.StudioProfileOptionLabels.goal(goalHu, AppLanguage.EN)
        val goalDe = com.example.localization.StudioProfileOptionLabels.goal(goalHu, AppLanguage.DE)
        val goalRo = com.example.localization.StudioProfileOptionLabels.goal(goalHu, AppLanguage.RO)
        val locEn = com.example.localization.StudioProfileOptionLabels.location(locationHu, AppLanguage.EN)
        val locDe = com.example.localization.StudioProfileOptionLabels.location(locationHu, AppLanguage.DE)
        val locRo = com.example.localization.StudioProfileOptionLabels.location(locationHu, AppLanguage.RO)
        val expEn = com.example.localization.StudioProfileOptionLabels.experience(experienceHu, AppLanguage.EN)
        val expDe = com.example.localization.StudioProfileOptionLabels.experience(experienceHu, AppLanguage.DE)
        val expRo = com.example.localization.StudioProfileOptionLabels.experience(experienceHu, AppLanguage.RO)

        TrainingProgramModel(
            id = generatedProgramId,
            nameHu = "Egyéni $goalHu Terv ($locationHu)",
            nameEn = "Personalized $goalEn Plan ($locEn)",
            subtitleHu = "$experienceHu • Heti $daysPerWeek nap • $durationMin perc • $shortGearTag",
            subtitleEn = "$expEn • $daysPerWeek days/week • $durationMin min",
            tagsHu = listOf(goalHu, locationHu, shortGearTag, "Egyéni").distinct(),
            isCustom = true,
            isActive = true,
            days = planned.days.mapIndexed { index, exercises ->
                val letter = 'A' + index
                val templateDay = source?.days?.getOrNull(index)
                com.example.model.ProgramDayModel(
                    id = "${generatedProgramId}_day$letter",
                    dayCode = "$letter nap",
                    titleHu = templateDay?.titleHu ?: "$letter nap",
                    titleEn = templateDay?.titleEn ?: "Day $letter",
                    durationMinutes = durationMin,
                    exercises = exercises,
                    titleDe = templateDay?.titleDe ?: "Tag $letter",
                    titleRo = templateDay?.titleRo ?: "Ziua $letter"
                )
            },
            nameDe = "Individueller $goalDe-Plan ($locDe)",
            nameRo = "Plan personalizat $goalRo ($locRo)",
            subtitleDe = "$expDe • $daysPerWeek Tage/Woche • $durationMin Min.",
            subtitleRo = "$expRo • $daysPerWeek zile/săpt. • $durationMin min",
            tagsEn = listOf(goalEn, locEn, "Custom").distinct(),
            tagsDe = listOf(goalDe, locDe, "Individuell").distinct(),
            tagsRo = listOf(goalRo, locRo, "Personalizat").distinct()
        )

    }
    val updatedPrograms = if (recommended == null) {
        listOf(newProgram) + state.programs.map { it.copy(isActive = false) }
    } else {
        state.programs.map { it.copy(isActive = it.id == newProgram.id) }
    }
    persistProgramsToRoomAsync(JSONObject()
        .put("height", heightCm).put("weight", weightKg.toDouble())
        .put("goal", goalHu).put("experience", experienceHu).put("location", locationHu)
        .put("gear", JSONArray(equipmentHu.split(',').map(String::trim).filter(String::isNotBlank)))
        .put("daysPerWeek", daysPerWeek).put("minutes", durationMin)
        .put("avoidAreas", JSONArray(excludedMuscles.toList()))
        .put("excluded", JSONArray(excludedExercises.toList())),
        completeOnboarding = completeOnboarding,
        proposedPrograms = updatedPrograms,
        profileProgramToWrite = newProgram.id.takeIf { recommended == null },
        profileSourceTemplateId = draft?.sourceId,
        trainingStyle = trainingStyle,
        progressionMode = progressionMode,
        onSaveResult = { success ->
            if (success) _uiState.update {
                it.copy(userHeightCm = heightCm, userWeightKg = weightKg, userFitnessGoalHu = goalHu,
                    userExperienceLevelHu = experienceHu, userWorkoutLocationHu = locationHu,
                    userSelectedEquipmentHu = equipmentHu, userScheduleDaysPerWeek = daysPerWeek,
                    userWorkoutDurationMin = durationMin, userExcludedMusclesHu = excludedMuscles,
                    userExcludedExercisesHu = excludedExercises,
                    programs = if (recommended == null) listOf(newProgram) +
                        it.programs.filterNot { program -> program.id == newProgram.id }
                            .map { program -> program.copy(isActive = false) }
                    else it.programs.map { program -> program.copy(isActive = program.id == newProgram.id) },
                    editingProgramId = newProgram.id)
            }
            onSaveResult?.invoke(success)
        })
}
