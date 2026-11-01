package com.example

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.repforge.app.*
import com.repforge.app.data.store.TrainPilotUserSettings
import com.repforge.shared.OwnershipJournal193
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PhoneReturnInstallationRegressionTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    @Before fun isolateNativeConnection() {
        val field = PhoneWorkoutExecution::class.java.getDeclaredField("store").apply { isAccessible = true }
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close(); field.set(null, null)
    }

    private fun roundTrip(timed: Boolean = false): JSONObject {
        val input = NativeWorkoutSession.quickCandidate(JSONObject("""
            {"id":"press","name":"Press","repUnit":"ism.","measurementType":"reps","loadType":"weight",
             "sets":[{"setId":"one","set":1,"reps":10,"weight":15,"done":false}]}
        """), TrainPilotUserSettings())
        NativeWorkoutSession.installPrepared(context, input)
        StudioWorkoutBridge.pause(context, true)
        val phone = PhoneOwnership193.journal(context)
        var watchFrame = JSONObject(ActiveWorkoutStore.current(context)!!.toString())
        val watchContext = object : ContextWrapper(context) {
            override fun getNoBackupFilesDir() = File(context.cacheDir, "watch-native-journal").apply { mkdirs() }
        }
        val watch = OwnershipJournal193(watchContext, object : OwnershipJournal193.Host {
            override fun current() = watchFrame
            override fun install(value: JSONObject) {
                watchFrame = JSONObject(value.toString())
                // These phone-only projection fields are absent from the actual Wear encoder.
                watchFrame.remove("nativeEditorProjection"); watchFrame.remove("phoneExecution162")
                watchFrame.remove("ownershipImport193")
            }
        })
        fun transfer(requester: OwnershipJournal193, source: OwnershipJournal193, targetNode: String,
                     sourceNode: String, frozen: () -> JSONObject) {
            val now = System.currentTimeMillis()
            val request = requester.request(targetNode, sourceNode, now).getJSONObject("outgoing")
            source.receive(request, targetNode, sourceNode, now)
            val offer = source.prepare(frozen(), sourceNode, now).getJSONObject("outgoing")
            val accepted = requester.receive(offer, sourceNode, targetNode, now).getJSONObject("outgoing")
            val committed = source.receive(accepted, targetNode, sourceNode, now).getJSONObject("outgoing")
            val installed = requester.receive(committed, sourceNode, targetNode, now).getJSONObject("outgoing")
            source.receive(installed, targetNode, sourceNode, now)
        }
        PhoneWorkoutExecution.store(context).suspendCommands(true)
        transfer(watch, phone, "watch-node", "phone-node") { ActiveWorkoutStore.current(context)!! }
        assertEquals("watch", ActiveWorkoutStore.current(context)!!.getString("timerOwner"))
        watchFrame.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(0)
            .put("done", true).put("reps", "10").put("weight", 15)
        if (timed) {
            watchFrame.getJSONArray("exercises").getJSONObject(0).apply {
                put("repUnit", "mp")
                getJSONArray("sets").getJSONObject(0).put("done", false)
            }
            watchFrame.put("automation", JSONObject("""{"enabled":true,"owner":"watch","phase":"work","workSeconds":10,"restSeconds":5,"rounds":2}"""))
            val timer = JSONObject("""
                {"key":"native|4|9","generation":4,"autoAdvance":true,"restOnly":false,"initialRest":3,
                 "steps":[{"exerciseId":"press","setId":"one","work":10,"rest":5,"rounds":2,"baseSeconds":7}],
                 "elapsedMs":18000,"phase":"work","stepIndex":0,"round":2,"remainingMs":10000,
                 "stepElapsedMs":15000,"totalMs":28000,"completed":0,"running":false,"stopped":false,
                 "ledger":{"schema":1,"owner":"timeline193","positionMs":18000,"workMs":10000,"restMs":8000,"pausedMs":9000,
                 "steps":[{"exerciseId":"press","setId":"one","workMs":10000,"restMs":8000,"pausedMs":9000}]}}
            """) .put("workoutId", watchFrame.getString("workoutId"))
            watchFrame.put("timerCheckpoint193", JSONObject().put("schema", 1).put("timer", timer).put("timeLogs193", JSONObject()))
        }
        transfer(phone, watch, "phone-node", "watch-node") { watchFrame }
        val returned = ActiveWorkoutStore.current(context)!!
        assertTrue(NativeWatchHandoffReceipt.confirmed(returned, phone.read(), returned.getString("workoutId"), "phone"))
        assertTrue(PhoneWorkoutExecution.store(context).commandsSuspended())
        assertThrows(IllegalStateException::class.java) { NativeWorkoutEditing.project(context) }
        return returned
    }

    @Test fun acknowledgedReturnPublishesEditablePausedWatchValuesAndRetryIsIdempotent() = runBlocking {
        roundTrip()
        val frame = NativePhoneReturnInstallation.complete(context)
        assertFalse(PhoneWorkoutExecution.store(context).commandsSuspended())
        assertEquals(1, frame.getInt("nativeEditorProjection"))
        assertTrue(frame.getBoolean("workoutPaused"))
        val set = frame.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(0)
        assertTrue(set.getBoolean("done")); assertEquals(150, set.getInt("weight") * set.getInt("reps"))
        assertEquals("installed", frame.getJSONObject("ownershipImport193").getString("state"))
        val retry = NativePhoneReturnInstallation.complete(context)
        assertEquals(frame.getLong("generation"), retry.getLong("generation"))
        assertEquals(frame.getJSONArray("exercises").toString(), retry.getJSONArray("exercises").toString())
    }

    @Test fun failedTimerArmRetainsFenceAndRetryArmsTheSameGenerationBeforeInstallingPausedTimer() = runBlocking {
        roundTrip(timed = true)
        try { NativePhoneReturnInstallation.complete(context) { error("Service unavailable") }; fail("Expected failed arm") }
        catch (_: IllegalStateException) { }
        val prepared = PhoneWorkoutExecution.store(context).snapshot()!!
        assertTrue(PhoneWorkoutExecution.store(context).commandsSuspended())
        val service = Robolectric.buildService(IntervalTimerService::class.java).create()
        try {
            val frame = NativePhoneReturnInstallation.complete(context) { next ->
                service.get().onStartCommand(Intent(context, IntervalTimerService::class.java).apply {
                    action = IntervalTimerService.ARM
                    putExtra("workoutId", next.getString("workoutId")); putExtra("generation", next.getLong("generation"))
                }, 0, 1)
            }
            assertEquals(prepared.getLong("generation"), frame.getLong("generation"))
            assertTrue(frame.getBoolean("workoutPaused"))
            assertFalse(PhoneWorkoutExecution.store(context).commandsSuspended())
            assertTrue(IntervalTimerService.armed(frame.getString("workoutId"), frame.getLong("generation")))
            assertFalse(IntervalTimerService.snapshot(context).optBoolean("running"))
        } finally { service.destroy() }
    }

    @Test fun missingOwnershipReceiptCannotUnlockLocalExecution() = runBlocking {
        NativeWorkoutSession.installPrepared(context, NativeWorkoutSession.quickCandidate(JSONObject("""
            {"id":"press","name":"Press","repUnit":"ism.","sets":[{"setId":"one","set":1,"reps":10,"done":false}]}
        """), TrainPilotUserSettings()))
        PhoneWorkoutExecution.store(context).suspendCommands(true)
        try { NativePhoneReturnInstallation.complete(context); fail("Expected receipt rejection") }
        catch (_: IllegalStateException) { }
        assertTrue(PhoneWorkoutExecution.store(context).commandsSuspended())
    }
    @Test fun returnedWorkoutCanActuallyFinalizeIntoRoomWithoutTheBusyError() {
        roundTrip()
        val frame = runBlocking { NativePhoneReturnInstallation.complete(context) }
        val database = androidx.room.Room.inMemoryDatabaseBuilder(context,
            com.repforge.app.data.db.TrainPilotDatabase::class.java).build()
        val worker = java.util.concurrent.Executors.newSingleThreadExecutor()
        try {
            val future = worker.submit<Unit> { runBlocking {
                TrainPilotWorkoutFinalization(context, database).finishWorkout(frame)
                val history = database.workoutHistoryDao().getWorkoutWithDetails(frame.getString("workoutId"))
                assertNotNull(history)
                assertEquals(1, database.workoutHistoryDao().getAllWorkoutsWithDetails().size)
                assertFalse(ActiveWorkoutStore.current(context)!!.getBoolean("active"))
            } }
            val deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(30)
            while (!future.isDone) {
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                if (System.nanoTime() > deadline) fail("Finalization timed out")
                try { future.get(10, java.util.concurrent.TimeUnit.MILLISECONDS) }
                catch (_: java.util.concurrent.TimeoutException) { }
            }
            future.get()
        } finally { worker.shutdownNow(); database.close() }
    }

}
