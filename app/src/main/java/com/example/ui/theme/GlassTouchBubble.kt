package com.example.ui.theme

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * 2808/11: touch "bubble" of the glass controls – a glowing, slightly magnifying lens under the
 * finger that follows it while pressed and springs away on release. Pure rules here (unit-tested);
 * the modifier below renders it. Original TrainPilot code, no third-party shader.
 */
internal object GlassTouchBubble {
    const val MIN_RADIUS_DP = 18f
    const val MAX_RADIUS_DP = 30f
    /** Center magnification of the AGSL lens (1 + MAGNIFY): "slightly", never a zoom. */
    const val MAGNIFY = .12f
    const val GLOW = .20f
    const val RIM = .32f
    const val DAMPING = .58f
    const val STIFFNESS = 520f

    /**
     * Only Liquid Glass with full optics (so never on the active measurement screens), only with
     * system animations on, and only for an enabled control.
     */
    fun enabled(glassActive: Boolean, motionEnabled: Boolean, controlEnabled: Boolean): Boolean =
        glassActive && motionEnabled && controlEnabled

    fun radiusPx(size: Size, density: Float): Float {
        val shorter = minOf(size.width, size.height)
        return (shorter * .75f).coerceIn(MIN_RADIUS_DP * density, MAX_RADIUS_DP * density)
    }

    /** Grows in on press; the release spring shrinks it (overshoot below 0 is clamped). */
    fun radiusFactor(progress: Float): Float = .55f + .45f * progress.coerceIn(0f, 1.15f)

    fun alpha(progress: Float): Float = progress.coerceIn(0f, 1f)

    /** The lens never leaves the control: the touch point is clamped to its bounds. */
    fun clampToBounds(position: Offset, size: Size): Offset =
        Offset(position.x.coerceIn(0f, size.width), position.y.coerceIn(0f, size.height))

    /** AGSL: dome-profile magnification of the control's own rendered content (premultiplied safe). */
    const val LENS_SOURCE = """
        uniform shader content;
        uniform float2 center;
        uniform float radius;
        uniform float strength;
        uniform float magnify;

        half4 main(float2 pos) {
            float2 d = pos - center;
            float r = length(d) / max(radius, 1.0);
            if (r >= 1.0 || strength <= 0.0) { return content.eval(pos); }
            float dome = 1.0 - r * r;
            float k = 1.0 - magnify * strength * dome;
            half4 c = content.eval(center + d * k);
            float lift = strength * 0.10 * dome;
            return half4(mix(c.rgb, half3(c.a), half(lift)), c.a);
        }
    """
}

/** Compiled once per process; a compile failure keeps the Compose-only glow for good. */
@RequiresApi(33)
private object GlassBubbleLens33 {
    private var failed = false
    private var program: RuntimeShader? = null

    fun effect(center: Offset, radius: Float, strength: Float): androidx.compose.ui.graphics.RenderEffect? {
        if (failed || strength <= .001f) return null
        val shader = program ?: runCatching { RuntimeShader(GlassTouchBubble.LENS_SOURCE) }
            .onFailure { failed = true }.getOrNull()?.also { program = it } ?: return null
        return runCatching {
            shader.setFloatUniform("center", center.x, center.y)
            shader.setFloatUniform("radius", radius)
            shader.setFloatUniform("strength", strength)
            shader.setFloatUniform("magnify", GlassTouchBubble.MAGNIFY)
            RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
        }.onFailure { failed = true }.getOrNull()
    }
}

/**
 * Applied by the shared press path ([com.repforge.app.ui.theme.glassPressMotion] and the outlined /
 * text / action buttons), so every glass button, chip and nav item gets it without per-call-site
 * changes. Pointer events are only observed (Initial pass, never consumed): clicks, scrolling and
 * the accessible touch target are unchanged. Draw/layer-phase only while the finger moves.
 */
@Composable
internal fun Modifier.glassTouchBubble(source: MutableInteractionSource, enabled: Boolean, shape: Shape?): Modifier {
    val palette = LocalTrainPilotPalette.current
    // Kyant supplies the optical surface. Retain the shared press motion, not a second AGSL lens.
    if (KyantGlass.active(palette) && LocalKyantScene.current.forThisWindow() != null) return this
    val motion = rememberGlassMotionEnabled()
    if (!GlassTouchBubble.enabled(palette.glassEffectsActive(LocalGlassOpticsEnabled.current), motion, enabled)) return this
    val held by source.collectIsPressedAsState()
    val touch = remember { mutableStateOf(Offset.Unspecified) }
    val progress = animateFloatAsState(if (held) 1f else 0f,
        spring(dampingRatio = GlassTouchBubble.DAMPING, stiffness = GlassTouchBubble.STIFFNESS), label = "glass bubble")
    // Same rule as the other glass passes: the AGSL lens only on a hardware canvas (seen while drawing).
    val hardware = remember { BooleanArray(1) }
    val lens = if (Build.VERSION.SDK_INT >= 33) Modifier.graphicsLayer {
        val at = touch.value
        renderEffect = if (!at.isSpecified || !hardware[0]) null else GlassBubbleLens33.effect(
            GlassTouchBubble.clampToBounds(at, size),
            GlassTouchBubble.radiusPx(size, density) * GlassTouchBubble.radiusFactor(progress.value),
            GlassTouchBubble.alpha(progress.value))
    } else Modifier
    return pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                // Keep the last point after release, so the bubble springs away where the finger left.
                event.changes.firstOrNull { it.pressed }?.let { touch.value = it.position }
            }
        }
    }.drawWithContent {
        drawIntoCanvas { hardware[0] = it.nativeCanvas.isHardwareAccelerated }
        drawContent()
        val strength = GlassTouchBubble.alpha(progress.value)
        val raw = touch.value
        if (strength <= .001f || !raw.isSpecified) return@drawWithContent
        val at = GlassTouchBubble.clampToBounds(raw, size)
        val radius = GlassTouchBubble.radiusPx(size, density) * GlassTouchBubble.radiusFactor(progress.value)
        fun bubble() {
            drawCircle(Brush.radialGradient(
                0f to Color.White.copy(alpha = GlassTouchBubble.GLOW * strength),
                .7f to Color.White.copy(alpha = GlassTouchBubble.GLOW * .35f * strength),
                1f to Color.Transparent, center = at, radius = radius), radius, at)
            drawCircle(Brush.linearGradient(
                0f to Color.White.copy(alpha = GlassTouchBubble.RIM * strength),
                1f to palette.primary.copy(alpha = .18f * strength),
                start = at - Offset(radius, radius), end = at + Offset(radius, radius)),
                radius * .96f, at, style = Stroke(1.dp.toPx()))
        }
        if (shape == null) bubble() else {
            val path = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithContent)) }
            clipPath(path) { bubble() }
        }
    }.then(lens)
}
