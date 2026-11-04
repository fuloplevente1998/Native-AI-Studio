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
import java.util.UUID
import java.util.concurrent.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.PAUSED)
class JournalPhotoAttachmentRegressionTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: TrainPilotDatabase
    private val files = mutableListOf<java.io.File>()

    @Before fun open() {
        val field = PhoneWorkoutExecution::class.java.getDeclaredField("store").apply { isAccessible = true }
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close(); field.set(null, null)
        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
    }
    @After fun close() { db.close(); files.forEach { it.delete() } }

    private fun <T> native(operation: suspend () -> T): T {
        val worker = Executors.newSingleThreadExecutor()
        val future = worker.submit<T> { runBlocking { operation() } }
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        try { while (true) {
            shadowOf(Looper.getMainLooper()).idle()
            try { return future.get(10, TimeUnit.MILLISECONDS) }
            catch (_: TimeoutException) { check(System.nanoTime() < deadline) }
        } } finally { future.cancel(true); worker.shutdownNow() }
    }

    private fun photo(): StoredPhotoMetadata {
        val id = UUID.randomUUID().toString()
        val file = WorkoutPhotoRepository(context).photoFile(id).apply { writeText("private photo"); files.add(this) }
        return StoredPhotoMetadata(id, file, 120, 160, file.length())
    }
    private fun seed(): WorkoutHistoryWithDetails = native {
        val id = UUID.randomUUID().toString()
        val previous = photo()
        val raw = JSONObject().put("id", id).put("key", id).put("started", "2026-11-01T10:00:00Z")
            .put("finished", "2026-11-01T10:30:00Z").put("active", false).put("programName", "Original")
            .put("feedback", JSONObject().put("note", "Keep this note").put("applied", true))
            .put("photos", JSONArray().put(JSONObject().put("id", previous.id).put("label", "before")
                .put("createdAt", "2026-11-01T10:00:00Z").put("width", previous.width)
                .put("height", previous.height).put("bytes", previous.bytes)))
            .put("exercises", JSONArray().put(JSONObject().put("id", "press").put("sets", JSONArray()
                .put(JSONObject().put("setId", "set-$id").put("set", 1).put("weight", 20)
                    .put("reps", 10).put("actualRir", 2).put("done", true)))))
        LegacyWebViewMigrator(context, db, TrainPilotSettingsStore(context)).saveWorkoutJsonToRoom(raw)
        db.workoutHistoryDao().getWorkoutWithDetailsById(id)!!
    }
    private fun detail(id: String) = native { db.workoutHistoryDao().getWorkoutWithDetailsById(id)!! }
    private fun attach(id: String, photo: StoredPhotoMetadata, label: String = "after") = native {
        NativeJournalPhotos.attach(context, db, NativeJournalPhotos.prepare(context, id, label), photo)
    }

    @Test fun cameraOrGalleryAttachmentPreservesEveryExistingRelationAndOtherWorkout() {
        val before = seed()
        val other = seed()
        val added = photo()
        attach(before.workout.id, added)
        val saved = detail(before.workout.id)
        assertEquals(1, saved.exercises.size)
        assertEquals(1, saved.exercises.single().sets.size)
        assertEquals(before.exercises, saved.exercises)
        assertEquals(before.workout, saved.workout.copy(rawJson = before.workout.rawJson))
        assertTrue(saved.photos.containsAll(before.photos))
        assertEquals(2, saved.photos.size)
        assertEquals("after", saved.photos.single { it.id == added.id }.label)
        assertEquals(other, detail(other.workout.id))
        val original = JSONObject(before.workout.rawJson)
        val updated = JSONObject(saved.workout.rawJson)
        assertEquals(2, updated.getJSONArray("photos").length())
        original.remove("photos"); updated.remove("photos")
        assertEquals(original.toString(), updated.toString())
    }

    @Test fun secondAttachmentKeepsFirstPhotoAndAllThreeCategories() {
        val before = seed()
        val after = photo(); val other = photo()
        attach(before.workout.id, after, "after")
        attach(before.workout.id, other, "other")
        val saved = detail(before.workout.id)
        assertEquals(before.exercises, saved.exercises)
        assertEquals(setOf("before", "after", "other"), saved.photos.map { it.label }.toSet())
        assertEquals(3, JSONObject(saved.workout.rawJson).getJSONArray("photos").length())
    }

    @Test fun duplicatePhotoRollsBackJsonAndCannotMoveAnotherWorkoutsPhoto() {
        val first = seed(); val second = seed()
        val shared = photo()
        attach(first.workout.id, shared)
        assertTrue(runCatching { attach(second.workout.id, shared) }.isFailure)
        assertEquals(second, detail(second.workout.id))
        assertTrue(detail(first.workout.id).photos.any { it.id == shared.id })
    }

    @Test fun deletedWorkoutCannotBeResurrectedByLateCameraResult() {
        val before = seed()
        val request = NativeJournalPhotos.prepare(context, before.workout.id, "after")
        native { db.workoutHistoryDao().deleteWorkoutById(before.workout.id) }
        val added = photo()
        assertTrue(runCatching { native { NativeJournalPhotos.attach(context, db, request, added) } }.isFailure)
        assertNull(native { db.workoutHistoryDao().getWorkoutWithDetailsById(before.workout.id) })
        assertEquals(0, native { db.workoutHistoryDao().getWorkoutCount() })
    }

    @Test fun tombstonedAliasAndRestoreRejectPendingAttachmentWithoutChangingHistory() {
        val before = seed()
        val added = photo()
        native { db.workoutHistoryDao().upsertTombstone(HistoryTombstoneEntity(
            before.workout.id, before.workout.workoutKey, JSONArray().put(before.workout.id).toString(), 1)) }
        assertTrue(runCatching { attach(before.workout.id, added) }.isFailure)
        assertEquals(before, detail(before.workout.id))
        native { db.workoutHistoryDao().deleteAllTombstones() }
        val request = NativeJournalPhotos.prepare(context, before.workout.id, "other")
        native { NativeDataReplacement.exclusively(context) { Unit } }
        assertTrue(runCatching { native { NativeJournalPhotos.attach(context, db, request, added) } }.isFailure)
        assertEquals(before, detail(before.workout.id))
    }
}
