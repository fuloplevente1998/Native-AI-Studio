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

    @Test fun overallAnswersNeverOverwriteExplicitExerciseAnswersAndFallbackOnlyFillsMissingEffort() {
        val raw = workout()
        Feedback.applyExercises(raw, "live", mapOf("squat" to 4), mapOf("squat" to "Saját gyakorlatválasz"), 1)
        val own = exercise(raw).toString()
        for (index in Feedback.efforts.indices) {
            raw.put("feedback", Feedback.chosen(index, "Az egész edzés", 2))
            assertEquals(own, exercise(raw).toString())
            assertEquals("hard", Feedback.readOwnExerciseEffort(exercise(raw)))
            assertEquals("hard", Feedback.readExerciseEffort(exercise(raw), raw))
            assertEquals("Saját gyakorlatválasz", Feedback.readExerciseNote(exercise(raw)))
            val missing = JSONObject().put("id", "unanswered")
            val overall = Feedback.efforts[index].let { if (it == "right") "good" else it }
            assertEquals(overall, Feedback.readExerciseEffort(missing, raw))
            assertFalse(missing.has("effort")) // read-only fallback does not manufacture an exercise answer
        }
    }

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

    @Test fun legacyRatingEffortScoreTranslatedLabelsAndExerciseNotesAreReadConsistentlyByCoach() {
        val v13Workout = workout("v13").put("feedback", JSONObject().put("rating", "right").put("note", "v13 root note"))
        assertEquals("right", Feedback.readWorkoutEffort(v13Workout))
        assertEquals("good", Feedback.readExerciseEffort(exercise(v13Workout), v13Workout))
        assertEquals("v13 root note", Feedback.readWorkoutNote(v13Workout))

        val scoreWorkout = workout("score")
        exercise(scoreWorkout).put("effortScore", 4).put("userNote", "legacy userNote")
        assertEquals("challenging", Feedback.readOwnExerciseEffort(exercise(scoreWorkout)))
        assertEquals("challenging", Feedback.readExerciseEffort(exercise(scoreWorkout), scoreWorkout))
        assertEquals("legacy userNote", Feedback.readExerciseNote(exercise(scoreWorkout)))

        val target = com.repforge.app.ui.coach.NativeCoachExerciseAdvice.Target("reps", min = 8.0, max = 8.0, sets = 1)
        val occRight = com.repforge.app.ui.coach.NativeCoachExerciseAdvice.occurrence(v13Workout, exercise(v13Workout), target)
        assertTrue(occRight.safe)
        assertFalse(occRight.adverse)

        val chosen = Feedback.chosen(2, "Pont jó volt", 1234L)!!
        assertEquals("good", chosen.getString("effort"))
        assertEquals("right", chosen.getString("rating"))
        assertEquals(3, chosen.getInt("effortScore"))
        assertEquals("phone", chosen.getString("source"))
    }

    @Test fun legacyExerciseRirDistinguishesZeroFromMissingAndRejectsOutOfRangeValues() {
        assertNull(Feedback.readLegacyExerciseRir(JSONObject()))
        assertEquals(0, Feedback.readLegacyExerciseRir(JSONObject().put("actualRir", 0)))
        assertEquals(0, Feedback.readLegacyExerciseRir(JSONObject().put("actualRir", "0")))
        assertEquals(3, Feedback.readLegacyExerciseRir(JSONObject().put("feedback", JSONObject().put("rir", 3))))
        assertEquals(10, Feedback.readLegacyExerciseRir(JSONObject().put("feedback", JSONObject().put("rir", "10"))))
        for (bad in listOf<Any>(JSONObject.NULL, "", -1, 11, 2.5, "bad")) {
            assertNull(Feedback.readLegacyExerciseRir(JSONObject().put("actualRir", bad)))
            assertNull(Feedback.readLegacyExerciseRir(JSONObject().put("feedback", JSONObject().put("rir", bad))))
        }
    }
    @Test fun unknownExplicitExerciseEffortDoesNotInheritAConflictingWorkoutAnswer() {
        val raw = workout().put("feedback", JSONObject().put("effort", "good"))
        val ex = exercise(raw).put("effort", "unknown-legacy-answer")
        assertEquals("", Feedback.readExerciseEffort(ex, raw))
        assertEquals("unknown-legacy-answer", ex.getString("effort"))
        ex.remove("effort")
        assertEquals("good", Feedback.readExerciseEffort(ex, raw))
    }

}
