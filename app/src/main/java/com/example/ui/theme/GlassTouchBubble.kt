package com.example.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first

/**
 * Touch lighting is drawn on the existing control. Kyant supplies the optical refraction;
 * the highlight follows the pointer without recording a second rendering layer.
 */
internal object GlassTouchBubble {
    const val MIN_RADIUS_DP = 18f
    const val MAX_RADIUS_DP = 30f
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

    /** Keep the touch highlight inside the control. */
    fun clampToBounds(position: Offset, size: Size): Offset =
        Offset(position.x.coerceIn(0f, size.width), position.y.coerceIn(0f, size.height))
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
    val motion = rememberGlassMotionEnabled()
    if (!GlassTouchBubble.enabled(palette.glassEffectsActive(LocalGlassOpticsEnabled.current), motion, enabled)) return this
    var heldPress by remember(source) { mutableStateOf<PressInteraction.Press?>(null) }
    val touch = remember(source) { mutableStateOf(Offset.Unspecified) }
    var pressCount by remember(source) { mutableIntStateOf(0) }
    val progress = remember(source) { Animatable(0f) }
    LaunchedEffect(source) {
        source.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    heldPress = interaction
                    if (!touch.value.isSpecified) touch.value = interaction.pressPosition
                    pressCount++
                }
                is PressInteraction.Release -> {
                    if (heldPress == interaction.press) heldPress = null
                }
                is PressInteraction.Cancel -> {
                    if (heldPress == interaction.press) heldPress = null
                }
                else -> Unit
            }
        }
    }
    LaunchedEffect(source, pressCount) {
        if (pressCount == 0) return@LaunchedEffect
        // Press and Release can arrive in one frame. Complete the short entry on either path.
        progress.animateTo(1f, tween(75))
        snapshotFlow { heldPress }.first { it == null }
        progress.animateTo(0f, spring(dampingRatio = GlassTouchBubble.DAMPING, stiffness = GlassTouchBubble.STIFFNESS))
    }
    return pointerInput(source) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                // Keep the last point after release, so the bubble springs away where the finger left.
                event.changes.firstOrNull { it.pressed }?.let { touch.value = it.position }
            }
        }
    }.drawWithCache {
        val outline = shape?.createOutline(size, layoutDirection, this)
        val path = outline?.let { Path().apply { addOutline(it) } }
        val baseRadius = GlassTouchBubble.radiusPx(size, density)
        val stroke = Stroke(1.dp.toPx())
        onDrawWithContent {
            drawContent()
            val strength = GlassTouchBubble.alpha(progress.value)
            val raw = touch.value
            if (strength <= .001f || !raw.isSpecified) return@onDrawWithContent
            val at = GlassTouchBubble.clampToBounds(raw, size)
            val radius = baseRadius * GlassTouchBubble.radiusFactor(progress.value)
            fun bubble() {
                val light = at + Offset(-radius * .16f, -radius * .24f)
                drawCircle(Brush.radialGradient(
                    0f to Color.White.copy(alpha = GlassTouchBubble.GLOW * strength),
                    .55f to Color.White.copy(alpha = GlassTouchBubble.GLOW * .38f * strength),
                    1f to Color.Transparent, center = light, radius = radius), radius, light)
                drawCircle(Brush.linearGradient(
                    0f to Color.White.copy(alpha = GlassTouchBubble.RIM * strength),
                    .56f to Color.Transparent,
                    1f to palette.primary.copy(alpha = .20f * strength),
                    start = at - Offset(radius, radius), end = at + Offset(radius, radius)),
                    radius * .87f, at, style = stroke)
            }
            if (path == null) bubble() else clipPath(path) { bubble() }
        }
    }
}
