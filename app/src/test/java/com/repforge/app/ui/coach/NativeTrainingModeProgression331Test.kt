package com.repforge.app.ui.coach

import com.repforge.app.data.store.NativeTrainingModes
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** #331: real progressive loads require evidence; neither modes nor previews fabricate history. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NativeTrainingModeProgression331Test {
    private val exercise = JSONObject().put("id", "dumbbell-press")
        .put("measurementType", "strength").put("repUnit", "reps")
        .put("loadType", "total").put("weight", 5.5)
    private val target = NativeCoachExerciseAdvice.Target("reps", min = 8.0,
        max = 12.0, sets = 2, text = "8-12")
    private val readiness = NativeCoachReadiness.Result(null, null, null, null, null,
        null, null, null, null, 0, false)

    private fun mode(rule: String) = NativeTrainingModes.Effective(programId = "home-basic",
        progressionMode = rule, explicitChoice = true, source = "program")

    private fun done(reps: Double, weight: Double = 5.5, effort: String = "good",
        rir: Int? = null, completed: Boolean = true): NativeCoachExerciseAdvice.Occurrence {
        val set = JSONObject().put("done", completed).put("reps", reps)
            .put("weight", weight)
        rir?.let { set.put("actualRir", it) }
        val ex = JSONObject().put("id", "dumbbell-press")
            .put("sets", JSONArray().put(set).put(JSONObject(set.toString())))
        return NativeCoachExerciseAdvice.Occurrence(
            workout = JSONObject(), exercise = ex,
            reps = if (completed) listOf(reps, reps) else emptyList(),
            weight = weight, allDone = completed, effort = effort,
            hitTop = completed && reps >= 12.0,
            belowMin = completed && reps < 8.0, average = if (completed) reps else null
        )
    }
    private fun recommend(rule: String,
        rows: List<NativeCoachExerciseAdvice.Occurrence>,
        weightStep: Double = .5,
        lib: JSONObject = exercise,
        recovery: NativeCoachReadiness.Result = readiness
    ): NativeTrainingModeProgression.Preview =
        requireNotNull(NativeTrainingModeProgression.compute(
            mode(rule), lib, target, rows, recovery, weightStep))

    @Test fun linearIncreasesAfterOneSafeCompletedSession() {
        val out = recommend("linear", listOf(done(9.0)))
        assertEquals("increase", out.action)
        assertEquals(5.5, out.baselineWeight, 0.001)
        assertEquals(6.0, out.proposedWeight, 0.001)
        assertFalse(out.autoApply)
    }

    @Test fun linearDoesNotIncreaseOnIncompleteOrBelowMinimumSessions() {
        assertEquals("hold", recommend("linear", listOf(done(9.0, completed = false))).action)
        assertEquals("hold", recommend("linear", listOf(done(7.0))).action)
    }

    @Test fun doubleRequiresThreeSafeTopBoundWorkouts() {
        assertEquals("hold", recommend("double", listOf(done(12.0))).action)
        val out = recommend("double", List(3) { done(12.0) })
        assertEquals("increase", out.action)
        assertEquals(6.0, out.proposedWeight, 0.001)
    }

    @Test fun repeatedPoorCompletedWorkoutsProposeAWeightReduction() {
        val out = recommend("double", List(3) { done(6.0, effort = "hard") })
        assertEquals("reduce", out.action)
        assertEquals("repeated-underperformance", out.reason)
        assertEquals(5.0, out.proposedWeight, 0.001)
        assertEquals("hold", recommend("double", listOf(done(6.0, effort = "hard"))).action)
    }

    @Test fun rirUsesExplicitMeasurementsAndConservativeDefaultTarget() {
        assertEquals("hold", recommend("rir", listOf(done(10.0), done(10.0))).action)
        val increase = recommend("rir", listOf(done(10.0, rir = 5), done(10.0, rir = 5)))
        assertEquals("increase", increase.action)
        assertEquals(6.0, increase.proposedWeight, 0.001)
        val deload = recommend("rir", listOf(done(10.0, rir = 0), done(10.0, rir = 1)))
        assertEquals("reduce", deload.action)
        assertEquals(5.0, deload.proposedWeight, 0.001)
    }

    @Test fun missingHistoryUsesStartingLoadNotAnInventedProgression() {
        val out = recommend("linear", emptyList())
        assertEquals("start", out.action)
        assertEquals(5.5, out.proposedWeight, 0.001)
        assertEquals(0, out.historyCount)
    }

    @Test fun painAndLowReadinessNeverProposeWeightIncreaseOrDecrease() {
        val withPain = readiness.copy(pain = true)
        assertEquals("hold", recommend("linear", listOf(done(12.0)), recovery = withPain).action)
        val low = readiness.copy(score = 45.0)
        assertEquals("hold", recommend("linear", listOf(done(12.0)), recovery = low).action)
    }

    @Test fun reductionNeverProposesZeroOrNegativeLoads() {
        val out = recommend("double", List(3) { done(6.0, weight = .5, effort = "hard") },
            weightStep = 1.0)
        assertEquals("hold", out.action)
        assertEquals(.5, out.proposedWeight, 0.001)
    }
}
