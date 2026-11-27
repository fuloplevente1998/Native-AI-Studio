package com.example.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix

/** UI-thread only, bounded last-value slots. Never retains a bitmap, view or drawing scope.
 * Exact keys preserve geometry, colors and contrast; repeated recording/drawing at the same
 * frame or static photo reuses paint objects. A changed key replaces, rather than grows, a slot.
 */
internal class GlassScenePaintCache {
    private class Slot<K : Any, V : Any> {
        private var key: K? = null
        private var value: V? = null

        fun get(next: K, build: () -> V): V {
            if (next == key) value?.let { return it }
            return build().also { key = next; value = it }
        }
    }

    private data class ToneKey(val paletteId: Int, val primary: Color, val cap: Float)
    private data class BaseKey(val background: Color, val top: Float, val bottom: Float, val cap: Float)
    private data class BlobKey(val tone: ToneKey, val origin: Offset, val width: Float,
        val height: Float, val seconds: Float)
    private data class CropKey(val imageWidth: Int, val imageHeight: Int, val width: Float, val height: Float)
    private val tones = Slot<ToneKey, List<Color>>()
    private val base = Slot<BaseKey, Brush>()
    private val blobs = Slot<BlobKey, List<Brush>>()
    private val crop = Slot<CropKey, Pair<Float, Offset>>()
    private val photo = Slot<Float, ColorFilter>()
    private val topShade = Slot<Pair<Float, Float>, Brush>()

    fun guardedTones(palette: TrainPilotPalette, cap: Float): List<Color> =
        tones.get(ToneKey(palette.id, palette.primary, cap)) { GlassScene.guardedTones(palette, cap) }

    fun baseBrush(palette: TrainPilotPalette, top: Float, bottom: Float, cap: Float): Brush =
        base.get(BaseKey(palette.background, top, bottom, cap)) {
            Brush.verticalGradient(listOf(GlassScene.base(palette, top, cap), GlassScene.base(palette, bottom, cap)))
        }

    fun blobBrushes(palette: TrainPilotPalette, origin: Offset, width: Float, height: Float,
        seconds: Float, cap: Float): List<Brush> =
        blobs.get(BlobKey(ToneKey(palette.id, palette.primary, cap), origin, width, height, seconds)) {
            val colors = guardedTones(palette, cap)
            GlassScene.blobs(width, height, seconds).mapIndexed { i, blob ->
                val tone = colors[i]
                Brush.radialGradient(
                    0f to tone.copy(alpha = blob.strength),
                    .5f to tone.copy(alpha = blob.strength * .46f),
                    1f to tone.copy(alpha = 0f),
                    center = Offset(blob.x - origin.x, blob.y - origin.y), radius = blob.radius.coerceAtLeast(1f))
            }
        }

    fun topShadeBrush(originY: Float, sceneHeight: Float): Brush =
        topShade.get(originY to sceneHeight) {
            Brush.verticalGradient(
                0f to Color.Black.copy(alpha = GlassScene.TOP_SHADE_ALPHA),
                1f to Color.Transparent,
                startY = -originY,
                endY = sceneHeight.coerceAtLeast(1f) * GlassScene.TOP_LIGHT_END - originY)
        }

    fun imageCrop(imageWidth: Int, imageHeight: Int, width: Float, height: Float): Pair<Float, Offset> =
        crop.get(CropKey(imageWidth, imageHeight, width, height)) {
            GlassScene.crop(imageWidth, imageHeight, width, height)
        }

    fun photoFilter(cap: Float): ColorFilter = photo.get(cap) {
        val dim = GlassScene.imageScaleFor(cap)
        ColorFilter.colorMatrix(ColorMatrix().apply { setToScale(dim, dim, dim, 1f) })
    }
}
