package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.repforge.app.*
import com.repforge.shared.WearMeasuredSetCommand
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/** Uses the same production command builder as Wear MainActivity/Undo, then the phone router/store. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.PAUSED)
class AuditPhoneOwnedBilateralUndoRegressionTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val store get() = PhoneWorkoutExecution.store(context)

    private fun resetStore() {
        val field = PhoneWorkoutExecution::class.java.getDeclaredField("store").apply { isAccessible = true }
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close()
        field.set(null, null)
    }

    @Before fun prepare() {
        resetStore()
        context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
        for (prefs in listOf("trainpilot_wear_sync", "trainpilot_wear_commands"))
            context.getSharedPreferences(prefs, Context.MODE_PRIVATE).edit().clear().commit()
        val set = JSONObject().put("setId", "side-1").put("done", false).put("reps", "30").put("weight", "0")
            .put("leftSeconds", 0).put("rightSeconds", 0).put("distanceMeters", 0)
        val second = JSONObject(set.toString()).put("setId", "side-2")
        val frame = JSONObject().put("active", true).put("workoutId", "bilateral-workout").put("generation", 7L)
            .put("executionRevision", 0L).put("nativeExecutionProtocol", 2).put("nativeEditorProjection", 1)
            .put("timerOwner", "phone").put("ownerEpoch193", 0L).put("workoutPaused", false).put("pauseRevision", 4L)
            .put("automaticEnabled", false).put("restSeconds", 30).put("restEndAt", 0L).put("currentExercise", 0)
            .put("started", "2026-11-16T08:00:00Z").put("timeLogs162", JSONObject().put("existing", JSONObject().put("schema", 1).put("key", "existing").put("owner", "phone")
                .put("workMs", 9100).put("restMs", 700).put("pausedMs", 300).put("positionMs", 10100)))
            .put("exercises", JSONArray().put(JSONObject().put("id", "side-plank").put("repUnit", "mp/oldal")
                .put("measurementType", "time").put("sets", JSONArray().put(set).put(second))))
        ActiveWorkoutStore.save(context, store.prepare(frame, 0))
    }

    @After fun close() { resetStore() }

    private fun first(frame: JSONObject = store.snapshot()!!) = frame.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(0)
    private fun second(frame: JSONObject = store.snapshot()!!) = frame.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(1)
    private fun raw(frame: JSONObject = store.snapshot()!!) = JSONObject().put("schema", 1).put("commandId", "wear-record")
        .put("action", "completeSet").put("workoutId", frame.getString("workoutId"))
        .put("exerciseId", "side-plank").put("setId", "side-1").put("exerciseIndex", 0).put("setIndex", 0)
        .put("ownerEpoch193", frame.getLong("ownerEpoch193")).put("pauseRevision", frame.getLong("pauseRevision"))
        .put("baseRevision", frame.optLong("revision")).put("sequence", 11L)
        .put("reps", "37").put("weight", "2.5").put("leftSeconds", 37).put("rightSeconds", 43)
    private fun completion() = WearMeasuredSetCommand.completion(store.snapshot()!!, raw())
    private fun undo(frame: JSONObject = store.snapshot()!!) = WearMeasuredSetCommand.undo(frame,
        frame.getJSONObject("lastRecord188"), "wear-undo", 1000, null).put("sequence", 12L)
    private fun count() = store.readableDatabase.rawQuery("SELECT count(*) FROM recordings", null).use { it.moveToFirst(); it.getInt(0) }
    private fun receipt(command: JSONObject, status: String = "applied"): JSONObject {
        // Data Layer availability is independent of the durable receipt. Its transport can retry.
        runCatching { WearCommandReceipts.complete(context, command, status, null) }
        return WearCommandQueueStore.receipt(context, command.getString("commandId"))!!
    }

    private fun deliverEarlierEdits(base: JSONObject) {
        for ((index, pair) in listOf("leftSeconds" to 37, "rightSeconds" to 43, "weight" to "2.5").withIndex()) {
            val edit = raw(base).put("action", "updateSet").put("commandId", "edit-$index").put("sequence", 8L + index)
                .put("generation", base.getLong("generation")).put("executionRevision", base.getLong("executionRevision"))
                .put("field", pair.first).put("value", pair.second)
            WearSyncRepository.commitWatchEditorMutation(context, ActiveWorkoutStore.current(context)!!, edit)
        }
    }

    @Test fun productionWearCommandRecordsMeasuredSidesAndDurableTargetedUndoInManualWorkout() {
        val before = store.snapshot()!!
        val other = second().toString()
        val command = completion()
        assertEquals("completeMeasuredSet", command.getString("action"))
        assertTrue(PhoneWorkoutExecution.compatible(command))
        assertNull(WearCommandQueueStore.receipt(context, "wear-record"))
        val recorded = WearCommandListenerService.dispatchNativeCommand(context, command, null)
        assertEquals("applied", recorded.getString("status"))
        assertTrue(store.recorded188(before.getString("workoutId"), 7, "wear-record"))
        assertEquals(37, recorded.getJSONObject("values").getInt("leftSeconds"))
        assertEquals(43, recorded.getJSONObject("values").getInt("rightSeconds"))
        assertEquals("completeMeasuredSet", receipt(command).getString("action"))
        assertTrue(store.snapshot()!!.getLong("restEndAt") > System.currentTimeMillis())
        assertFalse(store.snapshot()!!.has("setStopwatch"))
        val inverse = undo()
        resetStore() // Receipt and last-record identity survive a process/store reopen.
        val undone = WearSyncRepository.executeWatchUndo(context, inverse)
        assertEquals("applied", undone.getString("status"))
        val after = store.snapshot()!!
        assertFalse(first(after).getBoolean("done"))
        assertEquals(37, first(after).getInt("leftSeconds")); assertEquals(43, first(after).getInt("rightSeconds"))
        assertEquals("37", first(after).getString("reps")); assertEquals(2.5, first(after).getDouble("weight"), 0.0)
        assertEquals(other, second(after).toString())
        assertEquals(before.getJSONObject("timeLogs162").toString(), after.getJSONObject("timeLogs162").toString())
        assertEquals(0L, after.getLong("restEndAt")); assertTrue(after.getJSONObject("automation").getBoolean("paused"))
        assertFalse(after.getBoolean("automaticEnabled")); assertEquals("phone", after.getString("timerOwner"))
        assertEquals(7L, after.getLong("generation")); assertEquals("side-1", first(after).getString("setId"))
        assertEquals("applied", receipt(inverse).getString("status")); assertEquals(2, count())
        val merged = NativeWorkoutEventMerge.merge(before, after, JSONArray().put(recorded).put(undone))
        assertFalse(first(merged).getBoolean("done")); assertEquals(43, first(merged).getInt("rightSeconds"))
        assertFalse(ActiveWorkoutStore.current(context)!!.getJSONObject("automation").getBoolean("enabled"))
    }

    @Test fun undoCanArriveFirstAndDuplicatesCannotResurrectAnUndoneSet() {
        val frame = store.snapshot()!!
        val command = completion()
        val optimistic = PhoneWorkoutRecording.record(frame, command)
        optimistic.put("lastRecord188", WorkoutUndo188.capture(PhoneWorkoutRecording.recordingBase(frame, command), optimistic, command, "wear-record"))
        val inverse = undo(optimistic)
        // The watch has not received the phone's next executionRevision yet.
        inverse.put("executionRevision", frame.getLong("executionRevision"))
        assertEquals("deferred", WearCommandListenerService.dispatchNativeCommand(context, inverse, null).getString("status"))
        assertEquals(0, count()); assertNull(WearCommandQueueStore.receipt(context, "wear-undo"))
        assertEquals(1, WearCommandQueueStore.pending(context).length())
        assertEquals("applied", WearCommandListenerService.dispatchNativeCommand(context, command, null).getString("status"))
        assertFalse(first().getBoolean("done")); assertEquals(0, WearCommandQueueStore.pending(context).length())
        assertEquals("applied", WearCommandQueueStore.receipt(context, "wear-undo")!!.getString("status"))
        resetStore()
        assertEquals("duplicate", PhoneWorkoutExecution.execute(context, command).getString("status"))
        assertEquals("duplicate", WearSyncRepository.executeWatchUndo(context, inverse).getString("status"))
        assertFalse(first().getBoolean("done")); assertEquals(2, count())
        val collision = JSONObject(command.toString())
        collision.getJSONObject("wearBilateralValues").put("rightSeconds", 55)
        assertEquals("commandIdentityCollision", PhoneWorkoutExecution.execute(context, collision).getString("reason"))
        assertEquals(43, first().getInt("rightSeconds")); assertEquals(2, count())
    }

    @Test fun legacyBilateralCompletionUsesTheSameJournalAndReplayRemainsStableAfterUndo() {
        val command = raw()
        assertFalse(command.has("nativeExecutionProtocol"))
        WearSyncRepository.commitWatchEditorMutation(context, ActiveWorkoutStore.current(context)!!, command)
        assertTrue(first().getBoolean("done")); assertEquals(1, count())
        val inverse = undo().apply { remove("nativeExecutionProtocol"); put("native188", false) }
        assertEquals("applied", WearSyncRepository.executeWatchUndo(context, inverse).getString("status"))
        WearSyncRepository.commitWatchEditorMutation(context, ActiveWorkoutStore.current(context)!!, command)
        assertFalse(first().getBoolean("done")); assertEquals(2, count())
        val earlierEdit = JSONObject(command.toString()).put("action", "updateSet").put("sequence", 9L)
            .put("field", "leftSeconds").put("value", 5)
        WearSyncRepository.commitWatchEditorMutation(context, ActiveWorkoutStore.current(context)!!, earlierEdit)
        assertEquals(37, first().getInt("leftSeconds")); assertEquals(43, first().getInt("rightSeconds"))
    }

    @Test fun invalidSidesStaleTargetRevisionPauseGenerationAndEpochCannotRecord() {
        val valid = completion()
        val invalid = listOf(
            JSONObject(valid.toString()).apply { getJSONObject("wearBilateralValues").put("leftSeconds", 0) },
            JSONObject(valid.toString()).apply { getJSONObject("wearBilateralValues").put("rightSeconds", 86401) },
            JSONObject(valid.toString()).put("setId", "replaced"),
            JSONObject(valid.toString()).put("executionRevision", 0L),
            JSONObject(valid.toString()).put("pauseRevision", 3L),
            JSONObject(valid.toString()).put("generation", 6L),
            JSONObject(valid.toString()).put("ownerEpoch193", 1L))
        for (command in invalid) {
            assertEquals(command.toString(), "rejected", PhoneWorkoutExecution.execute(context, command).getString("status"))
            assertFalse(first().getBoolean("done")); assertEquals(0, count())
        }
        store.suspendCommands(true)
        assertEquals("rejected", PhoneWorkoutExecution.execute(context, valid).getString("status"))
        store.suspendCommands(false); store.setRestoreBlocked(true)
        assertEquals("rejected", PhoneWorkoutExecution.execute(context, valid).getString("status"))
        store.setRestoreBlocked(false)
        assertEquals(0, count())
    }

    @Test fun undoRequiresTheRecordedIdentityUnchangedGuardAndMatchingPauseAndGeneration() {
        assertEquals("applied", PhoneWorkoutExecution.execute(context, completion()).getString("status"))
        val inverse = undo()
        val wrong = listOf(
            JSONObject(inverse.toString()).put("recordId", "other").put("executionRevision", 1L),
            JSONObject(inverse.toString()).put("setId", "side-2"),
            JSONObject(inverse.toString()).put("generation", 6L),
            JSONObject(inverse.toString()).put("pauseRevision", 3L),
            JSONObject(inverse.toString()).put("expected", JSONArray()))
        for (command in wrong) {
            assertEquals("rejected", WearSyncRepository.executeWatchUndo(context, command).getString("status"))
            assertTrue(first().getBoolean("done")); assertEquals(1, count())
        }
        val changed = store.snapshot()!!
        second(changed).put("reps", "99")
        store.prepare(changed, changed.getLong("executionRevision"))
        assertEquals("rejected", WearSyncRepository.executeWatchUndo(context, inverse).getString("status"))
        assertTrue(first().getBoolean("done")); assertEquals("99", second().getString("reps"))
    }
    @Test fun earlierSameWatchEditsCanBridgeOnlyTheirOwnUnchangedMeasuredRevision() {
        val base = store.snapshot()!!
        val command = completion()
        deliverEarlierEdits(base)
        assertTrue(store.snapshot()!!.getLong("executionRevision") > command.getLong("executionRevision"))
        assertEquals("applied", WearCommandListenerService.dispatchNativeCommand(context, command, null).getString("status"))
        assertEquals(37, first().getInt("leftSeconds")); assertEquals(43, first().getInt("rightSeconds"))
        assertFalse(store.snapshot()!!.has("wearEditRevision250")); assertEquals(1, count())
    }

    @Test fun revisionBridgeStillRejectsInvalidMeasurementsAnotherTargetAndAnotherSource() {
        val base = store.snapshot()!!
        val command = completion()
        deliverEarlierEdits(base)
        val mismatch = JSONObject(command.toString()).apply { getJSONObject("wearBilateralValues").put("rightSeconds", 86401) }
        assertEquals("invalidMeasuredTime", PhoneWorkoutExecution.execute(context, mismatch).getString("reason"))
        assertEquals("revisionConflict", PhoneWorkoutExecution.execute(context, JSONObject(command.toString()).put("setId", "side-2")).getString("reason"))
        assertEquals("revisionConflict", PhoneWorkoutExecution.execute(context, JSONObject(command.toString()).put("_uri", "wear://other-node/command")).getString("reason"))
        assertFalse(first().getBoolean("done")); assertEquals(43, first().getInt("rightSeconds")); assertEquals(0, count())
    }

    @Test fun interveningPhoneValueEditInvalidatesTheEntireWatchRevisionBridge() {
        val base = store.snapshot()!!
        val command = completion()
        deliverEarlierEdits(base)
        val phoneEdit = store.snapshot()!!
        first(phoneEdit).put("leftSeconds", 38)
        NativeWorkoutEditing.commitEdit(context, phoneEdit)
        assertEquals("revisionConflict", PhoneWorkoutExecution.execute(context, command).getString("reason"))
        assertEquals(38, first().getInt("leftSeconds")); assertFalse(first().getBoolean("done")); assertEquals(0, count())
    }

    @Test fun missingMeasuredInputAndStaleCompletionProduceErrorReceiptsThroughTheRealRouter() {
        val base = store.snapshot()!!
        val missing = WearMeasuredSetCommand.completion(base, raw(base).apply { remove("rightSeconds") })
        assertEquals("rejected", WearCommandListenerService.dispatchNativeCommand(context, missing, null).getString("status"))
        assertEquals("error", WearCommandQueueStore.receipt(context, "wear-record")!!.getString("status"))
        assertEquals(0, WearCommandQueueStore.pending(context).length()); assertEquals(0, count())
        val stale = completion().put("commandId", "stale-completion").put("generation", 6L)
        assertEquals("rejected", WearCommandListenerService.dispatchNativeCommand(context, stale, null).getString("status"))
        assertEquals("error", WearCommandQueueStore.receipt(context, "stale-completion")!!.getString("status"))
        assertFalse(first().getBoolean("done")); assertEquals(0, count())
    }

    @Test fun restoreIsRetryableAndRunningPhoneStopwatchCannotBeReplacedByWearValues() {
        val command = completion()
        store.setRestoreBlocked(true)
        assertEquals("restoreInProgress", WearCommandListenerService.dispatchNativeCommand(context, command, null).getString("reason"))
        assertNull(WearCommandQueueStore.receipt(context, "wear-record")); assertEquals(1, WearCommandQueueStore.pending(context).length())
        store.suspendCommands(true)
        assertEquals("rejected", WearCommandListenerService.dispatchNativeCommand(context, command, null).getString("status"))
        assertNull(WearCommandQueueStore.receipt(context, "wear-record")); assertEquals(1, WearCommandQueueStore.pending(context).length())
        store.setRestoreBlocked(false); store.suspendCommands(false)
        val frame = store.snapshot()!!
        NativeSetStopwatch.start(frame, NativeWorkoutTarget(frame.getString("workoutId"), 7L, "side-plank", "side-1"), "left")
        store.prepare(frame, frame.getLong("executionRevision"))
        val rejected = PhoneWorkoutExecution.execute(context, command)
        assertEquals("setStopwatchStillActive", rejected.getString("reason"))
        assertTrue(NativeSetStopwatch.running(store.snapshot()!!.getJSONObject("setStopwatch")))
        assertEquals(0, first().getInt("leftSeconds")); assertEquals(0, count())
    }

    @Test fun fullWearOkCanOvertakeRemainingEditsInItsOwnUnbrokenRevisionChain() {
        val base = store.snapshot()!!
        val command = completion()
        val left = raw(base).put("action", "updateSet").put("commandId", "left").put("sequence", 8L)
            .put("generation", 7L).put("executionRevision", base.getLong("executionRevision"))
            .put("field", "leftSeconds").put("value", 37)
        WearSyncRepository.commitWatchEditorMutation(context, ActiveWorkoutStore.current(context)!!, left)
        assertEquals(0, first().getInt("rightSeconds"))
        assertEquals("applied", WearCommandListenerService.dispatchNativeCommand(context, command, null).getString("status"))
        for ((field, value, sequence) in listOf(Triple("rightSeconds", 43, 9L), Triple("weight", "2.5", 10L))) {
            val late = JSONObject(left.toString()).put("commandId", "late-$field").put("sequence", sequence).put("field", field).put("value", value)
            WearSyncRepository.commitWatchEditorMutation(context, ActiveWorkoutStore.current(context)!!, late)
        }
        assertTrue(first().getBoolean("done")); assertEquals(37, first().getInt("leftSeconds")); assertEquals(43, first().getInt("rightSeconds"))
        assertEquals(2.5, first().getDouble("weight"), 0.0); assertEquals(1, count())
    }

    @Test fun ordinaryPhoneMeasuredCasStillRejectsDifferentMeasuredValues() {
        val command = completion().apply { remove("wearBilateralValues"); put("measuredValues", "different-base") }
        assertEquals("measurementChanged", PhoneWorkoutExecution.execute(context, command).getString("reason"))
        assertFalse(first().getBoolean("done")); assertEquals(0, count())
    }

    @Test fun rejectedCompletionAlsoTerminatesAnInverseDeliveredFirst() {
        val base = store.snapshot()!!
        val command = completion()
        val optimistic = PhoneWorkoutRecording.record(base, command)
        optimistic.put("lastRecord188", WorkoutUndo188.capture(PhoneWorkoutRecording.recordingBase(base, command), optimistic, command, "wear-record"))
        val inverse = undo(optimistic).put("executionRevision", base.getLong("executionRevision"))
        assertEquals("deferred", WearCommandListenerService.dispatchNativeCommand(context, inverse, null).getString("status"))
        command.getJSONObject("wearBilateralValues").put("leftSeconds", 0)
        assertEquals("rejected", WearCommandListenerService.dispatchNativeCommand(context, command, null).getString("status"))
        assertEquals("error", WearCommandQueueStore.receipt(context, "wear-record")!!.getString("status"))
        assertEquals("error", WearCommandQueueStore.receipt(context, "wear-undo")!!.getString("status"))
        assertEquals(0, WearCommandQueueStore.pending(context).length()); assertEquals(0, count())
    }

    @Test fun partialWatchEditThenUndoThenOkDefersUntilTheMatchingMeasuredEventIsDurable() {
        val base = store.snapshot()!!
        val command = completion()
        val optimistic = PhoneWorkoutRecording.record(base, command)
        optimistic.put("lastRecord188", WorkoutUndo188.capture(PhoneWorkoutRecording.recordingBase(base, command), optimistic, command, "wear-record"))
        val inverse = undo(optimistic).put("executionRevision", base.getLong("executionRevision"))
        val left = raw(base).put("action", "updateSet").put("commandId", "left").put("sequence", 8L)
            .put("generation", 7L).put("executionRevision", base.getLong("executionRevision"))
            .put("field", "leftSeconds").put("value", 37)
        WearSyncRepository.commitWatchEditorMutation(context, ActiveWorkoutStore.current(context)!!, left)
        assertEquals("deferred", WearCommandListenerService.dispatchNativeCommand(context, inverse, null).getString("status"))
        assertNull(WearCommandQueueStore.receipt(context, "wear-undo")); assertEquals(1, WearCommandQueueStore.pending(context).length())
        assertEquals("applied", WearCommandListenerService.dispatchNativeCommand(context, command, null).getString("status"))
        assertEquals("applied", WearCommandQueueStore.receipt(context, "wear-undo")!!.getString("status"))
        assertFalse(first().getBoolean("done")); assertEquals(37, first().getInt("leftSeconds")); assertEquals(43, first().getInt("rightSeconds"))
        assertEquals(2, count()); assertEquals(0, WearCommandQueueStore.pending(context).length())
        assertEquals("duplicate", WearCommandListenerService.dispatchNativeCommand(context, inverse, null).getString("status"))
        assertFalse(first().getBoolean("done")); assertEquals(2, count())
    }

    @Test fun pausedRestoreKeepsMeasuredCommandRetryable() {
        val command = completion()
        val paused = store.snapshot()!!.put("workoutPaused", true).put("pauseRevision", 5L)
        store.prepare(paused, paused.getLong("executionRevision")); store.setRestoreBlocked(true)
        assertEquals("rejected", WearCommandListenerService.dispatchNativeCommand(context, command, null).getString("status"))
        assertNull(WearCommandQueueStore.receipt(context, "wear-record")); assertEquals(1, WearCommandQueueStore.pending(context).length())
        store.setRestoreBlocked(false)
        assertEquals(0, count())
    }

    @Test fun finalizationRejectsNewMeasuredEventsEvenWhenTheExecutionFrameIsStillActive() {
        val command = completion()
        ActiveWorkoutStore.saveFinalizing(context, ActiveWorkoutStore.current(context)!!.put("finalizationPending", true))
        assertEquals("rejected", PhoneWorkoutExecution.execute(context, command).getString("status"))
        assertFalse(first().getBoolean("done")); assertEquals(0, count())
    }

}
