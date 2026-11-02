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
        val model = ProgressViewModel(context as android.app.Application); models.put("photos", model)
        compose.setContent { TrainPilotTheme {
            StudioPrivatePhotos(TrainPilotUiState(privatePhotoWorkoutId = "restored"),
                TrainPilotStrings.forLanguage(AppLanguage.HU), {}, {}, model)
        } }
        compose.waitUntil(10_000) { !model.uiState.value.photosLoading && model.uiState.value.progressPhotos.size == 1 }
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("progress_photo_image_$photo").fetchSemanticsNodes().isNotEmpty() }
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
