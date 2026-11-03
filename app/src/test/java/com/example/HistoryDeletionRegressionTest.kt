package com.example

import android.content.Context
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.repforge.app.*
import com.repforge.app.data.db.*
import com.repforge.app.data.migration.LegacyWebViewMigrator
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.PAUSED)
class HistoryDeletionRegressionTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: TrainPilotDatabase
    @Before fun open() {
        val field = PhoneWorkoutExecution::class.java.getDeclaredField("store").apply { isAccessible = true }
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close(); field.set(null, null)
        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
    }
    @After fun close() { db.close() }
    private fun <T> native(block: suspend () -> T): T {
        val worker = Executors.newSingleThreadExecutor()
        val result = worker.submit<T> { runBlocking { block() } }
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        try { while (true) {
            shadowOf(Looper.getMainLooper()).idle()
            try { return result.get(10, TimeUnit.MILLISECONDS) }
            catch (_: TimeoutException) { check(System.nanoTime() < deadline) }
        } } finally { result.cancel(true); worker.shutdownNow() }
    }
    private fun seed(route: String = "", photo: String? = null): WorkoutHistoryWithDetails = native {
        val id = UUID.randomUUID().toString()
        val set = JSONObject().put("setId", "set-$id").put("set", 1).put("weight", 20).put("reps", 10).put("done", true)
        if (route.isNotEmpty()) set.put("gpsRouteKey163", route)
        val raw = JSONObject().put("id", id).put("key", id).put("started", "2026-11-01T10:00:00Z")
            .put("finished", "2026-11-01T10:30:00Z").put("active", false).put("programName", "Test")
            .put("exercises", JSONArray().put(JSONObject().put("id", "press").put("sets", JSONArray().put(set))))
        LegacyWebViewMigrator(context, db, TrainPilotSettingsStore(context)).saveWorkoutJsonToRoom(raw)
        if (photo != null) db.workoutHistoryDao().insertPhotos(listOf(WorkoutPhotoEntity(photo, id, "", "2026-11-01T10:00:00Z", 0, 1, 1, 4, null, null)))
        db.workoutHistoryDao().getWorkoutWithDetailsById(id)!!
    }
    private fun routeFile(key: String): File {
        val digest = MessageDigest.getInstance("SHA-256").digest(key.toByteArray()).joinToString("") { "%02x".format(it) }
        return File(context.noBackupFilesDir, "gps_routes/$digest.route").also { it.parentFile!!.mkdirs() }
    }
    private fun delete(target: WorkoutHistoryWithDetails) = native {
        NativeHistoryDeletion.delete(context, db, target, NativeDataReplacement.continuationForCurrentData(context))
    }
    @Test fun actualDeletionRemovesRelationsAndPrivateFilesButPreservesAnotherWorkout() {
        val photo = UUID.randomUUID().toString()
        val file = WorkoutPhotoRepository(context).photoFile(photo).apply { writeText("photo") }
        val route = "test:${UUID.randomUUID()}"
        val routeFile = routeFile(route).apply { writeText("route") }
        val target = seed(route, photo)
        val other = seed()
        delete(target)
        assertNull(native { db.workoutHistoryDao().getWorkoutWithDetailsById(target.workout.id) })
        assertNotNull(native { db.workoutHistoryDao().getWorkoutWithDetailsById(other.workout.id) })
        assertNotNull(native { db.workoutHistoryDao().getHistoryTombstone(target.workout.id) })
        assertEquals(1, native { db.workoutHistoryDao().getWorkoutCount() })
        assertFalse(file.exists()); assertFalse(routeFile.exists())
        assertTrue(native { db.keyValueDocumentDao().getHistoryCleanupDocuments().isEmpty() })
    }
    @Test fun anotherWorkoutsPhotoAndRouteReferencesArePreserved() {
        val photo = UUID.randomUUID().toString()
        val file = WorkoutPhotoRepository(context).photoFile(photo).apply { writeText("shared") }
        val route = "test:${UUID.randomUUID()}"
        val routeFile = routeFile(route).apply { writeText("shared") }
        val target = seed(route, photo)
        val other = seed(route)
        native { db.workoutHistoryDao().insertWorkout(other.workout.copy(rawJson =
            JSONObject(other.workout.rawJson).put("sharedPhoto", photo).toString())) }
        delete(target)
        assertTrue(file.exists()); assertTrue(routeFile.exists())
        assertNotNull(native { db.workoutHistoryDao().getWorkoutWithDetailsById(other.workout.id) })
        file.delete(); routeFile.delete()
    }
    @Test fun staleConfirmationCannotDeleteAChangedWorkout() {
        val target = seed()
        native { db.workoutHistoryDao().insertWorkout(target.workout.copy(programName = "Changed")) }
        assertTrue(runCatching { delete(target) }.isFailure)
        assertEquals("Changed", native { db.workoutHistoryDao().getWorkoutWithDetailsById(target.workout.id)!!.workout.programName })
        assertNull(native { db.workoutHistoryDao().getHistoryTombstone(target.workout.id) })
    }
    @Test fun failedFileCleanupRetainsAReceiptAndCanRecoverAfterRestart() {
        val route = "test:${UUID.randomUUID()}"
        val file = routeFile(route).apply { mkdirs(); resolve("blocked").writeText("test") }
        val target = seed(route)
        assertTrue(runCatching { delete(target) }.isFailure)
        assertNull(native { db.workoutHistoryDao().getWorkoutWithDetailsById(target.workout.id) })
        assertEquals(1, native { db.keyValueDocumentDao().getHistoryCleanupDocuments().size })
        file.resolve("blocked").delete(); file.delete(); file.writeText("route")
        native { NativeHistoryDeletion.recover(context, db) }
        assertFalse(file.exists())
        assertTrue(native { db.keyValueDocumentDao().getHistoryCleanupDocuments().isEmpty() })
    }
}
