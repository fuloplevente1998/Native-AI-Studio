package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

/**
 * Form/detail sheet with the same slots as an AlertDialog (title, text, confirm, dismiss), so an
 * edit dialog can become a bottom sheet 1:1. It opens only as high as its content, scrolls when
 * long, stays above the keyboard and keeps the main button at the bottom within thumb reach.
 * Confirmations (delete, discard, unsaved changes) stay centred AlertDialogs on purpose.
 */
@Composable
fun TrainPilotFormSheet(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    @Suppress("UNUSED_PARAMETER") containerColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
    @Suppress("UNUSED_PARAMETER") titleContentColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
    @Suppress("UNUSED_PARAMETER") textContentColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
    @Suppress("UNUSED_PARAMETER") shape: androidx.compose.ui.graphics.Shape? = null,
    @Suppress("UNUSED_PARAMETER") properties: androidx.compose.ui.window.DialogProperties? = null,
    testTag: String = "trainpilot_form_sheet",
    asSheet: Boolean = true,
    /** False when [text] already scrolls itself (nested vertical scrolling is not allowed). */
    scrollText: Boolean = true
) {
    if (!asSheet) {
        TrainPilotAlertDialog(onDismissRequest = onDismissRequest, confirmButton = confirmButton,
            modifier = modifier, dismissButton = dismissButton, title = title, text = text)
        return
    }
    val palette = LocalTrainPilotPalette.current
    var generation by remember { mutableIntStateOf(0) }
    key(generation) {
        TrainPilotScreenSheet(
            onDismiss = { generation++; onDismissRequest() },
            testTag = testTag
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(start = 18.dp, end = 18.dp, bottom = 16.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
            ) {
                if (title != null) {
                    androidx.compose.runtime.CompositionLocalProvider(
                        androidx.compose.material3.LocalContentColor provides palette.textPrimary,
                        androidx.compose.material3.LocalTextStyle provides androidx.compose.material3.MaterialTheme.typography.titleMedium
                    ) { title() }
                }
                if (text != null) {
                    androidx.compose.foundation.layout.Box(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .then(if (scrollText) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                    ) {
                        androidx.compose.runtime.CompositionLocalProvider(
                            androidx.compose.material3.LocalContentColor provides palette.textSecondary
                        ) { text() }
                    }
                }
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp, androidx.compose.ui.Alignment.End),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    dismissButton?.invoke()
                    confirmButton()
                }
            }
        }
    }
}
