package com.repforge.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.ui.journal.RouteCoordinatesUiModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExternalRouteStoreTest {
    @Test fun routeIsPrivateVersionBoundAndPrunedAfterDeletion() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
        val root = File(context.noBackupFilesDir, "route-test-${System.nanoTime()}")
        try {
            val w = ExternalWorkout(ExternalWorkoutIdentity(HealthJournalStore.HC, "third.party", "a"),
                1700000060000, 1700000000000, 1700000060000, 1, null, null, null, null, null, null)
            assertEquals(ExternalWorkoutImport.Outcome.IMPORTED, ExternalWorkoutImport.persist(context, db, w) {})
            val id = ExternalWorkoutImport.localId(w.identity)
            val row = requireNotNull(db.workoutHistoryDao().getWorkoutWithDetailsById(id)).workout
            val fp = requireNotNull(ExternalRouteStore.fingerprint(row))
            val store = ExternalRouteStore(root)
            val route = RouteCoordinatesUiModel(id, listOf(listOf(47.1 to 18.2, 47.2 to 18.3)), 2, 0.0, null)
            store.save(fp, route)
            assertEquals(route, ExternalRouteStore(root).read(fp))
            assertNull(store.read(fp.copy(version = fp.version + 1)))
            assertFalse(row.rawJson.contains("47.1"))
            store.prune(db.workoutHistoryDao())
            assertTrue(store.contains(fp))
            db.workoutHistoryDao().deleteWorkoutById(id)
            store.prune(db.workoutHistoryDao())
            assertFalse(store.contains(fp))
            assertTrue(store.delete(id))
        } finally { db.close(); root.deleteRecursively() }
        Unit
    }

    @Test fun invalidCoordinatesCannotReplaceSavedRoute() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val root = File(context.noBackupFilesDir, "route-test-${System.nanoTime()}")
        try {
            val id = "tp359-" + "a".repeat(64)
            val fp = ExternalRouteStore.Fingerprint(id, 1, 10, 20)
            val store = ExternalRouteStore(root)
            val route = RouteCoordinatesUiModel(id, listOf(listOf(1.0 to 2.0)), 1, 0.0, null)
            store.save(fp, route)
            assertTrue(runCatching { store.save(fp, route.copy(segments = listOf(listOf(91.0 to 2.0)))) }.isFailure)
            assertEquals(route, store.read(fp))
        } finally { root.deleteRecursively() }
    }
}
