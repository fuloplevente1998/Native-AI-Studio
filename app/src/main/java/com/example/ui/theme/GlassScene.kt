package com.example.ui.theme

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.Shader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/** 2809/1: what the page backdrop behind the glass does on the current screen. */
internal enum class GlassBackdropMode { OFF, STATIC, LIVING }

/** One soft palette blob of the living backdrop, in root pixels. */
internal data class GlassBlob(val x: Float, val y: Float, val radius: Float, val strength: Float)

/**
 * 2809/1 living backdrop (approved mockup A, plus the optional user image B). Pure rules and colour
 * math, shared by the AGSL scene (API 33+) and the cheap Compose path (API 31–32), so every glass
 * surface samples the same picture at its own root position. Original TrainPilot values.
 */
internal object GlassScene {
    /** Drift step: about 12 fps is enough for blobs that cross the screen in about a minute. */
    const val FRAME_MS = 83L
    /** 2810/6: brighter, larger blobs: more colour for the glass to refract (still guarded). */
    const val BLOB_STRENGTH = .95f
    /** Blob drift amplitude as a fraction of the screen. */
    const val DRIFT = .10f
    /** Knee of the luminance guard, as a fraction of [lumaCap]: darker colours pass unchanged. */
    const val KNEE = .55f
    private const val WRAP_SECONDS = 36_000f
    // Four light sources, in anchor order. Distinct combinations give the glass presets their
    // own backdrop without changing stored theme IDs, accent colors or the user photo path.
    private val goldHue = hueOf(Color(TRAIN_PILOT_GOLD))
    private val paletteHues = mapOf(
        33 to listOf(4f, 48f, 307f, 152f),     // Graphite
        34 to listOf(196f, 212f, 186f, 33f),   // Ice
        35 to listOf(222f, 233f, 254f, 20f),   // Sapphire
        36 to listOf(169f, 184f, 203f, 294f),  // Turquoise
        37 to listOf(146f, 111f, 166f, 41f),   // Emerald
        38 to listOf(268f, 254f, 295f, 174f),  // Amethyst
        39 to listOf(333f, 353f, 314f, 214f),  // Rose
        40 to listOf(goldHue, goldHue - 7f, goldHue + 6f, goldHue - 14f) // Gold
    )
    private val hueOffsets = floatArrayOf(0f, 48f, -56f, 150f)
    private val periods = floatArrayOf(53f, 67f, 59f, 79f)
    // x, y (fraction of the screen), radius (fraction of the width).
    private val anchors = arrayOf(floatArrayOf(.84f, .12f, .76f), floatArrayOf(.10f, .40f, .80f),
        floatArrayOf(.80f, .68f, .70f), floatArrayOf(.26f, .94f, .78f))
    val blobCount: Int get() = anchors.size

    /**
     * Brightest luminance the page backdrop may reach. The palettes' text and outline colours are
     * lifted against the glass ceiling ([GlassOptics.SURFACE_LUMA_MAX]); staying below it keeps text
     * >= 4.5:1 and outlines >= 3:1 on every blob colour and on any user image. 2810/6: the ceiling
     * rose (.084 -> .13), so the page is brighter; under glass the lens limits its own backdrop
     * ([GlassOptics.backdropCap]) because the white-ish tint adds light.
     */
    val lumaCap: Float = GlassOptics.SURFACE_LUMA_MAX * .96f

    /** Off for legacy palettes and on the active measurement screens; static when motion is off. */
    fun mode(isLiquidGlass: Boolean, opticsEnabled: Boolean, motionEnabled: Boolean,
        powerSave: Boolean = false): GlassBackdropMode = when {
        !isLiquidGlass || !opticsEnabled -> GlassBackdropMode.OFF
        !motionEnabled || powerSave -> GlassBackdropMode.STATIC
        else -> GlassBackdropMode.LIVING
    }

    /** The drift clock runs only for a living backdrop on a started (visible) screen. */
    fun ticks(mode: GlassBackdropMode, visible: Boolean, hasPhoto: Boolean = false): Boolean = mode == GlassBackdropMode.LIVING && visible && !hasPhoto

    /** The user image is only decoded and drawn for the glass family. */
    fun usesImage(isLiquidGlass: Boolean, path: String): Boolean = isLiquidGlass && path.isNotEmpty()

    fun advance(seconds: Float, deltaSeconds: Float): Float =
        ((seconds + deltaSeconds.coerceIn(0f, 1f)) % WRAP_SECONDS).let { if (it.isNaN()) 0f else it }

