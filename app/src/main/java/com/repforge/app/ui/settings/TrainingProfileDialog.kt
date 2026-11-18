package com.repforge.app.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.example.localization.LocalTrainPilotStrings
import com.example.localization.TrainPilotStrings
import com.example.model.AppLanguage
import com.example.ui.components.PersonalPlannerModalDialog
import org.json.JSONObject

/**
 * Uses the unified PersonalPlannerModalDialog (dropdown + multi-select equipment)
 * while preserving untouched keys in the saved #128 profile JSON.
 */
@Composable
internal fun TrainingProfileDialog(
    profileJson: String, language: String, saving: Boolean, failed: Boolean,
    onSave: (Map<String, String>) -> Unit, onDismiss: () -> Unit
) {
    val source = remember(profileJson) { runCatching { JSONObject(profileJson) }.getOrDefault(JSONObject()) }
    val appLanguage = remember(language) {
        when (language.lowercase()) {
            "en" -> AppLanguage.EN
            "de" -> AppLanguage.DE
            "ro" -> AppLanguage.RO
            else -> AppLanguage.HU
        }
    }
    val initialHeight = source.optInt("height", 179).coerceIn(140, 215)
    val initialWeight = source.optDouble("weight", 65.0).toFloat().coerceIn(40f, 180f)
    val initialGoal = source.optString("goal", "Izomépítés").ifBlank { "Izomépítés" }
    val initialExperience = source.optString("experience", "Kezdő").ifBlank { "Kezdő" }
    val initialLocation = source.optString("location", "Otthon").ifBlank { "Otthon" }
    val initialEquipment = source.optJSONArray("gear")?.let { arr ->
        (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }.joinToString(", ")
    }?.ifBlank { null } ?: "Kézisúlyzó és pad"
    val initialDays = source.optInt("daysPerWeek", 3).coerceIn(2, 6)
    val initialDuration = source.optInt("minutes", 45).coerceIn(20, 90)
    val initialAvoidAreas = source.optJSONArray("avoidAreas")?.let { arr ->
        (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }.toSet()
    } ?: emptySet()
    val initialExcluded = source.optJSONArray("excluded")?.let { arr ->
        (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }.toSet()
    } ?: emptySet()

    CompositionLocalProvider(LocalTrainPilotStrings provides TrainPilotStrings.forLanguage(appLanguage)) {
        PersonalPlannerModalDialog(
            initialHeightCm = initialHeight,
            initialWeightKg = initialWeight,
            initialGoalHu = initialGoal,
            initialExperienceHu = initialExperience,
            initialLocationHu = initialLocation,
            initialEquipmentHu = initialEquipment,
            initialDaysPerWeek = initialDays,
            initialDurationMin = initialDuration,
            initialExcludedMuscles = initialAvoidAreas,
            initialExcludedExercises = initialExcluded,
            saving = saving,
            failed = failed,
            onSaveAndOpenPlan = { heightCm, weightKg, goalHu, experienceHu, locationHu, equipmentHu, daysPerWeek, durationMin, excludedMuscles, excludedExercises ->
                onSave(
                    mapOf(
                        "height" to heightCm.toString(),
                        "weight" to weightKg.toString(),
                        "goal" to goalHu,
                        "experience" to experienceHu,
                        "location" to locationHu,
                        "gear" to equipmentHu,
                        "daysPerWeek" to daysPerWeek.toString(),
                        "minutes" to durationMin.toString(),
                        "avoidAreas" to excludedMuscles.joinToString(", "),
                        "excluded" to excludedExercises.joinToString(", ")
                    )
                )
            },
            onDismiss = { if (!saving) onDismiss() }
        )
    }
}

