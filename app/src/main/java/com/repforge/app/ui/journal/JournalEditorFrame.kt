package com.repforge.app.ui.journal

import android.graphics.Rect
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.TrainPilotRedCloseButton
import com.example.ui.screens.outdoorVisibleViewportPx
import com.repforge.app.ui.theme.LocalTrainPilotColors
import com.repforge.app.ui.theme.TrainPilotFrames
import com.repforge.app.ui.theme.trainPilotSoftGlow

/** Bound header, scrolling form and footer to the actual visible Android window, including IME. */
@Composable
internal fun JournalEditorFrame(title: String, saving: Boolean, onCancel: () -> Unit,
    content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalTrainPilotColors.current
    val density = LocalDensity.current
    val view = LocalView.current
    var bounds by remember(view) { mutableStateOf<List<Int>?>(null) }
    // The visible frame already excludes a resized keyboard. Subtracting IME height again
    // would collapse the form/footer on short windows.
    val bottomInset = WindowInsets.navigationBars.getBottom(density)
    BoxWithConstraints(Modifier.fillMaxSize().onGloballyPositioned { coordinates ->
        val visible = Rect()
        val location = IntArray(2)
        view.getWindowVisibleDisplayFrame(visible)
        view.getLocationOnScreen(location)
        val next = listOf(coordinates.size.height, location[1], visible.top, visible.bottom)
        if (bounds != next) bounds = next
    }.testTag("journal_editor_safe_host")) {
        val viewport = bounds?.let {
            outdoorVisibleViewportPx(it[0], it[1], it[2], it[3], bottomInset, with(density) { 12.dp.roundToPx() })
        }
        val top = with(density) { (viewport?.topInset ?: 0).toDp() }
        val height = viewport?.let { with(density) { it.height.toDp() }.coerceAtMost(maxHeight) } ?: maxHeight
        val shape = RoundedCornerShape(20.dp)
        Surface(color = colors.card, shape = shape,
            modifier = Modifier.align(Alignment.TopCenter).offset(y = top).height(height)
                .padding(horizontal = 8.dp, vertical = 6.dp).widthIn(max = 620.dp).fillMaxWidth()
                .trainPilotSoftGlow(colors.accent, shape)
                .border(TrainPilotFrames.page(colors.line, colors.accent), shape)
                .testTag("journal_editor_visible_viewport")) {
            Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(
                colors.accent.copy(alpha = 0.06f), colors.card, colors.card2))).padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    HorizontalDivider(Modifier.width(32.dp).height(4.dp), color = colors.muted.copy(alpha = 0.5f))
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    TrainPilotRedCloseButton(onClick = { if (!saving) onCancel() },
                        modifier = Modifier.testTag("journal_editor_cancel"))
                }
                content()
            }
        }
    }
}

/** Softer palette-derived boxes inside the editor's single page frame. */
@Composable
internal fun JournalEditorPanel(content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalTrainPilotColors.current
    Surface(color = colors.card2, shape = RoundedCornerShape(12.dp),
        border = TrainPilotFrames.stroke(TrainPilotFrames.panelWidth, colors.line)) {
        Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(
            colors.accent.copy(alpha = 0.05f), colors.card2))).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
    }
}
