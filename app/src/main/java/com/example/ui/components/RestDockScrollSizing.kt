package com.example.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity

/**
 * 2808/9: rest dock size follows the user's own scroll position again (as before #347), but only the
 * user's scroll changes it. Scrolling down past the header onto the set rows opens the large card;
 * scrolling back up above the set rows returns it to compact. A layout shift or the automatic
 * scroll after ticking a set never reaches this (no user input), so it cannot open the large card.
 * The arrow and the dock drag keep toggling manually.
 */
internal object RestDockScrollSizing {
    /** Same position threshold as the pre-#347 behaviour: the header has left the top of the list. */
    const val HEADER_SCROLL_PX = 130
    const val DIRECTION_SLOP_PX = 1f

    fun scrolledToSetRows(firstVisibleItemIndex: Int, firstVisibleItemScrollOffset: Int): Boolean =
        firstVisibleItemIndex > 0 || firstVisibleItemScrollOffset > HEADER_SCROLL_PX

    /**
     * [deltaY] is the scroll delta of the user's gesture (negative = finger moves up = list scrolls
     * down). Returns the new large state; anything not driven by the user keeps the current size.
     */
    fun afterScroll(large: Boolean, userDriven: Boolean, scrolledToSetRows: Boolean, deltaY: Float): Boolean = when {
        !userDriven -> large
        deltaY < -DIRECTION_SLOP_PX && scrolledToSetRows -> true
        deltaY > DIRECTION_SLOP_PX && !scrolledToSetRows -> false
        else -> large
    }
}

/**
 * Nested-scroll hook for the measurement list. A drag ([NestedScrollSource.UserInput]) starts a user
 * gesture; the fling that follows the same drag still counts (so a quick flick back to the top also
 * returns compact). Programmatic scrolls do not pass through nested scroll at all.
 */
@Composable
internal fun rememberRestDockScrollConnection(
    listState: LazyListState,
    restVisible: Boolean,
    large: Boolean,
    onLarge: (Boolean) -> Unit,
): NestedScrollConnection {
    val currentLarge = rememberUpdatedState(large)
    val currentVisible = rememberUpdatedState(restVisible)
    val currentOnLarge = rememberUpdatedState(onLarge)
    return remember(listState) {
        object : NestedScrollConnection {
            var userGesture = false
            var lastDelta = 0f

            fun resize(delta: Float) {
                if (!currentVisible.value) return
                val next = RestDockScrollSizing.afterScroll(currentLarge.value, userGesture,
                    RestDockScrollSizing.scrolledToSetRows(listState.firstVisibleItemIndex,
                        listState.firstVisibleItemScrollOffset), delta)
                if (next != currentLarge.value) currentOnLarge.value(next)
            }

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) userGesture = true
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                val delta = consumed.y + available.y
                if (delta != 0f) lastDelta = delta
                resize(delta)
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                // The final position after the fling settles decides once more, then the gesture ends.
                resize(lastDelta)
                userGesture = false
                return Velocity.Zero
            }
        }
    }
}
