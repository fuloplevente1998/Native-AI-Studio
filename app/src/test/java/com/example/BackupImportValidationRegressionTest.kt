package com.example

import android.content.Context
import android.net.Uri
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.repforge.app.BackupRestoreManager
import com.repforge.app.NativeDataReplacement
import com.repforge.app.NativeReplacementJournal
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.db.BodyWeightEntryEntity
import com.repforge.app.data.migration.LegacyWebViewMigrator
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.PAUSED)
class BackupImportValidationRegressionTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    private fun <T> awaitNative(operation: suspend () -> T): T {
        val worker = Executors.newSingleThreadExecutor()
        val future = worker.submit<T> { runBlocking { operation() } }
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        try {
            while (true) {
                shadowOf(Looper.getMainLooper()).idle()
                try { return future.get(10, TimeUnit.MILLISECONDS) }
                catch (waiting: TimeoutException) {
                    check(System.nanoTime() < deadline) { "Import did not finish within 30 seconds" }
                }
            }
        } finally { future.cancel(true); worker.shutdownNow() }
    }

    @Before fun resetIsolatedTestDataset() {
        // The production database is a singleton reused by these manager integration tests.
        // Reset only the Robolectric fixture; retain the strict one-workout precondition below.
        awaitNative {
            TrainPilotDatabase.getInstance(context).clearAllTables()
            TrainPilotSettingsStore(context).resetAllSettingsForEraseLocal()
        }
    }

    @Test fun canonicalJsonExportAndRestorePreserveWorkoutWeightAndSettings() = roundTrip(false)

    @Test fun canonicalZipExportAndRestorePreserveWorkoutWeightAndSettings() = roundTrip(true)

    private fun roundTrip(zip: Boolean) {
        val db = TrainPilotDatabase.getInstance(context)
        val settings = TrainPilotSettingsStore(context)
        val migrator = LegacyWebViewMigrator(context, db, settings)
        val weight = BodyWeightEntryEntity("round-trip-weight", "2026-11-01", "2026-11-01", 80.5, 123L,
            "{\"id\":\"round-trip-weight\",\"date\":\"2026-11-01\",\"kg\":80.5,\"updatedAt\":123}")
        val identity = awaitNative {
            migrator.saveWorkoutJsonToRoom(JSONObject()
                .put("id", "round-trip-workout").put("key", "round-trip-workout")
                .put("started", "2026-11-01T10:00:00Z").put("finished", "2026-11-01T10:45:00Z")
                .put("exercises", JSONArray()))
            db.bodyWeightDao().upsertWeight(weight)
            settings.setLanguage("de")
            settings.ensureCloudDeviceId()
        }
        val originalProfile = awaitNative { settings.current().profileJson }
        val file = File(context.cacheDir, "opaque-round-trip-document")
        val manager = BackupRestoreManager(context)
        val exported = awaitNative {
            if (zip) manager.exportZipArchiveToUri(Uri.fromFile(file)).map { Unit }
            else manager.exportJsonBackupToUri(Uri.fromFile(file)).map { Unit }
        }
        assertTrue(exported.exceptionOrNull()?.message ?: "export failed", exported.isSuccess)
        val originalBytes = file.readBytes()
        awaitNative {
            db.workoutHistoryDao().deleteAllWorkouts()
            db.bodyWeightDao().deleteAllWeights()
            settings.setLanguage("hu")
        }
        val restored = awaitNative { manager.importBackupFromUri(Uri.fromFile(file)) }
        assertTrue(restored.exceptionOrNull()?.message ?: "restore failed", restored.isSuccess)
        val workout = awaitNative { db.workoutHistoryDao().getWorkoutWithDetails("round-trip-workout") }
        assertNotNull(workout)
        assertEquals(2700, workout!!.workout.durationSeconds)
        assertEquals(80.5, awaitNative { db.bodyWeightDao().getWeightById(weight.id) }!!.kg, 0.0)
        val restoredSettings = awaitNative { settings.current() }
        assertEquals("de", restoredSettings.language)
        assertEquals(identity, restoredSettings.cloudDeviceId)
        // Profile serialization may acquire canonical defaults, so compare verified objects.
        assertEquals(com.repforge.app.NativeTrainingProfile.verified(JSONObject(originalProfile)).toString(),
            com.repforge.app.NativeTrainingProfile.verified(JSONObject(restoredSettings.profileJson)).toString())
        assertFalse(NativeDataReplacement.blocked(context))
        assertFalse(NativeReplacementJournal(context).exists())
        assertArrayEquals(originalBytes, file.readBytes())
    }

    @Test fun unrelatedAndDamagedJsonCannotEraseExistingWorkoutHistory() {
        val db = TrainPilotDatabase.getInstance(context)
        val migrator = LegacyWebViewMigrator(context, db, TrainPilotSettingsStore(context))
        awaitNative {
            migrator.saveWorkoutJsonToRoom(JSONObject()
                .put("id", "preserved-workout").put("key", "preserved-workout")
                .put("started", "2026-11-01T10:00:00Z").put("finished", "2026-11-01T10:45:00Z")
                .put("exercises", JSONArray()))
        }
        val original = awaitNative { db.workoutHistoryDao().getAllWorkoutsWithDetails() }
        assertEquals(1, original.size)
        for (payload in listOf("{}", "{\"settings\":{}}", "{\"history\":null}",
            "{\"history\":{},\"workouts\":[]}", "{\"history\":\"broken\"}", "{\"history\":[{}]}", "{\"history\":[null]}",
            "{\"history\":[{\"started\":\"2026-11-01T10:00:00Z\",\"photos\":[{\"id\":\"invalid-photo\"}]}]}")) {
            val file = File(context.cacheDir, "selected-backup.json").apply { writeText(payload) }
            val result = awaitNative { BackupRestoreManager(context).importBackupFromUri(Uri.fromFile(file)) }
            assertTrue("Must reject $payload", result.isFailure)
            assertEquals(original, awaitNative { db.workoutHistoryDao().getAllWorkoutsWithDetails() })
            assertFalse(NativeDataReplacement.blocked(context))
            assertFalse(NativeReplacementJournal(context).exists())
            assertEquals(payload, file.readText())
        }
    }

    @Test fun legacyAliasesAndEncodedCapacitorStorageRemainSupported() {
        val manager = BackupRestoreManager(context)
        for (key in listOf("history", "workouts", "loggedWorkouts")) {
            val normalized = manager.normalizeLegacyRepForgePayload(JSONObject()
                .put("data", JSONObject().put(key, JSONArray().put(JSONObject().put("id", "legacy")))))
            assertEquals("legacy", normalized.getJSONArray("history").getJSONObject(0).getString("id"))
        }
        val encoded = JSONObject().put("values", JSONObject().put("_cap_repforge:history", "[]"))
        assertEquals(0, manager.normalizeLegacyRepForgePayload(encoded).getJSONArray("history").length())
        assertEquals(0, manager.normalizeLegacyRepForgePayload(JSONObject("{\"history\":[]}")).getJSONArray("history").length())
    }

    @Test fun zipWithoutExtensionIsDetectedFromBytesBeforeValidation() {
        val file = File(context.cacheDir, "opaque-document-id")
        ZipOutputStream(file.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("backup.json"))
            zip.write("{}".toByteArray())
            zip.closeEntry()
        }
        val result = awaitNative { BackupRestoreManager(context).importBackupFromUri(Uri.fromFile(file)) }
        assertTrue(result.isFailure)
        assertEquals("Missing backup history.", result.exceptionOrNull()?.message)
        assertFalse(NativeReplacementJournal(context).exists())
    }

    @Test fun jsonWithMisleadingZipExtensionIsDetectedFromBytes() {
        val file = File(context.cacheDir, "selected.zip").apply { writeText("{}") }
        val result = awaitNative { BackupRestoreManager(context).importBackupFromUri(Uri.fromFile(file)) }
        assertTrue(result.isFailure)
        assertEquals("Missing backup history.", result.exceptionOrNull()?.message)
        assertFalse(NativeDataReplacement.blocked(context))
    }
}
