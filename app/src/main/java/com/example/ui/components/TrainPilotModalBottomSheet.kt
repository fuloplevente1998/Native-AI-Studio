package com.example.ui.components
import com.example.ui.theme.KyantWindowOverlay
import com.example.ui.theme.LocalGlassOverlay
import com.example.ui.theme.sheetUsesGlass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.collapse
import androidx.compose.material3.SheetValue
import kotlinx.coroutines.launch
import com.example.ui.theme.glassOverlayFill
import com.example.ui.theme.KyantGlass
import com.example.ui.theme.LocalGlassOpticsEnabled
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import com.example.ui.theme.LocalTrainPilotPalette
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
    dragHandle: @Composable (() -> Unit)? = {
        val p = LocalTrainPilotPalette.current
        if (p.isLiquidGlass) BottomSheetDefaults.DragHandle(color = p.cardBorder)
        else BottomSheetDefaults.DragHandle()
    },
    contentWindowInsets: @Composable () -> WindowInsets = { BottomSheetDefaults.windowInsets },
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = BottomSheetDefaults.ExpandedShape
    val palette = LocalTrainPilotPalette.current
    // Paint over a known gradient inside the sheet, not the unknown content/scrim behind it.
    val customPaint = palette.customCardPaint != null && containerColor == palette.surface
    val glass = sheetUsesGlass(palette, containerColor)
    val optical = glass && KyantGlass.active(palette) && LocalGlassOpticsEnabled.current
    val scope = rememberCoroutineScope()
    val fill = if (glass) palette.glassOverlayFill(containerColor) else if (customPaint) palette.background else containerColor
    var handleHeight by remember { mutableIntStateOf(0) }
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = shape,
        containerColor = if (optical) Color.Transparent else fill,
        contentColor = contentColor,
        dragHandle = if (glass || dragHandle == null) null else {
            { Box(Modifier.onSizeChanged { handleHeight = it.height }) { dragHandle() } }
        },
        // The phone sheets use bottom-only insets or handle their own. Include the same inset
        // padding in the outlined area so the stroke reaches the physical edge of the panel.
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        // Glass paints the handle and body in one surface; legacy retains Material's native handle.
        val panel: @Composable (Modifier) -> Unit = { backdrop ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (customPaint && !palette.isLiquidGlass) Modifier.background(palette.pageBrush).background(palette.cardPaint) else Modifier)
                    .then(backdrop)
                    .drawWithCache {
                        // Material moves and scales the sheet inside its caller modifier. Draw here
                        // so the stroke moves with the surface and extends around the drag handle.
                        val top = if (glass || dragHandle == null) 0f else handleHeight.toFloat()
                        val outline = shape.createOutline(Size(size.width, size.height + top), layoutDirection, this)
                        val path = Path().apply { addOutline(outline) }
                        val color = outlineColor.copy(alpha = outlineColor.alpha * 0.22f)
                        // 2809/2: Liquid Glass sheets: the glass rim, not a neutral hairline, outlines the panel.
                        val framed = !com.repforge.app.ui.theme.liquidGlassDropsFrame(palette, outlineColor)
                        val stroke = Stroke(width = TrainPilotFrames.pageWidth.toPx() * 2f)
                        onDrawWithContent {
                            drawContent()
                            if (glass) translate(top = -top) {
                                clipPath(path) { drawPath(path, palette.cardBorder.copy(alpha = .40f), style = stroke) }
                            } else if (framed) translate(top = -top) {
                                clipPath(path) { drawPath(path, color, style = stroke) }
                            }
                        }
                    }
                    .windowInsetsPadding(contentWindowInsets())
            ) {
                if (glass && dragHandle != null) {
                    Box(Modifier.fillMaxWidth().semantics {
                        dismiss {
                            scope.launch { sheetState.hide(); if (!sheetState.isVisible) onDismissRequest() }
                            true
                        }
                        if (sheetState.currentValue == SheetValue.PartiallyExpanded) {
                            expand { scope.launch { sheetState.expand() }; true }
                        } else if (sheetState.hasPartiallyExpandedState) {
                            collapse { scope.launch { sheetState.partialExpand() }; true }
                        }
                    }, contentAlignment = Alignment.Center) { dragHandle() }
                }
                content()
            }
        }
        if (optical) {
            KyantWindowOverlay(palette, shape) { backdrop -> panel(backdrop) }
        } else {
            CompositionLocalProvider(LocalGlassOverlay provides glass) { panel(Modifier) }
        }
    }
}
