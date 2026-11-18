package com.example

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.ui.components.TrainPilotRouteSheet
import com.example.ui.theme.TrainPilotTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Device bug after 2768-r3: journal detail sheet -> Source -> watch sheet, closing it froze the app.
 * A sheet that hid itself (swipe/back) must not stay composed as an invisible, touch-eating window.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-mdpi")
class RouteSheetDismissRegressionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun swipeClosingANestedSheetShowsTheParentSheetAgain() {
        var stack by mutableStateOf(listOf("journal_detail", "watch_sync"))
        var dismissals = 0
        compose.setContent {
            TrainPilotTheme {
                val route = stack.last()
                TrainPilotRouteSheet(
                    routeKey = route to stack.size,
                    onDismiss = { dismissals++; stack = stack.dropLast(1).ifEmpty { listOf("journal_detail") } },
                    testTag = "sheet_$route"
                ) { Text("content $route", Modifier.testTag("content_$route")) }
            }
        }
        compose.onNodeWithTag("content_watch_sync").assertIsDisplayed()
        compose.onNodeWithTag("sheet_watch_sync").performTouchInput { swipeDown() }
        compose.waitForIdle()
        assertEquals(1, dismissals)
        assertEquals(listOf("journal_detail"), stack)
        // The parent sheet is a fresh, visible instance (not the hidden one).
        compose.onNodeWithTag("content_journal_detail").assertIsDisplayed()
        compose.onNodeWithTag("content_watch_sync").assertDoesNotExist()
    }

    @Test fun refusedBackNavigationReopensTheSameSheetInsteadOfLeavingAHiddenWindow() {
        var dismissals = 0
        compose.setContent {
            TrainPilotTheme {
                TrainPilotRouteSheet(routeKey = "backup", onDismiss = { dismissals++ }, testTag = "sheet_backup") {
                    Text("content backup", Modifier.testTag("content_backup"))
                }
            }
        }
        compose.onNodeWithTag("sheet_backup").performTouchInput { swipeDown() }
        compose.waitForIdle()
        assertEquals(1, dismissals)
        compose.onNodeWithTag("content_backup").assertIsDisplayed()
    }

    @Test fun shortScreenOpensOnlyAsHighAsItsContentWithSlimHandle() {
        compose.setContent {
            TrainPilotTheme {
                com.example.ui.components.TrainPilotScreenSheet(onDismiss = {}, testTag = "short_sheet") {
                    androidx.compose.foundation.layout.Column(
                        Modifier.then(com.example.ui.components.sheetAwareFillMaxSize()).testTag("short_body")
                    ) { repeat(4) { Text("row $it") } }
                }
            }
        }
        compose.onNodeWithTag("short_body").assertIsDisplayed()
        val body = compose.onNodeWithTag("short_body").getUnclippedBoundsInRoot()
        val handle = compose.onNodeWithTag("short_sheet_handle", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue("Short content must not fill the screen (was ${body.bottom - body.top})", body.bottom - body.top < 300.dp)
        assertTrue("Drag handle area must stay slim", body.top - handle.top < 24.dp)
    }
}
