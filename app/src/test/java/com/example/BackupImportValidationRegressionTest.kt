package com.example

import android.content.Context
import android.net.Uri
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.repforge.app.BackupRestoreManager
import com.repforge.app.GoogleSyncRepository
import com.repforge.app.NativeCalendarBackup
import com.repforge.app.NativeCloudMergeGuard
import com.repforge.app.NativeCloudPhotoStage
import com.repforge.app.NativeDataReplacement
import com.repforge.app.NativeReplacementJournal
import com.repforge.app.HealthJournalStore
import com.repforge.app.NativeHealthBackupGate
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.db.BodyWeightEntryEntity
import com.repforge.app.data.db.CalendarScheduleEntity
import com.repforge.app.data.db.KeyValueDocumentEntity
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
            // Robolectric supplies a new application/files directory per method, while these
            // production singletons survive in the sandbox. Retire the old test connections
            // before opening the current context's files; otherwise a reopened helper reads a
            // different SQLite file from the cached singleton and reports a false data loss.
            val roomInstance = TrainPilotDatabase::class.java.getDeclaredField("INSTANCE").apply { isAccessible = true }
            (roomInstance.get(null) as? TrainPilotDatabase)?.close()
            roomInstance.set(null, null)
            val healthInstance = HealthJournalStore::class.java.getDeclaredField("instance").apply { isAccessible = true }
            (healthInstance.get(null) as? HealthJournalStore)?.close()
            healthInstance.set(null, null)
            TrainPilotDatabase.getInstance(context).clearAllTables()
            TrainPilotSettingsStore(context).resetAllSettingsForEraseLocal()
            val health = HealthJournalStore.get(context)
            assertEquals("Health singleton must use the current test's database file",
                context.getDatabasePath("health-journal.db").canonicalPath, File(health.readableDatabase.path).canonicalPath)
            health.erase()
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

    private val scheduledRows = listOf(
        scheduled("dec-morning", "2026-12-01", "07:05", "completed", 45),
        scheduled("dec-evening", "2026-12-01", "19:15", "planned", 90),
        scheduled("nov-last", "2026-11-30", "08:00", "skipped", 30),
        scheduled("cancelled", "2026-12-01", "19:15", "skipped", 90, cancelled = true)
    )
    private val goalsKey = LegacyWebViewMigrator.USER_GOALS_KEY
    private val stateKey = NativeCalendarBackup.STATE_KEY
    private val studioPlannerKey = NativeCalendarBackup.STUDIO_SETTINGS_KEY
    private val goals = JSONObject().put("workoutGoalPeriod", "monthly")
        .put("weeklyGoalTarget", 26).put("workoutGoalTarget", 26).put("stepsGoal", 8450)
    private val legacyPlanner = JSONObject().put("mode", "weekly").put("nativeWeeks", 5)
    private val studioPlanner = JSONObject().put("testPlannerPreference", "personal calendar settings")
    private val plan = JSONObject().put("programId", "program-1")

    private fun scheduled(id: String, date: String, time: String, status: String, minutes: Int,
        cancelled: Boolean = false): CalendarScheduleEntity {
        val start = java.time.Instant.parse("${date}T${time}:00Z")
        return CalendarScheduleEntity(id, "", date, start.toString(), start.plusSeconds(minutes * 60L).toString(),
            "program-1", "My program", "day-1", "Day one", status, 10L,
            JSONObject().put("timeLabel", time).put("durationMinutes", minutes)
                .put("titleHu", "Named workout").put("cancelled", cancelled).toString())
    }

    private suspend fun seedGoalsAndCalendar(db: TrainPilotDatabase) {
        val documents = db.keyValueDocumentDao()
        documents.putDocument(KeyValueDocumentEntity(goalsKey, goals.toString(), 11L))
        documents.putDocument(KeyValueDocumentEntity("plannerSettings", legacyPlanner.toString(), 11L))
        documents.putDocument(KeyValueDocumentEntity(studioPlannerKey, studioPlanner.toString(), 11L))
        documents.putDocument(KeyValueDocumentEntity("plan", plan.toString(), 11L))
        db.calendarScheduleDao().upsertEntries(scheduledRows)
        val oldDisplayOnly = JSONObject().put("id", "legacy-display-only").put("dateIso", "2027-01-03")
            .put("timeLabel", "11:15").put("durationMinutes", 75).put("status", "PLANNED")
            .put("titleHu", "Existing Studio event").put("programId", "program-1").put("dayId", "day-2")
        val state = JSONObject().put("events", JSONArray().put(oldDisplayOnly))
            .put("undoStack", JSONArray().put(JSONArray().put(JSONObject().put("id", "undone-old-event"))))
        documents.putDocument(KeyValueDocumentEntity(stateKey, state.toString(), 12L))
    }

    private suspend fun assertRecoveredGoalsAndCalendar(db: TrainPilotDatabase) {
        val documents = db.keyValueDocumentDao()
        val restoredGoals = JSONObject(requireNotNull(documents.getDocument(goalsKey)).json)
        assertEquals("monthly", restoredGoals.getString("workoutGoalPeriod"))
        assertEquals(26, restoredGoals.getInt("workoutGoalTarget"))
        assertEquals(8450, restoredGoals.getInt("stepsGoal"))
        assertEquals(legacyPlanner.toString(), documents.getDocument("plannerSettings")?.json)
        assertEquals(studioPlanner.toString(), documents.getDocument(studioPlannerKey)?.json)
        assertEquals(plan.toString(), documents.getDocument("plan")?.json)

        val rows = db.calendarScheduleDao().getAllEntries().associateBy { it.id }
        val state = JSONObject(requireNotNull(documents.getDocument(stateKey)).json)
        val events = state.getJSONArray("events")
        val eventsById = (0 until events.length()).map(events::getJSONObject).associateBy { it.getString("id") }
        assertEquals(setOf("dec-morning", "dec-evening", "nov-last", "legacy-display-only"), eventsById.keys)
        assertEquals(eventsById.keys + "cancelled", rows.keys)
        assertEquals(eventsById.size, events.length())
        assertFalse(state.has("undoStack"))
        assertTrue(JSONObject(rows.getValue("cancelled").rawJson).getBoolean("cancelled"))
        for (original in scheduledRows.filterNot { it.id == "cancelled" }) {
            assertEquals(original, rows.getValue(original.id).copy(rawJson = original.rawJson))
            val event = eventsById.getValue(original.id)
            assertEquals(original.date, event.getString("dateIso"))
            assertEquals(original.programId, event.getString("programId"))
            assertEquals(original.dayId, event.getString("dayId"))
            val raw = JSONObject(original.rawJson)
            assertEquals(raw.getString("timeLabel"), event.getString("timeLabel"))
            assertEquals(raw.getInt("durationMinutes"), event.getInt("durationMinutes"))
            assertEquals(raw.getString("titleHu"), event.getString("titleHu"))
        }
        assertEquals("COMPLETED", eventsById.getValue("dec-morning").getString("status"))
        assertEquals("PLANNED", eventsById.getValue("dec-evening").getString("status"))
        assertEquals("MISSED", eventsById.getValue("nov-last").getString("status"))
        assertEquals("2027-01-03", eventsById.getValue("legacy-display-only").getString("dateIso"))
        assertEquals("11:15", eventsById.getValue("legacy-display-only").getString("timeLabel"))
        assertEquals(75, eventsById.getValue("legacy-display-only").getInt("durationMinutes"))
        assertEquals("day-2", rows.getValue("legacy-display-only").dayId)
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
            seedGoalsAndCalendar(db)
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
        val payload = if (zip) ZipFile(file).use { archive ->
            JSONObject(archive.getInputStream(archive.getEntry("backup.json")).reader().use { it.readText() })
        } else JSONObject(file.readText())
        assertEquals(8450, payload.getJSONObject(goalsKey).getInt("stepsGoal"))
        assertEquals(5, payload.getJSONArray("scheduled").length())
        assertFalse(payload.has(stateKey))
        awaitNative {
            db.workoutHistoryDao().deleteAllWorkouts()
            db.bodyWeightDao().deleteAllWeights()
            db.keyValueDocumentDao().deleteAllDocuments()
            db.calendarScheduleDao().deleteAllEntries()
            db.calendarScheduleDao().upsertEntry(scheduled("stale-local", "2026-12-02", "09:30", "planned", 60))
            db.keyValueDocumentDao().putDocument(KeyValueDocumentEntity(stateKey,
                """{"events":[{"id":"stale-local"}],"undoStack":[[]]}""", 999L))
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
        awaitNative {
            val reopened = Room.databaseBuilder(context, TrainPilotDatabase::class.java,
                TrainPilotDatabase.DATABASE_NAME).build()
            try { assertRecoveredGoalsAndCalendar(reopened) } finally { reopened.close() }
        }
        assertEquals(identity, restoredSettings.cloudDeviceId)
        // Profile serialization may acquire canonical defaults, so compare verified objects.
        assertEquals(com.repforge.app.NativeTrainingProfile.verified(JSONObject(originalProfile)).toString(),
            com.repforge.app.NativeTrainingProfile.verified(JSONObject(restoredSettings.profileJson)).toString())
        assertFalse(NativeDataReplacement.blocked(context))
        assertFalse(NativeReplacementJournal(context).exists())
        assertArrayEquals(originalBytes, file.readBytes())
    }

    @Test fun legacyBackupPreservesAbsentGoalsAndLocalCalendarEntries() {
        val db = TrainPilotDatabase.getInstance(context)
        awaitNative { seedGoalsAndCalendar(db) }
        val legacy = JSONObject().put("history", JSONArray()).put("scheduled", JSONArray()
            .put(JSONObject().put("id", "legacy-added").put("date", "2026-12-10").put("start", "2026-12-10T10:00:00Z"))
            .put(JSONObject().put("id", "legacy-same-slot").put("date", "2026-12-01").put("start", "2026-12-01T07:05:00Z"))
            .put(JSONObject().put("id", "cancelled").put("date", "2026-12-10").put("start", "2026-12-10T07:00:00Z")))
        val file = File(context.cacheDir, "legacy-goals-and-calendar.json").apply { writeText(legacy.toString()) }
        awaitNative { BackupRestoreManager(context).importBackupFromUri(Uri.fromFile(file)).getOrThrow() }
        awaitNative {
            val docs = db.keyValueDocumentDao()
            assertEquals(goals.toString(), docs.getDocument(goalsKey)?.json)
            assertEquals(legacyPlanner.toString(), docs.getDocument("plannerSettings")?.json)
            assertEquals(studioPlanner.toString(), docs.getDocument(studioPlannerKey)?.json)
            assertEquals(plan.toString(), docs.getDocument("plan")?.json)
            assertEquals(scheduledRows.map { it.id }.toSet() + setOf("legacy-added", "legacy-display-only"),
                db.calendarScheduleDao().getAllEntries().map { it.id }.toSet())
            val events = JSONObject(requireNotNull(docs.getDocument(stateKey)).json).getJSONArray("events")
            assertEquals(5, events.length())
            assertTrue(JSONObject(requireNotNull(db.calendarScheduleDao().getEntry("cancelled")).rawJson)
                .getBoolean("cancelled"))
            val partial = JSONObject().put(goalsKey, JSONObject()
                .put("weeklyGoalTarget", 30).put("stepsGoal", JSONObject.NULL))
            LegacyWebViewMigrator(context, db, TrainPilotSettingsStore(context)).importBackupJson(partial, replaceAll = true)
            val updated = JSONObject(requireNotNull(docs.getDocument(goalsKey)).json)
            assertEquals(30, updated.getInt("workoutGoalTarget"))
            assertEquals("monthly", updated.getString("workoutGoalPeriod"))
            assertEquals(8450, updated.getInt("stepsGoal"))
            assertEquals(events.toString(), JSONObject(requireNotNull(docs.getDocument(stateKey)).json)
                .getJSONArray("events").toString())
        }
    }

    @Test fun driveMergeInstallsPortableGoalsAndCalendarWithoutDailyHealth() {
        val db = TrainPilotDatabase.getInstance(context)
        val settings = TrainPilotSettingsStore(context)
        val migrator = LegacyWebViewMigrator(context, db, settings)
        awaitNative {
            seedGoalsAndCalendar(db)
            settings.setHealthBackupConsent(true)
            val remote = migrator.exportBackupJson(includeHealthJournal = false,
                healthSnapshot = JSONObject().put("records", JSONArray().put(healthRecord("steps", 6820))))
            assertFalse(remote.has("nativeHealthJournal"))
            // Even an older or external snapshot with a daily journal cannot inject it into Drive merge.
            remote.put("nativeHealthJournal", JSONObject().put("records", JSONArray().put(healthRecord("remote-steps", 6820))))
            db.keyValueDocumentDao().deleteAllDocuments()
            db.calendarScheduleDao().deleteAllEntries()
            settings.setHealthBackupConsent(false)
            val local = migrator.exportBackupJson(includeHealthJournal = false)
            val merged = GoogleSyncRepository(context).mergeSync18(local, listOf(remote), null,
                emptyList(), emptySet(), emptySet(), preferLocalOnConflict = true)
            val installationBefore = NativeCloudMergeGuard.localInstallRevision.value
            var published = false
            NativeCloudPhotoStage(context, merged).use { photos ->
                photos.prepare(upload = { _, _ -> error("Unexpected photo upload") },
                    download = { _, _, _ -> error("Unexpected photo download") })
                NativeCloudMergeGuard.installAndPublish(context, local, merged, photos, "fixture-owner") { accepted ->
                    assertRecoveredGoalsAndCalendar(db)
                    assertFalse(accepted.has("nativeHealthJournal"))
                    assertFalse(NativeReplacementJournal(context).exists())
                    assertEquals(installationBefore + 1, NativeCloudMergeGuard.localInstallRevision.value)
                    published = true
                }
            }
            assertTrue(published)
            assertNotNull(db.keyValueDocumentDao().getDocument("cloudBase:fixture-owner"))
            assertFalse(settings.current().healthBackupConsent)
        }
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
            """{"history":[],"calendarScheduleVersion":1}""",
            """{"history":[],"calendarScheduleVersion":1,"scheduled":[{"date":"invalid"}]}""",
            """{"history":[],"calendarScheduleVersion":1,"scheduled":[{"date":"2026-12-10","timeLabel":"invalid"}]}""",
            """{"history":[],"calendarScheduleVersion":1,"scheduled":[{"date":"2026-12-10","durationMinutes":1441}]}""",
            """{"history":[],"native:userGoals":{"stepsGoal":"invalid"}}""",
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
    @Test fun largeHealthJournalRestoresThroughJsonCanonicalZipAndLegacyRepForgeZip() {
        // A normal app backup includes the built-in catalog. Otherwise each successful
        // restore launches a Wear-home refresh that must seed the fixture's missing
        // catalog, racing the next format's deliberately immediate exclusive restore.
        // Include that catalog in the source archive; do not retry imports or bypass
        // the real replacement lease. The 600-record/photo assertions remain below.
        awaitNative { TrainPilotDatabase.ensureBuiltinCatalogSeeded(TrainPilotDatabase.getInstance(context), context) }
        val original = seedPhotoHealthArchive()
        val data = ZipFile(original).use { zip ->
            com.repforge.app.BackupJsonIO.read(zip.getInputStream(zip.getEntry("backup.json")).reader()) as JSONObject
        }
        val records = JSONArray()
        val notes = "árvíztűrő 🏋️ ".repeat(500)
        repeat(600) { index -> records.put(healthRecord("large-$index", index).apply {
            getJSONObject("data").put("notes", notes)
        }) }
        data.getJSONObject("nativeHealthJournal").put("records", records)
        val payload = File(context.cacheDir, "large-backup.json")
        com.repforge.app.BackupJsonIO.writeFile(payload, data, BackupRestoreManager.JSON_LIMIT)
        assertTrue(payload.length() > 3 * 1024 * 1024)
        for (format in listOf("json", "canonical-zip", "repforge-zip")) {
            val selected = if (format == "json") payload else File(context.cacheDir, "$format.zip").also { archive ->
                ZipOutputStream(archive.outputStream()).use { zip ->
                    zip.putNextEntry(ZipEntry("backup.json")); payload.inputStream().use { it.copyTo(zip) }; zip.closeEntry()
                    zip.putNextEntry(ZipEntry("photos/$photoId.jpg")); zip.write(photoBytes); zip.closeEntry()
                    if (format == "canonical-zip") {
                        val hashes = JSONObject().put("backup.json", payload.inputStream().use { com.repforge.app.ArchiveIO.hash(it) })
                            .put("photos/$photoId.jpg", photoBytes.inputStream().use { com.repforge.app.ArchiveIO.hash(it) })
                        val manifest = JSONObject().put("format", "TrainPilotArchive").put("version", 1).put("sha256", hashes)
                        zip.putNextEntry(ZipEntry("manifest.json")); zip.write(manifest.toString().toByteArray()); zip.closeEntry()
                    }
                }
            }
            val result = awaitNative { BackupRestoreManager(context).importBackupFromUri(Uri.fromFile(selected)) }
            assertTrue("$format: ${result.exceptionOrNull()}", result.isSuccess)
            val restored = HealthJournalStore.get(context).exportSnapshot().getJSONArray("records")
            assertEquals("$format record count", 600, restored.length())
            assertEquals(notes, restored.getJSONObject(599).getJSONObject("data").getString("notes"))
            assertNotNull(awaitNative { TrainPilotDatabase.getInstance(context).workoutHistoryDao().getWorkoutWithDetails("photo-health-workout") })
            assertNull(awaitNative { TrainPilotDatabase.getInstance(context).keyValueDocumentDao().getDocument("legacy:nativeHealthJournal") })
            assertFalse(NativeReplacementJournal(context).exists())
        }
    }

}
