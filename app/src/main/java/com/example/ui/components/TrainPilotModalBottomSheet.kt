package com.example.ui.components
import com.example.ui.theme.GlassContrast
import com.example.ui.theme.GlassMaterialRole
import com.example.ui.theme.GlassOptics
import com.example.ui.theme.KyantMaterialSpec
import com.example.ui.theme.KyantWindowOverlay
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
import kotlinx.coroutines.flow.first
import com.example.ui.theme.glassOverlayFill
import com.example.ui.theme.KyantGlass
import com.example.ui.theme.LocalGlassOpticsEnabled
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.layout
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import com.repforge.app.ui.theme.TrainPilotFrames

/** Pin the last measured body height during a pointer gesture or Material's settling animation.
 * Real window constraint changes (rotation, IME) still clamp the pinned value. */
internal class SheetHeightLock {
    private var lastHeight: Int? = null
    private var pinnedHeight: Int? = null
    var pointerHeld = false
    val hasPin: Boolean get() = pinnedHeight != null

    fun heightFor(held: Boolean, min: Int, max: Int): Int? {
        if (!held) {
            pinnedHeight = null
            return null
        }
        val height = pinnedHeight ?: lastHeight?.also { pinnedHeight = it }
        return height?.coerceIn(min, max)
    }

    fun measured(height: Int) { lastHeight = height }
}

/** Observes the inner body/handle only. A pointer event must never invalidate sheet measurement. */
private fun Modifier.observeSheetGesture(lock: SheetHeightLock, onReleased: () -> Unit): Modifier =
    pointerInput(lock) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            lock.pointerHeld = true
            try {
                do {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                } while (event.changes.any { it.pressed })
            } finally {
                lock.pointerHeld = false
                if (lock.hasPin) onReleased()
            }
        }
    }

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
    staticGlass: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = BottomSheetDefaults.ExpandedShape
    val palette = LocalTrainPilotPalette.current
    // Paint over a known gradient inside the sheet, not the unknown content/scrim behind it.
    val customPaint = palette.customCardPaint != null && containerColor == palette.surface
    val glass = sheetUsesGlass(palette, containerColor)
    val glassSupported = glass && KyantGlass.active(palette) && LocalGlassOpticsEnabled.current
    val optical = glassSupported && !staticGlass
    val paintFrozenScene = glassSupported && staticGlass
    val parentScene = LocalGlassScene.current
    val frozenScene = if (paintFrozenScene) remember(parentScene) { freezeSheetScene(parentScene) } else null
    val scope = rememberCoroutineScope()
    val fill = if (glass) palette.glassOverlayFill(containerColor) else if (customPaint) palette.background else containerColor
    var handleHeight by remember { mutableIntStateOf(0) }
    val heightLock = remember(sheetState) { SheetHeightLock() }
    var releaseGeneration by remember(sheetState) { mutableIntStateOf(0) }
    val releasePin: () -> Unit = {
        scope.launch {
            // Let Material's drag settle start before invalidating the pinned layout.
            withFrameNanos { }
            snapshotFlow { sheetState.isAnimationRunning }.first { !it }
            if (!heightLock.pointerHeld && heightLock.hasPin) releaseGeneration++
        }
    }
    LaunchedEffect(sheetState) {
        snapshotFlow { sheetState.isAnimationRunning }.collect { running ->
            if (!running && !heightLock.pointerHeld && heightLock.hasPin) releaseGeneration++
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        // Keep Material's original sheet-level pointer and nested-scroll chain intact.
        modifier = modifier,
        sheetState = sheetState,
        shape = shape,
        containerColor = if (optical || paintFrozenScene) Color.Transparent else fill,
        contentColor = contentColor,
        dragHandle = if (glass || dragHandle == null) null else {
            { Box(Modifier.observeSheetGesture(heightLock, releasePin).onSizeChanged { handleHeight = it.height }) { dragHandle() } }
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
                    .observeSheetGesture(heightLock, releasePin)
                    .clipToBounds()
                    .layout { measurable, constraints ->
                        // Only react to genuine content/constraint changes; drag progress must not
                        // itself invalidate measurement and restart Material's anchored gesture.
                        releaseGeneration
                        val moving = Snapshot.withoutReadObservation { sheetState.isAnimationRunning }
                        val pinned = heightLock.heightFor(heightLock.pointerHeld || moving,
                            constraints.minHeight, constraints.maxHeight)
                        val measured = measurable.measure(constraints)
                        heightLock.measured(measured.height)
                        layout(measured.width, pinned ?: measured.height) { measured.placeRelative(0, 0) }
                    }
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
                    // Top safety belongs to the sheet host; offset-dependent top consumption
                    // must not feed back into the body height while dragging.
                    .windowInsetsPadding(contentWindowInsets().only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
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
            KyantWindowOverlay(palette, shape, staticSheet = true) { backdrop -> panel(backdrop) }
        } else {
            val staticBackdrop = if (frozenScene == null) Modifier else {
                val tint = GlassOptics.TINT.copy(alpha =
                    KyantMaterialSpec.forRole(GlassMaterialRole.Overlay, palette.glassClarity).tintAlpha)
                val backdropCap = GlassContrast.backdropCap(tint.alpha)
                Modifier.clip(shape).drawBehind {
                    // The same guarded scene as the Kyant window, painted directly while the
                    // sheet moves. No GraphicsLayer recording or first-use lens shader.
                    drawGlassSceneCompose(palette, frozenScene, Offset.Zero,
                        size.width.coerceAtLeast(1f), size.height.coerceAtLeast(1f), backdropCap)
                    drawRect(tint)
                }
            }
            CompositionLocalProvider(LocalGlassOverlay provides glass) { panel(staticBackdrop) }
        }
    }
}
