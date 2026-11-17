package com.repforge.app

import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.example.ui.theme.TrainPilotPalettes
import com.example.ui.viewmodel.TrainPilotViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PaletteSystemBarsTest {
    @Test fun allPaletteBackgroundsChooseContrastingIcons() {
        assertEquals(32, TrainPilotPalettes.allPalettes.size)
        for (palette in TrainPilotPalettes.allPalettes) {
            assertEquals("palette ${palette.id}", palette.id in 25..32, PaletteSystemBars.useDarkIcons(palette))
        }
    }

    @Test fun productionActivityReappliesCurrentPaletteOnReturn() {
        val activityController = Robolectric.buildActivity(MainActivity::class.java).create()
        val activity = activityController.get()
        val viewModel = ViewModelProvider(activity)[TrainPilotViewModel::class.java]
        val bars = WindowCompat.getInsetsController(activity.window, activity.window.decorView)

        viewModel.syncNativeSettings("en", TrainPilotPalettes.getById(25).key)
        activityController.start().resume()
        assertTrue(bars.isAppearanceLightStatusBars)
        assertTrue(bars.isAppearanceLightNavigationBars)

        activityController.pause().stop()
        viewModel.syncNativeSettings("en", TrainPilotPalettes.getById(1).key)
        activityController.start().resume()
        assertFalse(bars.isAppearanceLightStatusBars)
        assertFalse(bars.isAppearanceLightNavigationBars)
        activityController.pause().stop().destroy()
    }
}
