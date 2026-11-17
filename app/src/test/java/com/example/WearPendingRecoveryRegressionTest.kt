package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Looper
import android.os.SystemClock
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.repforge.app.*
import com.repforge.app.data.db.TrainPilotDatabase
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/** Actual listener/editor/interval dispatch, SQLite journal, durable inbox and receipts; no fake engine. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.PAUSED)
class WearPendingRecoveryRegressionTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val store get() = PhoneWorkoutExecution.store(context)
    private lateinit var db: TrainPilotDatabase
    private val roomField = TrainPilotDatabase::class.java.getDeclaredField("INSTANCE").apply { isAccessible = true }

    private fun reopenExecution() {
        val field = PhoneWorkoutExecution::class.java.getDeclaredField("store").apply { isAccessible = true }
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close()
        field.set(null, null)
    }

    @Before fun prepare() {
        reopenExecution()
        context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE)
        for (name in listOf("trainpilot_wear_sync", "trainpilot_wear_commands", "trainpilot_data_replacement", IntervalTimerService.PREFS))
            context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()
        for ((name, value) in listOf("scheduled" to false, "retryAttempt" to 0))
            WearPendingCommandRecovery::class.java.getDeclaredField(name).apply { isAccessible = true }.set(null, value)
        (roomField.get(null) as? TrainPilotDatabase)?.close()
        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
        roomField.set(null, db)
        val set = JSONObject().put("setId", "set-1").put("done", false).put("reps", "10").put("weight", "20")
            .put("leftSeconds", 0).put("rightSeconds", 0).put("distanceMeters", 0)
        val frame = JSONObject().put("active", true).put("workoutId", "recovery-workout").put("generation", 7L)
            .put("executionRevision", 0L).put("nativeExecutionProtocol", 2).put("nativeEditorProjection", 1)
            .put("timerOwner", "phone").put("ownerEpoch193", 0L).put("workoutPaused", false).put("pauseRevision", 4L)
            .put("automaticEnabled", false).put("restSeconds", 0).put("currentExercise", 0)
            .put("started", "2026-11-17T08:00:00Z").put("exercises", JSONArray().put(JSONObject()
                .put("id", "press").put("repUnit", "ism.").put("measurementType", "reps")
                .put("sets", JSONArray().put(set).put(JSONObject(set.toString()).put("setId", "set-2")))))
        ActiveWorkoutStore.save(context, store.prepare(frame, 0))
    }

    @After fun close() {
        context.getSharedPreferences("trainpilot_wear_commands", 0).edit().clear().commit()
        reopenExecution()
        db.close()
        roomField.set(null, null)
    }

    private fun <T> native(block: suspend () -> T): T {
        val worker = Executors.newSingleThreadExecutor()
        val future = worker.submit<T> { runBlocking { block() } }
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        try {
            while (true) {
                shadowOf(Looper.getMainLooper()).idle()
                try { return future.get(10, TimeUnit.MILLISECONDS) }
                catch (_: TimeoutException) { check(System.nanoTime() < deadline) { "Recovery pipeline timed out" } }
            }
        } finally { future.cancel(true); worker.shutdownNow() }
    }

    private fun command(action: String = "completeSet", id: String = "record") = JSONObject()
        .put("schema", 1).put("commandId", id).put("action", action).put("workoutId", "recovery-workout")
        .put("nativeExecutionProtocol", 2).put("generation", 7L).put("executionRevision", store.snapshot()!!.getLong("executionRevision"))
        .put("ownerEpoch193", 0L).put("pauseRevision", 4L).put("sequence", 11L)
        .put("exerciseId", "press").put("setId", "set-1").put("exerciseIndex", 0).put("setIndex", 0)
        .put("reps", "12").put("weight", "25")

    private fun first() = store.snapshot()!!.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(0)
    private fun count() = store.readableDatabase.rawQuery("SELECT count(*) FROM recordings", null).use { it.moveToFirst(); it.getInt(0) }
    private fun pending() = WearCommandQueueStore.pending(context).length()
    private fun receipt(id: String = "record") = WearCommandQueueStore.receipt(context, id)
    private fun drain() = native { WearPendingCommandRecovery.drain(context) }
    private fun editIngress(command: JSONObject) {
        WearPendingCommandRecovery.capture(context, command)
        native { WearSyncRepository.processIncomingWatchCommand(context, command, null) }
    }
    private fun completed(id: String = "record") {
        assertEquals("applied", receipt(id)!!.getString("status"))
        assertEquals(0, pending())
    }
    private fun install(frame: JSONObject) {
        ActiveWorkoutStore.save(context, store.prepare(frame, frame.getLong("executionRevision")))
    }
    private fun hold(command: JSONObject) {
        store.suspendCommands(true)
        assertEquals("rejected", WearCommandListenerService.dispatchNativeCommand(context, command, null).getString("status"))
        assertNull(receipt(command.getString("commandId")))
        assertEquals(1, pending())
        assertEquals(0, count())
    }

    @Test fun normalDeliveryAndWatchResendApplyOneDurableRecordingAndClearPending() {
        val command = command()
        hold(command)
        store.suspendCommands(false)
        assertEquals("applied", WearCommandListenerService.dispatchNativeCommand(context, command, null).getString("status"))
        completed()
        assertEquals("duplicate", WearCommandListenerService.dispatchNativeCommand(context, command, null).getString("status"))
        drain(); drain()
        assertTrue(first().getBoolean("done")); assertEquals(1, count())
    }

    @Test fun localRecoveryWithoutWatchResendingRecordsOnceAndReplaysAfterStoreReopen() {
        val command = command()
        hold(command)
        drain(); drain()
        assertEquals(1, pending()); assertNull(receipt()); assertFalse(first().getBoolean("done"))
        store.suspendCommands(false)
        reopenExecution() // The retry reads the persisted command and SQLite state, not the original object.
        drain()
        completed(); assertTrue(first().getBoolean("done")); assertEquals(1, count())
        val revision = store.snapshot()!!.getLong("executionRevision")
        reopenExecution(); drain(); drain()
        assertEquals(revision, store.snapshot()!!.getLong("executionRevision")); assertEquals(1, count())
        assertEquals("duplicate", WearCommandListenerService.dispatchNativeCommand(context, command, null).getString("status"))
    }

    @Test fun legacyUpdateSetBlockedByTheRealEditorRecoversWithoutAWatchResend() {
        val edit = command("updateSet", "edit").put("field", "weight").put("value", "31")
            .apply { remove("nativeExecutionProtocol") }
        store.suspendCommands(true)
        editIngress(edit)
        assertEquals(1, pending()); assertNull(receipt("edit")); assertEquals("20", first().getString("weight"))
        drain(); assertEquals(1, pending())
        store.suspendCommands(false)
        reopenExecution(); drain()
        completed("edit"); assertEquals("31", first().getString("weight")); assertEquals(0, count())
        val revision = store.snapshot()!!.getLong("executionRevision")
        editIngress(JSONObject(edit.toString())); drain()
        assertEquals(revision, store.snapshot()!!.getLong("executionRevision"))
    }

    @Test fun legacyCompleteSetUsesTheSameEditorAndIsIdempotentAfterRecovery() {
        val command = command().apply { remove("nativeExecutionProtocol") }
        store.suspendCommands(true)
        editIngress(command)
        assertEquals(1, pending()); assertFalse(first().getBoolean("done"))
        store.suspendCommands(false)
        drain(); completed()
        assertTrue(first().getBoolean("done"))
        val revision = store.snapshot()!!.getLong("executionRevision")
        editIngress(JSONObject(command.toString())); drain()
        assertEquals(revision, store.snapshot()!!.getLong("executionRevision"))
    }

    private fun timedFrame() {
        val frame = store.snapshot()!!.put("automaticEnabled", true)
            .put("timerConfig", JSONObject().put("work", 30).put("rest", 10).put("rounds", 1).put("autoAdvance", true))
        frame.getJSONArray("exercises").getJSONObject(0).put("repUnit", "mp").put("measurementType", "time")
        install(frame)
    }
    private fun arm(service: IntervalTimerService) {
        service.onStartCommand(Intent(context, IntervalTimerService::class.java).apply {
            action = IntervalTimerService.ARM
            putExtra("workoutId", "recovery-workout"); putExtra("generation", 7L)
        }, 0, 1)
    }

    @Test fun automaticStartRemainsPendingUntilTheExistingControllerIsExplicitlyArmed() {
        timedFrame()
        val command = command("automaticControl", "start").put("value", "start")
        assertEquals("controllerNotArmed", WearCommandListenerService.dispatchNativeCommand(context, command, null).getString("reason"))
        assertEquals(1, pending()); assertEquals(0, count()); assertNull(receipt("start"))
        drain(); assertEquals(1, pending())
        val service = Robolectric.buildService(IntervalTimerService::class.java).create()
        try {
            arm(service.get())
            drain(); completed("start")
            assertTrue(IntervalTimerService.snapshot(context).getBoolean("running")); assertEquals(1, count())
            val key = IntervalTimerService.snapshot(context).getString("key")
            drain(); drain()
            assertEquals(key, IntervalTimerService.snapshot(context).getString("key")); assertEquals(1, count())
        } finally { service.destroy() }
    }

    @Test fun legacyAutomaticPauseUsesActualIntervalDispatchAndPhaseFence() {
        timedFrame()
        val service = Robolectric.buildService(IntervalTimerService::class.java).create()
        try {
            arm(service.get())
            val start = command("automaticControl", "start").put("value", "start")
            WearCommandListenerService.dispatchNativeCommand(context, start, null)
            val timer = IntervalTimerService.snapshot(context)
            assertTrue(timer.getBoolean("running"))
            val pause = command("automaticControl", "pause").put("value", "pause")
                .put("automation", JSONObject().put("key", timer.getString("key")).put("phaseNumber", timer.getLong("phaseNumber")))
                .apply { remove("nativeExecutionProtocol") }
            store.suspendCommands(true)
            WearCommandListenerService.dispatchAutomaticCommand(context, pause, null)
            assertEquals(1, pending()); assertNull(receipt("pause")); assertTrue(IntervalTimerService.snapshot(context).getBoolean("running"))
            store.suspendCommands(false)
            drain(); completed("pause")
            assertFalse(IntervalTimerService.snapshot(context).getBoolean("running"))
            val revision = store.snapshot()!!.getLong("executionRevision")
            WearCommandListenerService.dispatchAutomaticCommand(context, pause, null); drain()
            assertFalse(IntervalTimerService.snapshot(context).getBoolean("running"))
            assertEquals(revision, store.snapshot()!!.getLong("executionRevision"))
        } finally { service.destroy() }
    }

    @Test fun startupAndRepeatedReconnectTriggersDrainThePersistedInboxWithoutResend() {
        hold(command())
        store.suspendCommands(false)
        reopenExecution()
        // A fresh listener's onCreate is the production process-start trigger; reconnect uses the same scheduler.
        val listener = Robolectric.buildService(WearCommandListenerService::class.java).create()
        try {
            val peer = object : com.google.android.gms.wearable.Node {
                override fun getId() = "watch-node"
                override fun getDisplayName() = "Watch"
                override fun isNearby() = true
            }
            listener.get().onPeerConnected(peer); listener.get().onPeerConnected(peer)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
            while (receipt() == null) {
                shadowOf(Looper.getMainLooper()).idle()
                check(System.nanoTime() < deadline) { "Scheduled durable retry did not complete" }
                Thread.yield()
            }
            drain() // Join any in-flight scheduled pass before checking/reopening its stores.
            completed(); assertEquals(1, count())
            WearPendingCommandRecovery.schedule(context); drain()
            assertEquals(1, count())
        } finally { listener.destroy() }
    }

    @Test fun restoreFenceNeverAcknowledgesOrAppliesUntilRecoveryFinishes() {
        val command = command()
        store.setRestoreBlocked(true)
        assertEquals("restoreInProgress", WearCommandListenerService.dispatchNativeCommand(context, command, null).getString("reason"))
        drain(); drain()
        assertEquals(1, pending()); assertNull(receipt()); assertFalse(first().getBoolean("done")); assertEquals(0, count())
        store.setRestoreBlocked(false)
        drain(); completed(); assertEquals(1, count())
    }

    @Test fun changedGenerationPauseAndEpochRejectPendingCommands() {
        val variants = listOf<(JSONObject) -> JSONObject>(
            { it.put("generation", 8L) }, { it.put("pauseRevision", 5L) }, { it.put("ownerEpoch193", 1L) })
        for ((index, change) in variants.withIndex()) {
            val base = store.snapshot()!!
            val command = command(id = "stale-$index").put("generation", base.getLong("generation"))
                .put("pauseRevision", base.optLong("pauseRevision")).put("ownerEpoch193", base.optLong("ownerEpoch193"))
            WearPendingCommandRecovery.capture(context, command)
            WearCommandQueueStore.enqueue(context, command, null)
            // The projection is also a terminal/pause admission fence, even before the engine mirrors it.
            ActiveWorkoutStore.save(context, change(JSONObject(base.toString())))
            drain()
            assertEquals("superseded", receipt("stale-$index")!!.getString("status"))
            assertEquals(0, pending()); assertFalse(first().getBoolean("done")); assertEquals(0, count())
            ActiveWorkoutStore.save(context, base)
        }
    }

    @Test fun closedDraftRejectsPendingCommandWithoutRecreatingTheWorkout() {
        hold(command())
        store.suspendCommands(false)
        val frame = store.snapshot()!!
        assertTrue(store.closeExecution(frame.getString("workoutId"), frame.getLong("generation"), frame.getLong("executionRevision")))
        ActiveWorkoutStore.clear(context)
        drain()
        assertEquals("superseded", receipt()!!.getString("status")); assertEquals(0, pending())
        assertFalse(store.snapshot()!!.getBoolean("active")); assertFalse(ActiveWorkoutStore.current(context)!!.getBoolean("active"))
        assertEquals(0, count())
    }

    @Test fun finalizationFenceRejectsPendingCommandBeforeTheEngineFrameCloses() {
        hold(command())
        store.suspendCommands(false)
        ActiveWorkoutStore.saveFinalizing(context, ActiveWorkoutStore.current(context)!!.put("finalizationPending", true))
        drain()
        assertEquals("superseded", receipt()!!.getString("status")); assertEquals(0, pending())
        assertFalse(first().getBoolean("done")); assertEquals(0, count())
    }

    @Test fun pausedDraftCannotBeResumedByDrainingAndOldPauseRevisionRejectsAfterExplicitResume() {
        val paused = store.setWorkoutPause(store.snapshot()!!.put("workoutPaused", true).put("pauseRevision", 5L), SystemClock.elapsedRealtime(), false)!!
        ActiveWorkoutStore.save(context, paused)
        val command = command().put("pauseRevision", 5L)
        hold(command)
        store.suspendCommands(false)
        drain(); assertEquals(1, pending()); assertNull(receipt())
        assertTrue(store.snapshot()!!.getBoolean("workoutPaused")); assertFalse(first().getBoolean("done"))
        val resumed = store.setWorkoutPause(store.snapshot()!!.put("workoutPaused", false).put("pauseRevision", 6L), SystemClock.elapsedRealtime(), false)!!
        ActiveWorkoutStore.save(context, resumed)
        drain()
        assertEquals("superseded", receipt()!!.getString("status")); assertEquals(0, pending()); assertEquals(0, count())
    }

    @Test fun legacyGenerationCapturedAtIngressCannotBeReboundAfterReplacementOrRestart() {
        val edit = command("updateSet", "old-edit").put("field", "weight").put("value", "99")
            .apply { remove("nativeExecutionProtocol"); remove("generation") }
        store.suspendCommands(true)
        editIngress(edit)
        assertEquals(1, pending())
        val persisted = WearCommandQueueStore.pending(context).getJSONObject(0)
        assertFalse(persisted.has("generation"))
        assertEquals(7L, persisted.getJSONObject("_phonePendingIdentity255").getLong("generation"))
        store.suspendCommands(false)
        val old = store.snapshot()!!
        assertTrue(store.closeExecution(old.getString("workoutId"), 7L, old.getLong("executionRevision")))
        ActiveWorkoutStore.clear(context)
        native { NativeDataReplacement.exclusively(context) { Unit } }
        val replacement = JSONObject(old.toString()).put("workoutId", "replacement-workout").put("generation", 8L).put("executionRevision", 0L)
        ActiveWorkoutStore.save(context, store.prepare(replacement, 0))
        reopenExecution(); drain()
        assertEquals("superseded", receipt("old-edit")!!.getString("status"))
        assertEquals(0, pending()); assertEquals("20", first().getString("weight"))
    }

    @Test fun invalidLegacyBilateralResultGetsAnErrorReceiptAfterTheTransientBlockClears() {
        val frame = store.snapshot()!!
        frame.getJSONArray("exercises").getJSONObject(0).put("repUnit", "mp/oldal").put("measurementType", "time")
        install(frame)
        val command = command().put("leftSeconds", 0).put("rightSeconds", 43).apply { remove("nativeExecutionProtocol") }
        store.suspendCommands(true)
        editIngress(command)
        assertEquals(1, pending()); assertNull(receipt())
        store.suspendCommands(false)
        drain()
        assertEquals("error", receipt()!!.getString("status")); assertEquals("completeSet", receipt()!!.getString("action"))
        assertEquals(0, pending()); assertFalse(first().getBoolean("done")); assertEquals(0, count())
    }

    @Test fun watchResendCannotReplaceThePersistedLegacyGenerationWithTheCurrentOne() {
        val edit = command("updateSet", "old-edit").put("field", "weight").put("value", "99")
            .apply { remove("nativeExecutionProtocol"); remove("generation") }
        val wire = JSONObject(edit.toString())
        store.suspendCommands(true)
        editIngress(edit)
        store.suspendCommands(false)
        ActiveWorkoutStore.save(context, ActiveWorkoutStore.current(context)!!.put("generation", 8L))
        WearPendingCommandRecovery.capture(context, wire, incoming = true)
        assertEquals(7L, wire.getJSONObject("_phonePendingIdentity255").getLong("generation"))
        editIngress(wire)
        assertEquals("superseded", receipt("old-edit")!!.getString("status")); assertEquals(0, pending())
        assertEquals("20", first().getString("weight")); assertEquals(0, count())
    }

    @Test fun crashBetweenEngineCommitAndInboxAckReconcilesDurableProofAfterClosure() {
        val command = command()
        assertEquals("applied", PhoneWorkoutExecution.execute(context, command).getString("status"))
        // This is the real durable gap: the engine committed, while the inbox receipt did not.
        assertNull(receipt())
        WearCommandQueueStore.enqueue(context, command, null)
        val frame = store.snapshot()!!
        assertTrue(store.closeExecution(frame.getString("workoutId"), frame.getLong("generation"), frame.getLong("executionRevision")))
        ActiveWorkoutStore.clear(context)
        reopenExecution(); drain()
        completed(); assertEquals(1, count())
        assertFalse(store.snapshot()!!.getBoolean("active")); assertFalse(ActiveWorkoutStore.current(context)!!.getBoolean("active"))
        drain(); assertEquals(1, count())
    }

    @Test fun legacyBilateralCommitWithoutInboxAckReconcilesItsWireFingerprintAfterClosureAndReopen() {
        val frame = store.snapshot()!!
        frame.getJSONArray("exercises").getJSONObject(0).put("repUnit", "mp/oldal").put("measurementType", "time")
        install(frame)
        val legacy = command().put("leftSeconds", 37).put("rightSeconds", 43).apply {
            remove("nativeExecutionProtocol"); remove("generation"); remove("executionRevision")
        }
        WearPendingCommandRecovery.capture(context, legacy)
        WearSyncRepository.commitWatchEditorMutation(context, ActiveWorkoutStore.current(context)!!, legacy)
        assertTrue(first().getBoolean("done")); assertEquals(1, count()); assertNull(receipt())
        WearCommandQueueStore.enqueue(context, legacy, null)
        val committed = store.snapshot()!!
        assertTrue(store.closeExecution(committed.getString("workoutId"), committed.getLong("generation"), committed.getLong("executionRevision")))
        ActiveWorkoutStore.clear(context)
        reopenExecution(); drain()
        completed(); assertEquals("completeSet", receipt()!!.getString("action")); assertEquals(1, count())
        assertFalse(store.snapshot()!!.getBoolean("active")); assertFalse(ActiveWorkoutStore.current(context)!!.getBoolean("active"))
        drain(); assertEquals(1, count())
    }

    private fun committedUndoWithoutInboxAck(): JSONObject {
        assertEquals("applied", PhoneWorkoutExecution.execute(context, command()).getString("status"))
        val frame = store.snapshot()!!
        val inverse = com.repforge.shared.WearMeasuredSetCommand.undo(frame, frame.getJSONObject("lastRecord188"), "inverse", 1000L, null)
            .put("sequence", 12L)
        WearPendingCommandRecovery.capture(context, inverse)
        assertEquals("applied", PhoneWorkoutExecution.execute(context, inverse).getString("status"))
        assertFalse(first().getBoolean("done")); assertEquals(2, count()); assertNull(receipt("inverse"))
        WearCommandQueueStore.enqueue(context, inverse, null)
        return inverse
    }

    @Test fun fastUndoDrainReconcilesCommittedInverseAfterClosureAndReopen() {
        committedUndoWithoutInboxAck()
        val frame = store.snapshot()!!
        assertTrue(store.closeExecution(frame.getString("workoutId"), frame.getLong("generation"), frame.getLong("executionRevision")))
        ActiveWorkoutStore.clear(context)
        reopenExecution()
        WearSyncRepository.drainNativeUndo188(context)
        completed("inverse"); assertEquals("undoSet", receipt("inverse")!!.getString("action"))
        assertFalse(store.snapshot()!!.getBoolean("active")); assertFalse(ActiveWorkoutStore.current(context)!!.getBoolean("active"))
        WearSyncRepository.drainNativeUndo188(context); drain()
        assertFalse(first().getBoolean("done")); assertEquals(2, count())
    }

    @Test fun fastUndoDrainReconcilesCommittedInverseAcrossPauseChangeWithoutResuming() {
        committedUndoWithoutInboxAck()
        val paused = store.setWorkoutPause(store.snapshot()!!.put("workoutPaused", true).put("pauseRevision", 5L), SystemClock.elapsedRealtime(), false)!!
        ActiveWorkoutStore.save(context, paused)
        reopenExecution()
        WearSyncRepository.drainNativeUndo188(context)
        completed("inverse")
        assertTrue(store.snapshot()!!.getBoolean("workoutPaused")); assertFalse(first().getBoolean("done"))
        WearSyncRepository.drainNativeUndo188(context); drain()
        assertTrue(store.snapshot()!!.getBoolean("workoutPaused")); assertEquals(2, count())
    }

    private fun closeCurrent() {
        val frame = store.snapshot()!!
        assertTrue(store.closeExecution(frame.getString("workoutId"), frame.getLong("generation"), frame.getLong("executionRevision")))
        ActiveWorkoutStore.clear(context)
    }

    private fun wire(listener: WearCommandListenerService, command: JSONObject) {
        val uri = Uri.parse("wear://watch-node" + WearCommandListenerService.COMMAND_PATH_PREFIX + command.getString("commandId"))
        // This is the same production dispatcher called by onDataChanged immediately after DataMap parsing.
        listener.dispatchReceivedCommand(JSONObject(command.toString()), uri)
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun awaitReceipt(id: String = "record") {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        while (receipt(id) == null) {
            shadowOf(Looper.getMainLooper()).idle()
            check(System.nanoTime() < deadline) { "Listener wire receipt did not complete" }
            Thread.yield()
        }
    }

    private fun committedLegacyBilateralWithoutAck(): JSONObject {
        val frame = store.snapshot()!!
        frame.getJSONArray("exercises").getJSONObject(0).put("repUnit", "mp/oldal").put("measurementType", "time")
        install(frame)
        val command = command().put("leftSeconds", 37).put("rightSeconds", 43).apply {
            remove("nativeExecutionProtocol"); remove("generation"); remove("executionRevision")
        }
        WearPendingCommandRecovery.capture(context, command)
        WearSyncRepository.commitWatchEditorMutation(context, ActiveWorkoutStore.current(context)!!, command)
        assertTrue(first().getBoolean("done")); assertEquals(1, count()); assertNull(receipt())
        WearCommandQueueStore.enqueue(context, command, null)
        return command
    }

    @Test fun lateNativeWireAfterCommitWithoutAckAndClosureUsesProofThenReplaysTheSameReceipt() {
        val command = command()
        assertEquals("applied", PhoneWorkoutExecution.execute(context, command).getString("status"))
        WearCommandQueueStore.enqueue(context, command, null)
        closeCurrent(); reopenExecution()
        val listener = Robolectric.buildService(WearCommandListenerService::class.java).create()
        try {
            wire(listener.get(), command); wire(listener.get(), command)
            awaitReceipt(); completed()
            val committedReceipt = receipt()!!.toString()
            wire(listener.get(), JSONObject(command.toString()).put("ownerEpoch193", 8L))
            assertEquals(committedReceipt, receipt()!!.toString()); assertEquals(1, count())
            assertFalse(store.snapshot()!!.getBoolean("active")); assertFalse(ActiveWorkoutStore.current(context)!!.getBoolean("active"))
        } finally { listener.destroy() }
    }

    @Test fun lateLegacyBilateralWireAfterCommitWithoutAckAndClosureUsesOriginalProof() {
        val command = committedLegacyBilateralWithoutAck()
        closeCurrent(); reopenExecution()
        val listener = Robolectric.buildService(WearCommandListenerService::class.java).create()
        try {
            wire(listener.get(), command); wire(listener.get(), command)
            awaitReceipt(); completed()
            val committedReceipt = receipt()!!.toString()
            wire(listener.get(), command)
            assertEquals(committedReceipt, receipt()!!.toString()); assertEquals(1, count())
            assertEquals(43, first().getInt("rightSeconds")); assertEquals("completeSet", receipt()!!.getString("action"))
            assertFalse(store.snapshot()!!.getBoolean("active")); assertFalse(ActiveWorkoutStore.current(context)!!.getBoolean("active"))
        } finally { listener.destroy() }
    }

    @Test fun legacyWireReachingRepositoryStalePauseAdmissionReconcilesItsCommittedProof() {
        val command = committedLegacyBilateralWithoutAck()
        val paused = store.setWorkoutPause(store.snapshot()!!.put("workoutPaused", true).put("pauseRevision", 5L), SystemClock.elapsedRealtime(), false)!!
        ActiveWorkoutStore.save(context, paused)
        val listener = Robolectric.buildService(WearCommandListenerService::class.java).create()
        try {
            wire(listener.get(), command)
            awaitReceipt(); completed()
            assertTrue(store.snapshot()!!.getBoolean("workoutPaused")); assertEquals(1, count())
            wire(listener.get(), command)
            assertEquals("applied", receipt()!!.getString("status")); assertEquals(1, count())
        } finally { listener.destroy() }
    }

    @Test fun closedListenerRejectsNativeLegacyAndUnsupportedWiresWithoutInventingProof() {
        val native = command()
        val legacy = command(id = "legacy").apply { remove("nativeExecutionProtocol"); remove("generation"); remove("executionRevision") }
        val unsupported = command("selectExercise", "unsupported")
        closeCurrent()
        val listener = Robolectric.buildService(WearCommandListenerService::class.java).create()
        try {
            for (command in listOf(native, legacy, unsupported)) {
                wire(listener.get(), command)
                awaitReceipt(command.getString("commandId"))
                assertEquals("superseded", receipt(command.getString("commandId"))!!.getString("status"))
            }
            assertEquals(0, pending()); assertEquals(0, count()); assertFalse(store.snapshot()!!.getBoolean("active"))
        } finally { listener.destroy() }
    }

    @Test fun lateWireDuringRestoreRetainsPendingProofUntilTheRestoreFenceClears() {
        val command = command()
        assertEquals("applied", PhoneWorkoutExecution.execute(context, command).getString("status"))
        WearCommandQueueStore.enqueue(context, command, null)
        closeCurrent(); store.setRestoreBlocked(true)
        val listener = Robolectric.buildService(WearCommandListenerService::class.java).create()
        try {
            wire(listener.get(), command)
            assertNull(receipt()); assertEquals(1, pending()); assertEquals(1, count())
            store.setRestoreBlocked(false)
            wire(listener.get(), command)
            awaitReceipt(); completed()
            assertEquals(1, count()); assertFalse(store.snapshot()!!.getBoolean("active"))
        } finally { listener.destroy() }
    }

    @Test fun lateLegacyWireWithChangedMeasuredPayloadCannotClaimTheCommittedProof() {
        val command = committedLegacyBilateralWithoutAck()
        closeCurrent()
        val listener = Robolectric.buildService(WearCommandListenerService::class.java).create()
        try {
            wire(listener.get(), JSONObject(command.toString()).put("rightSeconds", 55))
            awaitReceipt()
            assertEquals("error", receipt()!!.getString("status")); assertEquals(0, pending())
            assertEquals(43, first().getInt("rightSeconds")); assertEquals(1, count())
            assertFalse(store.snapshot()!!.getBoolean("active"))
        } finally { listener.destroy() }
    }

    @Test fun staleNativeRevisionIsTerminalAndCannotSpinForeverInTheInbox() {
        val command = command().put("executionRevision", 0L)
        hold(command)
        store.suspendCommands(false)
        drain()
        assertEquals("error", receipt()!!.getString("status")); assertEquals(0, pending())
        assertFalse(first().getBoolean("done")); assertEquals(0, count())
    }
}
