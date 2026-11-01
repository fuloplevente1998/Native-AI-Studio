package com.example.model

import org.json.JSONObject

/** Read only persisted profile fields; UI defaults never establish completion. */
internal object StudioProfileCompletion {
    fun complete(profile: JSONObject): Boolean =
        profile.optDouble("height", Double.NaN).let { it.isFinite() && it in 60.0..260.0 } &&
        profile.optDouble("weight", Double.NaN).let { it.isFinite() && it in 10.0..500.0 } &&
        listOf("goal", "experience", "location").all { profile.optString(it).isNotBlank() } &&
        (profile.optJSONArray("gear")?.length() ?: 0) > 0 &&
        profile.optInt("daysPerWeek") in 1..7 && profile.optInt("minutes") in 5..300
}
