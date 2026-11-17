package com.repforge.app

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.example.ui.theme.TrainPilotPalettes
import com.example.ui.viewmodel.TrainPilotViewModel
import com.repforge.app.data.store.ThemePaletteOption
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.Shadows.shadowOf
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.PAUSED)
class PaletteSystemBarsTest {
    @Test fun allPaletteBackgroundsChooseContrastingIcons() {
        assertEquals(32, TrainPilotPalettes.allPalettes.size)
        for (palette in TrainPilotPalettes.allPalettes) {
            assertEquals("palette ${palette.id}", palette.id in 25..32, PaletteSystemBars.useDarkIcons(palette))
        }
    }

    @Test fun productionActivityFollowsStoredPaletteLiveAndOnReturn() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settings = TrainPilotSettingsStore(context, publishTheme = {})
        fun writePalette(id: Int) {
            val palette = TrainPilotPalettes.getById(id)
            background {
                settings.setTheme(ThemePaletteOption(palette.key, palette.nameHu, palette.nameEn,
                    "#000000", "#000000", if (id >= 25) "light" else "matte"))
                assertEquals(palette.key, settings.current().themeKey)
            }
        }
        writePalette(25)
        val activityController = Robolectric.buildActivity(MainActivity::class.java).create()
        try {
            val activity = activityController.get()
            val viewModel = ViewModelProvider(activity)[TrainPilotViewModel::class.java]
            val bars = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            fun awaitPalette(id: Int, darkIcons: Boolean) = await {
                viewModel.uiState.value.selectedPaletteId == id &&
                    bars.isAppearanceLightStatusBars == darkIcons &&
                    bars.isAppearanceLightNavigationBars == darkIcons
            }

            activityController.start().resume()
            awaitPalette(25, true)
            writePalette(1)
            awaitPalette(1, false)
            writePalette(25)
            awaitPalette(25, true)

            activityController.pause().stop()
            writePalette(1)
            // Lifecycle-aware collection resumes at STARTED, before onResume reapplies the bars.
            activityController.start()
            await { viewModel.uiState.value.selectedPaletteId == 1 }
            activityController.resume()
            awaitPalette(1, false)
        } finally {
            activityController.pause().stop().destroy()
        }
    }

    private fun await(condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        while (!condition()) {
            shadowOf(Looper.getMainLooper()).idleFor(50, TimeUnit.MILLISECONDS)
            check(System.nanoTime() < deadline) { "Timed out waiting for palette and system bars" }
            Thread.sleep(10)
        }
    }

    private fun background(block: suspend () -> Unit) {
        val executor = Executors.newSingleThreadExecutor()
        val future = executor.submit { runBlocking { block() } }
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        try {
            while (true) {
                shadowOf(Looper.getMainLooper()).idleFor(50, TimeUnit.MILLISECONDS)
                try { future.get(10, TimeUnit.MILLISECONDS); return }
                catch (_: TimeoutException) { check(System.nanoTime() < deadline) }
            }
        } finally {
            future.cancel(true)
            executor.shutdownNow()
        }
    }
}
