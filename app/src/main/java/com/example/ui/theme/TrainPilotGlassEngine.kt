package com.example.ui.theme

import android.graphics.Paint
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import androidx.compose.foundation.background
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
internal fun Modifier.trainPilotGlassMaterial(palette: TrainPilotPalette, shape: Shape,
    role: GlassMaterialRole = GlassMaterialRole.Panel): Modifier {
    if (!palette.isLiquidGlass) return this
    if (LocalGlassOverlay.current) return background(glassOverlayRolePaint(palette, role), shape)
        .overlayRoleRim(palette, shape, role)
    if (role == GlassMaterialRole.Tile) return trainPilotGlassRim(palette, shape, role)
    // Dense/measurement surfaces keep a bounded paint with a curved vector edge. Actual
    // lenses are admitted through the explicit shared drop path, never by opening the route gate.
    if (!LocalGlassOpticsEnabled.current || role == GlassMaterialRole.Button ||
        role == GlassMaterialRole.Input || role == GlassMaterialRole.Performance)
        return liquidDropMaterial(palette, shape, role, live = false)
    return kyantGlassOrNull(palette, shape, role) ?: glassSceneModifier(palette, shape)
}

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
                        val photo = scene?.imageShader(sceneWidth, sceneHeight)
                        val seconds = if (photo == null) scene?.seconds ?: 0f else 0f
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
            val shadow = if (path != null && radii.any { it > 0f }) path else null
            onDrawWithContent {
                // 2810/6: soft drop shadow outside rounded glass (darkens the page only, never under text).
                if (shadow != null) drawGlassDropShadow(shadow)
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
                        if (paintComposeScene(optics.backdropCap)) drawRect(GlassOptics.TINT.copy(alpha = optics.coat))
                        else {
                            drawRect(fallback)
                            drawRect(glassFallbackPaint(palette, palette.glassClarity))
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
    try { canvas.drawRect(0f, 0f, width, height, glassPaint) }
    finally { glassPaint.shader = null }
    return true
}

/**
 * Vector approximation of the shader's rim for paths without AGSL (API 31–32, measurement screens,
 * API < 31 opaque cards): a thin specular edge, top strongest (~[GlassOptics.specular] white), sides
 * and bottom weaker. 2810/6: no wide sheen, nothing under text. Continuous in the slider like the shader.
 */
internal fun DrawScope.drawGlassHighlights(path: Path, @Suppress("UNUSED_PARAMETER") palette: TrainPilotPalette,
    optics: GlassOptics, highlight: Float) {
    val strength = (optics.specular + highlight).coerceIn(0f, .6f)
    val rim = Brush.verticalGradient(
        0f to Color.White.copy(alpha = strength),
        .5f to Color.White.copy(alpha = strength * .40f),
        1f to Color.White.copy(alpha = strength * .27f))
    drawPath(path, rim, style = Stroke(1.dp.toPx()))
}

/** Soft shadow below a rounded glass surface: a few widening strokes outside the outline, offset down. */
internal fun DrawScope.drawGlassDropShadow(path: Path) {
    val drop = 2.dp.toPx()
    val step = 3.dp.toPx()
    clipPath(path, ClipOp.Difference) {
        translate(top = drop) {
            for (layer in 1..4) drawPath(path, Color.Black.copy(alpha = .045f), style = Stroke(width = step * layer))
        }
    }
}

/**
 * A highlight over a painted control; its semantic color and text stay unchanged. Vector only, so it
 * also stays on the active measurement screens (2808/10 light glass).
 */
internal fun glassPrismaticRimRole(role: GlassMaterialRole): Boolean =
    role in listOf(GlassMaterialRole.Panel, GlassMaterialRole.Navigation,
        GlassMaterialRole.Button, GlassMaterialRole.Selection, GlassMaterialRole.Input, GlassMaterialRole.Overlay)

@Composable
internal fun Modifier.trainPilotGlassRim(palette: TrainPilotPalette, shape: Shape,
    role: GlassMaterialRole = GlassMaterialRole.Tile): Modifier {
    if (!palette.glassRimActive()) return this
    // Dense tiles keep a single cached edge gradient; no extra lens or draw pass.
    // Measurement and battery saver retain the original restrained edge.
    val fullRim = LocalGlassOpticsEnabled.current && !LocalGlassPowerSave.current
    val prism = glassPrismaticRimRole(role) && fullRim
    val tileEdge = role == GlassMaterialRole.Tile && fullRim
    return drawWithCache {
        val path = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache)) }
        // 2810/6: a specular edge, not a frame: top strongest, sides and bottom weaker, ~1 dp inside.
        val strength = (GlassOptics.from(palette.glassClarity).specular * when (role) {
            GlassMaterialRole.Selection -> 1.15f
            GlassMaterialRole.Performance -> .70f
            else -> 1f
        }).coerceAtMost(.6f)
        val light = Color.White.copy(alpha = strength)
        val mid = Color.White.copy(alpha = strength * .40f)
        val dim = Color.White.copy(alpha = strength * .27f)
        val shine = if (role == GlassMaterialRole.Button || role == GlassMaterialRole.Selection) {
            Brush.linearGradient(0f to light, .55f to mid, 1f to dim,
                end = Offset(size.width * .70f, size.height))
        } else if (tileEdge) {
            Brush.linearGradient(0f to light, .45f to mid, .82f to palette.primary.copy(alpha = strength * .55f), 1f to dim,
                end = Offset(size.width, size.height))
        } else {
            Brush.verticalGradient(0f to light, .5f to mid, 1f to dim)
        }
        // Two directional, palette-colored highlights stay confined to the curved rim.
        // This is a specular vector approximation, not an additional backdrop capture.
        val clear = palette.glassClarity.coerceIn(0, 100) / 100f
        val prismShine = if (prism) Brush.linearGradient(
            0f to palette.primary.copy(alpha = .17f + .16f * clear),
            .30f to Color.White.copy(alpha = .24f + .30f * clear),
            .56f to Color.Transparent,
            .80f to palette.primary.copy(alpha = .20f + .20f * clear),
            1f to Color.Transparent,
            start = Offset.Zero, end = Offset(size.width, size.height)) else null
        // Cache the brushes with the outline, not once per draw during a ticking workout.
        val bevel = Brush.verticalGradient(
            0f to Color.Black.copy(alpha = .02f),
            .50f to Color.Black.copy(alpha = .06f),
            1f to Color.Black.copy(alpha = .32f))
        onDrawWithContent {
            drawContent()
            clipPath(path) {
                drawPath(path, bevel, style = Stroke(6.dp.toPx()))
                if (prismShine != null) drawPath(path, prismShine, style = Stroke((3f + 2f * clear).dp.toPx()))
                drawPath(path, shine, style = Stroke(2.dp.toPx()))
            }
        }
    }
}
