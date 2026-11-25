package com.repforge.app.wear

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import java.time.Instant
import java.time.ZoneId
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

/** 2808 watch regressions: stable pulse line, real daily steps, bezel arcs, watch contrast. */
class Wear2808Test {
    private val now = Instant.parse("2026-11-24T21:46:00Z").toEpochMilli()
    private val zone = ZoneId.of("Europe/Budapest")

    @Test fun pulseLineDoesNotAlternateWhenSamplesArriveBetweenClockTicks() {
        // Reproduces the 0.5 s flicker: the UI clock ticks once per second, passive samples
        // arrive in between and are a few hundred ms "newer" than the last tick.
        for (offset in listOf(-900L, -400L, 0L, 300L, 700L, 999L)) {
            val line = wearHomePulse(null, WearDailyPulse(87, now + offset), now)
            assertEquals(87, line.bpm)
            assertEquals(WearPulseFreshness.RECENT, line.freshness)
            assertEquals("87 bpm", wearHomePulseText(line, zone, "hu"))
        }
        // Consecutive ticks with the same sample never switch to an empty value.
        val sample = WearDailyPulse(87, now + 400L)
        val ticks = (0..6).map { wearHomePulse(null, sample, now + it * 1000L) }
        assertTrue(ticks.all { it.bpm == 87 })
    }

    @Test fun olderPulseKeepsValueWithEarlierMarkerAndMissingIsDash() {
        val earlier = wearHomePulse(null, WearDailyPulse(87, now - 10 * 60000L), now)
        assertEquals(87, earlier.bpm)
        assertEquals(WearPulseFreshness.PREVIOUS, earlier.freshness)
        assertEquals("87 bpm · korábbi 22:36", wearHomePulseText(earlier, zone, "hu"))
        assertEquals("87 bpm · earlier 22:36", wearHomePulseText(earlier, zone, "en"))
        val none = wearHomePulse(null, null, now)
        assertNull(none.bpm)
        assertEquals("–", wearHomePulseText(none, zone, "hu"))
        assertNull(wearHomePulse(null, WearDailyPulse(87, now - 31 * 60000L), now).bpm)
        assertNull(wearHomePulse(null, WearDailyPulse(87, now + 60000L), now).bpm)
        assertNull(wearHomePulse(null, WearDailyPulse(0, now), now).bpm)
        val live = wearHomePulse(112, WearDailyPulse(87, now), now)
        assertEquals(112, live.bpm)
        assertEquals(WearPulseFreshness.LIVE, live.freshness)
    }

    @Test fun liveWorkoutPulseToleratesSubSecondTickLagButNotStaleData() {
        val data = JSONObject().put("workoutId", "w").put("source", "wear_health_services").put("state", "active")
            .put("heartRate", 128).put("updatedAt", now + 400L).put("lastSampleAt", now + 400L)
        assertEquals(128, wearHomeLivePulse(data, "w", true, now))
        assertNull(wearHomeLivePulse(data, "w", true, now + 16000L))
        assertNull(wearHomeLivePulse(data.put("lastSampleAt", now + 60000L).put("updatedAt", now + 60000L), "w", true, now))
    }

    @Test fun staleSameDayStepTotalIsNotPresentedAsCurrentProgress() {
        val morning = now - 13 * 3600000L
        assertFalse(wearDailyStepsFresh(morning, now))
        assertTrue(wearDailyStepsFresh(now - 20 * 60000L, now))
        assertFalse(wearDailyStepsFresh(null, now))
        assertFalse(wearDailyStepsFresh(now + 1000L, now))
        // Missing steps or goal stay unknown, never 0 %.
        assertNull(wearDailyStepPercent(null, 8000))
        assertEquals(78, wearDailyStepPercent(6240, 8000))
    }

    @Test fun healthServicesIntervalEndIsClampedOnlyWithinClockTolerance() {
        assertEquals(now, wearDailySampleTime(now + 30000L, now))
        assertEquals(now - 5000L, wearDailySampleTime(now - 5000L, now))
        assertNull(wearDailySampleTime(now + 10 * 60000L, now))
        assertNull(wearDailySampleTime(0L, now))
        // A clamped sample passes the unchanged same-day acceptance rule; a later one replaces it.
        val first = wearDailySampleTime(now + 30000L, now)!!
        assertTrue(wearDailyAccept(0L, first, now, zone))
        assertTrue(wearDailyAccept(first, now + 60000L, now + 60000L, zone))
    }

    @Test fun bezelArcsHugTheEdgeAndLabelsNeverOverlapThemOnAllSizes() {
        for (diameter in listOf(160f, 176f, 192f, 216f, 240f)) {
            val arcs = wearHomeArcLayout(diameter)
            assertTrue("small inset", arcs.inset <= 4f)
            assertTrue("longer sweep than the old 90°", arcs.sweep >= 100f)
            // Concentric and mirrored left/right.
            assertEquals(180f, arcs.batteryStart + arcs.stepsStart + arcs.sweep, .01f)
            assertEquals(180f, arcs.batteryLabel + arcs.stepsLabel, .01f)
            // Labels sit on the ring past the lower arc ends.
            assertTrue(arcs.batteryLabel + arcs.labelSweep / 2f < arcs.batteryStart)
            assertTrue(arcs.stepsLabel - arcs.labelSweep / 2f > arcs.stepsStart + arcs.sweep)
            // Both labels stay apart and away from the top status line.
            assertTrue(arcs.batteryLabel - arcs.labelSweep / 2f > arcs.stepsLabel + arcs.labelSweep / 2f + 30f)
            assertTrue(270f - (arcs.batteryStart + arcs.sweep) >= 18f)
            // Labels stay below the three home buttons (their lowest point is above 30° below centre).
            assertTrue(arcs.stepsLabel - arcs.labelSweep / 2f >= 30f)
        }
    }