    fun blobs(width: Float, height: Float, seconds: Float): List<GlassBlob> = anchors.mapIndexed { i, a ->
        val phase = 2f * PI.toFloat() * seconds / periods[i]
        val dx = DRIFT * sin(phase + i * 1.7f)
        val dy = DRIFT * cos(phase * .83f + i * .9f)
        GlassBlob((a[0] + dx) * width, (a[1] + dy) * height, a[2] * width, BLOB_STRENGTH)
    }

    /** Vivid hue rotations of the theme accent (the shader guards their luminance after mixing). */
    fun tones(palette: TrainPilotPalette): List<Color> {
        val hues = paletteHues[palette.id] ?: hueOffsets.map { hueOf(palette.primary) + it }
        return hues.map { Color.hsv(((it % 360f) + 360f) % 360f, .90f, 1f) }
    }

    /**
     * Pre-guarded tones for the Compose path: an sRGB alpha mix of guarded colours stays guarded
     * (the sRGB decode is convex). [cap]: the page guard, or a glass surface's [GlassOptics.backdropCap].
     */
    fun guardedTones(palette: TrainPilotPalette, cap: Float = lumaCap): List<Color> = tones(palette).map { guard(it, cap) }

    /** Page base: the palette backdrop with a gentle top-lit vignette, guarded like the shader. */
    fun base(palette: TrainPilotPalette, v: Float, cap: Float = lumaCap): Color {
        val k = .80f + .20f * (1f - v.coerceIn(0f, 1f))
        return guard(Color(palette.background.red * k, palette.background.green * k, palette.background.blue * k), cap)
    }

    /** Exact mirror of the AGSL guard: luminance above the knee is softly compressed below [cap]. */
    fun guard(color: Color, cap: Float = lumaCap): Color {
        val lumaCap = cap.coerceAtLeast(1e-5f)
        val r = linear(color.red.coerceIn(0f, 1f))
        val g = linear(color.green.coerceIn(0f, 1f))
        val b = linear(color.blue.coerceIn(0f, 1f))
        val l = .2126f * r + .7152f * g + .0722f * b
        val knee = lumaCap * KNEE
        if (l <= knee) return color.copy(alpha = 1f)
        val mapped = knee + (lumaCap - knee) * (1f - exp((knee - l) / (lumaCap - knee)))
        val k = mapped / l
        return Color(srgb(r * k), srgb(g * k), srgb(b * k))
    }

    /** Linear dim for a user image on the Compose path: even pure white stays at [lumaCap]. */
    val imageScale: Float get() = imageScaleFor(lumaCap)

    fun imageScaleFor(cap: Float): Float = srgb(cap.coerceIn(0f, 1f))

    fun linear(c: Float): Float = if (c <= .04045f) c / 12.92f else ((c + .055f) / 1.055f).pow(2.4f)
    fun srgb(l: Float): Float = if (l <= .0031308f) l * 12.92f else 1.055f * l.coerceAtLeast(0f).pow(1f / 2.4f) - .055f

    fun hueOf(color: Color): Float {
        val maxC = maxOf(color.red, color.green, color.blue)
        val minC = minOf(color.red, color.green, color.blue)
        val d = maxC - minC
        if (d <= 1e-4f) return 0f
        val h = when (maxC) {
            color.red -> 60f * (((color.green - color.blue) / d) % 6f)
            color.green -> 60f * ((color.blue - color.red) / d + 2f)
            else -> 60f * ((color.red - color.green) / d + 4f)
        }
        return (h + 360f) % 360f
    }

    /** Centre-crop of an image onto the scene, in root pixels: scale, then offset. */
    fun crop(imageWidth: Int, imageHeight: Int, sceneWidth: Float, sceneHeight: Float): Pair<Float, Offset> {
        val scale = max(sceneWidth / imageWidth.coerceAtLeast(1), sceneHeight / imageHeight.coerceAtLeast(1))
        return scale to Offset((sceneWidth - imageWidth * scale) / 2f, (sceneHeight - imageHeight * scale) / 2f)
    }
}

/** Shared drift time and the decoded user image. Read only while drawing: a tick redraws, never recomposes. */
@Stable
internal class GlassSceneState {
    // UI-thread cache shared by the visible and recorded scene; no bitmap or view references.
    val paintCache = GlassScenePaintCache()
    var seconds by mutableFloatStateOf(0f)
    private var openSheets by mutableIntStateOf(0)
    val coveredBySheet: Boolean get() = openSheets > 0

    fun sheetOpened() { openSheets++ }
    fun sheetClosed() { openSheets-- }

