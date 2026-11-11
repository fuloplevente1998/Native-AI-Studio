package com.example

import android.content.Context
import androidx.compose.ui.graphics.toArgb
import androidx.test.core.app.ApplicationProvider
import com.example.ui.theme.TrainPilotPalettes
import com.repforge.app.data.store.ThemePaletteOption
import com.repforge.app.data.store.TrainPilotSettingsStore
import com.repforge.app.data.store.TrainPilotUserSettings
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PhoneWearThemeRegressionTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private fun hex(color: androidx.compose.ui.graphics.Color) = "#%06x".format(java.util.Locale.ROOT, color.toArgb() and 0xFFFFFF)

    @Test fun everyPhonePaletteSerializesItsActualColorsEvenWhenStoredAccentsWereCorrupt() {
        assertEquals(32, TrainPilotPalettes.allPalettes.size)
        for (palette in TrainPilotPalettes.allPalettes) {
            val json = TrainPilotUserSettings(themeKey = palette.key, themeAccent = "#000000", themeAccent2 = "#000000").themeJson()
            assertEquals(palette.key, json.getString("key"))
            assertEquals(hex(palette.primary), json.getString("accent"))
            assertEquals(hex(palette.secondary), json.getString("accent2"))
            assertEquals(hex(palette.background), json.getString("background"))
            assertEquals(hex(palette.textPrimary), json.getString("text"))
            assertEquals(hex(palette.surface), json.getString("surface"))
        }
        assertEquals(TrainPilotPalettes.getById(1).key, TrainPilotUserSettings().themeJson().getString("key"))
    }

    @Test fun selectedThemeIsCommittedBeforePublicationAndSurvivesStoreRecreation() = runBlocking {
        val palette = TrainPilotPalettes.getById(25)
        var delivered: JSONObject? = null
        var sends = 0
        val store = TrainPilotSettingsStore(context, publishTheme = { ctx ->
            delivered = TrainPilotSettingsStore(ctx).current().themeJson()
            sends++
        })
        store.setTheme(ThemePaletteOption(palette.key, "fixture", "fixture", hex(palette.primary), hex(palette.secondary), "light"))
        assertEquals(1, sends)
        assertEquals(palette.key, delivered!!.getString("key"))
        assertEquals("light", delivered!!.getString("family"))
        assertEquals(hex(palette.background), delivered!!.getString("background"))
        assertEquals(palette.key, TrainPilotSettingsStore(context).current().themeKey)
    }

    @Test fun unavailableWatchTransportKeepsTheSelectedThemeForReconnect() = runBlocking {
        val palette = TrainPilotPalettes.getById(9)
        val store = TrainPilotSettingsStore(context, publishTheme = { throw IllegalStateException("offline") })
        store.setTheme(ThemePaletteOption(palette.key, "fixture", "fixture", hex(palette.primary), hex(palette.secondary), "contrast"))
        val restored = TrainPilotSettingsStore(context).current()
        assertEquals(palette.key, restored.themeKey)
        assertEquals(hex(palette.primary), restored.themeJson().getString("accent"))
    }
}
