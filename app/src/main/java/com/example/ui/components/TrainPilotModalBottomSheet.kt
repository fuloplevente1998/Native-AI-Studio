package com.example.ui.components
import com.example.ui.theme.GlassContrast
import com.example.ui.theme.GlassMaterialRole
import com.example.ui.theme.GlassOptics
import com.example.ui.theme.KyantMaterialSpec
import com.example.ui.theme.LocalGlassScene
import com.example.ui.theme.drawGlassSceneCompose
import com.example.ui.theme.freezeSheetScene
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
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
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
    @Suppress("UNUSED_PARAMETER") staticGlass: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = BottomSheetDefaults.ExpandedShape
    val palette = LocalTrainPilotPalette.current
    // Paint over a known gradient inside the sheet, not the unknown content/scrim behind it.
    val customPaint = palette.customCardPaint != null && containerColor == palette.surface
    val glass = sheetUsesGlass(palette, containerColor)
    val paintFrozenScene = glass && KyantGlass.active(palette) && LocalGlassOpticsEnabled.current
    val parentScene = LocalGlassScene.current
    val frozenScene = if (paintFrozenScene) remember(parentScene) { freezeSheetScene(parentScene) } else null
    if (glass && parentScene != null) {
        DisposableEffect(parentScene) {
            parentScene.sheetOpened()
            onDispose { parentScene.sheetClosed() }
        }
    }
    val scope = rememberCoroutineScope()
    val fill = if (glass) palette.glassOverlayFill(containerColor) else if (customPaint) palette.background else containerColor
    // Material owns measurement, anchors, drag and settling; glass is only static paint.
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = shape,
        containerColor = if (paintFrozenScene) Color.Transparent else fill,
        contentColor = contentColor,
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        val backdrop = if (frozenScene == null) Modifier else {
            val tint = GlassOptics.TINT.copy(alpha =
                KyantMaterialSpec.forRole(GlassMaterialRole.Overlay, palette.glassClarity).tintAlpha)
            val backdropCap = GlassContrast.backdropCap(tint.alpha)
            Modifier.clip(shape).drawBehind {
                drawGlassSceneCompose(palette, frozenScene, Offset.Zero,
                    size.width.coerceAtLeast(1f), size.height.coerceAtLeast(1f), backdropCap)
                drawRect(tint)
            }
        }
        CompositionLocalProvider(
            LocalGlassOverlay provides glass,
            LocalTrainPilotSheetMotion provides true
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .then(if (customPaint && !palette.isLiquidGlass) Modifier.background(palette.pageBrush).background(palette.cardPaint) else Modifier)
                    .then(backdrop)
                    .drawWithCache {
                        val path = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache)) }
                        val color = outlineColor.copy(alpha = outlineColor.alpha * 0.22f)
                        val framed = !com.repforge.app.ui.theme.liquidGlassDropsFrame(palette, outlineColor)
                        val stroke = Stroke(width = TrainPilotFrames.pageWidth.toPx() * 2f)
                        onDrawWithContent {
                            drawContent()
                            if (glass || framed) clipPath(path) {
                                drawPath(path, if (glass) palette.cardBorder.copy(alpha = .40f) else color, style = stroke)
                            }
                        }
                    }
                    .windowInsetsPadding(contentWindowInsets().only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            ) {
                // A fixed handle inside the paint surface needs no separate size observer.
                if (dragHandle != null) {
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
    }
}
