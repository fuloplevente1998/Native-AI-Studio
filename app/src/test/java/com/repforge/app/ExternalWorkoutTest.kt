package com.repforge.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExternalWorkoutTest {
    private val start = Instant.parse("2026-11-02T10:00:00Z").toEpochMilli()
    private val end = start + 1_800_000
    private val day = HealthJournalStore.day(start)
    private fun row(id: String, source: String, modified: Long = end, title: String? = null) =
        JSONObject().put("channel", HealthJournalStore.HC).put("source", source)
            .put("type", "ExerciseSessionRecord").put("recordId", id)
            .put("startMs", start).put("endMs", end).put("lastModifiedMs", modified)
            .put("day", day).put("data", JSONObject().put("title", title ?: JSONObject.NULL))

    @Test fun absentMetricsRemainAbsentAndTimeIsElapsed() {
        val workout = ExternalWorkout.fromRecord(row("one", "example.watch"))
        assertNull(workout.originalExerciseType)
        assertNull(workout.segments)
        assertEquals(1_800_000L, workout.elapsedMs)
        assertTrue(workout.summary().isNull("exerciseType"))
        assertTrue(workout.summary().isNull("clientRecordId"))
        assertFalse(workout.summary().has("activeCalories"))
        assertFalse(workout.summary().has("activeDurationMs"))
        assertTrue(externalWorkoutSegment(Instant.ofEpochMilli(start), Instant.ofEpochMilli(end), 1, 0).isNull("repetitions"))
        assertTrue(externalWorkoutLap(Instant.ofEpochMilli(start), Instant.ofEpochMilli(end), null).isNull("lengthMeters"))
    }

    @Test fun existingJournalKeepsIdentityUpdatesDeletesAndSeparateOrigins() {
        HealthJournalStore(ApplicationProvider.getApplicationContext<Context>()).use { store ->
            store.erase()
            try {
                val generation = store.generation()
                val a = row("one", "example.watch")
                val b = row("two", "example.other")
                val own = row("own", "own.package")
                store.importRecords(JSONArray().put(a).put(b).put(own), generation)
                store.importRecords(JSONArray().put(a).put(b), generation)
                fun candidates() = store.externalWorkoutPage(day, day, "own.package").workouts
                assertEquals(2, candidates().size)
                assertNotEquals(candidates()[0].identity, candidates()[1].identity)
                val previous = candidates().first { it.identity.recordId == "one" }
                store.importRecords(JSONArray().put(row("one", "example.watch", end + 1, "edited")), generation)
                store.importRecords(JSONArray().put(a), generation)
                val changed = candidates().first { it.identity.recordId == "one" }
                assertEquals(previous.identity, changed.identity)
                assertEquals("edited", changed.title)
                store.applyChanges(JSONArray(), JSONArray().put(JSONObject().put("recordId", "one").put("deletedMs", end + 2)), generation)
                store.importRecords(JSONArray().put(a), generation)
                assertEquals(listOf("two"), candidates().map { it.identity.recordId })
            } finally { store.erase() }
        }
    }
}
