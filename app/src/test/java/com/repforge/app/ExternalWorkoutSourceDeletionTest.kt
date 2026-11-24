package com.repforge.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.repforge.app.data.db.TrainPilotDatabase
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExternalWorkoutSourceDeletionTest {
    @Test fun deletedSourceSnapshotCannotBeRepopulatedBySameVersion() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
        try {
            val w = ExternalWorkout(ExternalWorkoutIdentity(HealthJournalStore.HC, "third.party", "a"),
                1700000060000, 1700000000000, 1700000060000, 1, null, null, null, null, null, null)
            val metrics = JSONObject().put("values", JSONObject().put("steps", 12))
            assertEquals(ExternalWorkoutImport.Outcome.IMPORTED, ExternalWorkoutImport.persist(context, db, w, metrics) {})
            val id = ExternalWorkoutImport.localId(w.identity)
            val dao = db.workoutHistoryDao()
            val row = requireNotNull(dao.getWorkoutWithDetailsById(id)).workout
            val raw = JSONObject(row.rawJson).put("note", "local")
            val source = raw.getJSONObject("externalWorkout359")
            source.put("sessionEvidence", JSONObject().put("vo2Max", 42))
            ExternalWorkoutSourceDeletion.markDeleted(source)
            assertFalse(source.has("metrics"))
            assertFalse(source.has("sessionEvidence"))
            assertFalse(source.has("segments"))
            assertEquals(1, dao.updateJournalPhotoJson(id, row.rawJson, raw.toString()))
            assertEquals(ExternalWorkoutImport.Outcome.ALREADY_IMPORTED, ExternalWorkoutImport.persist(context, db, w, metrics) {})
            val current = JSONObject(requireNotNull(dao.getWorkoutWithDetailsById(id)).workout.rawJson)
            assertEquals("local", current.getString("note"))
            assertTrue(current.getJSONObject("externalWorkout359").optBoolean("sourceDeleted"))
            assertFalse(current.getJSONObject("externalWorkout359").has("metrics"))
        } finally { db.close() }
        Unit
    }

    @Test fun onlyUnannotatedCopiesMayBeAutomaticallyRemoved() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
        try {
            val w = ExternalWorkout(ExternalWorkoutIdentity(HealthJournalStore.HC, "third.party", "a"),
                1700000060000, 1700000000000, 1700000060000, 1, null, null, null, null, null, null)
            assertEquals(ExternalWorkoutImport.Outcome.IMPORTED, ExternalWorkoutImport.persist(context, db, w) {})
            val detail = requireNotNull(db.workoutHistoryDao().getWorkoutWithDetailsById(ExternalWorkoutImport.localId(w.identity)))
            val raw = JSONObject(detail.workout.rawJson)
            assertTrue(ExternalWorkoutSourceDeletion.pureCopy(detail, raw))
            assertFalse(ExternalWorkoutSourceDeletion.pureCopy(detail, JSONObject(raw.toString()).put("note", "local")))
            assertFalse(ExternalWorkoutSourceDeletion.pureCopy(detail.copy(workout = detail.workout.copy(editedAt = "changed")), raw))
        } finally { db.close() }
        Unit
    }
}
