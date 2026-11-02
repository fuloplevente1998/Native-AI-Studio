package com.repforge.app

import com.repforge.app.data.db.WorkoutHistoryEntity
import org.json.JSONObject
import java.time.Instant

/** Legacy RepForge stores measured watch telemetry under healthWear129, not Health Connect. */
internal object NativeSavedWearHealth {
    fun fromRaw(raw: JSONObject): JSONObject? {
        raw.optJSONObject("wear")?.let { return it }
        raw.optJSONObject("healthSummary")?.let { return it }
        val legacy = raw.optJSONObject("healthWear129") ?: return null
        if (legacy.optString("source") != "wear_health_services" || legacy.optInt("schema") != 1 ||
            legacy.optLong("revision") < 1 || legacy.optString("watchId").isBlank()) return null
        val expected = listOf("syncId", "id", "started", "workout", "dayId")
            .firstNotNullOfOrNull { raw.optString(it).takeIf(String::isNotBlank) } ?: return null
        val actual = legacy.optString("workoutId")
        val generatedId = com.repforge.shared.LegacySyncIdentity18.legacyHistoryId(raw)
        val originalHadNoId = raw.optString("id").let { it.isBlank() || it == generatedId } &&
            raw.optString("syncId").let { it.isBlank() || it == generatedId }
        val same = expected == actual || (originalHadNoId &&
            runCatching { Instant.parse(raw.getString("started")) == Instant.parse(actual) }.getOrDefault(false))
        return legacy.takeIf { same }
    }
    fun read(workout: WorkoutHistoryEntity): JSONObject? =
        workout.wearMetricsJson?.let { runCatching { JSONObject(it) }.getOrNull() }
            ?: runCatching { fromRaw(JSONObject(workout.rawJson)) }.getOrNull()
}
