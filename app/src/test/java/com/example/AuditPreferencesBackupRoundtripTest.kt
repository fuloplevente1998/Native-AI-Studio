package com.example

import android.content.Context
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.migration.LegacyWebViewMigrator
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.PAUSED)
class AuditPreferencesBackupRoundtripTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()

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

    @Test fun actualExportAndImportPreserveDistinctRestAndAppearance() {
        val db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
        val settings = TrainPilotSettingsStore(context, publishTheme = {})
        try {
            native {
                settings.resetAllSettingsForEraseLocal()
                settings.setFontScaleMultiplier(1.28f)
                settings.setCompactPhoneMode(true)
                settings.updateTimerPreferences(restSeconds = 120)
                settings.setIntervalPreset(73, 17, 4, false, true, false)
                val backup = LegacyWebViewMigrator(context, db, settings).exportBackupJson(includeHealthJournal = false)
                val portable = backup.getJSONObject("settings")
                assertEquals(1.28, portable.getDouble("fontScaleMultiplier"), 0.001)
                assertTrue(portable.getBoolean("compactPhoneMode"))
                assertEquals(120, portable.getInt("restSeconds"))
                assertEquals(17, portable.getInt("intervalRestSeconds"))
                val preset = portable.getJSONObject("intervalPreset110")
                assertEquals(73, preset.getInt("work")); assertEquals(17, preset.getInt("rest"))
                assertEquals(4, preset.getInt("rounds")); assertFalse(preset.getBoolean("sound"))
                assertTrue(preset.getBoolean("vibration")); assertFalse(preset.getBoolean("autoAdvance"))
                assertFalse(portable.getBoolean("healthBackupConsent"))
                assertFalse(backup.getBoolean("healthIncluded"))
                settings.resetAllSettingsForEraseLocal()
                settings.importPortableSettings(backup)
                val restored = TrainPilotSettingsStore(context, publishTheme = {}).current()
                assertEquals(1.28f, restored.fontScaleMultiplier, 0.001f)
                assertTrue(restored.compactPhoneMode)
                assertEquals(120, restored.restSeconds); assertEquals(17, restored.intervalRestSeconds)
                assertEquals(73, restored.workSeconds); assertEquals(4, restored.rounds)
                assertFalse(restored.soundEnabled); assertTrue(restored.vibrationEnabled)
                assertFalse(restored.intervalAutoAdvance); assertFalse(restored.healthBackupConsent)
            }
        } finally { db.close() }
    }
}
