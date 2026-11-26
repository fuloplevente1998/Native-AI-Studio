package com.repforge.app.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import com.example.ui.theme.GlassMotion
import com.example.ui.theme.KyantGlass
import com.example.ui.theme.LocalGlassOverlay
import com.example.ui.theme.LocalKyantScene
import com.example.ui.theme.forThisWindow
import com.example.ui.theme.glassEffectsActive
import com.example.ui.theme.glassTouchBubble
import com.example.ui.theme.rememberGlassMotionEnabled
import androidx.compose.ui.graphics.drawscope.scale
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.theme.LocalGlassOpticsEnabled
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlin.math.max

/** A short release may arrive in the same frame as Press (notably inside scrolling lists). */
@Composable
private fun interactionAlpha(source: MutableInteractionSource, enabled: Boolean): Float {
    val held by source.collectIsPressedAsState()
    val focused by source.collectIsFocusedAsState()
    val hovered by source.collectIsHoveredAsState()
    var pulse by remember(source) { mutableIntStateOf(0) }
    var handled by remember(source) { mutableIntStateOf(0) }
    val pressAlpha = remember(source) { Animatable(0f) }
    LaunchedEffect(source) {
        source.interactions.collect { if (it is PressInteraction.Press) pulse++ }
    }
    LaunchedEffect(source, enabled, pulse) {
        if (!enabled) {
            handled = pulse
            pressAlpha.snapTo(0f)
        } else if (pulse != handled) {
            handled = pulse
            pressAlpha.animateTo(0.12f, tween(70))
            snapshotFlow { held }.first { !it }
            pressAlpha.animateTo(0f, tween(130))
        }
    }
    val focusAlpha by animateFloatAsState(when {
        !enabled -> 0f
        focused -> 0.12f
        hovered -> 0.08f
        else -> 0f
    }, label = "control focus")
    return if (enabled) max(pressAlpha.value, focusAlpha) else 0f
}

/** Button-like rows share the same shaped feedback without a second growing ripple. */
@Composable
fun Modifier.trainPilotClickable(shape: Shape, enabled: Boolean = true, onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    return trainPilotPressIndication(shape, source, enabled)
        .clickable(interactionSource = source, indication = null, enabled = enabled,
            role = Role.Button, onClick = onClick)
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun Modifier.trainPilotCombinedClickable(
    shape: Shape,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    onLongClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier {
    val source = remember { MutableInteractionSource() }
    return trainPilotPressIndication(shape, source, enabled)
        .combinedClickable(
            interactionSource = source,
            indication = null,
            enabled = enabled,
            role = Role.Button,
            onClickLabel = onClickLabel,
            onLongClickLabel = onLongClickLabel,
            onLongClick = onLongClick,
            onClick = onClick
        )
}

/**
 * Glass-only press: spring scale to [GlassMotion.PRESS_SCALE] with a light-side highlight that
 * brightens while held; release springs back with a small overshoot. Under reduced motion only
 * the highlight remains (no scale). Draw-only: layout and the accessible touch bounds stay put.
 */
@Composable
internal fun Modifier.glassPressMotion(source: MutableInteractionSource, enabled: Boolean, shape: Shape? = null): Modifier {
    val palette = LocalTrainPilotPalette.current
    if (!palette.glassEffectsActive(LocalGlassOpticsEnabled.current)) return this
    val kyant = KyantGlass.active(palette) &&
        (LocalGlassOverlay.current || LocalKyantScene.current.forThisWindow() != null)
    val motion = rememberGlassMotionEnabled()
    val held by source.collectIsPressedAsState()
    val press = animateFloatAsState(
        targetValue = if (enabled && held) 1f else 0f,
        animationSpec = if (motion) spring(dampingRatio = .55f, stiffness = Spring.StiffnessMediumLow) else snap(),
        label = "glass press"
    )
    // 2808/11: the touch bubble rides inside the same scaled draw (glass + motion only, see GlassTouchBubble).
    return drawWithContent {
        val progress = press.value
        val factor = if (motion) GlassMotion.pressScale(progress) else 1f
        scale(factor, factor) {
            this@drawWithContent.drawContent()
            val glow = progress.coerceIn(0f, 1f) * GlassMotion.PRESS_HIGHLIGHT
            if (glow > .001f && !kyant) {
                val light = Brush.radialGradient(listOf(Color.White.copy(alpha = glow), Color.Transparent),
                    center = Offset(size.width * .3f, 0f), radius = maxOf(size.width, size.height) * .9f)
                if (shape == null) drawRect(light) else {
                    val path = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithContent)) }
                    clipPath(path) { drawRect(light) }
                }
            }
        }
    }.glassTouchBubble(source, enabled, shape)
}

/** Paint on the visual child while the accessible parent retains the larger hit area. */
@Composable
internal fun Modifier.trainPilotPressIndication(
    shape: Shape, source: MutableInteractionSource, enabled: Boolean = true
): Modifier {
    val alpha = interactionAlpha(source, enabled)
    val tint = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
    return glassPressMotion(source, enabled, shape).clip(shape).drawWithContent { drawContent(); if (alpha > 0f) drawRect(tint) }
}

/** A held press covers the whole painted outline, including wide buttons. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TrainPilotPressFeedback(
    colors: ButtonColors,
    enabled: Boolean,
    interactionSource: MutableInteractionSource?,
    content: @Composable (ButtonColors, MutableInteractionSource) -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val alpha = interactionAlpha(source, enabled)
    val tint = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
    val feedbackColors = colors.copy(containerColor = tint.compositeOver(colors.containerColor))
    CompositionLocalProvider(LocalRippleConfiguration provides null) {
        content(feedbackColors, source)
    }
}