    private fun palette(family: String, bg: String?, text: String?, accent: String, accent2: String, extra: JSONObject.() -> Unit = {}) =
        WearHomeTheme.parse(JSONObject().put("key", "t").put("accent", accent).put("accent2", accent2).put("family", family).apply {
            if (bg != null) put("background", bg).put("surface", bg).put("elevated", bg)
            if (text != null) put("text", text).put("muted", text)
            extra()
        })

    @Test fun everyPaletteRendersReadableOnTheWatch() {
        val palettes = listOf(
            WearHomeTheme.DEFAULT,
            palette("light", null, null, "#2563eb", "#1d4ed8"),
            palette("light", "#ffffff", "#151b26", "#2563eb", "#1d4ed8") { put("outline", "#2563eb").put("onAccent", "#ffffff") },
            palette("light", "#fff8e1", "#5d4037", "#ffd54f", "#ffe082"),
            palette("basic", "#111111", "#333333", "#202020", "#303030"),
            palette("contrast", "#7f7f7f", "#808080", "#7a7a7a", "#858585"),
            palette("vivid", "#0b0f14", "#f6f8fb", "#ff3366", "#00e5ff"),
            // 2809: a custom light theme (not the light family) with light text on white.
            palette("basic", "#ffffff", "#f0f0f0", "#ffeb3b", "#e0f7fa"),
        )
        for (raw in palettes) {
            val t = raw.forWatch()
            val surfaces = listOf(t.backgroundColor, t.surfaceColor, t.elevatedColor)
            // 2809: light palettes stay genuinely light on the watch, dark palettes keep the dark base.
            if (raw.family == "light" || raw.backgroundColor.luminance() >= .4f)
                assertTrue("light watch base ${raw.background}", surfaces.all { it.luminance() >= .4f })
            else assertTrue("dark watch base ${raw.background}", surfaces.all { it.luminance() <= .18f })
            for (s in surfaces) {
                assertTrue("text ${raw.text}", wearContrast(t.textColor, s) >= 4.5f)
                assertTrue("muted ${raw.muted}", wearContrast(t.mutedColor, s) >= 4.5f)
                assertTrue("secondary ${raw.accent2}", wearContrast(t.secondaryColor, s) >= 4.5f)
                assertTrue("accent control ${raw.accent}", wearContrast(t.accentColor, s) >= 3f)
            }
            assertTrue(wearContrast(t.accentColor, t.backgroundColor) >= 4.5f)
            assertTrue(wearContrast(t.accentColor, t.surfaceColor) >= 4.5f)
            assertTrue("outline", wearContrast(t.outlineColor, t.backgroundColor) >= 3f)
            assertTrue("on accent", wearContrast(t.onAccentColor, t.accentColor) >= 4.5f)
            assertEquals(t, t.forWatch())
        }
    }

    @Test fun liquidGlassAccentFamiliesStayOpaqueAndReadableOnWatch() {
        // Phone LiquidGlassPalettes seed colors; transport uses family=contrast.
        val seeds = listOf(
            "graphite" to "#d0dbe8", "ice" to "#bcebff", "sapphire" to "#8db9ff",
            "turquoise" to "#70e5ef", "emerald" to "#78f2bc", "amethyst" to "#d0b2ff",
            "rose" to "#ffb9d8", "amber" to "#ffd18c",
        )
        for ((name, accent) in seeds) for (surface in listOf("#111722", "#555b64", "#737983")) {
            val raw = palette("contrast", "#172333", "#ffffff", accent, accent) {
                put("key", "glass_$name").put("surface", surface).put("elevated", surface)
                    .put("muted", "#e0e8f1").put("onAccent", "#000000")
            }
            val stored = raw.toJson().toString()
            val watch = raw.forWatch()
            assertEquals(stored, raw.toJson().toString())
            assertEquals(raw, WearHomeTheme.parse(raw.toJson()))
            assertEquals(raw.key, watch.key)
            for (bg in listOf(watch.backgroundColor, watch.surfaceColor, watch.elevatedColor)) {
                assertEquals(1f, bg.alpha, 0f)
                assertTrue(wearContrast(watch.textColor, bg) >= 4.5f)
                assertTrue(wearContrast(watch.mutedColor, bg) >= 4.5f)
                assertTrue(wearContrast(watch.secondaryColor, bg) >= 4.5f)
                assertTrue(wearContrast(watch.accentColor, bg) >= 3f)
            }
            assertTrue(wearContrast(watch.onAccentColor, watch.accentColor) >= 4.5f)
        }
    }

    @Test fun darkDefaultKeepsItsIdentityAndStoredPaletteIsUntouched() {
        val watch = WearHomeTheme.DEFAULT.forWatch()
        assertEquals(WearHomeTheme.DEFAULT.accentColor, watch.accentColor)
        assertEquals(WearHomeTheme.DEFAULT.secondaryColor, watch.secondaryColor)
        assertEquals(WearHomeTheme.DEFAULT.backgroundColor, watch.backgroundColor)
        assertEquals(WearHomeTheme.DEFAULT.textColor, watch.textColor)
        val light = palette("light", "#ffffff", "#151b26", "#2563eb", "#1d4ed8")
        val lightWatch = light.forWatch()
        assertEquals(Color.White, light.backgroundColor)
        // 2809: the light theme is light on the watch too; its readable ink/accent are kept as they are.
        assertEquals(Color.White, lightWatch.backgroundColor)
        assertEquals(Color.White, lightWatch.surfaceColor)
        assertEquals(light.textColor, lightWatch.textColor)
        assertEquals(light.accentColor, lightWatch.accentColor)
        assertEquals(light, WearHomeTheme.parse(light.toJson()))
    }
}
