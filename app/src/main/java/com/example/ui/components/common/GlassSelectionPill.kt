package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.translate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.example.ui.theme.GlassContrast
import com.example.ui.theme.GlassOptics
import com.example.ui.theme.LocalGlassOpticsEnabled
import com.example.ui.theme.LocalGlassPowerSave
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Constraints
import com.example.ui.theme.GlassMaterialRole
import com.example.ui.theme.GlassMotion
import com.example.ui.theme.GlassControlDrag
import com.example.ui.theme.glassDragTransform
import com.example.ui.theme.glassDragStretch
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.unit.dp
import com.example.ui.theme.TrainPilotPalette
import com.example.ui.theme.glassHeaderControl
import com.example.ui.theme.rememberGlassMotionEnabled
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Item bounds (left, right in px, row coordinates) reported by the items under one pill. */
@Stable
internal class GlassPillTrack {
    val drag = GlassControlDrag()
    // The host can own the motion state above a measured/subcomposed header.
    internal val left = Animatable(0f)
    internal val right = Animatable(0f)
    internal var placed by mutableStateOf(false)
    internal val bounds = mutableStateMapOf<Int, Pair<Float, Float>>()
}

/** Signed reflection displacement, constrained to the moving pill and zero at rest. */
internal fun pillReflection(remaining: Float, drag: Float, width: Float, enabled: Boolean): Float {
    if (!enabled || width <= 0f) return 0f
    val movement = (remaining / (width * .65f) + drag / (width * .12f)).coerceIn(-1f, 1f)
    return movement * (2f - kotlin.math.abs(movement))
}

/** Reports this item's horizontal bounds to the shared pill; no effect without a track. */
internal fun Modifier.glassPillSlot(track: GlassPillTrack?, index: Int): Modifier =
    if (track == null || index < 0) this else onPlaced { coordinates ->
        val left = coordinates.positionInParent().x
        val bounds = left to left + coordinates.size.width
        if (track.bounds[index] != bounds) track.bounds[index] = bounds
    }

/** Own the travel coroutine at the same lifetime as the retained track, not a subcomposed header. */
@Composable
internal fun GlassPillTrackMotion(track: GlassPillTrack, selectedIndex: Int) {
    val target = track.bounds[selectedIndex]
    val motion = rememberGlassMotionEnabled()
    val left = track.left
    val right = track.right
    LaunchedEffect(target, motion) {
        val (toLeft, toRight) = target ?: return@LaunchedEffect
        if (!GlassMotion.animateTravel(motion, track.placed)) {
            left.snapTo(toLeft)
            right.snapTo(toRight)
            track.placed = true
            return@LaunchedEffect
        }
        val towardsEnd = toLeft > left.value
        launch { left.animateTo(toLeft, spring(GlassMotion.EDGE_DAMPING, GlassMotion.edgeStiffness(leading = !towardsEnd))) }
        launch { right.animateTo(toRight, spring(GlassMotion.EDGE_DAMPING, GlassMotion.edgeStiffness(leading = towardsEnd))) }
    }
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
    @Suppress("UNUSED_PARAMETER") outline: Color,
    driveMotion: Boolean = true
) {
    if (driveMotion) GlassPillTrackMotion(track, selectedIndex)
    val target = track.bounds[selectedIndex]
    val motion = rememberGlassMotionEnabled()
    val dragProgress = animateFloatAsState(if (motion && track.drag.held) 1f else 0f,
        spring(dampingRatio = .62f, stiffness = 520f), label = "Liquid navigation touch")
    val left = track.left
    val right = track.right

    if (target == null || !track.placed) return
    val restWidth = target.second - target.first
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val reflect = motion && LocalGlassOpticsEnabled.current && !LocalGlassPowerSave.current &&
        lifecycle.isAtLeast(Lifecycle.State.STARTED)
    val reflection = if (reflect) Modifier.drawWithCache {
        // The brush is retained per draw size. Moving the highlight only changes its
        // position and alpha, with no additional backdrop capture or clock.
        val radius = size.height * .62f
        val tint = GlassContrast.limit(palette.primary, GlassOptics.SURFACE_LUMA_MAX - .006f)
        val light = Brush.radialGradient(listOf(tint, tint.copy(alpha = 0f)), Offset.Zero,
            radius.coerceAtLeast(1f))
        onDrawWithContent {
            drawContent()
            val remaining = (target.first + target.second - left.value - right.value) * .5f
            val drag = glassDragTransform(track.drag.offset, 4.dp.toPx(), dragProgress.value).x
            val direction = pillReflection(remaining, drag, restWidth, enabled = true)
            if (direction != 0f) {
                translate(size.width * (.5f + .32f * direction), size.height * .13f) {
                    drawCircle(light, radius, center = Offset.Zero, alpha = kotlin.math.abs(direction) * .22f)
                }
            }
        }
    } else Modifier
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
                val travel = glassDragTransform(track.drag.offset, 4.dp.toPx(), if (motion) dragProgress.value else 0f)
                translationX = travel.x
                translationY = travel.y
                scaleX = 1f + glassDragStretch(travel.x, restWidth)
                scaleY = if (motion) GlassMotion.squash(right.value - left.value, restWidth) +
                    glassDragStretch(travel.y, size.height) else 1f
            }
            .clip(shape)
            .then(reflection)
            .glassHeaderControl(palette, shape, GlassMaterialRole.Selection)
            .testTag("glass_selection_pill")
    )
}
