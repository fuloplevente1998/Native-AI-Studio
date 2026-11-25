package com.example.ui.theme

import android.graphics.Paint
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Controlled backdrop: the page background is a procedural scene drawn in root coordinates, and
 * every glass surface on that page samples the SAME scene at its own root position. A card in a
 * scrolling list therefore refracts exactly what lies behind it (the page background), without
 * reading pixels from other windows, protected content or arbitrary siblings.
 * Only used by Liquid Glass; all other palettes keep their existing paint.
 */
@Composable
internal fun Modifier.trainPilotGlassScene(palette: TrainPilotPalette): Modifier =
    if (palette.isLiquidGlass) glassSceneModifier(palette, null) else this

@Composable
internal fun Modifier.trainPilotGlassMaterial(palette: TrainPilotPalette, shape: Shape): Modifier =
    if (palette.isLiquidGlass) glassSceneModifier(palette, shape) else this

/** UI-thread only; the paint is reused, the shader it carries is snapshotted by each draw call. */
private val glassPaint by lazy { Paint(Paint.ANTI_ALIAS_FLAG) }

@Composable
private fun Modifier.glassSceneModifier(palette: TrainPilotPalette, shape: Shape?): Modifier {
    val effectsEnabled = LocalGlassOpticsEnabled.current
    val scene = LocalGlassScene.current
    val configuration = LocalConfiguration.current
    val localDensity = LocalDensity.current
    val sceneWidth = with(localDensity) { configuration.screenWidthDp.dp.toPx() }.coerceAtLeast(1f)
    val sceneHeight = with(localDensity) { configuration.screenHeightDp.dp.toPx() }.coerceAtLeast(1f)
    // Position is read only in the draw phase: scrolling re-binds uniforms, never recomposes or recompiles.
    val origin = remember { mutableStateOf(Offset.Zero) }
    val optics = remember(palette.glassClarity) { GlassOptics.from(palette.glassClarity) }
    return onGloballyPositioned { coordinates -> origin.value = coordinates.positionInRoot() }
        .drawWithCache {
            val outline = shape?.createOutline(size, layoutDirection, this)
            val path = outline?.let { Path().apply { addOutline(it) } }
            val radii = outline?.glassCornerRadii() ?: FloatArray(4)
            val fallback = Brush.verticalGradient(listOf(palette.background, palette.customPageEnd ?: palette.background))
            fun DrawScope.paintMaterial(): Boolean {
                if (!effectsEnabled) return false
                var painted = false
                drawIntoCanvas { canvas ->
                    val native = canvas.nativeCanvas
                    if (native.isHardwareAccelerated && Build.VERSION.SDK_INT >= 33) painted = runCatching {
                        val at = origin.value
                        // 2809/1: drift time and image are read while drawing, so a tick only redraws.
                        val seconds = scene?.seconds ?: 0f
                        val photo = scene?.imageShader(sceneWidth, sceneHeight)
                        drawGlass33(native, size.width, size.height, shape != null, radii, sceneWidth, sceneHeight,
                            at, palette, optics, localDensity.density, seconds, photo)
                    }.getOrDefault(false)
                }
                return painted
            }
            /** API 31–32 (no AGSL): the same backdrop drawn with Compose brushes, no blur or capture. */
            fun DrawScope.paintComposeScene(): Boolean {
                if (!effectsEnabled || Build.VERSION.SDK_INT < 31) return false
                var hardware = false
                drawIntoCanvas { hardware = it.nativeCanvas.isHardwareAccelerated }
                if (!hardware) return false
                drawGlassSceneCompose(palette, scene, origin.value, sceneWidth, sceneHeight)
                return true
            }
            onDrawWithContent {
                val painted = if (path == null) paintMaterial() else {
                    var drawn = false
                    clipPath(path) { drawn = paintMaterial() }
                    drawn
                }
                if (!painted) {
                    // Opaque fallback: API < 31, software rendering, compile failure or an active measurement
                    // screen. 2808/10: the cheap vector rim/sheen stays there too (light glass, no shader/blur).
                    // 2809/1: API 31–32 paints the guarded backdrop with Compose brushes instead of the flat gradient.
                    if (path == null) { if (!paintComposeScene()) drawRect(fallback) } else clipPath(path) {
                        if (!paintComposeScene()) drawRect(fallback)
                        drawRect(palette.cardPaint)
                        drawGlassHighlights(path, palette, optics, 0f)
                    }
                }
                // 2809/2: no neutral hairline frame here either; the rim highlight above defines the shape.
                drawContent()
            }
        }
}

@RequiresApi(33)
private fun drawGlass33(canvas: android.graphics.Canvas, width: Float, height: Float, lens: Boolean, radii: FloatArray,
    sceneWidth: Float, sceneHeight: Float, origin: Offset, palette: TrainPilotPalette, optics: GlassOptics,
    density: Float, seconds: Float, photo: Shader?): Boolean {
    val shader = if (!lens) GlassPrograms33.boundScene(sceneWidth, sceneHeight, origin.x, origin.y, palette,
        seconds, photo, guard = true)
    else GlassPrograms33.sceneMaterial(width, height, radii, sceneWidth, sceneHeight,
        origin.x, origin.y, palette, optics, density, 0f, seconds, photo)
    if (shader == null) return false
    glassPaint.shader = shader
    canvas.drawRect(0f, 0f, width, height, glassPaint)
    glassPaint.shader = null
    return true
}

/**
 * Vector approximation of the shader's rim for paths without AGSL (API 31–32 live blur, API < 33
 * opaque cards): a top sheen and a light-facing rim. Continuous in the slider like the shader.
 */
internal fun DrawScope.drawGlassHighlights(path: Path, palette: TrainPilotPalette, optics: GlassOptics, highlight: Float) {
    val strength = (optics.specular + highlight).coerceIn(0f, 1f)
    drawRect(Brush.verticalGradient(0f to Color.White.copy(alpha = .10f * strength), .45f to Color.Transparent))
    val rim = Brush.linearGradient(
        0f to Color.White.copy(alpha = .35f + .45f * strength),
        .5f to palette.cardBorder.copy(alpha = .28f),
        1f to Color.White.copy(alpha = .12f + .20f * strength),
        start = Offset.Zero, end = Offset(size.width, size.height))
    drawPath(path, rim, style = Stroke(1.2.dp.toPx()))
}

/**
 * A highlight over a painted control; its semantic color and text stay unchanged. Vector only, so it
 * also stays on the active measurement screens (2808/10 light glass).
 */
@Composable
internal fun Modifier.trainPilotGlassRim(palette: TrainPilotPalette, shape: Shape): Modifier =
    if (!palette.glassRimActive()) this else drawWithCache {
        val path = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache)) }
        val strength = GlassOptics.from(palette.glassClarity).specular
        val shine = Brush.linearGradient(
            0f to Color.White.copy(alpha = .30f + .40f * strength),
            .55f to Color.White.copy(alpha = .06f),
            1f to palette.primary.copy(alpha = .30f),
            start = Offset.Zero, end = Offset(size.width * .4f, size.height))
        onDrawWithContent {
            drawContent()
            clipPath(path) { drawPath(path, shine, style = Stroke(1.5.dp.toPx())) }
        }
    }
