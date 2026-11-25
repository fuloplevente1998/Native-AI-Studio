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
    if (palette.isLiquidGlass) glassSceneModifier(palette, null, GlassRole.Panel) else this

/** 2811/G1: [role] picks the semantic material (default [GlassRole.Panel] = the pre-role look). */
@Composable
internal fun Modifier.trainPilotGlassMaterial(palette: TrainPilotPalette, shape: Shape,
    role: GlassRole = GlassRole.Panel): Modifier =
    if (palette.isLiquidGlass) glassSceneModifier(palette, shape, role) else this

/** UI-thread only; the paint is reused, the shader it carries is snapshotted by each draw call. */
private val glassPaint by lazy { Paint(Paint.ANTI_ALIAS_FLAG) }

@Composable
private fun Modifier.glassSceneModifier(palette: TrainPilotPalette, shape: Shape?, requested: GlassRole): Modifier {
    val effectsEnabled = LocalGlassOpticsEnabled.current
    val scene = LocalGlassScene.current
    val configuration = LocalConfiguration.current
    val localDensity = LocalDensity.current
    val sceneWidth = with(localDensity) { configuration.screenWidthDp.dp.toPx() }.coerceAtLeast(1f)
    val sceneHeight = with(localDensity) { configuration.screenHeightDp.dp.toPx() }.coerceAtLeast(1f)
    // Position is read only in the draw phase: scrolling re-binds uniforms, never recomposes or recompiles.
    val origin = remember { mutableStateOf(Offset.Zero) }
    // Measurement screens: the light Performance material (no shadow, softer rim); else the requested role.
    val role = GlassRole.resolve(requested, effectsEnabled)
    val optics = remember(palette.glassClarity, role) { GlassOptics.from(palette.glassClarity, role) }
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
            /**
             * API 31–32 (no AGSL) and, 2810/3, the active measurement screens (cheap, static): the same
             * backdrop drawn with Compose brushes, guarded at [cap]; no blur, shader or capture.
             */
            fun DrawScope.paintComposeScene(cap: Float): Boolean {
                if (Build.VERSION.SDK_INT < 31) return false
                var hardware = false
                drawIntoCanvas { hardware = it.nativeCanvas.isHardwareAccelerated }
                if (!hardware) return false
                drawGlassSceneCompose(palette, scene, origin.value, sceneWidth, sceneHeight, cap)
                return true
            }
            val shadow = if (path != null && optics.elevation > 0f && radii.any { it > 0f }) path else null
            onDrawWithContent {
                // 2810/6: soft drop shadow outside rounded glass (darkens the page only, never under text).
                if (shadow != null) drawGlassDropShadow(shadow, optics.elevation)
                val painted = if (path == null) paintMaterial() else {
                    var drawn = false
                    clipPath(path) { drawn = paintMaterial() }
                    drawn
                }
                if (!painted) {
                    // No AGSL: API 31–32, compile failure or an active measurement screen paint the guarded
                    // Compose backdrop (cards: limited to the glass backdrop cap) + the thin white-ish tint +
                    // the vector rim. API < 31 / software: the previous opaque gradient + paint.
                    if (path == null) { if (!paintComposeScene(GlassScene.lumaCap)) drawRect(fallback) } else clipPath(path) {
                        // 2811/1: a page-matched surface uses the page guard itself (no luminance step).
                        val cap = if (role.matchesPage) GlassScene.lumaCap else optics.backdropCap
                        if (paintComposeScene(cap)) drawRect(GlassOptics.TINT.copy(alpha = optics.coat))
                        else {
                            drawRect(fallback)
                            drawRect(glassFallbackPaint(palette.glassClarity))
                        }
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
 * Vector approximation of the shader's rim for paths without AGSL (API 31–32, measurement screens,
 * API < 31 opaque cards): a thin specular edge, top strongest (~[GlassOptics.specular] white), sides
 * and bottom weaker. 2810/6: no wide sheen, nothing under text. Continuous in the slider like the shader.
 */
internal fun DrawScope.drawGlassHighlights(path: Path, @Suppress("UNUSED_PARAMETER") palette: TrainPilotPalette,
    optics: GlassOptics, highlight: Float) {
    val strength = (optics.specular + highlight).coerceIn(0f, GlassRimLight.MAX_SHINE)
    val rim = Brush.verticalGradient(*GlassRimLight.gradient(strength).toTypedArray())
    drawPath(path, rim, style = Stroke(1.dp.toPx()))
}

/**
 * Soft shadow below a rounded glass surface: a few widening strokes outside the outline, offset down.
 * 2811/G1: [elevation] is the role's lift (1 = Panel, the 2810 shadow; Tile/Input/Performance: none).
 */
internal fun DrawScope.drawGlassDropShadow(path: Path, elevation: Float = 1f) {
    val lift = elevation.coerceIn(0f, 1.5f)
    if (lift <= 0f) return
    val drop = 2.dp.toPx() * lift.coerceAtLeast(.5f)
    val step = 3.dp.toPx()
    clipPath(path, ClipOp.Difference) {
        translate(top = drop) {
            for (layer in 1..4) drawPath(path, Color.Black.copy(alpha = .045f * lift), style = Stroke(width = step * layer))
        }
    }
}

/**
 * A highlight over a painted control; its semantic color and text stay unchanged. Vector only, so it
 * also stays on the active measurement screens (2808/10 light glass).
 */
@Composable
internal fun Modifier.trainPilotGlassRim(palette: TrainPilotPalette, shape: Shape, role: GlassRole = GlassRole.Panel): Modifier =
    if (!palette.glassRimActive()) this else drawWithCache {
        val path = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache)) }
        // 2810/6: a specular edge, not a frame: top strongest, sides and bottom weaker, ~1 dp inside.
        // 2811/G1: the strength follows the role (Panel = the previous rim).
        val strength = GlassOptics.from(palette.glassClarity, role).specular.coerceAtMost(GlassRimLight.MAX_SHINE)
        val shine = Brush.verticalGradient(*GlassRimLight.gradient(strength).toTypedArray())
        onDrawWithContent {
            drawContent()
            clipPath(path) { drawPath(path, shine, style = Stroke(2.dp.toPx())) }
        }
    }
