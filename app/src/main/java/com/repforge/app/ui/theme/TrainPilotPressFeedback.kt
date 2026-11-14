package com.repforge.app.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver

/** Draw a held state over the painted shape; place before the clickable modifier. */
@Composable
fun Modifier.trainPilotPressFeedback(shape: Shape, pressed: Boolean): Modifier {
    val alpha by animateFloatAsState(if (pressed) 0.12f else 0f, label = "control press")
    val tint = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
    return clip(shape).drawWithContent {
        drawContent()
        if (alpha > 0f) drawRect(tint)
    }
}

/** Button-like rows share the same shaped feedback without a second growing ripple. */
@Composable
fun Modifier.trainPilotClickable(shape: Shape, enabled: Boolean = true, onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val focused by source.collectIsFocusedAsState()
    val hovered by source.collectIsHoveredAsState()
    return trainPilotPressFeedback(shape, enabled && (pressed || focused || hovered))
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
    val pressed by source.collectIsPressedAsState()
    val focused by source.collectIsFocusedAsState()
    val hovered by source.collectIsHoveredAsState()
    val alpha by animateFloatAsState(when {
        !enabled -> 0f
        pressed || focused -> 0.12f
        hovered -> 0.08f
        else -> 0f
    }, label = "button press")
    val tint = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
    val feedbackColors = colors.copy(containerColor = tint.compositeOver(colors.containerColor))
    CompositionLocalProvider(LocalRippleConfiguration provides null) {
        content(feedbackColors, source)
    }
}
