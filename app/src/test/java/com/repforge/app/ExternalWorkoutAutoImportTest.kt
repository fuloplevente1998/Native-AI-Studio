package com.repforge.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExternalWorkoutAutoImportTest {
    private val day = LocalDate.parse("2026-11-24")
    private fun row(id: String = "a", modified: Long = 1L) = JSONObject().put("id", id)
        .put("source", "third.party").put("start", "2026-11-24T10:00:00Z")
        .put("end", "2026-11-24T10:15:00Z").put("lastModifiedMs", modified)
    private fun result(vararg rows: JSONObject) = JSONObject().put("exerciseSessions", JSONArray(rows.toList()))

    @Test fun explicitConsentIsDefaultOffAndToggleInvalidatesPriorFlight() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertFalse(ExternalWorkoutAutoImport.enabled(context))
        assertNull(ExternalWorkoutAutoImport.admit(context, HealthJournalStore.HC, day, ZoneOffset.UTC))
        assertTrue(ExternalWorkoutAutoImport.setEnabled(context, true))
        val old = ExternalWorkoutAutoImport.consent(context)
        assertNotNull(old)
        assertNotNull(ExternalWorkoutAutoImport.admit(context, HealthJournalStore.HC, day, ZoneOffset.UTC))
        assertTrue(ExternalWorkoutAutoImport.setEnabled(context, false))
        assertNull(ExternalWorkoutAutoImport.consent(context))
        assertTrue(ExternalWorkoutAutoImport.setEnabled(context, true))
        assertNotEquals(old, ExternalWorkoutAutoImport.consent(context))
        ExternalWorkoutAutoImport.setEnabled(context, false)
    }

    @Test fun onlyObservedCompleteExerciseReadsAdmitLatestIdentity() {
        val f = ExternalWorkoutAutoImport.Flight(1, HealthJournalStore.HC, day, ZoneOffset.UTC)
        f.observe(day, result(row("failed")).put("warnings", JSONArray().put("READ_EXERCISE: denied")))
        f.observe(day, result(row("failed2")).put("failedFields", JSONArray().put("exerciseSessions")))
        f.observe(day.minusDays(30), result(row("old")))
        f.observe(day, result(row("missing").apply { remove("lastModifiedMs") }))
        f.observe(day, result(row(modified = 2)).put("warnings", JSONArray().put("Partial Health Connect read permission")))
        f.observe(day, result(row(modified = 1), row("other")))
        val rows = f.candidates()
        assertEquals(2, rows.size)
        assertEquals(2L, rows.first().modified)
        assertEquals("third.party", rows.first().identity.source)
        assertEquals(HealthJournalStore.HC, rows.first().identity.provider)
    }

    @Test fun budgetIsBoundedAndIdentityIsNotDroppedOnRepeat() {
        val f = ExternalWorkoutAutoImport.Flight(1, HealthJournalStore.SAMSUNG, day, ZoneOffset.UTC)
        f.observe(day, result(*(0..600).map { row(it.toString()) }.toTypedArray()))
        f.observe(day, result(row("0", 9)))
        assertEquals(512, f.candidates().size)
        assertEquals(9L, f.candidates().first().modified)
    }
}
