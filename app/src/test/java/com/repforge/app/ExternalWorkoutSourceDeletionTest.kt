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
