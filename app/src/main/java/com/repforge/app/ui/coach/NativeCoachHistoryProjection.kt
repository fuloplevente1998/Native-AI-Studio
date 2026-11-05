package com.repforge.app.ui.coach

import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.db.WorkoutHistoryEntity
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Read-only reconciliation with already persisted set completion; original JSON is never rewritten.
 * No repetitions, weights, RIR or measurements are filled in from missing values.
 */
internal class NativeCoachHistoryProjection(private val db: TrainPilotDatabase) {
    suspend fun read(row: WorkoutHistoryEntity): JSONObject {
        val raw = JSONObject(row.rawJson)
        val exercises = raw.optJSONArray("exercises") ?: return raw
        val needsCompletion = (0 until exercises.length()).any { index ->
            val sets = exercises.optJSONObject(index)?.optJSONArray("sets")
            (0 until (sets?.length() ?: 0)).any { sets?.optJSONObject(it)?.has("done") == false }
        }
        if (!needsCompletion) return raw
        val persisted = db.workoutHistoryDao().getWorkoutWithDetailsById(row.id) ?: return raw
        for (index in 0 until exercises.length()) {
            val exercise = exercises.optJSONObject(index) ?: continue
            val saved = persisted.exercises.singleOrNull {
                it.exercise.orderIndex == index && it.exercise.exerciseId == exercise.optString("id")
            } ?: continue
            val sets = exercise.optJSONArray("sets") ?: continue
            for (position in 0 until sets.length()) {
                val set = sets.optJSONObject(position) ?: continue
                if (set.has("done")) continue
                val id = set.optString("setId")
                val number = set.optInt("set", set.optInt("number", position + 1))
                val stored = saved.sets.singleOrNull { if (id.isNotBlank()) it.setId == id else it.setNumber == number }
                if (stored != null) set.put("done", stored.done)
            }
        }
        return raw
    }

    companion object {
        /** Same accepted timestamp forms and date-only UTC convention as Coach selection. */
        fun finished(workout: JSONObject): Instant? {
            val stamp = workout.optString("finished")
            return runCatching { Instant.parse(stamp) }.getOrNull()
                ?: runCatching { LocalDateTime.parse(stamp).atZone(ZoneId.systemDefault()).toInstant() }.getOrNull()
                ?: runCatching { LocalDate.parse(stamp).atStartOfDay(ZoneId.of("UTC")).toInstant() }.getOrNull()
        }
    }
}
