package com.example.ui.components

import com.repforge.app.ui.theme.trainPilotBorder

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.MainTab
import com.example.ui.theme.LocalTrainPilotPalette

import androidx.compose.ui.semantics.contentDescription
import com.example.model.SubScreen

/**
 * Reusable, theme-following scroll-more indicator for vertical full-page scrollable screens (#198).
 * Renders a subtle downward chevron/indicator when content extends below the visible viewport,
 * disappearing automatically when the user reaches the bottom or when all content fits.
 * Strictly non-blocking: pointer-transparent and never obscures bottom action buttons.
 */
@Composable
fun TrainPilotScrollMoreIndicator(
    visible: Boolean,
    modifier: Modifier = Modifier,
    contentDescription: String = "Görgess lejjebb a további tartalomért"
) {
    val palette = LocalTrainPilotPalette.current
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(200)),
        exit = androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(200)),
        modifier = modifier
    ) {
        Surface(
            shape = CircleShape,
            color = palette.surfaceElevated.copy(alpha = 0.88f),
            shadowElevation = 2.dp,
            modifier = Modifier
                .trainPilotBorder(1.dp, palette.primary.copy(alpha = 0.45f), CircleShape)
                .testTag("trainpilot_scroll_more_indicator")
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = contentDescription,
                    tint = palette.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun TrainPilotScrollableBox(
    scrollState: androidx.compose.foundation.ScrollState,
    modifier: Modifier = Modifier,
    indicatorBottomPadding: androidx.compose.ui.unit.Dp = 12.dp,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit
) {
    val canScrollForward by remember {
        androidx.compose.runtime.derivedStateOf {
            scrollState.maxValue > 0 && scrollState.value < (scrollState.maxValue - 8)
        }
    }
    Box(modifier = modifier) {
        content()
        TrainPilotScrollMoreIndicator(
            visible = canScrollForward,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = indicatorBottomPadding)
        )
    }
}

@Composable
fun TrainPilotScrollableListBox(
    lazyListState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier,
    indicatorBottomPadding: androidx.compose.ui.unit.Dp = 12.dp,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit
) {
    val canScrollForward by remember {
        androidx.compose.runtime.derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            val total = layoutInfo.totalItemsCount
            if (total == 0) false
            else {
                val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()
                if (lastVisible == null) false
                else (lastVisible.index < total - 1) || (lastVisible.offset + lastVisible.size > layoutInfo.viewportEndOffset)
            }
        }
    }
    Box(modifier = modifier) {
        content()
        TrainPilotScrollMoreIndicator(
            visible = canScrollForward,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = indicatorBottomPadding)
        )
    }
}

/** Screens opened from the Workout tab keep the Workout tab highlighted (library, detail, quick/active workouts…). */
internal fun isWorkoutTabSelected(currentTab: MainTab, currentSubScreen: SubScreen): Boolean =
    currentTab == MainTab.WORKOUT && currentSubScreen !in setOf(
        SubScreen.SETTINGS_MAIN, SubScreen.CALENDAR_PLANNER, SubScreen.COACH_DETAIL, SubScreen.PROGRAMS_LIST,
        SubScreen.WATCH_AND_SYNC, SubScreen.BACKUP_RESTORE, SubScreen.PERMISSIONS_SCREEN, SubScreen.PRIVATE_PHOTOS
    )
