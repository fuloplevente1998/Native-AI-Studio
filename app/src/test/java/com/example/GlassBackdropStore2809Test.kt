package com.example

import android.content.Context
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.migration.LegacyWebViewMigrator
import com.repforge.app.GlassBackdropImages
import com.repforge.app.NativeDataReplacement
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/** 2809/1 B: the background image choice is durable and portable as a validated private path only. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GlassBackdropStore2809Test {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private fun ticket() = NativeDataReplacement.continuationForCurrentData(context)
    private fun path(c: Char) = "glass_backdrop/backdrop_" + c.toString().repeat(32) + ".jpg"
    private fun restore(store: TrainPilotSettingsStore, settings: JSONObject, replace: Boolean) = runBlocking {
        store.importPortableSettings(JSONObject().put("settings", settings), replaceTrainingModesOnMissing = replace)
    }

    @Test fun choiceIsDurableAndReplacingReturnsThePreviousFileForCleanup() = runBlocking {
        val store = TrainPilotSettingsStore(context, publishTheme = {})
        store.setGlassBackdropImageForDataset("", ticket())
        assertEquals("", store.current().glassBackdropImage)
        assertEquals("", store.setGlassBackdropImageForDataset(path('a'), ticket()))
        assertEquals(path('a'), TrainPilotSettingsStore(context).current().glassBackdropImage)
        assertEquals("previous file to delete after commit", path('a'), store.setGlassBackdropImageForDataset(path('b'), ticket()))
        assertEquals(path('b'), store.current().glassBackdropImage)
        assertEquals(path('b'), store.setGlassBackdropImageForDataset("", ticket()))
        assertEquals("", store.current().glassBackdropImage)
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { store.setGlassBackdropImageForDataset("../databases/trainpilot.db", ticket()) }
        }
        assertEquals("", store.current().glassBackdropImage)
    }

    @Test fun backupRoundTripCarriesOnlyThePathAndRejectsForeignPaths() = runBlocking {
        val store = TrainPilotSettingsStore(context, publishTheme = {})
        store.setGlassBackdropImageForDataset(path('c'), ticket())
        // Export value (LegacyWebViewMigrator puts exactly this): the relative private path, no image data.
        val exported = GlassBackdropImages.portable(store.current().glassBackdropImage)
        assertEquals(path('c'), exported)
        assertTrue(exported.length < 80)
        store.setGlassBackdropImageForDataset("", ticket())
        restore(store, JSONObject().put(GlassBackdropImages.PORTABLE_KEY, exported), replace = true)
        assertEquals(path('c'), store.current().glassBackdropImage)
        // A foreign or malformed path never lands in settings; a merge keeps the current choice.
        restore(store, JSONObject().put(GlassBackdropImages.PORTABLE_KEY, "../files/workout_photos/x.jpg"), replace = false)
        assertEquals(path('c'), store.current().glassBackdropImage)
        restore(store, JSONObject().put(GlassBackdropImages.PORTABLE_KEY, 42), replace = false)
        assertEquals(path('c'), store.current().glassBackdropImage)
        // Merge without the key keeps it; a full replace without the key clears it (living backdrop).
        restore(store, JSONObject().put("language", "hu"), replace = false)
        assertEquals(path('c'), store.current().glassBackdropImage)
        restore(store, JSONObject().put("language", "hu"), replace = true)
        assertEquals("", store.current().glassBackdropImage)
        // An explicit empty value clears too.
        store.setGlassBackdropImageForDataset(path('d'), ticket())
        restore(store, JSONObject().put(GlassBackdropImages.PORTABLE_KEY, ""), replace = false)
        assertEquals("", store.current().glassBackdropImage)
    }

    private fun <T> native(operation: suspend () -> T): T {
        val worker = Executors.newSingleThreadExecutor()
        val future = worker.submit<T> { runBlocking { operation() } }
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        try {
            while (true) {
                shadowOf(Looper.getMainLooper()).idle()
                try { return future.get(10, TimeUnit.MILLISECONDS) }
                catch (_: TimeoutException) { check(System.nanoTime() < deadline) }
            }
        } finally { future.cancel(true); worker.shutdownNow() }
    }

    @Test fun realBackupExportCarriesThePathOnlyAndRestoresIt() {
        val db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
        val settings = TrainPilotSettingsStore(context, publishTheme = {})
        try {
            native {
                settings.resetAllSettingsForEraseLocal()
                settings.setGlassBackdropImageForDataset(path('f'), ticket())
                // A stray image file must never be embedded: only the private path travels.
                GlassBackdropImages.file(context, path('f'))!!.apply { parentFile!!.mkdirs(); writeBytes(ByteArray(4096) { 7 }) }
                val backup = LegacyWebViewMigrator(context, db, settings).exportBackupJson(includeHealthJournal = false)
                assertEquals(path('f'), backup.getJSONObject("settings").getString(GlassBackdropImages.PORTABLE_KEY))
                assertTrue("no image bytes in the JSON", backup.toString().length < 200_000)
                settings.resetAllSettingsForEraseLocal()
                assertEquals("", settings.current().glassBackdropImage)
                settings.importPortableSettings(backup)
                assertEquals(path('f'), TrainPilotSettingsStore(context, publishTheme = {}).current().glassBackdropImage)
                assertNotNull("the private file is used on this device", GlassBackdropImages.file(context, path('f'))?.takeIf { it.isFile })
            }
        } finally { db.close() }
    }

    @Test fun scenePhotoCacheReusesAndInvalidatesOnImageOrSizeChange() {
        val scene = com.example.ui.theme.GlassSceneState()
        val first = android.graphics.Bitmap.createBitmap(8, 8, android.graphics.Bitmap.Config.ARGB_8888)
        val second = android.graphics.Bitmap.createBitmap(8, 8, android.graphics.Bitmap.Config.ARGB_8888)
        scene.image = first
        val firstImage = scene.imageBitmap
        val firstShader = scene.imageShader(100f, 200f)
        assertNotNull(firstImage)
        assertNotNull(firstShader)
        assertSame(firstImage, scene.imageBitmap)
        assertSame(firstShader, scene.imageShader(100f, 200f))
        assertNotSame(firstShader, scene.imageShader(200f, 100f))
        scene.image = null
        assertNull(scene.imageBitmap)
        assertNull(scene.imageShader(200f, 100f))
        assertFalse(first.isRecycled)
        scene.image = second
        assertNotSame(firstImage, scene.imageBitmap)
        assertNotSame(firstShader, scene.imageShader(100f, 200f))
    }

    @Test fun missingFileOnThisDeviceFallsBackToTheLivingBackdrop() {
        assertNull(GlassBackdropImages.load(context, path('e')))
        assertNull(GlassBackdropImages.load(context, "../databases/trainpilot.db"))
        assertNull(GlassBackdropImages.file(context, "/etc/passwd"))
        assertEquals(java.io.File(context.filesDir, path('e')), GlassBackdropImages.file(context, path('e')))
    }
}
