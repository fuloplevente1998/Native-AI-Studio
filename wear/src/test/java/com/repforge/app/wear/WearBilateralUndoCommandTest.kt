package com.repforge.app.wear

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.repforge.shared.WearMeasuredSetCommand
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.PAUSED)
class WearBilateralUndoCommandTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    @Before fun clear() {
        for (prefs in listOf("trainpilot_wear_snapshot", "trainpilot_wear_outbox"))
            context.getSharedPreferences(prefs, Context.MODE_PRIVATE).edit().clear().commit()
    }
    private fun before() = WearWorkout(1, "p", "Plan", "d", "", "2026-11-16T08:00:00Z", "bilateral-workout", 0, 30, 0,
        listOf(WearExercise("side-plank", "Plank", "body", "mp/oldal", "time", listOf(
            WearSet(1, "37", "2.5", false, 37, 43, 0.0, "side-1"),
            WearSet(2, "30", "0", false, 0, 0, 0.0, "side-2")))),
        timerOwner = "phone", nativeExecutionProtocol = 2, generation = 7, executionRevision = 1, pauseRevision = 4)
    private fun recorded(before: WearWorkout = before()): WearWorkout {
        val ex = before.exercises[0]
        return WearUndo188.capture(before, before.copy(restEndAt = System.currentTimeMillis() + 30000,
            exercises = listOf(ex.copy(sets = listOf(ex.sets[0].copy(done = true), ex.sets[1]))) + before.exercises.drop(1)), 0, 0, "wear-record", 1000)
    }
    private fun completion(workout: WearWorkout = before()): JSONObject = WearMeasuredSetCommand.completion(
        WorkoutSnapshotStore.encode(workout), JSONObject().put("schema", 1).put("commandId", "wear-record")
            .put("action", "completeSet").put("workoutId", workout.workoutId).put("exerciseId", "side-plank").put("setId", "side-1")
            .put("sequence", 11L).put("reps", "37").put("weight", "2.5").put("leftSeconds", 37).put("rightSeconds", 43))
    private fun pending(): JSONArray = JSONArray(context.getSharedPreferences("trainpilot_wear_outbox", Context.MODE_PRIVATE).getString("pending", "[]"))
    private fun receipt(command: JSONObject, status: String = "applied") = JSONObject().put("schema", 1)
        .put("commandId", command.getString("commandId")).put("workoutId", command.getString("workoutId"))
        .put("action", command.getString("action")).put("sequence", command.getLong("sequence")).put("status", status).put("receivedAt", 2000)

    @Test fun productionCompletionBuildsMeasuredTransportAndCaptureRoutesUndoToNativeEngine() {
        val workout = before()
        assertTrue(nativePhoneExecution(workout, "completeSet", workout.exercise, null))
        val command = completion(workout)
        assertEquals("completeMeasuredSet", command.getString("action"))
        assertEquals(37, command.getJSONObject("wearBilateralValues").getInt("leftSeconds"))
        assertEquals(43, command.getJSONObject("wearBilateralValues").getInt("rightSeconds"))
        assertEquals(7L, command.getLong("generation")); assertEquals(1L, command.getLong("executionRevision"))
        assertEquals(4L, command.getLong("pauseRevision")); assertEquals("side-1", command.getString("setId"))
        assertTrue(WearUndo188.record(recorded())!!.getBoolean("native188"))
        assertEquals("completeSet", completion(workout.copy(timerOwner = "watch")).getString("action"))
        assertEquals("completeSet", completion(workout.copy(nativeExecutionProtocol = 0)).getString("action"))
    }

    @Test fun measuredCompletionIsDurableTrackedAndOnlyItsMatchingReceiptSettlesIt() {
        WorkoutSnapshotStore.saveWorkout(context, recorded())
        val command = completion()
        assertTrue(WearSaveSyncModel.tracked(command))
        assertTrue(WearCommandOutbox.enqueue(context, command) > 0)
        val durable = pending().getJSONObject(0)
        assertEquals(43, durable.getJSONObject("wearBilateralValues").getInt("rightSeconds"))
        val wrong = receipt(durable).put("action", "completeSet")
        assertFalse(WearCommandOutbox.acceptReceipt(context, WearCommandOutbox.RECEIPT_PREFIX + "wear-record", wrong.toString()))
        assertEquals(1, pending().length())
        val applied = receipt(durable)
        assertTrue(WearCommandOutbox.acceptReceipt(context, WearCommandOutbox.RECEIPT_PREFIX + "wear-record", applied.toString()))
        assertEquals(0, pending().length())
        assertTrue(WearCommandOutbox.acceptReceipt(context, WearCommandOutbox.RECEIPT_PREFIX + "wear-record", applied.toString()))
        assertEquals(WearSavePhase.SYNCED, WearCommandOutbox.state(context, "bilateral-workout")!!.phase)
    }

    @Test fun actualWearUndoPersistsInverseAndRecoversItsCommandUntilAppliedReceipt() {
        val completed = recorded()
        WorkoutSnapshotStore.saveWorkout(context, completed)
        val undone = WearUndo188.undo(context, "wear-record")!!
        assertFalse(undone.exercises[0].sets[0].done)
        assertEquals(37, undone.exercises[0].sets[0].leftSeconds); assertEquals(43, undone.exercises[0].sets[0].rightSeconds)
        assertEquals(completed.exercises[0].sets[1], undone.exercises[0].sets[1]); assertEquals(0L, undone.restEndAt)
        assertTrue(WearUndo188.blocked(undone))
        val command = pending().getJSONObject(0)
        assertEquals("undoSet", command.getString("action")); assertEquals(2, command.getInt("nativeExecutionProtocol"))
        assertEquals("wear-record", command.getString("recordId")); assertEquals(4L, command.getLong("pauseRevision"))
        assertEquals(7L, command.getLong("generation")); assertEquals(0L, command.getLong("ownerEpoch193"))
        WearUndo188.recover(context)
        assertEquals(1, pending().length()); assertEquals(command.getString("commandId"), pending().getJSONObject(0).getString("commandId"))
        val applied = receipt(command)
        assertTrue(WearCommandOutbox.acceptReceipt(context, WearCommandOutbox.RECEIPT_PREFIX + command.getString("commandId"), applied.toString()))
        assertEquals("applied", WearUndo188.marker(WorkoutSnapshotStore.load(context)!!)!!.getString("status"))
        WearUndo188.recover(context)
        assertEquals(0, pending().length()); assertFalse(WorkoutSnapshotStore.load(context)!!.exercises[0].sets[0].done)
    }

    @Test fun legacyFalseNativeMarkerStillUsesDurablePhoneUndoAndRejectedReceiptReconciles() {
        val completed = recorded().let { it.copy(lastRecord188 = JSONObject(it.lastRecord188!!).put("native188", false).toString()) }
        WorkoutSnapshotStore.saveWorkout(context, completed)
        WearUndo188.undo(context, "wear-record")
        val command = pending().getJSONObject(0)
        assertEquals(2, command.getInt("nativeExecutionProtocol"))
        val error = receipt(command, "error").put("undoFrame", WorkoutSnapshotStore.encode(completed))
        assertTrue(WearCommandOutbox.acceptReceipt(context, WearCommandOutbox.RECEIPT_PREFIX + command.getString("commandId"), error.toString()))
        val corrected = WorkoutSnapshotStore.load(context)!!
        assertTrue(corrected.exercises[0].sets[0].done); assertEquals(43, corrected.exercises[0].sets[0].rightSeconds)
        assertNull(corrected.undo188); assertNull(corrected.lastRecord188)
        assertEquals(WearSavePhase.ERROR, WearCommandOutbox.state(context, corrected.workoutId)!!.phase)
    }
    @Test fun canonicalSideAliasesAndMissingPayloadUseTheMeasuredBoundaryWithoutThrowing() {
        val base = before()
        val alias = base.copy(exercises = listOf(base.exercises[0].copy(id = "custom", repUnit = "seconds/side", measurementType = "bilateral_time")))
        val raw = JSONObject().put("action", "completeSet").put("exerciseId", "custom")
        val missing = WearMeasuredSetCommand.completion(WorkoutSnapshotStore.encode(alias), raw)
        assertEquals("completeMeasuredSet", missing.getString("action"))
        assertTrue(missing.getJSONObject("wearBilateralValues").isNull("leftSeconds"))
        assertTrue(missing.getJSONObject("wearBilateralValues").isNull("rightSeconds"))
        assertTrue(missing.getJSONObject("wearBilateralValues").isNull("weight"))
        assertTrue(nativePhoneExecution(alias, "completeSet", alias.exercise, null))
    }

    @Test fun failedCompletionCorrectsUntouchedOptimismAndRecoveryReplaysItsDurableReceipt() {
        val completed = recorded()
        WorkoutSnapshotStore.saveWorkout(context, completed)
        val command = completion()
        WearCommandOutbox.enqueue(context, command)
        val rejected = receipt(command, "error").put("recordFrame250", WorkoutSnapshotStore.encode(before()))
        // Model a process stopping between durable receipt acceptance and snapshot reconciliation.
        val prefs = context.getSharedPreferences("trainpilot_wear_outbox", Context.MODE_PRIVATE)
        prefs.edit().putString("pending", "[]").putString("results191", JSONArray().put(rejected).toString()).commit()
        WearUndo188.recover(context)
        val corrected = WorkoutSnapshotStore.load(context)!!
        assertFalse(corrected.exercises[0].sets[0].done); assertEquals(37, corrected.exercises[0].sets[0].leftSeconds)
        assertEquals(43, corrected.exercises[0].sets[0].rightSeconds); assertEquals(0L, corrected.restEndAt)
        assertNull(corrected.lastRecord188); assertNull(corrected.undo188)
        assertEquals(WearSavePhase.ERROR, WearCommandOutbox.state(context, corrected.workoutId)!!.phase)
    }

    @Test fun failedCompletionNeverErasesALaterLocalEdit() {
        val completed = recorded()
        val ex = completed.exercises[0]
        val later = completed.copy(exercises = listOf(ex.copy(sets = listOf(ex.sets[0], ex.sets[1].copy(reps = "99")))))
        WorkoutSnapshotStore.saveWorkout(context, later)
        val command = completion()
        WearCommandOutbox.enqueue(context, command)
        val rejected = receipt(command, "error").put("recordFrame250", WorkoutSnapshotStore.encode(before()))
        assertTrue(WearCommandOutbox.acceptReceipt(context, WearCommandOutbox.RECEIPT_PREFIX + "wear-record", rejected.toString()))
        val kept = WorkoutSnapshotStore.load(context)!!
        assertEquals("99", kept.exercises[0].sets[1].reps); assertEquals(43, kept.exercises[0].sets[0].rightSeconds)
        assertEquals("conflict", WearUndo188.marker(kept)!!.getString("status")); assertTrue(WearUndo188.blocked(kept))
    }

    @Test fun lateFailureCannotReplaceANewerPhoneSelectionWithTheSameSetGuard() {
        val initial = before()
        val extra = initial.exercises[0].copy(id = "other", sets = listOf(initial.exercises[0].sets[1].copy(setId = "other-set")))
        val completed = recorded(initial.copy(exercises = initial.exercises + extra))
        val newer = completed.copy(currentExercise = 1, executionRevision = 2)
        WorkoutSnapshotStore.saveWorkout(context, newer)
        val command = completion()
        WearCommandOutbox.enqueue(context, command)
        val rejected = receipt(command, "error").put("recordFrame250", WorkoutSnapshotStore.encode(completed))
        assertTrue(WearCommandOutbox.acceptReceipt(context, WearCommandOutbox.RECEIPT_PREFIX + "wear-record", rejected.toString()))
        val kept = WorkoutSnapshotStore.load(context)!!
        assertEquals(1, kept.currentExercise); assertEquals(2L, kept.executionRevision)
        assertEquals("conflict", WearUndo188.marker(kept)!!.getString("status"))
    }

    @Test fun lateFailureCannotEraseALaterLocalSelectionEvenWhenExecutionRevisionIsUnchanged() {
        val completed = recorded()
        WorkoutSnapshotStore.saveWorkout(context, completed)
        val command = completion()
        WearCommandOutbox.enqueue(context, command)
        WorkoutSnapshotStore.saveWorkout(context, completed.copy(localSequence = command.getLong("sequence") + 1))
        val rejected = receipt(command, "error").put("recordFrame250", WorkoutSnapshotStore.encode(before()))
        assertTrue(WearCommandOutbox.acceptReceipt(context, WearCommandOutbox.RECEIPT_PREFIX + "wear-record", rejected.toString()))
        val kept = WorkoutSnapshotStore.load(context)!!
        assertTrue(kept.exercises[0].sets[0].done)
        assertEquals(command.getLong("sequence") + 1, kept.localSequence)
        assertEquals("conflict", WearUndo188.marker(kept)!!.getString("status"))
    }

}
