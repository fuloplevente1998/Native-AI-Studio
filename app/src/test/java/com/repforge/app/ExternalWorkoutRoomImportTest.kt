package com.repforge.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.repforge.app.data.db.HistoryTombstoneEntity
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.migration.NativeBackupPrivacy
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExternalWorkoutRoomImportTest {
    private fun workout(id: String, source: String = "example.watch", provider: String = HealthJournalStore.HC,
                        start: Long = 1_700_000_000_000L, clientId: String? = null) =
        ExternalWorkout(ExternalWorkoutIdentity(provider, source, id), start + 60_000, start,
            start + 60_000, 1, "Original title", clientId, null, null, null, null)

    @Test fun identityIsStableAndFramed() {
        val a = workout("record")
        assertEquals(ExternalWorkoutImport.localId(a.identity), ExternalWorkoutImport.localId(a.copy(startMs = a.startMs + 1).identity))
        assertNotEquals(ExternalWorkoutImport.localId(a.identity), ExternalWorkoutImport.localId(workout("record", "different.app").identity))
        assertNotEquals(ExternalWorkoutImport.localId(a.identity), ExternalWorkoutImport.localId(workout("record", provider = HealthJournalStore.SAMSUNG).identity))
    }

    @Test fun roomImportIsIdempotentAndNeverMergesOverlapOrResurrectsDeletion() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
        try {
            val a = workout("record")
            val id = ExternalWorkoutImport.localId(a.identity)
            suspend fun save(w: ExternalWorkout) = ExternalWorkoutImport.persist(context, db, w) {}
            assertEquals(ExternalWorkoutImport.Outcome.IMPORTED, save(a))
            assertEquals(ExternalWorkoutImport.Outcome.ALREADY_IMPORTED, save(a))
            val mirror = workout("other", provider = HealthJournalStore.SAMSUNG)
            assertEquals(ExternalWorkoutImport.Outcome.REVIEW_OVERLAP, save(mirror))
            assertEquals(ExternalWorkoutImport.Outcome.UPDATED, ExternalWorkoutImport.persist(context, db, mirror, targetId = id) {})
            assertEquals(ExternalWorkoutImport.Outcome.ALREADY_IMPORTED, save(mirror))
            assertTrue(ExternalWorkoutImport.localId(mirror.identity) in ExternalWorkoutAssociation.linkedIds(
                JSONObject(requireNotNull(db.workoutHistoryDao().getWorkoutWithDetailsById(id)).workout.rawJson)))
            assertEquals(ExternalWorkoutImport.Outcome.UNAVAILABLE, save(workout("self", clientId = "trainpilot:example")))
            val row = requireNotNull(db.workoutHistoryDao().getWorkoutWithDetailsById(id))
            assertEquals("external", row.workout.originDevice)
            assertTrue(row.exercises.isEmpty())
            assertNull(row.workout.health240Json)
            assertEquals(1, db.workoutHistoryDao().getAllWorkoutsWithDetails().size)
            val modified = a.copy(modifiedMs = a.modifiedMs + 1, title = "Updated")
            val metrics = JSONObject().put("values", JSONObject().put("steps", 42))
            assertEquals(ExternalWorkoutImport.Outcome.UPDATED, ExternalWorkoutImport.persist(context, db, modified, metrics) {})
            val refreshed = requireNotNull(db.workoutHistoryDao().getWorkoutWithDetailsById(id)).workout
            assertEquals(a.startMs, refreshed.startedEpochMs)
            assertEquals(42, JSONObject(refreshed.rawJson).getJSONObject("externalWorkout359").getJSONObject("metrics").getJSONObject("values").getInt("steps"))
            assertEquals(ExternalWorkoutImport.Outcome.ALREADY_IMPORTED, save(a))
            val moved = modified.copy(modifiedMs = modified.modifiedMs + 1, startMs = a.startMs + 1000)
            assertEquals(ExternalWorkoutImport.Outcome.UPDATED, save(moved))
            val review = requireNotNull(db.workoutHistoryDao().getWorkoutWithDetailsById(id)).workout
            assertEquals(a.startMs, review.startedEpochMs)
            assertTrue(JSONObject(review.rawJson).getJSONObject("externalWorkout359").getBoolean("windowChanged"))
            db.workoutHistoryDao().upsertTombstone(HistoryTombstoneEntity(id, id, JSONArray().put(id).toString(), a.endMs))
            db.workoutHistoryDao().deleteWorkoutById(id)
            assertEquals(ExternalWorkoutImport.Outcome.LOCALLY_DELETED, save(a))
            assertNull(db.workoutHistoryDao().getWorkoutWithDetailsById(id))
        } finally { db.close() }
        Unit
    }

    @Test fun existingImportsRefreshWithoutConsentToCreateNewHistory() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
        try {
            val a = workout("existing")
            val b = workout("unseen", start = a.endMs + 1000)
            assertEquals(ExternalWorkoutImport.Outcome.UNAVAILABLE,
                ExternalWorkoutImport.persist(context, db, a, allowCreate = false) {})
            assertEquals(ExternalWorkoutImport.Outcome.IMPORTED, ExternalWorkoutImport.persist(context, db, a) {})
            val late = JSONObject().put("values", JSONObject().put("distanceMeters", 150))
            assertEquals(ExternalWorkoutImport.Outcome.UPDATED,
                ExternalWorkoutImport.persist(context, db, a, late, allowCreate = false) {})
            assertEquals(ExternalWorkoutImport.Outcome.UNAVAILABLE,
                ExternalWorkoutImport.persist(context, db, b, allowCreate = false) {})
            val linked = workout("mirror", provider = HealthJournalStore.SAMSUNG)
            assertEquals(ExternalWorkoutImport.Outcome.UPDATED,
                ExternalWorkoutImport.persist(context, db, linked, targetId = ExternalWorkoutImport.localId(a.identity)) {})
            assertEquals(ExternalWorkoutImport.Outcome.UPDATED,
                ExternalWorkoutImport.persist(context, db, linked.copy(modifiedMs = linked.modifiedMs + 1,
                    startMs = linked.startMs + 1000), late, allowCreate = false) {})
            val row = requireNotNull(db.workoutHistoryDao().getWorkoutWithDetailsById(ExternalWorkoutImport.localId(a.identity)))
            val snapshot = ExternalWorkoutAssociation.snapshots(JSONObject(row.workout.rawJson)).single()
            assertTrue(snapshot.optBoolean("windowChanged"))
            assertFalse(snapshot.has("metrics"))
            assertEquals(1, db.workoutHistoryDao().getAllWorkoutsWithDetails().size)
        } finally { db.close() }
        Unit
    }

    @Test fun backupRestoreKeepsIncomingDeletionAndOnlyExistingLinks() {
        val row = ExternalWorkoutImport.raw(workout("a"))
        val link = ExternalWorkoutImport.localId(workout("mirror").identity)
        val unrelated = ExternalWorkoutImport.localId(workout("unrelated").identity)
        val old = JSONObject(row.toString()).put("externalLinkedIds359", JSONArray().put(link).put(unrelated))
            .put("externalLinkedSources359", JSONObject().put(link, workout("mirror").summary())
                .put(unrelated, workout("unrelated").summary()))
        val incoming = JSONObject(row.toString()).put("externalLinkedIds359", JSONArray().put(link))
        incoming.getJSONObject("externalWorkout359").put("sourceDeleted", true)
        val root = JSONObject().put("history", JSONArray().put(incoming))
        NativeBackupPrivacy.preserveLocalWorkoutHealth(root, listOf(old))
        assertTrue(incoming.getJSONObject("externalWorkout359").optBoolean("sourceDeleted"))
        val restored = incoming.getJSONObject("externalLinkedSources359")
        assertTrue(restored.has(link))
        assertFalse(restored.has(unrelated))
        val sanitized = NativeBackupPrivacy.sanitize(incoming, includeHealth = false)
        assertFalse(sanitized.has("externalLinkedSources359"))
        assertEquals(listOf(link), ExternalWorkoutAssociation.linkedIds(sanitized))
    }

    @Test fun uneditedSourceWindowCorrectionUpdatesCanonicalTimeline() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
        try {
            val first = workout("corrected")
            assertEquals(ExternalWorkoutImport.Outcome.IMPORTED, ExternalWorkoutImport.persist(context, db, first) {})
            val id = ExternalWorkoutImport.localId(first.identity)
            val corrected = first.copy(startMs = first.startMs + 1000, modifiedMs = first.modifiedMs + 1)
            assertEquals(ExternalWorkoutImport.Outcome.UPDATED,
                ExternalWorkoutImport.persist(context, db, corrected, allowCreate = false) {})
            val row = requireNotNull(db.workoutHistoryDao().getWorkoutWithDetailsById(id)).workout
            assertEquals(corrected.startMs, row.startedEpochMs)
            assertEquals(59, row.durationSeconds)
            assertFalse(JSONObject(row.rawJson).getJSONObject("externalWorkout359").optBoolean("windowChanged"))
            val edited = JSONObject(row.rawJson).put("note", "My note")
            assertEquals(1, db.workoutHistoryDao().updateJournalPhotoJson(id, row.rawJson, edited.toString()))
            val again = corrected.copy(startMs = corrected.startMs + 1000, modifiedMs = corrected.modifiedMs + 1)
            assertEquals(ExternalWorkoutImport.Outcome.UPDATED, ExternalWorkoutImport.persist(context, db, again) {})
            val preserved = requireNotNull(db.workoutHistoryDao().getWorkoutWithDetailsById(id)).workout
            assertEquals(corrected.startMs, preserved.startedEpochMs)
            assertTrue(JSONObject(preserved.rawJson).getJSONObject("externalWorkout359").optBoolean("windowChanged"))
            assertEquals("My note", JSONObject(preserved.rawJson).getString("note"))
        } finally { db.close() }
        Unit
    }

    @Test fun externalSessionCanBeAnnotatedWithoutInventingExercise() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
        try {
            val w = workout("annotate")
            assertEquals(ExternalWorkoutImport.Outcome.IMPORTED, ExternalWorkoutImport.persist(context, db, w) {})
            val detail = requireNotNull(db.workoutHistoryDao().getWorkoutWithDetailsById(ExternalWorkoutImport.localId(w.identity)))
            val draft = NativeJournalEditor.from(detail).copy(feedbackEffort = "hard", note = "Measured session")
            val encoded = NativeJournalEditor.encode(detail, draft, emptyMap())
            assertEquals(0, encoded.getJSONArray("exercises").length())
            assertEquals("hard", encoded.getJSONObject("feedback").getString("effort"))
            assertEquals("Measured session", encoded.getJSONObject("feedback").getString("note"))
            assertEquals(w.identity.recordId, encoded.getJSONObject("externalWorkout359").getString("id"))
        } finally { db.close() }
        Unit
    }

    @Test fun backupWithoutHealthConsentRemovesSourceDetailsButKeepsOpaqueIdentity() {
        val raw = ExternalWorkoutImport.raw(workout("record"))
        val sanitized = NativeBackupPrivacy.sanitize(raw, includeHealth = false)
        assertFalse(sanitized.has("externalWorkout359"))
        assertEquals(raw.getString("id"), sanitized.getString("id"))
        assertTrue(sanitized.getBoolean("externalWorkoutImported359"))
        assertFalse(sanitized.toString().contains("example.watch"))
        assertFalse(sanitized.toString().contains("Original title"))
        assertTrue(NativeBackupPrivacy.sanitize(raw, includeHealth = true).has("externalWorkout359"))
    }
}
