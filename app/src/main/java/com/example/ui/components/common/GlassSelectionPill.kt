package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlassMotion
import com.example.ui.theme.GlassRole
import com.example.ui.theme.TrainPilotPalette
import com.example.ui.theme.glassHeaderMaterial
import com.example.ui.theme.rememberGlassMotionEnabled
import com.example.ui.theme.trainPilotGlassRim
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Item bounds (left, right in px, row coordinates) reported by the items under one pill. */
@Stable
internal class GlassPillTrack {
    internal val bounds = mutableStateMapOf<Int, Pair<Float, Float>>()
}

/** Reports this item's horizontal bounds to the shared pill; no effect without a track. */
internal fun Modifier.glassPillSlot(track: GlassPillTrack?, index: Int): Modifier =
    if (track == null || index < 0) this else onPlaced { coordinates ->
        val left = coordinates.positionInParent().x
        val bounds = left to left + coordinates.size.width
        if (track.bounds[index] != bounds) track.bounds[index] = bounds
    }

/**
 * One glass selection pill shared by a row of items. Its two edges ride separate springs: the
 * edge facing the travel direction is stiffer, the trailing one follows, so the pill stretches
 * and slightly squashes like a drop of liquid, then settles. Reduced motion: it jumps.
 * Must be the first child of the Box that also holds the item row (drawn behind the labels).
 */
@Composable
internal fun BoxScope.GlassSelectionPill(
    track: GlassPillTrack,
    selectedIndex: Int,
    shape: Shape,
    palette: TrainPilotPalette,
    @Suppress("UNUSED_PARAMETER") outline: Color
) {
    val target = track.bounds[selectedIndex]
    val motion = rememberGlassMotionEnabled()
    val left = remember { Animatable(0f) }
    val right = remember { Animatable(0f) }
    var placed by remember { mutableStateOf(false) }
    LaunchedEffect(target, motion) {
        val (toLeft, toRight) = target ?: return@LaunchedEffect
        if (!GlassMotion.animateTravel(motion, placed)) {
            left.snapTo(toLeft)
            right.snapTo(toRight)
            placed = true
            return@LaunchedEffect
        }
        val towardsEnd = toLeft > left.value
        launch { left.animateTo(toLeft, spring(GlassMotion.EDGE_DAMPING, GlassMotion.edgeStiffness(leading = !towardsEnd))) }
        launch { right.animateTo(toRight, spring(GlassMotion.EDGE_DAMPING, GlassMotion.edgeStiffness(leading = towardsEnd))) }
    }
    if (target == null || !placed) return
    val restWidth = target.second - target.first
    Box(
        Modifier
            .matchParentSize()
            .layout { measurable, constraints ->
                val start = left.value
                val end = maxOf(right.value, start)
                val height = constraints.maxHeight
                val placeable = measurable.measure(Constraints.fixed((end - start).roundToInt().coerceAtLeast(0), height))
                layout(constraints.maxWidth, height) { placeable.place(start.roundToInt(), 0) }
            }
            .graphicsLayer {
                scaleY = if (motion) GlassMotion.squash(right.value - left.value, restWidth) else 1f
            }
            .clip(shape)
            // 2811/G1: the Selection role (clearer lens, brighter rim) over the same recorded backdrop.
            .glassHeaderMaterial(palette, shape, GlassRole.Selection)
            // 2809/2: rim light instead of a selection hairline; the label keeps the selected foreground.
            .trainPilotGlassRim(palette, shape, GlassRole.Selection)
            .testTag("glass_selection_pill")
    )
}
