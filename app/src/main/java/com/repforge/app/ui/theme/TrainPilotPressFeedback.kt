package com.repforge.app.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.clickable
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
    val alpha = interactionAlpha(source, enabled)
    val tint = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
    return clip(shape).drawWithContent { drawContent(); if (alpha > 0f) drawRect(tint) }
        .clickable(interactionSource = source, indication = null, enabled = enabled,
            role = Role.Button, onClick = onClick)
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
