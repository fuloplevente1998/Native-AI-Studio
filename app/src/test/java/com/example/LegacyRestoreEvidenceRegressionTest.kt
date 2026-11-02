package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Looper
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.localization.TrainPilotStrings
import com.example.model.AppLanguage
import com.example.ui.components.StudioPrivatePhotos
import com.example.ui.screens.PrivatePhotosScreen
import com.example.ui.theme.TrainPilotTheme
import com.example.ui.viewmodel.TrainPilotUiState
import com.repforge.app.*
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.migration.LegacyWebViewMigrator
import com.repforge.app.data.store.TrainPilotSettingsStore
import com.repforge.app.ui.journal.JournalHealthPanel
import com.repforge.app.ui.progress.ProgressViewModel
import com.repforge.app.ui.text.nativeResources
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w480dp-h960dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class LegacyRestoreEvidenceRegressionTest {
    @get:Rule val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val models = androidx.lifecycle.ViewModelStore()
    private val photo = "12345678-1234-1234-1234-123456789abc"
    private val start = "2026-10-29T19:11:00Z"
    private fun raw() = JSONObject().put("id", "restored").put("syncId", "watch-session")
        .put("started", start).put("finished", "2026-10-29T20:15:00Z")
        .put("programName", "Restored workout").put("exercises", JSONArray())
        .put("photos", JSONArray().put(JSONObject().put("id", photo).put("label", "before")))
        .put("healthWear129", JSONObject().put("schema", 1).put("source", "wear_health_services")
            .put("workoutId", "watch-session").put("watchId", "watch").put("revision", 2)
            .put("averageHeartRate", 101.6).put("minHeartRate", 74).put("maxHeartRate", 137)
            .put("heartRateSamples", 3627).put("totalCalories", 404).put("activeDurationSeconds", 3813.7))
        .put("health240", JSONObject().put("windowStart", start).put("windowEnd", "2026-10-29T20:15:00Z")
            .put("steps", 544).put("activeCalories", 8).put("totalCalories", 72.2))
    private fun <T> awaitNative(block: suspend () -> T): T {
        val pool = Executors.newSingleThreadExecutor()
        val future = pool.submit<T> { runBlocking { block() } }
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        try {
            while (true) {
                shadowOf(Looper.getMainLooper()).idle()
                try { return future.get(10, TimeUnit.MILLISECONDS) }
                catch (_: TimeoutException) { check(System.nanoTime() < deadline) { "Restore timed out" } }
            }
        } finally { future.cancel(true); pool.shutdownNow() }
    }
    @Before fun prepare() {
        val room = TrainPilotDatabase::class.java.getDeclaredField("INSTANCE").apply { isAccessible = true }
        (room.get(null) as? TrainPilotDatabase)?.close(); room.set(null, null)
        val health = HealthJournalStore::class.java.getDeclaredField("instance").apply { isAccessible = true }
        (health.get(null) as? HealthJournalStore)?.close(); health.set(null, null)
        awaitNative { TrainPilotDatabase.getInstance(context).clearAllTables(); TrainPilotSettingsStore(context).resetAllSettingsForEraseLocal() }
    }
    @After fun close() { models.clear() }
    private fun restore() {
        val image = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        val bytes = ByteArrayOutputStream().also { image.compress(Bitmap.CompressFormat.JPEG, 90, it) }.toByteArray()
        image.recycle()
        val zip = File(context.cacheDir, "legacy-real-photo.zip")
        ZipOutputStream(zip.outputStream()).use {
            it.putNextEntry(ZipEntry("backup.json"))
            it.write(JSONObject().put("history", JSONArray().put(raw())).put("healthIncluded", true).toString().toByteArray())
            it.closeEntry(); it.putNextEntry(ZipEntry("photos/$photo.jpg")); it.write(bytes); it.closeEntry()
        }
        awaitNative { BackupRestoreManager(context).importZipArchiveFromUri(Uri.fromFile(zip)).getOrThrow() }
        assertArrayEquals(bytes, File(context.filesDir, "workout_photos/$photo.jpg").readBytes())
    }
    @Test fun legacyZipRestoresRealPhotoAndStudioGalleryActuallyDecodesIt() {
        restore()
        val db = TrainPilotDatabase.getInstance(context)
        val indexed = awaitNative { com.repforge.app.ui.progress.NativeProgressPhotoData.load(db) }
        assertEquals("ZIP metadata must index the actual saved photo", 1, indexed.size)
        val model = ProgressViewModel(context as android.app.Application); models.put("photos", model)
        compose.setContent { TrainPilotTheme {
            StudioPrivatePhotos(TrainPilotUiState(privatePhotoWorkoutId = "restored"),
                TrainPilotStrings.forLanguage(AppLanguage.HU), {}, {}, model)
        } }
        try {
            compose.waitUntil(10_000) {
            shadowOf(Looper.getMainLooper()).idle() !model.uiState.value.photosLoading && model.uiState.value.progressPhotos.size == 1 }
        } catch (error: androidx.compose.ui.test.ComposeTimeoutException) {
            throw AssertionError("Restored photo is indexed but controller did not publish it: ${model.uiState.value}", error)
        }
        compose.waitUntil(10_000) {
            shadowOf(Looper.getMainLooper()).idle() compose.onAllNodesWithTag("progress_photo_image_$photo", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("progress_photo_image_$photo", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("progress_photo_$photo").performClick()
        compose.onNodeWithTag("progress_photo_detail").assertExists()
        compose.onNodeWithText("Edzés: Restored workout", substring = true).assertExists()
        assertEquals("restored", model.uiState.value.progressPhotos.single().workoutId)
    }
    @Test fun legacyWatchEnergyAndHeartRateRemainSeparateFromHealthConnectIncludingAlreadyImportedRows() {
        restore()
        val db = TrainPilotDatabase.getInstance(context)
        val detail = awaitNative { db.workoutHistoryDao().getWorkoutWithDetailsById("restored")!! }
        assertEquals(404, JSONObject(detail.workout.wearMetricsJson!!).getInt("totalCalories"))
        val previousImport = detail.copy(workout = detail.workout.copy(wearMetricsJson = null))
        assertEquals(404, NativeSavedWearHealth.read(previousImport.workout)!!.getInt("totalCalories"))
        compose.setContent { TrainPilotTheme {
            JournalHealthPanel(previousImport, nativeResources(context, "hu"), "hu", false, null, true) { }
        } }
        compose.onNodeWithTag("journal_wear_totalCalories_value").assertTextEquals("404 kcal")
        compose.onNodeWithTag("journal_hc_totalCalories_value").assertTextEquals("72,2 kcal")
        compose.onNodeWithTag("journal_hc_activeCalories_value").assertTextEquals("8 kcal")
    }
    @Test fun samsungHealthConnectAndWearRespectBackupConsentInBothJsonAndZipWithoutDeletingLocalData() {
        restore()
        val db = TrainPilotDatabase.getInstance(context)
        val settings = TrainPilotSettingsStore(context)
        val samsung = JSONObject().put("windowStart", start).put("windowEnd", "2026-10-29T20:15:00Z").put("averageHeartRate", 134)
        val payload = raw()
        payload.put("health240", NativeWorkoutHealthSource.attach(payload.getJSONObject("health240"), HealthJournalStore.SAMSUNG, samsung))
        awaitNative {
            LegacyWebViewMigrator(context, db, settings).saveWorkoutJsonToRoom(payload)
            HealthJournalStore.get(context).samsungDay("2026-10-29", JSONObject().put("steps", 654), JSONArray(), HealthJournalStore.get(context).generation())
        }
        for (consent in listOf(false, true)) {
            awaitNative { settings.setHealthBackupConsent(consent) }
            for (zip in listOf(false, true)) {
                val file = File(context.cacheDir, "consent-$consent-$zip")
                awaitNative {
                    val manager = BackupRestoreManager(context)
                    if (zip) manager.exportZipArchiveToUri(Uri.fromFile(file)).getOrThrow()
                    else manager.exportJsonBackupToUri(Uri.fromFile(file)).getOrThrow()
                }
                val data = if (zip) java.util.zip.ZipFile(file).use { archive ->
                    assertNotNull(archive.getEntry("photos/$photo.jpg"))
                    JSONObject(archive.getInputStream(archive.getEntry("backup.json")).bufferedReader().readText())
                } else JSONObject(file.readText())
                val saved = data.getJSONArray("history").getJSONObject(0)
                assertEquals(consent, data.getBoolean("healthIncluded"))
                assertEquals(consent, saved.has("healthWear129"))
                assertEquals(consent, saved.has("health240"))
                assertEquals(consent, data.has("nativeHealthJournal"))
                if (consent) {
                    assertEquals(134, NativeWorkoutHealthSource.snapshot(saved.getJSONObject("health240"), HealthJournalStore.SAMSUNG)!!.getInt("averageHeartRate"))
                    val days = data.getJSONObject("nativeHealthJournal").getJSONArray("days")
                    assertTrue((0 until days.length()).any { days.getJSONObject(it).optString("channel") == HealthJournalStore.SAMSUNG })
                }
            }
            val local = awaitNative { db.workoutHistoryDao().getWorkoutWithDetailsById("restored")!! }
            assertEquals(404, NativeSavedWearHealth.read(local.workout)!!.getInt("totalCalories"))
            assertNotNull(local.workout.health240Json)
            assertTrue(File(context.filesDir, "workout_photos/$photo.jpg").isFile)
        }
    }
    @Test fun optedOutCloudProjectionKeepsCachesOnlyInTheLocalInstallCopyAndRejectsChangedWindows() {
        val local = raw()
        val network = com.repforge.app.data.migration.NativeBackupPrivacy.sanitize(JSONObject().put("history", JSONArray().put(local)), false)
        val install = JSONObject(network.toString())
        com.repforge.app.data.migration.NativeBackupPrivacy.preserveLocalWorkoutHealth(install, listOf(local))
        assertFalse(network.getJSONArray("history").getJSONObject(0).has("healthWear129"))
        assertTrue(install.getJSONArray("history").getJSONObject(0).has("healthWear129"))
        assertTrue(local.has("healthWear129"))
        val changed = JSONObject(network.toString())
        changed.getJSONArray("history").getJSONObject(0).put("finished", "2026-10-29T23:00:00Z")
        com.repforge.app.data.migration.NativeBackupPrivacy.preserveLocalWorkoutHealth(changed, listOf(local))
        assertFalse(changed.getJSONArray("history").getJSONObject(0).has("healthWear129"))
    }
    @Test fun watchIdentityCanReferenceOriginalIdAfterCloudAddsANewSyncId() {
        val row = raw()
        row.getJSONObject("healthWear129").put("workoutId", row.getString("id"))
        assertEquals(404, NativeSavedWearHealth.fromRaw(row)!!.getInt("totalCalories"))
        row.getJSONObject("healthWear129").put("workoutId", "other-session")
        assertNull(NativeSavedWearHealth.fromRaw(row))
    }
    @Test fun oldTimestampIdentitySurvivesGeneratedNativeIdButForeignWatchSummaryIsRejected() {
        val row = raw().apply { remove("id"); remove("syncId") }
        row.getJSONObject("healthWear129").put("workoutId", start)
        val id = com.repforge.shared.LegacySyncIdentity18.legacyHistoryId(row)
        row.put("id", id)
        assertEquals(404, NativeSavedWearHealth.fromRaw(row)!!.getInt("totalCalories"))
        row.getJSONObject("healthWear129").put("workoutId", "foreign-workout")
        assertNull(NativeSavedWearHealth.fromRaw(row))
        row.getJSONObject("healthWear129").put("workoutId", start).put("source", "samsung_health")
        assertNull(NativeSavedWearHealth.fromRaw(row))
    }
}
