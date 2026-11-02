package com.example

import com.repforge.app.HealthConnectReadDiagnosis
import com.repforge.app.HealthConnectRepository
import com.repforge.app.HealthJournalStore
import com.repforge.app.NativeHealthProviderData
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HealthConnectReadDiagnosisRegressionTest {
    private fun read(granted: Set<String> = HealthConnectRepository.READ_PERMS.toSet()): JSONObject {
        val permissions = JSONObject()
        for (permission in HealthConnectRepository.READ_PERMS) permissions.put(permission, permission in granted)
        return JSONObject().put("permissions", permissions)
            .put("warnings", JSONArray()).put("availableStepOrigins", JSONArray())
            .put("steps", JSONObject.NULL).put("activeCalories", JSONObject.NULL)
    }

    @Test fun successfulEmptyReadIsNotFalselyReportedAsSdkErrorOrMeasuredZero() {
        val diagnosis = HealthConnectReadDiagnosis.inspect(read(), "auto")
        assertEquals(HealthConnectReadDiagnosis.NO_DATA, diagnosis.getString("status"))
        assertFalse(diagnosis.getBoolean("hasMeasurements"))
        assertEquals(0, diagnosis.getJSONArray("readErrorFields").length())
        assertFalse(diagnosis.getJSONArray("observedMetricKeys").toString().contains("steps"))
    }

    @Test fun missingGrantsAreDistinctFromEmptyDataAndNeverReportedAsSuccessfulFullRead() {
        val empty = HealthConnectReadDiagnosis.inspect(read(setOf("READ_STEPS")), "auto")
        assertEquals(HealthConnectReadDiagnosis.NO_DATA_LIMITED_GRANTS, empty.getString("status"))
        assertEquals(1, empty.getJSONArray("grantedReadPermissions").length())
        assertEquals(HealthConnectRepository.READ_PERMS.size - 1, empty.getJSONArray("missingReadPermissions").length())
        val partial = HealthConnectReadDiagnosis.inspect(read(setOf("READ_STEPS")).put("steps", 42), "auto")
        assertEquals(HealthConnectReadDiagnosis.PARTIAL_GRANTS, partial.getString("status"))
        assertTrue(partial.getBoolean("hasMeasurements"))
    }

    @Test fun sdkReadErrorIsNotMisclassifiedAsNoSourceData() {
        val day = read().put("warnings", JSONArray().put("READ_STEPS: Health Connect timed out"))
        val diagnosis = HealthConnectReadDiagnosis.inspect(day, "auto")
        assertEquals(HealthConnectReadDiagnosis.READ_ERROR, diagnosis.getString("status"))
        assertEquals(1, diagnosis.getJSONArray("readErrorFields").length())
    }

    @Test fun missingExplicitStepOriginIsNotSilentlyReplacedByAnotherOrigin() {
        val available = "com.example.observed"
        val selected = "com.example.selected"
        val day = read().put("availableStepOrigins", JSONArray().put(available))
        val diagnosis = HealthConnectReadDiagnosis.inspect(day, selected)
        assertEquals(HealthConnectReadDiagnosis.STEP_ORIGIN_NOT_OBSERVED, diagnosis.getString("status"))
        assertEquals(selected, diagnosis.getString("preferredStepOrigin"))
        assertEquals(available, diagnosis.getJSONArray("observedStepOrigins").getString(0))
        assertTrue(day.isNull("steps"))
    }

    @Test fun diagnosticSurvivesHcJournalAndNeverBecomesSamsungDailyData() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        HealthJournalStore(context).use { store ->
            store.erase()
            val day = LocalDate.parse("2026-11-01")
            val payload = read().put("source", "@aggregate")
            payload.put("hcReadDiagnosis", HealthConnectReadDiagnosis.inspect(payload, "auto"))
            store.healthDay(day.toString(), payload, store.generation())
            val persisted = NativeHealthProviderData.day(store, HealthJournalStore.HC, day)
            assertNotNull(persisted)
            assertEquals(HealthConnectReadDiagnosis.NO_DATA,
                persisted!!.getJSONObject("hcReadDiagnosis").getString("status"))
            assertNull(NativeHealthProviderData.day(store, HealthJournalStore.SAMSUNG, day))
            store.erase()
        }
    }

    @Test fun genuineMeasuredValueRemainsAvailableInSourceScopedResult() {
        val diagnosis = HealthConnectReadDiagnosis.inspect(
            read().put("steps", 0).put("availableStepOrigins", JSONArray().put("com.example.source")),
            "com.example.source"
        )
        assertEquals(HealthConnectReadDiagnosis.DATA_PRESENT, diagnosis.getString("status"))
        assertTrue(diagnosis.getBoolean("hasMeasurements"))
        assertEquals("com.example.source", diagnosis.getString("preferredStepOrigin"))
    }
}
