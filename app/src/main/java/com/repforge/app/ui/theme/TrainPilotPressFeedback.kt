package com.repforge.app.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.graphics.graphicsLayer
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

/** Glass-only press spring. Compose respects the platform animation duration scale. */
@Composable
internal fun Modifier.glassPressMotion(source: MutableInteractionSource, enabled: Boolean): Modifier {
    if (!LocalTrainPilotPalette.current.isLiquidGlass || !LocalGlassOpticsEnabled.current) return this
    val held by source.collectIsPressedAsState()
    val scale = animateFloatAsState(
        targetValue = if (enabled && held) .965f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow),
        label = "glass press"
    )
    return graphicsLayer { scaleX = scale.value; scaleY = scale.value }
}

/** Paint on the visual child while the accessible parent retains the larger hit area. */
@Composable
internal fun Modifier.trainPilotPressIndication(
    shape: Shape, source: MutableInteractionSource, enabled: Boolean = true
): Modifier {
    val alpha = interactionAlpha(source, enabled)
    val tint = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
    return glassPressMotion(source, enabled).clip(shape).drawWithContent { drawContent(); if (alpha > 0f) drawRect(tint) }
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
