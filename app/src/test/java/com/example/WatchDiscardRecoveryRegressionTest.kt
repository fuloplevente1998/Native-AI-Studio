package com.example

import android.content.Context
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.repforge.app.*
import com.repforge.app.data.db.HistoryTombstoneEntity
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.store.TrainPilotUserSettings
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.PAUSED)
class WatchDiscardRecoveryRegressionTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: TrainPilotDatabase
    private val command get() = JSONObject().put("commandId", "old-discard").put("workoutId", "old")
        .put("action", "discardWorkout").put("sequence", 17)
    private fun orphan() = command.put("schema", 1).put("confirmed", true).put("ownerEpoch193", 0)
        .put("_uri", "wear://watch-node" + WearCommandListenerService.COMMAND_PATH_PREFIX + "old-discard")
        .put("finalSnapshot", frame().put("ownershipProtocol193", 1).put("timerOwner", "watch").put("ownerEpoch193", 0))
    private fun frame(id: String = "old") = JSONObject().put("workoutId", id).put("active", true)
    @Before fun prepare() {
        val field = PhoneWorkoutExecution::class.java.getDeclaredField("store").apply { isAccessible = true }
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close(); field.set(null, null)
        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
    }
    @After fun close() { db.close() }
    private fun <T> native(block: suspend () -> T): T {
        val worker = Executors.newSingleThreadExecutor()
        val future = worker.submit<T> { runBlocking { block() } }
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        try {
            while (true) {
                shadowOf(Looper.getMainLooper()).idle()
                try { return future.get(10, TimeUnit.MILLISECONDS) }
                catch (_: TimeoutException) { check(System.nanoTime() < deadline) }
            }
        } finally { future.cancel(true); worker.shutdownNow() }
    }
    private suspend fun tombstone() = db.workoutHistoryDao().upsertTombstone(
        HistoryTombstoneEntity("old", "old", "[\"old\"]", 1))

    @Test fun durableDeletionConfirmsAnOldRequestAfterPhoneDraftHasClosed() {
        assertEquals(NativeWatchDiscardAdmission.Decision.REJECT,
            native { NativeWatchDiscardAdmission.resolve(context, db, command) })
        native { tombstone() }
        assertEquals(NativeWatchDiscardAdmission.Decision.ALREADY_DISCARDED,
            native { NativeWatchDiscardAdmission.resolve(context, db, command) })
        assertNull(ActiveWorkoutStore.current(context))
        assertEquals(1, native { db.workoutHistoryDao().getTombstones().size })
    }
    @Test fun confirmingAnOldDeletionPreservesANewActiveWorkoutAndItsEngine() {
        NativeWorkoutSession.installPrepared(context, NativeWorkoutSession.quickCandidate(JSONObject("""
            {"id":"press","name":"Press","repUnit":"ism.","measurementType":"reps","loadType":"weight",
             "sets":[{"setId":"new-one","set":1,"reps":10,"weight":15,"done":false}]}
        """), TrainPilotUserSettings()))
        val current = ActiveWorkoutStore.current(context)!!.toString()
        val engine = PhoneWorkoutExecution.store(context).snapshot()!!.toString()
        assertEquals(NativeWatchDiscardAdmission.Decision.RECORD_ORPHAN_DISCARD,
            native { NativeDataReplacement.mutate(context) { NativeWatchDiscardAdmission.reconcile(context, db, orphan()) } })
        assertEquals(NativeWatchDiscardAdmission.Decision.ALREADY_DISCARDED,
            native { NativeWatchDiscardAdmission.resolve(context, db, orphan()) })
        assertEquals(1, native { db.workoutHistoryDao().getTombstones().size })
        for (action in listOf("startWorkout", "startQuickWorkout", "addQuickExercise", "watchAutomationState", "resumeWorkout")) {
            assertTrue(native { NativeWatchDiscardAdmission.rejectsResurrection(db, command.put("action", action)) })
            assertFalse(native { NativeWatchDiscardAdmission.rejectsResurrection(db, command.put("workoutId", "new").put("action", action)) })
        }
        assertEquals(current, ActiveWorkoutStore.current(context)!!.toString())
        assertEquals(engine, PhoneWorkoutExecution.store(context).snapshot()!!.toString())
    }
    @Test fun savedLegacyAliasCannotBeHiddenByAnOrphanDiscardMarker() {
        native {
            com.repforge.app.data.migration.LegacyWebViewMigrator(context, db,
                com.repforge.app.data.store.TrainPilotSettingsStore(context)).saveWorkoutJsonToRoom(
                JSONObject().put("id", "saved-real").put("started", "2026-11-02T10:00:00Z")
                    .put("finished", "2026-11-02T10:10:00Z").put("exercises", org.json.JSONArray()))
            db.openHelper.writableDatabase.execSQL(
                "UPDATE workout_history SET workoutKey = ?, syncId = ? WHERE id = ?",
                arrayOf("old", "different-sync", "saved-real"))
            assertTrue(db.workoutHistoryDao().hasSavedWorkoutId("old"))
            // A late start or periodic mirror after upgrade must not resurrect saved aliases either.
            for (action in listOf("startWorkout", "startQuickWorkout", "addQuickExercise", "watchAutomationState", "resumeWorkout")) {
                assertTrue(NativeWatchDiscardAdmission.rejectsResurrection(db, command.put("action", action)))
                assertTrue(NativeWatchDiscardAdmission.rejectsResurrection(db, command.put("action", action).put("workoutId", "saved-real")))
                assertFalse(NativeWatchDiscardAdmission.rejectsResurrection(db, command.put("action", action).put("workoutId", "recoverable-draft")))
            }
            assertEquals(NativeWatchDiscardAdmission.Decision.REJECT,
                NativeDataReplacement.mutate(context) { NativeWatchDiscardAdmission.reconcile(context, db, orphan()) })
            assertFalse(db.workoutHistoryDao().isTombstoned("old", JSONObject.quote("old")))
            assertNotNull(db.workoutHistoryDao().getWorkoutWithDetails("saved-real"))
        }
    }
    @Test fun absenceSavedHistoryAndForeignWorkoutsNeverAuthorizeDeletion() {
        for (current in listOf(null, frame().put("active", false), frame("new")))
            assertEquals(NativeWatchDiscardAdmission.Decision.REJECT,
                NativeWatchDiscardAdmission.decide(command, current, null, true, false, false))
        assertEquals(NativeWatchDiscardAdmission.Decision.REJECT,
            NativeWatchDiscardAdmission.decide(command, frame(), frame(), true, false, true))
        assertEquals(NativeWatchDiscardAdmission.Decision.REJECT,
            NativeWatchDiscardAdmission.decide(command, frame(), frame("new"), true, false, false))
    }
    @Test fun activeExecutionAndUnfinishedSaveCannotBeMistakenForConfirmedDeletion() {
        assertEquals(NativeWatchDiscardAdmission.Decision.REJECT,
            NativeWatchDiscardAdmission.decide(command, null, frame(), false, true, false))
        assertEquals(NativeWatchDiscardAdmission.Decision.REJECT,
            NativeWatchDiscardAdmission.decide(command, frame().put("finalizationPending", true), frame(), true, true, false))
        assertEquals(NativeWatchDiscardAdmission.Decision.REJECT,
            NativeWatchDiscardAdmission.decide(command, null, null, true, true, false, restoreBlocked = true))
    }
    @Test fun orphanDiscardNeedsExplicitConfirmationAndBoundSnapshotAndTransportIdentity() {
        assertTrue(NativeWatchDiscardAdmission.orphanProof(orphan()))
        assertTrue(NativeWatchDiscardAdmission.orphanProof(orphan().apply { getJSONObject("finalSnapshot").put("timerOwner", "phone") }))
        for ((key, value) in listOf("confirmed" to false, "ownerEpoch193" to 1, "_uri" to "wear://watch-node/other"))
            assertFalse(key, NativeWatchDiscardAdmission.orphanProof(orphan().put(key, value)))
        assertFalse(NativeWatchDiscardAdmission.orphanProof(orphan().apply { getJSONObject("finalSnapshot").put("workoutId", "new") }))
        assertEquals(NativeWatchDiscardAdmission.Decision.REJECT,
            NativeWatchDiscardAdmission.decide(command, null, null, false, false, true, orphanAuthorized = true))
    }
    @Test fun onlyMatchingOwnedDraftCanApplyOrRetryItsPendingDiscard() {
        assertEquals(NativeWatchDiscardAdmission.Decision.APPLY,
            NativeWatchDiscardAdmission.decide(command, frame(), frame(), true, false, false))
        assertEquals(NativeWatchDiscardAdmission.Decision.APPLY,
            NativeWatchDiscardAdmission.decide(command, frame().put("finalizationPending", true).put("discardPending", true), frame(), true, true, false))
        assertEquals(NativeWatchDiscardAdmission.Decision.REJECT,
            NativeWatchDiscardAdmission.decide(command, frame(), frame(), false, false, false))
    }
}
