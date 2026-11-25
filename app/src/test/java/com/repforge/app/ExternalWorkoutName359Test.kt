package com.repforge.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.model.AppLanguage
import com.example.model.LoggedWorkoutEntry
import com.example.model.externalSourceLabel
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.migration.LegacyWebViewMigrator
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** #359: an imported workout can be renamed with the journal editor; sync never overwrites it. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExternalWorkoutName359Test {
    private fun workout(id: String, start: Long = 1_700_000_000_000L) =
        ExternalWorkout(ExternalWorkoutIdentity(HealthJournalStore.SAMSUNG, "com.sec.android.app.shealth", id),
            start + 60_000, start, start + 60_000, 1, "Source title", null, null, null, null, null)

    @Test fun renameSurvivesResyncAndKeepsSourceIdentityAndMeasurements() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
        try {
            val w = workout("rename")
            val id = ExternalWorkoutImport.localId(w.identity)
            val metrics = JSONObject().put("values", JSONObject().put("averageHeartRate", 131.0))
            assertEquals(ExternalWorkoutImport.Outcome.IMPORTED, ExternalWorkoutImport.persist(context, db, w, metrics) {})
            val imported = requireNotNull(db.workoutHistoryDao().getWorkoutWithDetailsById(id))
            // No generated source text in the user's description/note or name.
            assertEquals("", NativeJournalEditor.from(imported).note)
            assertEquals("", NativeJournalEditor.from(imported).title)

            val draft = NativeJournalEditor.from(imported).copy(title = "  Esti futás  ")
            val encoded = NativeJournalEditor.encode(imported, draft, emptyMap())
            LegacyWebViewMigrator(context, db, TrainPilotSettingsStore(context)).saveWorkoutJsonToRoom(encoded)
            val renamed = requireNotNull(db.workoutHistoryDao().getWorkoutWithDetailsById(id))
            assertEquals("Esti futás", NativeJournalEditor.savedTitle(JSONObject(renamed.workout.rawJson)))
            assertEquals(1, db.workoutHistoryDao().getAllWorkoutsWithDetails().size)

            // A later provider update (automatic sync / re-import) refreshes only the source snapshot.
            val later = w.copy(modifiedMs = w.modifiedMs + 1, title = "Provider renamed")
            val lateMetrics = JSONObject().put("values", JSONObject().put("averageHeartRate", 131.0).put("maxHeartRate", 170.0))
            assertEquals(ExternalWorkoutImport.Outcome.UPDATED, ExternalWorkoutImport.persist(context, db, later, lateMetrics, allowCreate = false) {})
            assertEquals(ExternalWorkoutImport.Outcome.ALREADY_IMPORTED, ExternalWorkoutImport.persist(context, db, later, lateMetrics) {})
            val raw = JSONObject(requireNotNull(db.workoutHistoryDao().getWorkoutWithDetailsById(id)).workout.rawJson)
            assertEquals("Esti futás", NativeJournalEditor.savedTitle(raw))
            val source = raw.getJSONObject("externalWorkout359")
            assertEquals("rename", source.getString("id"))
            assertEquals("com.sec.android.app.shealth", source.getString("source"))
            assertEquals(170.0, source.getJSONObject("metrics").getJSONObject("values").getDouble("maxHeartRate"), 0.0)
            assertEquals(1, db.workoutHistoryDao().getAllWorkoutsWithDetails().size)

            // Clearing the name returns to the default label without touching source data.
            val current = requireNotNull(db.workoutHistoryDao().getWorkoutWithDetailsById(id))
            val cleared = NativeJournalEditor.encode(current, NativeJournalEditor.from(current).copy(title = ""), emptyMap())
            assertFalse(cleared.has(NativeJournalEditor.USER_TITLE))
            assertEquals("rename", cleared.getJSONObject("externalWorkout359").getString("id"))
        } finally { db.close() }
        Unit
    }

    @Test fun invalidNameIsRejected() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
        try {
            val w = workout("invalid")
            assertEquals(ExternalWorkoutImport.Outcome.IMPORTED, ExternalWorkoutImport.persist(context, db, w) {})
            val detail = requireNotNull(db.workoutHistoryDao().getWorkoutWithDetailsById(ExternalWorkoutImport.localId(w.identity)))
            val tooLong = assertThrows(IllegalArgumentException::class.java) {
                NativeJournalEditor.encode(detail, NativeJournalEditor.from(detail).copy(title = "x".repeat(81)), emptyMap())
            }
            assertEquals("Invalid workout name", tooLong.message)
            val control = assertThrows(IllegalArgumentException::class.java) {
                NativeJournalEditor.encode(detail, NativeJournalEditor.from(detail).copy(title = "a\u0007b"), emptyMap())
            }
            assertEquals("Invalid workout name", control.message)
        } finally { db.close() }
        Unit
    }

    @Test fun journalShowsUserNameAndSourceOnlyAsSourceLabel() {
        val base = LoggedWorkoutEntry(id = "tp359-x", dateLabelHu = "", titleHu = "External workout", durationMinutes = 30,
            originDevice = "external", externalProvider = "samsung_health", externalSource = "com.sec.android.app.shealth")
        assertEquals("Külső edzés", base.localizedTitle(AppLanguage.HU))
        assertEquals("Esti futás", base.copy(externalUserTitle = "Esti futás").localizedTitle(AppLanguage.EN))
        assertFalse(base.localizedTitle(AppLanguage.HU).contains("shealth"))
        assertEquals("Samsung Health · com.sec.android.app.shealth", externalSourceLabel(base.externalProvider, base.externalSource))
        assertEquals("", externalSourceLabel(null, " "))
    }
}
