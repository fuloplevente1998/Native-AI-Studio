package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.theme.glassOverlayFill

/** Popup windows use a single bounded matte fill: no capture, blur or per-row lens. */
@Composable
internal fun TrainPilotDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    shape: Shape? = null,
    containerColor: Color? = null,
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val p = LocalTrainPilotPalette.current
    if (p.isLiquidGlass) {
        DropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest,
            modifier = modifier, scrollState = scrollState,
            shape = shape ?: RoundedCornerShape(12.dp),
            containerColor = p.glassOverlayFill(p.surfaceCard), tonalElevation = 0.dp,
            border = border ?: BorderStroke(1.dp, p.cardBorder.copy(alpha = .60f))) {
            CompositionLocalProvider(LocalContentColor provides p.textPrimary) { content() }
        }
    } else if (shape != null && containerColor != null) {
        DropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest,
            modifier = modifier, scrollState = scrollState, shape = shape,
            containerColor = containerColor, border = border, content = content)
    } else {
        DropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest,
            modifier = modifier, scrollState = scrollState, border = border, content = content)
    }
}
