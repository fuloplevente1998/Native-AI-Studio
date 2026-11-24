package com.repforge.app

import org.json.JSONObject

/** Only documented public getters. Framework extensions may be absent on older devices.
 * No alpha Jetpack dependency, hidden API access, ordinal conversion or fabricated defaults.
 */
object ExternalSessionEvidence {
    private fun call(target: Any, name: String): Any? = try {
        target.javaClass.getMethod(name).invoke(target)
    } catch (_: ReflectiveOperationException) { null }
      catch (_: SecurityException) { null }
      catch (_: LinkageError) { null }

    @JvmStatic fun putPositive(json: JSONObject, key: String, number: Number?) {
        val value = number?.toDouble() ?: return
        if (value.isFinite() && value > 0) json.put(key, value)
    }

    internal fun platformRpe(target: Any, out: JSONObject) {
        if (call(target, "hasRateOfPerceivedExertion") != true) return
        val value = (call(target, "getRateOfPerceivedExertion") as? Number)?.toDouble() ?: return
        if (value.isFinite() && value in 0.0..10.0) out.put("rpe", value)
    }

    internal fun platformStrength(target: Any, out: JSONObject) {
        call(target, "getWeight")?.let { weight ->
            val grams = (call(weight, "getInGrams") as? Number)?.toDouble()
            if (grams != null && grams.isFinite() && grams >= 0) out.put("weightKg", grams / 1000)
        }
        if (call(target, "hasSetIndex") == true) {
            val index = (call(target, "getSetIndex") as? Number)?.toInt()
            if (index != null && index >= 0) out.put("setIndex", index)
        }
        platformRpe(target, out)
    }

    internal fun directValues(workout: ExternalWorkout): Map<String, Double> = buildMap {
        if (workout.identity.provider == HealthJournalStore.SAMSUNG) {
            val evidence = workout.sessionEvidence
            for (key in listOf("distanceMeters", "averageHeartRate", "minHeartRate", "maxHeartRate",
                "averageSpeedMps", "maxSpeedMps", "vo2Max", "sessionDurationSeconds")) {
                val value = (evidence?.opt(key) as? Number)?.toDouble() ?: continue
                if (!value.isFinite() || value <= 0) continue
                if (key == "sessionDurationSeconds" && value > workout.elapsedMs / 1000.0) continue
                put(key, value)
            }
        }
        // Only explicitly recorded pause segments. No claim about unrecorded breaks.
        val segments = workout.segments
        if (workout.identity.provider == HealthJournalStore.HC && segments != null && segments.length() <= 10_000) {
            val spans = (0 until segments.length()).mapNotNull { i ->
                val row = segments.optJSONObject(i) ?: return@mapNotNull null
                if (!row.optBoolean("pause")) return@mapNotNull null
                val start = row.optLong("startMs", -1); val end = row.optLong("endMs", -1)
                if (start >= workout.startMs && end <= workout.endMs && end > start) start to end else null
            }.sortedBy { it.first }
            var total = 0L; var covered = workout.startMs
            for ((start, end) in spans) {
                if (end > covered) { total += end - maxOf(start, covered); covered = end }
            }
            if (total > 0) put("recordedPauseSeconds", total / 1000.0)
        }
    }
}
