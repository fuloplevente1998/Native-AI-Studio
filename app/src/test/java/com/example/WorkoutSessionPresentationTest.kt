package com.example

import com.example.model.ExerciseSetItem
import com.example.ui.components.exerciseFullyCompleted
import com.repforge.app.StudioWorkoutBridge
import com.repforge.app.StudioWorkoutSessionPresentation
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
class WorkoutSessionPresentationTest {
    @Test fun completionRequiresAllActualSetsAndNeverAnEmptyOrPartialList() {
        val set = ExerciseSetItem(1, 12f, 10, 2, completed = false)
        assertFalse(exerciseFullyCompleted(null))
        assertFalse(exerciseFullyCompleted(emptyList()))
        assertFalse(exerciseFullyCompleted(listOf(set)))
        assertFalse(exerciseFullyCompleted(listOf(set.copy(completed = true), set.copy(setNumber = 2))))
        assertTrue(exerciseFullyCompleted(listOf(set.copy(completed = true), set.copy(setNumber = 2, completed = true))))
    }

    @Test fun canonicalElapsedExcludesPauseSurvivesSerializationAndResumesOnlyExplicitly() {
        val start = Instant.parse("2026-11-05T10:00:00Z")
        val frame = JSONObject().put("active", true).put("started", start.toString())
            .put("activeIntervals", JSONArray().put(JSONObject().put("start", start.toString()).put("end", JSONObject.NULL)))
        assertEquals(60L, StudioWorkoutSessionPresentation.elapsedSeconds(frame, start.plusSeconds(60)))
        val paused = StudioWorkoutBridge.transition(frame, true, start.plusSeconds(75).toEpochMilli())
        val restored = JSONObject(paused.toString())
        assertEquals(75L, StudioWorkoutSessionPresentation.elapsedSeconds(restored, start.plusSeconds(600)))
        assertEquals(75L, StudioWorkoutSessionPresentation.elapsedSeconds(restored, start.plusSeconds(900)))
        val resumed = StudioWorkoutBridge.transition(restored, false, start.plusSeconds(900).toEpochMilli())
        assertEquals(105L, StudioWorkoutSessionPresentation.elapsedSeconds(resumed, start.plusSeconds(930)))
        assertEquals("01:45", StudioWorkoutSessionPresentation.durationLabel(105))
        assertEquals("1:00:05", StudioWorkoutSessionPresentation.durationLabel(3605))
    }

    @Test fun dayComesFromTheActualSessionAndMalformedDataDoesNotCreateElapsedTime() {
        val frame = JSONObject().put("programName", "Otthoni A/B – Alap").put("dayId", "a").put("dayName", "Alap A")
        assertEquals("Alap A", StudioWorkoutSessionPresentation.dayLabel(frame))
        assertEquals("Alap B", StudioWorkoutSessionPresentation.dayLabel(frame.put("dayName", "Alap B")))
        assertNull(StudioWorkoutSessionPresentation.dayLabel(frame.put("quickWorkout", true)))
        assertNull(StudioWorkoutSessionPresentation.elapsedSeconds(frame, Instant.now()))
        val pausedLegacy = JSONObject().put("started", Instant.now().minusSeconds(60).toString()).put("workoutPaused", true)
        assertNull(StudioWorkoutSessionPresentation.elapsedSeconds(pausedLegacy, Instant.now()))
    }
}
