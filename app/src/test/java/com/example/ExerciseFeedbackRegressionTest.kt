package com.example

import com.repforge.app.StudioWorkoutFeedback as Feedback
import com.repforge.app.ui.coach.NativeCoachReadiness
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExerciseFeedbackRegressionTest {
    private fun workout(id: String = "live") = JSONObject().put("workoutId", id)
        .put("finished", "2026-11-03T08:00:00Z").put("exercises", JSONArray().put(JSONObject()
            .put("id", "squat").put("unknown", "retain").put("sets", JSONArray().put(JSONObject()
                .put("setId", "stable").put("done", true).put("weight", 25).put("reps", "8").put("actualRir", 0)))))
    private fun exercise(workout: JSONObject) = workout.getJSONArray("exercises").getJSONObject(0)

    @Test fun annotationsMergeOntoLatestCanonicalSetsAndReachCoach() {
        val input = workout()
        Feedback.attachExerciseEdits(input, "live", mapOf("squat" to 5), mapOf("squat" to "Térdfájdalom"))
        val canonical = workout()
        exercise(canonical).getJSONArray("sets").getJSONObject(0).put("weight", 27.5)
        Feedback.mergePendingExerciseEdits(input, canonical, 10)
        val ex = exercise(canonical)
        assertEquals("pain", ex.getString("effort"))
        assertEquals("Térdfájdalom", ex.getJSONObject("feedback").getString("note"))
        assertEquals("retain", ex.getString("unknown"))
        val set = ex.getJSONArray("sets").getJSONObject(0)
        assertEquals(27.5, set.getDouble("weight"), 0.0)
        assertEquals(0, set.getInt("actualRir")); assertEquals("stable", set.getString("setId"))
        assertFalse(canonical.has("studioExerciseFeedback"))
        val result = NativeCoachReadiness.calculate(JSONObject(), JSONObject(), listOf(canonical),
            Instant.parse("2026-11-03T10:00:00Z"), ZoneOffset.UTC)
        assertTrue(result.pain); assertNotNull(result.score)
    }
    @Test fun noSelectionAndWrongWorkoutPreserveExistingMetadataWhileClearIsExplicit() {
        val raw = workout()
        exercise(raw).put("effort", "old-imported-value")
        val before = raw.toString()
        Feedback.applyExercises(raw, "another", mapOf("squat" to 5), emptyMap(), 1)
        Feedback.applyExercises(raw, "live", emptyMap(), emptyMap(), 1)
        assertEquals(before, raw.toString())
        Feedback.applyExercises(raw, "live", mapOf("squat" to -1), emptyMap(), 2)
        assertFalse(exercise(raw).has("effort"))
        assertFalse(exercise(raw).getJSONArray("sets").getJSONObject(0).isNull("actualRir"))
    }
    @Test fun appliedFeedbackLocksAnswerButAllowsAnnotationWithoutReapplyingProgression() {
        val raw = workout().put("feedback", JSONObject().put("applied", true))
        exercise(raw).put("effort", "hard").put("feedback", JSONObject().put("applied", true).put("note", "régi"))
        Feedback.applyExercises(raw, "live", emptyMap(), mapOf("squat" to "új"), 1)
        assertTrue(exercise(raw).getJSONObject("feedback").getBoolean("applied"))
        assertEquals("hard", exercise(raw).getString("effort"))
        assertEquals("új", exercise(raw).getJSONObject("feedback").getString("note"))
        try { Feedback.applyExercises(raw, "live", mapOf("squat" to 0), emptyMap(), 2); fail("Applied answer changed") }
        catch (_: IllegalStateException) { }
    }
}
