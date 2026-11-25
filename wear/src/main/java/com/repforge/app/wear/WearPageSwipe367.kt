package com.repforge.app.wear

import kotlin.math.abs

/**
 * #367 phase 2 (3.7/3.8): page-swipe thresholds for the main pager and the workout controls page.
 * The values 24 dp (edge), 36 dp (commit), the platform touch slop and 500 ms (tap) are the ones
 * the code used before; the changes are that the edge guard only protects the system back
 * direction, and a short quick flick counts as well as a long slow drag.
 */
/** Rightward drags (system back / dismiss direction) may not start inside this left band. */
internal const val WEAR_SWIPE_BACK_EDGE_DP = 24f

/** Physical rim: a swipe never starts here (both sides); a tap still works. */
internal const val WEAR_SWIPE_RIM_DP = 6f

/** A slow drag changes the page after this much horizontal travel. */
internal const val WEAR_SWIPE_COMMIT_DP = 36f

/** A quick flick changes the page after this much travel at [WEAR_SWIPE_FLING_VELOCITY_DP_S]. */
internal const val WEAR_SWIPE_FLING_MIN_DP = 16f
internal const val WEAR_SWIPE_FLING_VELOCITY_DP_S = 450f

/** Horizontal travel must exceed vertical travel by this factor, so diagonal scrolls never page. */
internal const val WEAR_SWIPE_DOMINANCE = 1.5f

/** A touch shorter than this and within the slop is a tap. */
internal const val WEAR_TAP_MAX_MS = 500L

/**
 * [dx] > 0 is a rightward drag. Only that direction collides with the system back swipe, so it
 * keeps the [backEdge] band; a leftward drag may start anywhere except on the physical [rim].
 */
internal fun wearSwipeMayStart(startX: Float, width: Float, dx: Float, backEdge: Float, rim: Float): Boolean =
    if (dx > 0f) width > backEdge + rim && startX >= backEdge && startX <= width - rim
    else dx < 0f && width > 2f * rim && startX >= rim && startX <= width - rim

/**
 * Page direction to commit on release: [decided] (1 = next page / leftward, -1 = previous /
 * rightward) is fixed when the touch slop is crossed, because the start-zone rules (system back
 * band) were applied for that direction. The gesture only commits when the final travel still goes
 * the decided way, so a left start in the back band that returns rightwards never flips the page.
 * 0 = no page change.
 */
internal fun wearSwipePageDirection(decided: Int, dxDp: Float, dyDp: Float, velocityDpPerSec: Float): Int {
    if (decided != 1 && decided != -1) return 0
    if ((dxDp < 0f) != (decided == 1) || dxDp == 0f) return 0
    return if (wearSwipeCommits(dxDp, dyDp, velocityDpPerSec)) decided else 0
}

/** Decide on release (all values in dp and dp/s). The direction is the sign of [dxDp]. */
internal fun wearSwipeCommits(dxDp: Float, dyDp: Float, velocityDpPerSec: Float): Boolean {
    val distance = abs(dxDp)
    if (distance < WEAR_SWIPE_FLING_MIN_DP) return false
    if (distance < WEAR_SWIPE_DOMINANCE * abs(dyDp)) return false
    if (distance >= WEAR_SWIPE_COMMIT_DP) return true
    return velocityDpPerSec * dxDp > 0f && abs(velocityDpPerSec) >= WEAR_SWIPE_FLING_VELOCITY_DP_S
}