    private var currentImage by mutableStateOf<Bitmap?>(null)
    var image: Bitmap?
        get() = currentImage
        set(value) {
            if (currentImage === value) return
            // Clear references, but never recycle a bitmap still held by a recorded frame.
            cachedImage = null
            cachedFor = null
            shader = null
            currentImage = value
        }
    val imageBitmap: ImageBitmap? get() = image?.let { bitmap -> cachedImage?.takeIf { cachedFor === bitmap }
        ?: bitmap.asImageBitmap().also { cachedImage = it; cachedFor = bitmap } }
    private var cachedImage: ImageBitmap? = null
    private var cachedFor: Bitmap? = null
    private var shader: BitmapShader? = null
    private var shaderWidth = 0f
    private var shaderHeight = 0f

    /** UI thread only. One shader per image and scene size; each draw call snapshots it. */
    fun imageShader(sceneWidth: Float, sceneHeight: Float): Shader? {
        val bitmap = image ?: return null
        if (shader == null || sceneWidth != shaderWidth || sceneHeight != shaderHeight) {
            val (scale, at) = GlassScene.crop(bitmap.width, bitmap.height, sceneWidth, sceneHeight)
            shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
                setLocalMatrix(Matrix().apply { setScale(scale, scale); postTranslate(at.x, at.y) })
            }
            shaderWidth = sceneWidth
            shaderHeight = sceneHeight
        }
        return shader
    }
}

/** A sheet captures the scene once; the root drift clock cannot invalidate that capture. */
internal fun freezeSheetScene(source: GlassSceneState?): GlassSceneState = GlassSceneState().apply {
    seconds = source?.seconds ?: 0f
    image = source?.image
}

internal val LocalGlassScene = staticCompositionLocalOf<GlassSceneState?> { null }

/**
 * Root host of the living backdrop: one low-rate clock (paused when the app is not started, off on
 * the measurement screens and for legacy palettes, frozen when system animations are off) and the
 * user image decoded once off the main thread.
 */
@Composable
internal fun GlassSceneHost(imagePath: String, loadImage: suspend (String) -> Bitmap?, content: @Composable () -> Unit) {
    val palette = LocalTrainPilotPalette.current
    val state = remember { GlassSceneState() }
    val powerSave = rememberGlassPowerSave(palette.isLiquidGlass)
    val mode = GlassScene.mode(palette.isLiquidGlass, LocalGlassOpticsEnabled.current,
        rememberGlassMotionEnabled(), powerSave)
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val ticking = GlassScene.ticks(
        mode, lifecycle.isAtLeast(Lifecycle.State.STARTED) && !state.coveredBySheet, hasPhoto = state.image != null
    )
    val wantImage = GlassScene.usesImage(palette.isLiquidGlass, imagePath)
    LaunchedEffect(wantImage, imagePath) {
        state.image = if (!wantImage) null else withContext(Dispatchers.IO) { runCatching { loadImage(imagePath) }.getOrNull() }
    }
    LaunchedEffect(ticking, mode) {
        if (mode == GlassBackdropMode.STATIC) state.seconds = 0f
        if (!ticking) return@LaunchedEffect
        var last = System.nanoTime()
        while (true) {
            delay(GlassScene.FRAME_MS)
            val now = System.nanoTime()
            state.seconds = GlassScene.advance(state.seconds, (now - last) / 1e9f)
            last = now
        }
    }
    CompositionLocalProvider(LocalGlassScene provides state, LocalGlassPowerSave provides powerSave) { content() }
}

/**
 * Compose path of the backdrop (API 31–32, no AGSL): guarded base, guarded blobs as radial
 * gradients, or the user image dimmed linearly so even white stays below the guard.
 */
internal fun DrawScope.drawGlassSceneCompose(palette: TrainPilotPalette, scene: GlassSceneState?, origin: Offset,
    sceneWidth: Float, sceneHeight: Float, cap: Float = GlassScene.lumaCap) {
    val cache = scene?.paintCache ?: GlassScenePaintCache()
    val top = (-origin.y / sceneHeight).coerceIn(0f, 1f)
    val bottom = ((size.height - origin.y) / sceneHeight).coerceIn(0f, 1f)
    drawRect(cache.baseBrush(palette, top, bottom, cap))
    val image = scene?.imageBitmap
    if (image != null) {
        val (scale, at) = cache.imageCrop(image.width, image.height, sceneWidth, sceneHeight)
        drawImage(image, srcOffset = IntOffset.Zero, srcSize = IntSize(image.width, image.height),
            dstOffset = IntOffset((at.x - origin.x).roundToInt(), (at.y - origin.y).roundToInt()),
            dstSize = IntSize((image.width * scale).roundToInt(), (image.height * scale).roundToInt()),
            colorFilter = cache.photoFilter(cap))
        return
    }
    for (brush in cache.blobBrushes(palette, origin, sceneWidth, sceneHeight, scene?.seconds ?: 0f, cap))
        drawRect(brush)
}
