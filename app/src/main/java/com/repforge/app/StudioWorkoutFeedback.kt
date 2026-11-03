package com.repforge.app

import org.json.JSONObject

/** The six Studio choices use the same machine values as the canonical Coach. */
internal object StudioWorkoutFeedback {
    fun chosen(index: Int, note: String, now: Long): JSONObject? {
        val rating = listOf("easy", "light", "right", "challenging", "hard", "pain").getOrNull(index)
        if (rating == null && note.isBlank()) return null
        return JSONObject().put("note", note).put("updatedAt", now).also {
            if (rating != null) it.put("rating", rating).put("effort", rating)
        }
    }
}
