package com.repforge.app.data.migration

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/** Portable backups keep route references and measurements, but never private coordinate samples. */
internal object NativeBackupPrivacy {
    private val healthKeys = setOf("health240", "healthconnect", "wear", "healthsummary", "healthwear129",
        "nativehealthjournal", "healthledger", "healthjournal", "wellnesslatest", "recoveryhistory")
    fun sanitize(root: JSONObject, includeHealth: Boolean = true): JSONObject = copy(root, includeHealth) as JSONObject
    /** Restore only same-workout, same-window local health into a local install copy, never an upload. */
    fun preserveLocalWorkoutHealth(incoming: JSONObject, local: List<JSONObject>) {
        val rows = incoming.optJSONArray("history") ?: return
        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            val identities = listOf("id", "syncId", "workoutId").map { row.optString(it) }.filter(String::isNotBlank).toSet()
            val old = local.firstOrNull { candidate ->
                listOf("id", "syncId", "workoutId").any { candidate.optString(it).isNotBlank() && candidate.optString(it) in identities } &&
                    candidate.optString("started") == row.optString("started") && candidate.optString("finished") == row.optString("finished") &&
                    com.repforge.shared.LegacySyncIdentity18.canonical(candidate.optJSONArray("activeIntervals")) ==
                    com.repforge.shared.LegacySyncIdentity18.canonical(row.optJSONArray("activeIntervals"))
            } ?: continue
            for (key in old.keys()) if (key.lowercase(Locale.ROOT) in healthKeys || key in setOf("nativeHealthWindowStale", "nativeOriginalHealthWindow"))
                row.put(key, old.get(key))
        }
    }
    private fun copy(value: Any, includeHealth: Boolean): Any = when (value) {
        is JSONObject -> JSONObject().also { target ->
            for (key in value.keys()) {
                val item = value.get(key)
                val normalized = key.lowercase(Locale.ROOT)
                if (!includeHealth && normalized in healthKeys) continue
                if (normalized in setOf("lat", "lon", "lng", "latitude", "longitude") && item is Number) continue
                if (normalized == "coordinates" && item is JSONArray) continue
                target.put(key, copy(item, includeHealth))
            }
        }
        is JSONArray -> JSONArray().also { target -> for (i in 0 until value.length()) target.put(copy(value.get(i), includeHealth)) }
        else -> value
    }
}
