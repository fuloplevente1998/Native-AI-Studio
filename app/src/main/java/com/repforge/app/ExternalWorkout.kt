package com.repforge.app

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/** A provider session identity is independent of title and editable timestamps.
 * This is a raw import candidate, NOT a completed TrainPilot workout or a dedup decision.
 */
internal data class ExternalWorkoutIdentity(val provider: String, val source: String, val recordId: String)

internal data class ExternalWorkout(
    val identity: ExternalWorkoutIdentity,
    val modifiedMs: Long,
    val startMs: Long,
    val endMs: Long,
    val originalExerciseType: Int?,
    val title: String?,
    val clientRecordId: String?,
    val startZoneOffsetSeconds: Int?,
    val endZoneOffsetSeconds: Int?,
    val segments: JSONArray?,
    val laps: JSONArray?
) {
    // Elapsed time only: not measured active time.
    val elapsedMs: Long get() = endMs - startMs

    fun summary(): JSONObject = JSONObject()
        .put("id", identity.recordId)
        .put("provider", identity.provider)
        .put("source", identity.source)
        .put("start", Instant.ofEpochMilli(startMs).toString())
        .put("end", Instant.ofEpochMilli(endMs).toString())
        .put("durationMinutes", elapsedMs / 60_000L)
        .put("elapsedMs", elapsedMs)
        .put("lastModifiedMs", modifiedMs)
        .put("exerciseType", originalExerciseType ?: JSONObject.NULL)
        .put("title", title ?: "")
        .put("clientRecordId", clientRecordId ?: JSONObject.NULL)
        .put("startZoneOffsetSeconds", startZoneOffsetSeconds ?: JSONObject.NULL)
        .put("endZoneOffsetSeconds", endZoneOffsetSeconds ?: JSONObject.NULL)
        .put("segments", segments ?: JSONObject.NULL)
        .put("laps", laps ?: JSONObject.NULL)

    companion object {
        fun fromRecord(row: JSONObject): ExternalWorkout {
            require(row.getString("type") == "ExerciseSessionRecord")
            val provider = row.getString("channel")
            require(provider == HealthJournalStore.HC || provider == HealthJournalStore.SAMSUNG)
            val start = row.getLong("startMs")
            val end = row.getLong("endMs")
            require(end >= start)
            val data = row.getJSONObject("data")
            return ExternalWorkout(
                ExternalWorkoutIdentity(provider, row.getString("source"), row.getString("recordId")),
                row.getLong("lastModifiedMs"), start, end,
                data.nullableInt("exerciseType"), data.nullableString("title"),
                data.nullableString("clientRecordId"), row.nullableInt("startZoneOffsetSeconds"),
                row.nullableInt("endZoneOffsetSeconds"), data.optJSONArray("segments"), data.optJSONArray("laps")
            )
        }
    }
}

private fun JSONObject.nullableString(key: String): String? =
    if (isNull(key)) null else getString(key).takeIf { it.isNotBlank() }

private fun JSONObject.nullableInt(key: String): Int? = if (isNull(key)) null else getInt(key)

// The SDKs use zero as the default for repetition counts. Do not present that as measured repetitions.
internal fun externalWorkoutSegment(start: Instant, end: Instant, type: Int, repetitions: Int): JSONObject =
    JSONObject().put("startMs", start.toEpochMilli()).put("endMs", end.toEpochMilli())
        .put("segmentType", type).put("repetitions", if (repetitions > 0) repetitions else JSONObject.NULL)

internal fun externalWorkoutLap(start: Instant, end: Instant, meters: Double?): JSONObject =
    JSONObject().put("startMs", start.toEpochMilli()).put("endMs", end.toEpochMilli())
        .put("lengthMeters", meters?.takeIf { it.isFinite() && it > 0 } ?: JSONObject.NULL)

/** Read from the existing durable, generation-protected journal; no secondary sync or duplicate store.
 * Candidate pages are deliberately source-specific; no inferred cross-provider merges.
 */
internal data class ExternalWorkoutPage(val workouts: List<ExternalWorkout>, val next: Long, val nextTime: Long)

internal fun HealthJournalStore.externalWorkoutPage(
    from: String, to: String, ownPackage: String, before: Long = 0L, beforeTime: Long = 0L, limit: Int = 32
): ExternalWorkoutPage {
    val page = page(from, to, HealthJournalStore.HC, "", before, beforeTime, limit,
        recordType = "ExerciseSessionRecord", includeSummaries = false)
    val records = page.getJSONArray("records")
    val workouts = (0 until records.length()).map { records.getJSONObject(it) }
        .filter { it.getString("source") != ownPackage }
        .map(ExternalWorkout::fromRecord)
    return ExternalWorkoutPage(workouts, page.optLong("nextCursor"), page.optLong("nextTime"))
}
