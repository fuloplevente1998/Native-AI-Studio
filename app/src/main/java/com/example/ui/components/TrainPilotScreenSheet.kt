package com.example.ui.components

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.ui.theme.LocalTrainPilotPalette

/**
 * Bottom sheet that hosts an existing full screen 1:1 (same content and controls), like the
 * Quick workout sheet: opens fully expanded from the bottom; X, system back and swipe down close
 * it, and the screen underneath keeps its scroll position and state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainPilotScreenSheet(
    onDismiss: () -> Unit,
    testTag: String,
    content: @Composable () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.trainPilotSheetTopSafeArea().fillMaxHeight().testTag(testTag),
        containerColor = palette.surface,
        contentColor = palette.textPrimary
    ) {
        androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().fillMaxHeight()) { content() }
    }
}

/**
 * Route-bound sheet. A ModalBottomSheet that hid itself (system back / swipe down) must never stay
 * composed: its invisible window would keep swallowing every touch and the app looks frozen. Each
 * route and each dismissal therefore gets a fresh sheet instance, e.g. when closing a sheet opened
 * from another sheet (journal detail -> watch & sync) or when navigation back is refused.
 */
@Composable
fun TrainPilotRouteSheet(
    routeKey: Any,
    onDismiss: () -> Unit,
    testTag: String,
    content: @Composable () -> Unit
) {
    var generation by remember { mutableIntStateOf(0) }
    key(routeKey, generation) {
        TrainPilotScreenSheet(
            onDismiss = {
                generation++
                onDismiss()
            },
            testTag = testTag,
            content = content
        )
    }
}
