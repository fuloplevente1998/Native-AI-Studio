package com.example.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    /** The vector-only indicator is safe even without an optical backdrop or animation. */
    fun enabled(liquid: Boolean, controlEnabled: Boolean): Boolean = liquid && controlEnabled

    /** Do not reuse the previous touch for a new press, including synthetic interactions. */
    fun pressPoint(event: Offset, localPointer: Offset): Offset =
        if (localPointer.isSpecified) localPointer else event

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
internal fun Modifier.glassTouchBubble(source: MutableInteractionSource, enabled: Boolean, shape: Shape?, trackDrag: Boolean = false): Modifier {
    val palette = LocalTrainPilotPalette.current
    if (!GlassTouchBubble.enabled(palette.isLiquidGlass, enabled)) return this
    val motion = rememberGlassMotionEnabled() && !LocalGlassPowerSave.current
    val touch = remember(source) { mutableStateOf(Offset.Unspecified) }
    // Local coordinates are needed when the visual child is smaller than the hit target.
    val localPointer = remember(source) { mutableStateOf(Offset.Unspecified) }
    val held = remember(source) { mutableStateOf<Interaction?>(null) }
    val progress = remember(source) { Animatable(0f) }
    LaunchedEffect(source, motion, trackDrag) {
        progress.snapTo(0f)
        held.value = null
        var animation: Job? = null
        source.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    held.value = interaction
                    touch.value = GlassTouchBubble.pressPoint(interaction.pressPosition, localPointer.value)
                    animation?.cancel()
                    animation = launch {
                        progress.snapTo(0f)
                        if (motion) progress.animateTo(1f, tween(75)) else progress.snapTo(1f)
                    }
                }
                is PressInteraction.Release -> if (held.value == interaction.press) {
                    held.value = null
                    animation?.cancel()
                    animation = launch {
                        // A press/release pair may arrive in the same frame. Finish the local
                        // entry, or briefly show a static mark with reduced motion.
                        if (motion) {
                            if (progress.value < .8f) progress.animateTo(1f, tween(50))
                            progress.animateTo(0f, spring(dampingRatio = GlassTouchBubble.DAMPING,
                                stiffness = GlassTouchBubble.STIFFNESS))
                        } else {
                            progress.snapTo(1f)
                            delay(80L)
                            progress.snapTo(0f)
                        }
                    }
                }
                is PressInteraction.Cancel -> if (held.value == interaction.press) {
                    held.value = null
                    animation?.cancel()
                    animation = launch {
                        if (motion) progress.animateTo(0f, spring(dampingRatio = GlassTouchBubble.DAMPING,
                            stiffness = GlassTouchBubble.STIFFNESS)) else progress.snapTo(0f)
                    }
                }
                // Only native adjustable controls opt in. A scroll drag that cancels a
                // normal button's press must not leave the highlight on that button.
                is DragInteraction.Start -> if (trackDrag && localPointer.value.isSpecified) {
                    held.value = interaction
                    touch.value = localPointer.value
                    animation?.cancel()
                    animation = launch {
                        if (motion) progress.animateTo(1f, tween(75)) else progress.snapTo(1f)
                    }
                }
                is DragInteraction.Stop -> if (held.value == interaction.start) {
                    held.value = null
                    animation?.cancel()
                    animation = launch {
                        if (motion) progress.animateTo(0f, tween(130)) else progress.snapTo(0f)
                    }
                }
                is DragInteraction.Cancel -> if (held.value == interaction.start) {
                    held.value = null
                    animation?.cancel()
                    animation = launch { progress.snapTo(0f) }
                }
                else -> Unit
            }
        }
    }
    return pointerInput(source) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val point = event.changes.firstOrNull { it.pressed }?.position ?: Offset.Unspecified
                localPointer.value = point
                if (held.value != null && point.isSpecified) touch.value = point
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
