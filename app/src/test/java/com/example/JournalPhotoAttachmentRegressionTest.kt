package com.example

import android.content.Context
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.repforge.app.*
import com.repforge.app.data.db.*
import com.repforge.app.data.migration.NativeBackupPrivacy
import com.repforge.app.data.migration.LegacyWebViewMigrator
import com.repforge.app.data.store.TrainPilotSettingsStore
import com.repforge.app.ui.progress.NativeProgressPhotoData
import com.repforge.app.data.store.TrainPilotUserSettings
import com.repforge.app.ui.progress.NativeWeightJournalRepository
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

    @Test fun weightPhotoStaysAttachedToTheSameMeasurementAndCannotBeDuplicated() {
        val id = UUID.randomUUID().toString()
        val weight = native {
            val raw = JSONObject()
                .put("id", id)
                .put("date", "2026-11-21")
                .put("dayKey", "2026-11-21")
                .put("kg", 82.5)
            db.bodyWeightDao().upsertWeight(BodyWeightEntryEntity(
                id, "2026-11-21", "2026-11-21", 82.5, 1L, raw.toString()
            ))
            db.bodyWeightDao().getWeightById(id)!!
        }
        val added = photo()
        native {
            NativeWeightPhotos.attach(
                context,
                db,
                NativeWeightPhotos.prepare(context, weight.id),
                added
            )
        }
        val attached = native { db.bodyWeightDao().getWeightById(id)!! }
        val photos = JSONObject(attached.rawJson).getJSONArray("photos")
        assertEquals(1, photos.length())
        assertEquals(added.id, photos.getJSONObject(0).getString("id"))
        assertEquals("other", photos.getJSONObject(0).getString("label"))
        val projected = native { NativeProgressPhotoData.load(db) }
        assertEquals(1, projected.count { it.id == added.id && it.kind == "weight" })
        assertTrue(runCatching {
            native {
                NativeWeightPhotos.attach(
                    context,
                    db,
                    NativeWeightPhotos.prepare(context, weight.id),
                    added
                )
            }
        }.isFailure)

        native {
            NativeWeightJournalRepository(context, db).edit(attached, 81.4, "2026-11-20")
            val edited = db.bodyWeightDao().getWeightById(id)!!
            assertEquals("2026-11-20", edited.dayKey)
            assertEquals(added.id, JSONObject(edited.rawJson).getJSONArray("photos").getJSONObject(0).getString("id"))
            assertEquals(id, NativeProgressPhotoData.load(db).single { it.id == added.id }.weightId)
        }
    }

    @Test fun stalePickerResultsAfterReplacementLeaveNeitherReferencesNorPrivateFiles() = native {
        val repository = NativeWeightJournalRepository(context, db)
        repository.add(82.5, "2026-11-21")
        val old = db.bodyWeightDao().getAllWeights().single()
        val weightRequest = NativeWeightPhotos.prepare(context, old.id)
        val looseRequest = NativeLoosePhotos.prepare(context)
        val replacement = old.copy(kg = 79.0, rawJson = JSONObject(old.rawJson).put("kg", 79.0).toString())
        NativeDataReplacement.exclusively(context) {
            db.bodyWeightDao().deleteAllWeights()
            db.bodyWeightDao().upsertWeight(replacement)
        }

        val weightPhoto = photo()
        val loosePhoto = photo()
        assertTrue(runCatching {
            NativeWeightPhotos.importAndAttach(context, db, weightRequest) { Result.success(weightPhoto) }
        }.isFailure)
        assertTrue(runCatching {
            NativeLoosePhotos.importAndAttach(context, db, looseRequest) { Result.success(loosePhoto) }
        }.isFailure)
        assertEquals(replacement, db.bodyWeightDao().getWeightById(old.id))
        assertEquals(0, NativeLoosePhotos.count(db))
        assertFalse(weightPhoto.file.exists())
        assertFalse(loosePhoto.file.exists())
    }

    @Test fun deletingOneRestoredWeightEntryKeepsFilesReferencedByAnotherEntry() = native {
        val repository = NativeWeightJournalRepository(context, db)
        val shared = photo()
        val unique = photo()
        fun backedUpWeight(id: String, kg: Double, vararg photos: StoredPhotoMetadata): JSONObject =
            JSONObject().put("id", id).put("date", "2026-11-21").put("kg", kg)
                .put("photos", JSONArray(photos.map { JSONObject().put("id", it.id) }))

        val first = UUID.randomUUID().toString()
        val second = UUID.randomUUID().toString()
        val weights = JSONArray().put(backedUpWeight(first, 82.5, unique, shared))
            .put(backedUpWeight(second, 83.0, shared))
        val backup = JSONObject().put("history", JSONArray()).put("weights", weights)
        LegacyWebViewMigrator(context, db, TrainPilotSettingsStore(context)).importBackupJson(backup, replaceAll = false)

        repository.delete(db.bodyWeightDao().getWeightById(first)!!)
        assertNull(db.bodyWeightDao().getWeightById(first))
        assertFalse(unique.file.exists())
        assertTrue(shared.file.exists())
        assertEquals(second, NativeProgressPhotoData.load(db).single { it.id == shared.id }.weightId)

        repository.delete(db.bodyWeightDao().getWeightById(second)!!)
        assertFalse(shared.file.exists())
    }

    @Test fun weightJournalEditsAreFencedAndDeletionTombstonesOnlyTheExactMeasurement() = native {
        val firstId = UUID.randomUUID().toString()
        val secondId = UUID.randomUUID().toString()
        val firstRaw = JSONObject().put("id", firstId).put("date", "2026-11-21")
            .put("dayKey", "2026-11-21").put("kg", 82.5).put("portableNote", "preserve")
        db.bodyWeightDao().upsertWeight(BodyWeightEntryEntity(
            firstId, "2026-11-21", "2026-11-21", 82.5, 1L, firstRaw.toString()
        ))
        db.bodyWeightDao().upsertWeight(BodyWeightEntryEntity(
            secondId, "2026-11-21", "2026-11-21", 83.0, 2L,
            JSONObject().put("id", secondId).put("date", "2026-11-21").put("kg", 83.0).toString()
        ))

        val repository = NativeWeightJournalRepository(context, db, now = { 100L })
        val expected = db.bodyWeightDao().getWeightById(firstId)!!
        repository.edit(expected, 23.4, "2026-11-20")
        val edited = db.bodyWeightDao().getWeightById(firstId)!!
        assertEquals(23.4, edited.kg, 0.0)
        assertEquals("2026-11-20", edited.dayKey)
        assertEquals("preserve", JSONObject(edited.rawJson).getString("portableNote"))
        assertTrue("edit timestamp must advance beyond the old record", edited.updatedAt > expected.updatedAt)

        repository.delete(edited)
        assertNull(db.bodyWeightDao().getWeightById(firstId))
        assertNotNull("a second measurement on the same day must remain", db.bodyWeightDao().getWeightById(secondId))
        val index = NativeWeightTombstones18.Index(NativeWeightTombstones18.load(db))
        assertTrue(index.deleted(JSONObject().put("id", firstId).put("date", edited.date).put("kg", edited.kg)))

        repository.add(20.5, "2026-11-21")
        assertTrue(db.bodyWeightDao().getAllWeights().any { it.kg == 20.5 })
    }

    @Test fun weightAndLoosePhotosAreUploadedThroughAuthorizedDriveSync() {
        val added = photo()
        val loose = photo()
        val row = JSONObject().put("id", UUID.randomUUID().toString()).put("date", "2026-11-21")
            .put("kg", 82.5).put("photos", JSONArray().put(JSONObject().put("id", added.id).put("label", "other")))
        val looseRow = JSONObject().put("id", loose.id).put("label", "other")
            .put("createdAt", "2026-11-21T10:00:00Z")
        val data = JSONObject().put("history", JSONArray()).put("weights", JSONArray().put(row))
            .put("loosePhotos", JSONArray().put(looseRow))
        val stage = NativeCloudPhotoStage(context, data)
        var uploads = 0
        try {
            stage.prepare(
                upload = { _, _ -> uploads++; "drive-photo-$uploads" },
                download = { _, _, _ -> fail("Local photos should upload, not download.") }
            )
            assertEquals(2, uploads)
            assertTrue(File(stage.photos, "${added.id}.jpg").isFile)
            assertTrue(File(stage.photos, "${loose.id}.jpg").isFile)
            stage.requireUnchanged()
            val drive = NativeBackupPrivacy.sanitize(data, includeHealth = false)
            assertTrue(drive.getJSONArray("weights").getJSONObject(0).has("photos"))
            assertTrue(drive.has("loosePhotos"))
        } finally {
            stage.close()
        }
    }

    @Test fun loosePhotoPersistsInTheGlobalGalleryAndManualBackup() = native {
        val added = photo()
        NativeLoosePhotos.attach(context, db, NativeLoosePhotos.prepare(context), added)

        val projected = NativeProgressPhotoData.load(db)
        assertTrue(projected.any { it.id == added.id && it.kind == "other" })
        val backup = LegacyWebViewMigrator(context, db, TrainPilotSettingsStore(context))
            .exportBackupJson(includeHealthJournal = false)
        val backupPhotos = NativeCloudPhotoStage.photoRows(backup)
        assertTrue(backupPhotos.any { it.optString("id") == added.id && it.optString("label") == "other" })
        assertEquals(1, NativeLoosePhotos.count(db))
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

    private fun activeWorkout(): JSONObject = NativeWorkoutSession.installPrepared(context,
        NativeWorkoutSession.quickCandidate(JSONObject("""
            {"id":"press","name":"Press","repUnit":"ism.","measurementType":"reps","loadType":"weight",
             "sets":[{"setId":"photo-set","set":1,"weight":20,"reps":10,"done":true}]}
        """), TrainPilotUserSettings()))

    @Test fun summaryPhotoSurvivesNativeProjectionAndActuallyFinalizesIntoRoom() {
        val frame = activeWorkout()
        val id = frame.getString("workoutId")
        val added = photo()
        val request = NativeWorkoutPhotos.prepare(context, id, "before")
        native { NativeWorkoutPhotos.attach(context, request, added) }
        val projected = NativeWorkoutEditing.project(context)
        assertEquals(added.id, projected.getJSONArray("photos").getJSONObject(0).getString("id"))
        assertEquals(added.id, PhoneWorkoutExecution.store(context).snapshot()!!
            .getJSONArray("photos").getJSONObject(0).getString("id"))
        native { TrainPilotWorkoutFinalization(context, db).finishWorkout(projected) }
        val saved = detail(id)
        assertEquals(1, saved.exercises.single().sets.size)
        assertTrue(saved.exercises.single().sets.single().done)
        assertEquals(added.id, saved.photos.single().id)
        assertEquals("before", saved.photos.single().label)
        assertEquals(added.id, JSONObject(saved.workout.rawJson).getJSONArray("photos").getJSONObject(0).getString("id"))
        val late = photo()
        assertTrue(runCatching { native { NativeWorkoutPhotos.attach(context, request, late) } }.isFailure)
        assertEquals(saved, detail(id))
    }

    @Test fun summaryRemovalIsDurableAndWrongGenerationCannotReceivePhoto() {
        val frame = activeWorkout()
        val id = frame.getString("workoutId")
        val added = photo()
        val request = NativeWorkoutPhotos.prepare(context, id, "other")
        assertTrue(runCatching { native {
            NativeWorkoutPhotos.attach(context, request.copy(generation = request.generation + 1), added)
        } }.isFailure)
        native { NativeWorkoutPhotos.attach(context, request, added) }
        native { NativeWorkoutPhotos.remove(context, request, added.id) }
        val projected = NativeWorkoutEditing.project(context)
        assertEquals(0, projected.getJSONArray("photos").length())
        assertEquals(frame.getJSONArray("exercises").toString(), projected.getJSONArray("exercises").toString())
    }
}
