package com.repforge.app.wear

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WearThemePersistenceRegressionTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private fun theme(family: String = "light") = JSONObject().put("key", "phone-light")
        .put("accent", "#2563eb").put("accent2", "#1d4ed8").put("family", family)
        .put("background", "#ffffff").put("surface", "#f4f7fb").put("elevated", "#e5ebf3")
        .put("text", "#151b26").put("muted", "#48566c").put("outline", "#2563eb").put("onAccent", "#ffffff")
    private fun home(stamp: Long, palette: JSONObject?) = JSONObject().put("publishedAt", stamp).apply {
        if (palette != null) put("theme", palette)
    }.toString()

    @Test fun themePacketRoundTripsFullPaletteAndAllNativeFamilies() {
        for (family in listOf("matte", "contrast", "glow", "light", "basic", "vivid")) {
            val parsed = WearHomeTheme.parse(theme(family))
            assertEquals(family, parsed.family)
            assertEquals(Color.White, parsed.backgroundColor)
            assertEquals(Color(0xFF151B26), parsed.textColor)
            assertEquals(parsed, WearHomeTheme.parse(parsed.toJson()))
            val material = parsed.materialColors()
            assertEquals(parsed.surfaceColor, material.surfaceContainer)
            assertEquals(parsed.elevatedColor, material.surfaceContainerHigh)
            assertEquals(parsed.backgroundColor, material.background)
            assertEquals(parsed.textColor, material.onSurface)
            assertEquals(parsed.onAccentColor, material.onPrimary)
        }
        assertEquals(WearHomeTheme.DEFAULT, WearHomeTheme.parse(theme().put("accent", "transparent")))
    }

    @Test fun restartAndPartialOrOlderHomePacketsRetainLastValidTheme() {
        val selected = WatchHomeStore.save(context, home(100, theme()))!!
        assertEquals("phone-light", selected.theme.key)
        assertEquals(selected.theme, WatchHomeStore.load(context)!!.theme)
        val partial = WatchHomeStore.save(context, home(200, null))!!
        assertEquals(selected.theme, partial.theme)
        val malformed = WatchHomeStore.save(context, home(300, theme().put("accent", "broken")))!!
        assertEquals(selected.theme, malformed.theme)
        val old = WatchHomeStore.save(context, home(50, theme().put("key", "stale")))!!
        assertEquals(selected.theme, old.theme)
        assertEquals(300L, WatchHomeStore.load(context)!!.publishedAt)
        val next = WatchHomeStore.save(context, home(400, theme("contrast").put("key", "next")))!!
        assertEquals("next", WatchHomeStore.load(context)!!.theme.key)
        assertEquals(next.theme, WatchHomeStore.load(context)!!.theme)
    }
}
