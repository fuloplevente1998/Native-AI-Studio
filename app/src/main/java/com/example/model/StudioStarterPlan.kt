package com.example.model

import com.repforge.app.NativePlannerPolicy
import com.repforge.app.NativeTrainingPrescription
import org.json.JSONObject

/** A missing or outdated reading cannot alter a saved training prescription. */
internal fun suggestGentlerStarterPlan(
    reading: com.repforge.app.healthreadings.RecoveryDayReading?,
    today: java.time.LocalDate = java.time.LocalDate.now()
): Boolean = reading?.let {
    it.day == today && it.state == com.repforge.app.healthreadings.ReadingState.AVAILABLE &&
        it.source != com.repforge.app.healthreadings.RecoverySource.NONE && it.coachScore?.let { score -> score >= 0.0 && score < 55.0 } == true
} ?: false

/** Selection rules for the shared first-run and Home profile planner. */
internal class StudioStarterPlan(
    private val gear: Set<String>,
    private val bodyweight: Boolean,
    private val location: String,
    private val experience: String,
    private val minutes: Int,
    private val excludedMuscles: Set<String>,
    private val excludedExercises: Set<String>,
    private val gentlerStart: Boolean = false
) {
    private val returning = experience == "Újrakezdő"
    private val beginner = returning || experience !in setOf("Középhaladó", "Haladó", "intermediate", "advanced")
    private val advanced = experience in setOf("Haladó", "advanced")
    private val capacity = NativePlannerPolicy.capacity(minutes).coerceAtMost(if (returning) 5 else 7)
    private val outdoor = location.startsWith("Szabadtér")
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
                    "Térdkímélő" -> pattern(exercise) in setOf("squat", "lunge", "knee-extension", "jump") ||
                        exercise.id in setOf("squat-jump", "lunge-jump", "jumping-jacks")
                    "Alsó hát" -> pattern(exercise) in setOf("hinge", "horizontal-pull", "core-stability")
                    else -> exercise.primaryMusclesHu.contains(it, ignoreCase = true) ||
                        exercise.category == muscleGroups[it.lowercase()] ||
                        NativePlannerPolicy.isExcludedPattern(pattern(exercise), areaKeys[it.lowercase()] ?: it.lowercase())
                }
            }) return false
        if (beginner && !NativePlannerPolicy.exerciseMetadata(exercise.id).optBoolean("beginnerSafe")) return false
        if (outdoor && needs.any { it in setOf("machines", "bench", "barbell") }) return false
        return (needs.isNotEmpty() || bodyweight) && NativePlannerPolicy.hasEquipment(exercise.id, needs, gear)
    }

    private fun templates(programs: List<TrainingProgramModel>, goal: String, weeklyDays: Int): List<TrainingProgramModel> {
        val homeIds = if (beginner) listOf(
            if (returning) "home-quick-30" else "home-basic",
            if (returning) "home-basic" else "home-quick-30"
        ) else listOf("home-level2", "home-basic", "home-varied", "home-quick-30")
        val gymIds = when {
            beginner || minutes <= 40 -> listOf("gym-fullbody", "gym-ab")
            goal == "Erőnövelés" && "rack" in gear && "barbell" in gear ->
                listOf("gym-strength-abc", "gym-ab", "gym-fullbody")
            weeklyDays >= 4 && minutes >= 55 -> listOf("gym-upper-lower", "gym-ab", "gym-fullbody", "gym-ppl")
            else -> listOf("gym-ab", "gym-fullbody", "gym-upper-lower", "gym-ppl")
        }
        val specialties = buildList {
            if ("kettlebell" in gear) add("kettlebell-ab")
            if ("bands" in gear) add("bands-ab")
        }
        val priority = when {
            outdoor -> listOf("calisthenics-ab") + specialties
            location == "Edzőterem" -> gymIds
            location == "Vegyes (Otthon + Terem)" && "machines" in gear -> gymIds + homeIds + specialties
            else -> homeIds + specialties
        }
        val byId = programs.filter { !it.isCustom && it.days.isNotEmpty() && it.days.size <= weeklyDays }
            .associateBy { it.id }
        return priority.mapNotNull(byId::get)
    }

    fun recommend(programs: List<TrainingProgramModel>, goal: String, weeklyDays: Int): TrainingProgramModel? =
        templates(programs, goal, weeklyDays).firstOrNull { program ->
            // The profile offers a general training plan. Upper-body specialist presets remain
            // available from Programs, and require lower-body slots when used as a template here.
            generalPatterns.all { group -> program.days.any { day -> day.exercises.any { pattern(it) in group } } } &&
                program.days.all { day ->
                    day.exercises.isNotEmpty() && day.exercises.size <= capacity &&
                        minutesFor(day.exercises) <= minutes && day.exercises.all(::eligible)
                }
        }

    data class Draft(val sourceId: String?, val days: List<List<ExerciseItem>>)

    fun buildFromTemplates(catalog: List<ExerciseItem>, programs: List<TrainingProgramModel>,
        goal: String, weeklyDays: Int): Draft? {
        val homeWithWeights = location == "Otthon" && ("dumbbell" in gear || "dumbbells" in gear)
        if (homeWithWeights) return homeDraft(catalog, goal)

        val available = sortedEligible(catalog).associateBy { it.id }
        val catalogById = catalog.associateBy { it.id }
        val profile = recipeProfile(goal)
        for (template in templates(programs, goal, weeklyDays)) {
            val isSpecialist = template.id in setOf("calisthenics-ab", "bands-ab")
            if (!outdoor && (template.id == "kettlebell-ab" || isSpecialist) &&
                template.days.none { day -> day.exercises.any { available.containsKey(it.id) && NativePlannerPolicy.equipmentNeeds(it.id)?.isNotEmpty() == true } }) continue
            if (template.id.startsWith("home-")) continue // The final fallback uses curated Home A/B.
            val days = template.days.map { day ->
                val slots = day.exercises.map { it.id to substitutionsFor(pattern(it)) }
                val lowerBody = listOf("bodyweight-squat" to generalPatterns[0], "glute-bridge" to generalPatterns[1])
                // These two presets are upper-body focused. Reserve one slot for each lower-body
                // pattern before filling the upper-body slots from the selected source day.
                val candidateSlots = if (isSpecialist) {
                    lowerBody.filter { (_, patterns) -> day.exercises.none { pattern(it) in patterns } } + slots
                } else slots
                val picked = mutableListOf<ExerciseItem>()
                for ((original, patterns) in candidateSlots) {
                    if (picked.size >= capacity) break
                    val sourcePattern = available[original]?.let(::pattern)
                        ?: catalogById[original]?.let(::pattern)
                    val substitutes = available.values.filter { it.id != original && pattern(it) in patterns }
                        .sortedBy { pattern(it) != sourcePattern }
                    val choices = sequenceOf(available[original]).filterNotNull() + substitutes.asSequence()
                    val chosen = choices.filter { ex -> picked.none { it.id == ex.id } }
                        .map { prescribe(it, profile) }
                        .firstOrNull { minutesFor(picked + it) <= minutes }
                    if (chosen != null) picked += chosen
                }
                picked.toList()
            }
            if (days.all { it.size >= 2 }) return Draft(template.id, days)
        }
        return when {
            outdoor || location == "Edzőterem" -> null
            else -> homeDraft(catalog, goal)
        }
    }

    private fun substitutionsFor(pattern: String): Set<String> = when (pattern) {
        "horizontal-pull", "vertical-pull" -> setOf("horizontal-pull", "vertical-pull")
        "core-flexion", "core-bracing", "core-stability" -> setOf("core-flexion", "core-bracing", "core-stability")
        "squat", "lunge" -> setOf("squat", "lunge")
        else -> setOf(pattern)
    }

    private fun homeDraft(catalog: List<ExerciseItem>, goal: String): Draft? =
        build(catalog, goal).takeIf { days -> days.all { it.isNotEmpty() } }?.let { Draft("home-basic", it) }

    private fun sortedEligible(catalog: List<ExerciseItem>) = catalog.filter(::eligible).sortedWith(
        compareBy<ExerciseItem> { NativePlannerPolicy.exerciseMetadata(it.id).optString("complexity") != "low" }
            .thenBy { it.id }
    )

    private fun recipeProfile(goal: String) = JSONObject()
        .put("goal", when (goal) { "Erőnövelés" -> "strength"; "Izomépítés" -> "muscle"; else -> "fitness" })
        .put("experience", if (beginner) "beginner" else "intermediate")
        .put("minutes", minutes)

    private fun prescribe(exercise: ExerciseItem, profile: JSONObject): ExerciseItem {
        val metadata = NativePlannerPolicy.exerciseMetadata(exercise.id).put("id", exercise.id)
            .put("loadType", exercise.loadType).put("repUnit", exercise.repUnit)
        val recipe = NativeTrainingPrescription.create(metadata, profile)
        val reps = if (exercise.isTimed) exercise.targetSeconds.toString() else recipe.getString("reps")
        val sets = when {
            gentlerStart -> 2
            advanced && minutes >= 45 && metadata.optBoolean("compound") -> 3
            else -> recipe.getInt("sets")
        }
        return exercise.copy(defaultSets = sets, defaultRestSec = recipe.getInt("rest"),
            defaultReps = reps.takeWhile(Char::isDigit).toIntOrNull() ?: exercise.defaultReps,
            targetRepsText = reps, defaultWeightKg = 0f)
    }

    fun build(catalog: List<ExerciseItem>, goal: String): List<List<ExerciseItem>> {
        val available = sortedEligible(catalog).associateBy { it.id }
        val profile = recipeProfile(goal)
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
            val prescribed = picked.map { prescribe(it, profile) }
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
        val muscleGroups = mapOf("láb" to ExerciseCategory.LEGS, "mell" to ExerciseCategory.CHEST,
            "hát" to ExerciseCategory.BACK, "váll" to ExerciseCategory.SHOULDERS,
            "kar" to ExerciseCategory.ARMS, "törzs" to ExerciseCategory.CORE)
        val areaKeys = mapOf("láb" to "legs", "mell" to "chest", "hát" to "back", "váll" to "shoulders",
            "kar" to "arms", "törzs" to "core")
        val generalPatterns = listOf(setOf("squat", "lunge"), setOf("hinge", "knee-flexion"),
            setOf("horizontal-push", "vertical-push"), setOf("horizontal-pull", "vertical-pull"))
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
