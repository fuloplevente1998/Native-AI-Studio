package com.example.followup.watchinfo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.model.watchinfo.WatchInfoCardData
import com.example.model.watchinfo.WatchInfoStatus
import com.example.ui.components.watchinfo.StudioWatchInformationCard
import com.example.ui.theme.TrainPilotTheme
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h960dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class WatchInfoCardRegressionTest {
    @get:Rule val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private fun verifiedReply() = WatchInfoCardData.fromVerifiedReply(
        nodeName = "Teszt Wear node",
        reply = JSONObject().put("model", "SM-RTEST")
            .put("os", "16").put("appVersion", "0.1.0-beta")
            .put("appBuild", 2743).put("battery", 75),
        status = "Óraadatok frissítve.",
        lastSync = "12:34:56"
    )

    @Test fun twoFramedPanelsDisplayIndependentRealReplyFields() {
        compose.setContent {
            TrainPilotTheme { StudioWatchInformationCard(verifiedReply(), onRefresh = {}) }
        }
        compose.onNodeWithTag("watch_device_info_card", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("watch_info_device_panel", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("watch_info_system_panel", useUnmergedTree = true).assertExists()
        val device = compose.onNodeWithTag("watch_info_device_panel", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val system = compose.onNodeWithTag("watch_info_system_panel", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertEquals(device.top, system.top, 1f)
        org.junit.Assert.assertTrue(device.right <= system.left)
        for (value in listOf("Teszt Wear node", "SM-RTEST", "75%", "16", "0.1.0-beta", "2743")) {
            compose.onNodeWithText(value, useUnmergedTree = true).assertExists()
        }
        compose.onNodeWithTag("watch_device_info_status", useUnmergedTree = true).assertExists()
    }

    @Test fun missingBatteryAndOptionalBuildStayUnknownNotZero() {
        val missing = WatchInfoCardData.fromVerifiedReply(
            nodeName = " ",
            reply = JSONObject().put("model", JSONObject.NULL)
                .put("os", "").put("battery", JSONObject.NULL)
        )
        assertNull(missing.name)
        assertNull(missing.model)
        assertNull(missing.androidVersion)
        assertNull(missing.batteryPercent)
        assertNull(missing.appVersion)
        assertNull(missing.appBuild)
        for (invalid in listOf(-1, 101, 37.5, "88")) {
            assertNull(WatchInfoCardData.fromVerifiedReply("Wear",
                JSONObject().put("battery", invalid)).batteryPercent)
        }
        compose.setContent { TrainPilotTheme { StudioWatchInformationCard(missing, {}) } }
        compose.onNodeWithText("0%", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("null", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test fun syncIconFiresOnceAndBusyDisablesIconAndCard() {
        var data by mutableStateOf(verifiedReply())
        var callbacks = 0
        compose.setContent { TrainPilotTheme {
            StudioWatchInformationCard(data, onRefresh = { callbacks++ })
        } }
        compose.onNodeWithTag("watch_refresh_device_info", useUnmergedTree = true).performClick()
        assertEquals(1, callbacks)
        // Busy deliberately contains an infinite progress animation. Assert semantics
        // at a controlled frame instead of asking Espresso to finish that animation.
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { data = data.copy(loading = true, status = "Óraadatok lekérése…") }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("watch_refresh_device_info", useUnmergedTree = true).assertIsNotEnabled()
        compose.onNodeWithTag("watch_device_info_card", useUnmergedTree = true).assertIsNotEnabled()
        compose.onNodeWithTag("watch_device_info_status", useUnmergedTree = true).assertExists()
        assertEquals(1, callbacks)
    }

    @Test fun wholeCardRetainsRefreshAndTimeoutIsShownInsideIt() {
        var callbacks = 0
        compose.setContent { TrainPilotTheme {
            StudioWatchInformationCard(
                data = verifiedReply().copy(status = "Az óra nem válaszolt.",
                    statusKind = WatchInfoStatus.TIMEOUT, loading = false),
                onRefresh = { callbacks++ }
            )
        } }
        compose.onNodeWithTag("watch_device_info_status", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Az óra nem válaszolt.", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("watch_device_info_card", useUnmergedTree = true).performClick()
        assertEquals(1, callbacks)
    }

    @Test fun invalidBuildAndLiteralNullStringsAreNotPresentedAsMeasurements() {
        val response = JSONObject().put("model", "null")
            .put("appVersion", JSONObject.NULL).put("appBuild", "2743")
        val parsed = WatchInfoCardData.fromVerifiedReply("Test", response)
        assertNull(parsed.model)
        assertNull(parsed.appVersion)
        assertNull(parsed.appBuild)
    }
}
