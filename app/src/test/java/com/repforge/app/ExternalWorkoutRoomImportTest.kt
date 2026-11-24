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
        Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build().use { db ->
            val a = workout("record")
            val id = ExternalWorkoutImport.localId(a.identity)
            suspend fun save(w: ExternalWorkout) = ExternalWorkoutImport.persist(context, db, w) {}
            assertEquals(ExternalWorkoutImport.Outcome.IMPORTED, save(a))
            assertEquals(ExternalWorkoutImport.Outcome.ALREADY_IMPORTED, save(a))
            assertEquals(ExternalWorkoutImport.Outcome.REVIEW_OVERLAP, save(workout("other", provider = HealthJournalStore.SAMSUNG)))
            assertEquals(ExternalWorkoutImport.Outcome.UNAVAILABLE, save(workout("self", clientId = "trainpilot:example")))
            val row = requireNotNull(db.workoutHistoryDao().getWorkoutWithDetailsById(id))
            assertEquals("external", row.workout.originDevice)
            assertTrue(row.exercises.isEmpty())
            assertNull(row.workout.health240Json)
            assertEquals(1, db.workoutHistoryDao().getAllWorkoutsWithDetails().size)
            db.workoutHistoryDao().upsertTombstone(HistoryTombstoneEntity(id, id, JSONArray().put(id).toString(), a.endMs))
            db.workoutHistoryDao().deleteWorkoutById(id)
            assertEquals(ExternalWorkoutImport.Outcome.LOCALLY_DELETED, save(a))
            assertNull(db.workoutHistoryDao().getWorkoutWithDetailsById(id))
        }
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
