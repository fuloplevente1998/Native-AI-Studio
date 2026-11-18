package com.example.model

import com.repforge.app.NativePlannerPolicy
import com.repforge.app.NativeTrainingPrescription
import org.json.JSONObject

/** Selection rules for the shared first-run and Home profile planner. */
internal class StudioStarterPlan(
    private val gear: Set<String>,
    private val bodyweight: Boolean,
    private val location: String,
    private val experience: String,
    private val minutes: Int,
    private val excludedMuscles: Set<String>,
    private val excludedExercises: Set<String>
) {
    private val returning = experience == "Újrakezdő"
    private val beginner = returning || experience !in setOf("Középhaladó", "Haladó", "intermediate", "advanced")
    private val advanced = experience in setOf("Haladó", "advanced")
    private val capacity = NativePlannerPolicy.capacity(minutes).coerceAtMost(if (returning) 5 else 7)
    private val rackPresses = setOf("bench-press", "incline-bench-press", "close-grip-bench")

    internal fun eligible(exercise: ExerciseItem): Boolean {
        val needs = NativePlannerPolicy.equipmentNeeds(exercise.id) ?: return false
        if (exercise.measurementType == "distance" ||
            com.repforge.app.NativeOutdoorStart.eligible(exercise.id, exercise.measurementType, exercise.repUnit) ||
            excludedExercises.any { excluded ->
                excluded.isNotBlank() && (exercise.id.equals(excluded, ignoreCase = true) ||
                    exercise.nameHu.startsWith(excluded, ignoreCase = true))
            } ||
            excludedMuscles.any {
                when (it) {
                    "Térdkímélő" -> pattern(exercise) in setOf("squat", "lunge", "knee-extension", "jump")
                    "Alsó hát" -> pattern(exercise) in setOf("hinge", "horizontal-pull", "core-stability")
                    else -> exercise.primaryMusclesHu.contains(it, ignoreCase = true) ||
                        exercise.category == when (it) {
                            "Láb" -> ExerciseCategory.LEGS
                            "Mell" -> ExerciseCategory.CHEST
                            "Hát" -> ExerciseCategory.BACK
                            "Váll" -> ExerciseCategory.SHOULDERS
                            "Kar" -> ExerciseCategory.ARMS
                            "Törzs" -> ExerciseCategory.CORE
                            else -> null
                        }
                }
            }) return false
        if (beginner && !NativePlannerPolicy.exerciseMetadata(exercise.id).optBoolean("beginnerSafe")) return false
        if (exercise.id == "db-bench-press" && "lifting-bench" !in gear) return false
        if (exercise.id in rackPresses && ("rack" !in gear || "lifting-bench" !in gear)) return false
        if (exercise.id == "incline-bench-press" && "incline-bench" !in gear) return false
        if (exercise.id == "incline-pushup" && "support" !in gear) return false
        if (exercise.id == "decline-pushup" && "bench" !in gear && "step" !in gear) return false
        return if (needs.isEmpty()) bodyweight else needs.all { it in gear }
    }

    fun recommend(programs: List<TrainingProgramModel>): TrainingProgramModel? {
        val home = location == "Otthon"
        val gym = location == "Edzőterem"
        val preferred = when {
            returning -> "home-quick-30"
            beginner -> "home-basic"
            else -> "home-level2"
        }
        return programs.asSequence().filter { program ->
            !program.isCustom && program.days.isNotEmpty() && when {
                home -> program.id.startsWith("home-") && (!beginner || program.id != "home-level2")
                gym -> program.id.startsWith("gym-")
                location.startsWith("Szabadtér") -> program.tagsHu.any { it.contains("Szabadtér", ignoreCase = true) }
                else -> true
            } && program.days.all { day ->
                day.exercises.isNotEmpty() && day.exercises.size <= capacity &&
                    minutesFor(day.exercises) <= minutes && day.exercises.all(::eligible)
            }
        }.sortedBy { program ->
            when (program.id) {
                preferred -> 0
                "home-basic" -> 1
                else -> 2
            }
        }.firstOrNull()
    }

    fun build(catalog: List<ExerciseItem>, goal: String): List<List<ExerciseItem>> {
        val available = catalog.filter(::eligible).sortedWith(
            compareBy<ExerciseItem> { NativePlannerPolicy.exerciseMetadata(it.id).optString("complexity") != "low" }
                .thenBy { it.id }
        ).associateBy { it.id }
        val profile = JSONObject()
            .put("goal", when (goal) { "Erőnövelés" -> "strength"; "Izomépítés" -> "muscle"; else -> "fitness" })
            .put("experience", if (beginner) "beginner" else "intermediate")
            .put("minutes", minutes)
        return daySlots.map { slots ->
            val picked = mutableListOf<ExerciseItem>()
            for ((preferences, patterns) in slots) {
                if (picked.size == capacity) break
                val ordered = if (returning && preferences.first() == "pushup") {
                    listOf("incline-pushup", "modified-pushup") + preferences
                } else preferences
                val candidate = ordered.firstNotNullOfOrNull { id -> available[id]?.takeIf { it !in picked } }
                    ?: available.values.firstOrNull { it !in picked && pattern(it) in patterns }
                    ?: continue
                picked += candidate
            }
            val prescribed = picked.map { exercise ->
                val metadata = NativePlannerPolicy.exerciseMetadata(exercise.id).put("id", exercise.id)
                    .put("loadType", exercise.loadType).put("repUnit", exercise.repUnit)
                val recipe = NativeTrainingPrescription.create(metadata, profile)
                val reps = if (exercise.isTimed) exercise.targetSeconds.toString() else recipe.getString("reps")
                val sets = if (advanced && minutes >= 45 &&
                    metadata.optBoolean("compound")) 3 else recipe.getInt("sets")
                exercise.copy(defaultSets = sets, defaultRestSec = recipe.getInt("rest"),
                    defaultReps = reps.takeWhile(Char::isDigit).toIntOrNull() ?: exercise.defaultReps,
                    targetRepsText = reps, defaultWeightKg = 0f)
            }
            prescribed.take(prescribed.indices.firstOrNull { minutesFor(prescribed.take(it + 1)) > minutes } ?: prescribed.size)
        }
    }

    private fun pattern(exercise: ExerciseItem) =
        NativePlannerPolicy.exerciseMetadata(exercise.id).optString("movementPattern")

    private fun minutesFor(exercises: List<ExerciseItem>) = 4.0 + exercises.sumOf {
        val seconds = if (it.isTimed) it.targetSeconds.coerceAtLeast(39) else 39
        val sides = if (it.isBilateral) 2 else 1
        1.0 + (it.defaultSets * seconds * sides + (it.defaultSets - 1) * it.defaultRestSec) / 60.0
    }

    private companion object {
        val daySlots = listOf(
            listOf(
                listOf("goblet-squat", "bodyweight-squat") to setOf("squat"),
                listOf("db-floor-press", "barbell-floor-press", "pushup") to setOf("horizontal-push"),
                listOf("one-arm-row", "band-row", "chest-supported-db-row") to setOf("horizontal-pull", "vertical-pull"),
                listOf("db-rdl", "glute-bridge") to setOf("hinge"),
                listOf("side-plank", "dead-bug") to setOf("core-bracing", "core-stability"),
                listOf("db-ohp") to setOf("vertical-push", "shoulder-isolation"),
                listOf("db-curl") to setOf("elbow-flexion")
            ),
            listOf(
                listOf("bulgarian-split-squat", "reverse-lunge", "goblet-squat", "bodyweight-squat") to setOf("lunge", "squat"),
                listOf("pushup", "modified-pushup", "db-floor-press") to setOf("horizontal-push"),
                listOf("barbell-row", "one-arm-row", "band-row", "chest-supported-db-row") to setOf("horizontal-pull", "vertical-pull"),
                listOf("db-rdl", "single-leg-glute-bridge", "glute-bridge") to setOf("hinge"),
                listOf("bird-dog", "crunch") to setOf("core-stability", "core-flexion"),
                listOf("lateral-raise", "db-ohp") to setOf("shoulder-isolation", "vertical-push"),
                listOf("oh-triceps") to setOf("elbow-extension")
            )
        )
    }
}
