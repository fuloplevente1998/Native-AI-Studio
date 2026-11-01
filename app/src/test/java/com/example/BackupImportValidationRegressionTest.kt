package com.example

import android.content.Context
import android.net.Uri
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.repforge.app.BackupRestoreManager
import com.repforge.app.NativeDataReplacement
import com.repforge.app.NativeReplacementJournal
import com.repforge.app.HealthJournalStore
import com.repforge.app.NativeHealthBackupGate
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
import java.util.concurrent.ExecutionException
import java.util.zip.ZipFile
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
            HealthJournalStore.get(context).erase()
        }
    }

    private val photoId = "12345678-1234-1234-1234-123456789abc"
    private val photoBytes = byteArrayOf(0xff.toByte(), 0xd8.toByte(), 1, 2, 3, 0xff.toByte(), 0xd9.toByte())
    private val oldPhotoBytes = byteArrayOf(9, 8, 7)
    private val photoFile get() = File(context.filesDir, "workout_photos/$photoId.jpg")

    private fun healthRecord(id: String, value: Int): JSONObject {
        val time = 1793527200000L
        return JSONObject().put("channel", HealthJournalStore.HC).put("source", "com.example.fixture")
            .put("type", "StepsRecord").put("recordId", id).put("lastModifiedMs", time)
            .put("startMs", time).put("endMs", time + 1000).put("day", HealthJournalStore.day(time))
            .put("data", JSONObject().put("value", value))
    }

    private fun seedPhotoHealthArchive(): File {
        val db = TrainPilotDatabase.getInstance(context)
        val settings = TrainPilotSettingsStore(context)
        awaitNative {
            settings.setHealthBackupConsent(true)
            LegacyWebViewMigrator(context, db, settings).saveWorkoutJsonToRoom(JSONObject()
                .put("id", "photo-health-workout").put("key", "photo-health-workout")
                .put("started", "2026-11-01T10:00:00Z").put("finished", "2026-11-01T10:45:00Z")
                .put("exercises", JSONArray()).put("photos", JSONArray().put(JSONObject()
                    .put("id", photoId).put("label", "test fixture").put("createdAt", 123L)
                    .put("updatedAt", 123L).put("width", 1).put("height", 1).put("bytes", photoBytes.size))))
            val health = HealthJournalStore.get(context)
            health.importRecords(JSONArray().put(healthRecord("restored-health", 123)), health.generation())
            assertEquals("Seeded Health record before export", 1, health.exportSnapshot().getJSONArray("records").length())
        }
        photoFile.parentFile!!.mkdirs()
        photoFile.writeBytes(photoBytes)
        val archive = File(context.cacheDir, "photo-health-archive")
        val exported = awaitNative { BackupRestoreManager(context).exportZipArchiveToUri(Uri.fromFile(archive)) }
        assertTrue(exported.exceptionOrNull()?.message ?: "archive export failed", exported.isSuccess)
        ZipFile(archive).use { zip ->
            val data = JSONObject(zip.getInputStream(zip.getEntry("backup.json")).use { String(it.readBytes()) })
            assertEquals("Health record actually present in exported ZIP", 1,
                data.getJSONObject("nativeHealthJournal").getJSONArray("records").length())
        }
        return archive
    }

    private fun replaceLiveDatasetBeforeRestore() {
        awaitNative {
            val db = TrainPilotDatabase.getInstance(context)
            db.workoutHistoryDao().deleteAllWorkouts()
            LegacyWebViewMigrator(context, db, TrainPilotSettingsStore(context)).saveWorkoutJsonToRoom(JSONObject()
                .put("id", "old-live-workout").put("started", "2026-10-31T10:00:00Z")
                .put("finished", "2026-10-31T10:05:00Z").put("exercises", JSONArray()))
            val health = HealthJournalStore.get(context)
            health.erase()
            health.importRecords(JSONArray().put(healthRecord("old-live-health", 7)), health.generation())
        }
        photoFile.writeBytes(oldPhotoBytes)
    }

    private fun assertPhotoHealthRestored(checkpoint: String = "normal ZIP restore") {
        val workout = awaitNative {
            TrainPilotDatabase.getInstance(context).workoutHistoryDao().getWorkoutWithDetails("photo-health-workout")
        }
        assertNotNull(workout)
        assertEquals(photoId, workout!!.photos.single().id)
        assertArrayEquals(photoBytes, photoFile.readBytes())
        // A second SQLite helper verifies persisted journal data independently of the manager.
        val snapshot = awaitNative {
            val reopened = HealthJournalStore(context)
            try {
                assertEquals("", reopened.pendingRestore())
                reopened.exportSnapshot()
            } finally { reopened.close() }
        }
        val records = snapshot.getJSONArray("records")
        val liveCount = awaitNative { HealthJournalStore.get(context).exportSnapshot().getJSONArray("records").length() }
        assertEquals("$checkpoint: reopened Health count; live singleton count=$liveCount", 1, records.length())
        assertEquals("restored-health", records.getJSONObject(0).getString("recordId"))
        assertEquals(123, records.getJSONObject(0).getJSONObject("data").getInt("value"))
        assertFalse(NativeReplacementJournal(context).exists())
        assertFalse(NativeDataReplacement.blocked(context))
        assertTrue(NativeHealthBackupGate.available())
    }

    @Test fun zipRestoresPhotoBytesAndConsentedHealthJournal() {
        val archive = seedPhotoHealthArchive()
        val bytes = archive.readBytes()
        replaceLiveDatasetBeforeRestore()
        val result = awaitNative { BackupRestoreManager(context).importBackupFromUri(Uri.fromFile(archive)) }
        assertTrue(result.exceptionOrNull()?.message ?: "restore failed", result.isSuccess)
        assertPhotoHealthRestored()
        assertArrayEquals(bytes, archive.readBytes())
    }

    private class SimulatedProcessDeath(val checkpoint: String) : Error(checkpoint)

    @Test fun durableReceiptReplaysAfterEachPhotoHealthAndRoomCheckpoint() {
        val checkpoints = listOf("receipt-prepared", "photo-installed:$photoId", "photos-installed",
            "health-prepared", "health-installed", "room-settings-imported", "health-committed",
            "data-imported", "receipt-committed")
        for (checkpoint in checkpoints) {
            println("Restore fault injection: $checkpoint")
            resetIsolatedTestDataset()
            val archive = seedPhotoHealthArchive()
            val bytes = archive.readBytes()
            replaceLiveDatasetBeforeRestore()
            val interrupted = BackupRestoreManager(context).apply {
                restoreCheckpoint = { point -> if (point == checkpoint) throw SimulatedProcessDeath(point) }
            }
            val error = assertThrows(ExecutionException::class.java) {
                awaitNative { interrupted.importBackupFromUri(Uri.fromFile(archive)) }
            }
            assertTrue("Expected fault at $checkpoint, got ${error.cause}", error.cause is SimulatedProcessDeath)
            assertTrue(NativeReplacementJournal(context).exists())
            assertTrue(NativeDataReplacement.blocked(context))
            val recovered = awaitNative { BackupRestoreManager(context).resumeInterruptedReplacement() }
            assertTrue("$checkpoint: ${recovered.exceptionOrNull()}", recovered.isSuccess)
            assertPhotoHealthRestored(checkpoint)
            assertArrayEquals(bytes, archive.readBytes())
        }
    }

    @Test fun checksumFailureLeavesCurrentPhotoHealthAndRoomUntouched() {
        val source = seedPhotoHealthArchive()
        replaceLiveDatasetBeforeRestore()
        val tampered = File(context.cacheDir, "tampered-archive")
        ZipFile(source).use { input ->
            ZipOutputStream(tampered.outputStream()).use { output ->
                for (entry in input.entries().asSequence()) {
                    output.putNextEntry(ZipEntry(entry.name))
                    output.write(if (entry.name == "photos/$photoId.jpg") byteArrayOf(0)
                        else input.getInputStream(entry).use { it.readBytes() })
                    output.closeEntry()
                }
            }
        }
        val result = awaitNative { BackupRestoreManager(context).importBackupFromUri(Uri.fromFile(tampered)) }
        assertTrue(result.isFailure)
        assertNotNull(awaitNative {
            TrainPilotDatabase.getInstance(context).workoutHistoryDao().getWorkoutWithDetails("old-live-workout")
        })
        assertArrayEquals(oldPhotoBytes, photoFile.readBytes())
        val records = awaitNative { HealthJournalStore.get(context).exportSnapshot().getJSONArray("records") }
        assertEquals("old-live-health", records.getJSONObject(0).getString("recordId"))
        assertFalse(NativeReplacementJournal(context).exists())
        assertFalse(NativeDataReplacement.blocked(context))
        assertTrue(NativeHealthBackupGate.available())
    }

    @Test fun healthJournalIsOmittedWithoutBackupConsent() {
        awaitNative {
            val health = HealthJournalStore.get(context)
            health.importRecords(JSONArray().put(healthRecord("private-health", 99)), health.generation())
            TrainPilotSettingsStore(context).setHealthBackupConsent(false)
        }
        val file = File(context.cacheDir, "no-health-consent.json")
        val result = awaitNative { BackupRestoreManager(context).exportJsonBackupToUri(Uri.fromFile(file)) }
        assertTrue(result.exceptionOrNull()?.message ?: "export failed", result.isSuccess)
        assertFalse(JSONObject(file.readText()).has("nativeHealthJournal"))
        val records = awaitNative { HealthJournalStore.get(context).exportSnapshot().getJSONArray("records") }
        assertEquals("private-health", records.getJSONObject(0).getString("recordId"))
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
