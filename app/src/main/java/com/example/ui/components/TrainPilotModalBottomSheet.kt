package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.onSizeChanged
import com.repforge.app.ui.theme.TrainPilotFrames

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TrainPilotModalBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
    containerColor: Color,
    modifier: Modifier = Modifier,
    contentColor: Color = contentColorFor(containerColor),
    outlineColor: Color = contentColor,
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    contentWindowInsets: @Composable () -> WindowInsets = { BottomSheetDefaults.windowInsets },
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = BottomSheetDefaults.ExpandedShape
    var handleHeight by remember { mutableIntStateOf(0) }
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        dragHandle = if (dragHandle == null) null else {
            { Box(Modifier.onSizeChanged { handleHeight = it.height }) { dragHandle() } }
        },
        // The phone sheets use bottom-only insets or handle their own. Include the same inset
        // padding in the outlined area so the stroke reaches the physical edge of the panel.
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawWithCache {
                    // Material moves and scales the sheet inside its caller modifier. Draw here
                    // so the stroke moves with the surface and extends around the drag handle.
                    val top = if (dragHandle == null) 0f else handleHeight.toFloat()
                    val outline = shape.createOutline(Size(size.width, size.height + top), layoutDirection, this)
                    val path = Path().apply { addOutline(outline) }
                    val color = outlineColor.copy(alpha = outlineColor.alpha * 0.22f)
                    val stroke = Stroke(width = TrainPilotFrames.pageWidth.toPx() * 2f)
                    onDrawWithContent {
                        drawContent()
                        translate(top = -top) {
                            clipPath(path) { drawPath(path, color, style = stroke) }
                        }
                    }
                }
                .windowInsetsPadding(contentWindowInsets()),
            content = content
        )
    }
}
