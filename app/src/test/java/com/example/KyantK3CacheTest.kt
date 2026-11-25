package com.example

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*
import org.junit.Assert.*
import org.junit.Test

class KyantK3CacheTest {
    private val p = LiquidGlassPalettes.all.first()
    private val cap = GlassScene.lumaCap

    @Test fun guardedTonesReuseOnlyTheSameColorAndCap() {
        val cache = GlassScenePaintCache()
        val first = cache.guardedTones(p, cap)
        assertSame(first, cache.guardedTones(p, cap))
        assertEquals(GlassScene.guardedTones(p, cap), first)
        val changed = p.copy(primary = Color.Green)
        val other = cache.guardedTones(changed, cap)
        assertNotSame(first, other)
        assertEquals(GlassScene.guardedTones(changed, cap), other)
        val darker = cache.guardedTones(changed, cap / 2)
        assertNotSame(other, darker)
        assertEquals(GlassScene.guardedTones(changed, cap / 2), darker)
    }

    @Test fun brushCacheReusesExactFramesAndInvalidatesEveryDrawInput() {
        val cache = GlassScenePaintCache()
        val base = cache.baseBrush(p, 0f, 1f, cap)
        assertSame(base, cache.baseBrush(p, 0f, 1f, cap))
        assertNotSame(base, cache.baseBrush(p, 0f, .9f, cap))
        assertNotSame(base, cache.baseBrush(p.copy(background = Color.Red), 0f, 1f, cap))
        assertNotSame(base, cache.baseBrush(p, 0f, 1f, cap / 2))
        fun frame(palette: TrainPilotPalette = p, origin: Offset = Offset.Zero,
            width: Float = 1080f, height: Float = 2340f, time: Float = 0f, limit: Float = cap) =
            cache.blobBrushes(palette, origin, width, height, time, limit)
        val first = frame()
        assertEquals(GlassScene.blobCount, first.size)
        assertSame(first, frame())
        val moved = frame(origin = Offset(5f, 10f))
        assertNotSame(first, moved)
        val resized = frame(origin = Offset(5f, 10f), width = 900f)
        assertNotSame(moved, resized)
        val rotated = frame(origin = Offset(5f, 10f), width = 900f, height = 1800f)
        assertNotSame(resized, rotated)
        val ticked = frame(origin = Offset(5f, 10f), width = 900f, height = 1800f, time = 1f)
        assertNotSame(rotated, ticked)
        val limited = frame(origin = Offset(5f, 10f), width = 900f, height = 1800f, time = 1f, limit = cap / 2)
        assertNotSame(ticked, limited)
        assertNotSame(limited, frame(p.copy(primary = Color.Green), Offset(5f, 10f), 900f, 1800f, 1f, cap / 2))
        // Single-slot eviction, not a growing per-frame map.
        assertNotSame(first, frame())
    }

    @Test fun photoGeometryReusesDimensionsAndInvalidatesOnImageOrViewportResize() {
        val cache = GlassScenePaintCache()
        val first = cache.imageCrop(1440, 960, 1080f, 2340f)
        assertSame(first, cache.imageCrop(1440, 960, 1080f, 2340f))
        assertEquals(GlassScene.crop(1440, 960, 1080f, 2340f), first)
        val differentImage = cache.imageCrop(960, 1440, 1080f, 2340f)
        assertNotSame(first, differentImage)
        assertEquals(GlassScene.crop(960, 1440, 1080f, 2340f), differentImage)
        val landscape = cache.imageCrop(960, 1440, 2340f, 1080f)
        assertNotSame(differentImage, landscape)
        assertEquals(GlassScene.crop(960, 1440, 2340f, 1080f), landscape)
    }
}
