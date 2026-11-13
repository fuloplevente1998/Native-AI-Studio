package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.ui.components.trainPilotSheetTopSafeArea
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h900dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class SheetTopSafeAreaRegressionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun cutoutWinsOverStatusBarEvenWhenAnAncestorConsumesThoseInsets() {
        compose.setContent {
            val status = WindowInsets(top = 24.dp)
            val cutout = WindowInsets(top = 56.dp)
            Box(Modifier.size(320.dp, 600.dp).consumeWindowInsets(status.union(cutout)).testTag("window")) {
                Box(Modifier.trainPilotSheetTopSafeArea(status, cutout).fillMaxSize().testTag("sheet"))
            }
        }
        val window = compose.onNodeWithTag("window").getUnclippedBoundsInRoot()
        val sheet = compose.onNodeWithTag("sheet").getUnclippedBoundsInRoot()
        assertEquals(56.dp, sheet.top - window.top)
        assertEquals(window.bottom, sheet.bottom)
        assertEquals(544.dp, sheet.bottom - sheet.top)
    }

    @Test fun statusBarWinsWithoutAddingTheCutoutHeightAgain() {
        compose.setContent {
            Box(Modifier.size(320.dp, 600.dp).testTag("window")) {
                Box(Modifier.trainPilotSheetTopSafeArea(WindowInsets(top = 48.dp), WindowInsets(top = 32.dp))
                    .fillMaxSize().testTag("sheet"))
            }
        }
        val window = compose.onNodeWithTag("window").getUnclippedBoundsInRoot()
        val sheet = compose.onNodeWithTag("sheet").getUnclippedBoundsInRoot()
        assertEquals(48.dp, sheet.top - window.top)
        assertEquals(window.bottom, sheet.bottom)
    }

    @Test fun changingWindowObstructionUpdatesTheSheetWithoutAConstantSpacer() {
        var top by mutableStateOf(0)
        compose.setContent {
            Box(Modifier.size(320.dp, 600.dp).testTag("window")) {
                Box(Modifier.trainPilotSheetTopSafeArea(WindowInsets(top = top.dp), WindowInsets(0, 0, 0, 0))
                    .fillMaxSize().testTag("sheet"))
            }
        }
        val window = compose.onNodeWithTag("window").getUnclippedBoundsInRoot()
        assertEquals(window.top, compose.onNodeWithTag("sheet").getUnclippedBoundsInRoot().top)
        compose.runOnIdle { top = 64 }
        val sheet = compose.onNodeWithTag("sheet").getUnclippedBoundsInRoot()
        assertEquals(64.dp, sheet.top - window.top)
        assertEquals(window.bottom, sheet.bottom)
    }
}
