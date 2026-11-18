package com.repforge.app

import com.repforge.app.data.db.ExerciseLibraryEntity
import org.json.JSONObject
import java.text.Collator
import java.util.Locale

/** Data-only final main tp128Template/NEEDS132/rf148RolePools policy; no JS runtime in the app. */
internal object NativePlannerPolicy {
    private val policy: JSONObject by lazy {
        val input = requireNotNull(javaClass.getResourceAsStream("/native-planner-main-7473.json"))
        JSONObject(input.bufferedReader().use { it.readText() }).also {
            check(it.getJSONObject("source").getString("appBlob") == "b5567576bed9a8f93608a6bf1ed8db1dee4723f4")
        }
    }
    private fun strings(array: org.json.JSONArray?): List<String> =
        (0 until (array?.length() ?: 0)).map { array!!.getString(it) }
    fun equipmentNeeds(id: String): List<String>? =
        policy.getJSONObject("needs").optJSONArray(id)?.let(::strings)

    fun exerciseMetadata(id: String): JSONObject =
        JSONObject(policy.getJSONObject("meta").optJSONObject(id)?.toString() ?: "{}")

    fun metadata(exercise: ExerciseLibraryEntity): JSONObject {
        val raw = JSONObject(exercise.rawJson)
        val defaults = policy.getJSONObject("meta").optJSONObject(exercise.id)
        if (defaults != null) for (key in defaults.keys()) if (!raw.has(key)) raw.put(key, defaults.get(key))
        val fallback = mapOf("movementPattern" to "other", "difficulty" to "beginner", "complexity" to "low",
            "beginnerSafe" to true, "compound" to false)
        for ((key, value) in fallback) if (!raw.has(key) || raw.isNull(key)) raw.put(key, value)
        if (raw.optJSONArray("goalTags") == null) {
            val isolation = raw.optString("movementPattern") in setOf("elbow-flexion", "elbow-extension", "shoulder-isolation",
                "rear-delt", "chest-isolation", "knee-extension", "knee-flexion", "calf")
            raw.put("goalTags", org.json.JSONArray(if (isolation) listOf("muscle", "fitness", "fatloss")
                else listOf("fitness", "muscle", "strength", "fatloss")))
        }
        return raw
    }
    private val anchoredBands = setOf("band-row", "band-lat-pulldown", "band-chest-press", "band-face-pull",
        "band-triceps-pushdown", "band-pallof-press")
    private val rackPresses = setOf("bench-press", "incline-bench-press", "close-grip-bench")

    fun hasEquipment(id: String, needs: List<String>, gear: Set<String>): Boolean {
        if (id in anchoredBands && "band-anchor" !in gear) return false
        if (id == "inverted-row") return "low-bar" in gear
        if (id == "barbell-back-squat" && "rack" !in gear) return false
        if (id in setOf("db-bench-press", "single-arm-db-press", "chest-supported-db-row") &&
            "lifting-bench" !in gear) return false
        if (id in rackPresses && ("rack" !in gear || "lifting-bench" !in gear)) return false
        if (id == "incline-bench-press" && "incline-bench" !in gear) return false
        if (id == "incline-pushup" && "support" !in gear) return false
        return needs.all { it in gear || (it == "dumbbell" && "dumbbells" in gear) ||
            (id == "decline-pushup" && it == "bench" && "step" in gear) }
    }

    fun available(exercise: ExerciseLibraryEntity, gear: Set<String>): Boolean {
        if (exercise.measurementType == "distance") return false
        val raw = metadata(exercise)
        val needs = if (exercise.custom) raw.optJSONArray("gearNeeds") ?: org.json.JSONArray()
            else policy.getJSONObject("needs").optJSONArray(exercise.id) ?: return false
        return hasEquipment(exercise.id, strings(needs), gear)
    }
    fun isExcludedPattern(pattern: String, area: String): Boolean =
        pattern in strings(policy.getJSONObject("areas").optJSONArray(area))

    fun avoided(exercise: ExerciseLibraryEntity, areas: Set<String>): Boolean {
        val pattern = metadata(exercise).optString("movementPattern")
        return areas.any { isExcludedPattern(pattern, it) } ||
            ((pattern.isBlank() || pattern == "other") && exercise.primaryMuscle in areas)
    }
    fun slots(templateId: String): List<Pair<String, List<String>>> {
        val templates = policy.getJSONArray("templates")
        val template = (0 until templates.length()).map { templates.getJSONObject(it) }.first { it.getString("id") == templateId }
        val days = template.getJSONArray("days")
        return (0 until days.length()).map { i -> days.getJSONObject(i).let { it.getString("id") to strings(it.getJSONArray("exercises")) } }
    }
    fun role(exercise: ExerciseLibraryEntity): String? {
        val pattern = metadata(exercise).optString("movementPattern")
        val roles = policy.getJSONObject("roles")
        return roles.keys().asSequence().firstOrNull { pattern in strings(roles.getJSONArray(it)) }
    }
    private fun score(exercise: ExerciseLibraryEntity, profile: JSONObject, role: String): Int {
        val meta = metadata(exercise)
        var score = if (profile.optString("goal") in strings(meta.optJSONArray("goalTags"))) 4 else 0
        if (profile.optString("experience") == "beginner") {
            score += if (meta.optBoolean("beginnerSafe")) 4 else -10
            if (exercise.id in strings(policy.getJSONArray("beginnerFavorites"))) score += 2
        } else if (meta.optString("difficulty") == "intermediate") score += 2
        if (meta.optBoolean("compound") && role in setOf("leg", "hinge", "push", "pull", "shoulder")) score += 2
        if (profile.optString("activity") == "physical" && meta.optString("complexity") != "low") score -= 2
        return score
    }
    fun pool(role: String, eligible: List<ExerciseLibraryEntity>, profile: JSONObject): List<ExerciseLibraryEntity> {
        val patterns = strings(policy.getJSONObject("roles").optJSONArray(role))
        val collator = Collator.getInstance(Locale.ENGLISH)
        return eligible.filter { metadata(it).optString("movementPattern") in patterns }.sortedWith { a, b ->
            val byScore = score(b, profile, role).compareTo(score(a, profile, role))
            if (byScore != 0) byScore else collator.compare(a.id, b.id)
        }
    }
    fun capacity(minutes: Int): Int = when {
        minutes <= 25 -> 3; minutes <= 40 -> 4; minutes <= 55 -> 5; minutes <= 70 -> 6; else -> 7
    }
}
