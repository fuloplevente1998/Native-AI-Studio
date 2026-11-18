package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
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

/** True inside [TrainPilotScreenSheet]: hosted screens wrap their height instead of filling it. */
val LocalTrainPilotInSheet = androidx.compose.runtime.staticCompositionLocalOf { false }

/** fillMaxSize on a page; fillMaxWidth inside a sheet so the sheet opens only as high as its content. */
@Composable
fun sheetAwareFillMaxSize(): Modifier =
    if (LocalTrainPilotInSheet.current) Modifier.fillMaxWidth() else Modifier.fillMaxSize()

/**
 * Bottom sheet that hosts an existing full screen 1:1 (same content and controls), like the
 * Quick workout sheet. It opens only as high as its content (short screens such as Backup stay
 * about half height, long ones are capped and scroll), uses the page background so no extra band
 * appears, and has a slim drag handle. X, system back and swipe down close it; the screen
 * underneath keeps its scroll position and state.
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
        modifier = Modifier.trainPilotSheetTopSafeArea().testTag(testTag),
        containerColor = palette.background,
        contentColor = palette.textPrimary,
        dragHandle = {
            androidx.compose.foundation.layout.Box(
                Modifier
                    .padding(top = 6.dp, bottom = 2.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .background(palette.textSecondary.copy(alpha = 0.45f), RoundedCornerShape(2.dp))
                    .testTag("${testTag}_handle")
            )
        }
    ) {
        androidx.compose.runtime.CompositionLocalProvider(LocalTrainPilotInSheet provides true) {
            androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth()) { content() }
        }
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
