package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.ui.components.TrainPilotRedCloseButton
import com.example.ui.theme.TrainPilotTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class RoundedPressFeedbackRegressionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun pressFeedbackStaysInsideThePaintedButtonAndTheLargerHitTargetStillClicks() {
        var clicks = 0
        compose.setContent { TrainPilotTheme {
            Box(Modifier.size(100.dp).background(Color.White).testTag("canvas")) {
                TrainPilotRedCloseButton(onClick = { clicks++ }, modifier = Modifier.align(Alignment.Center), testTag = "close")
            }
        } }
        val target = compose.onNodeWithTag("close")
        val bounds = target.getUnclippedBoundsInRoot()
        assertTrue(bounds.right - bounds.left >= 48.dp && bounds.bottom - bounds.top >= 48.dp)
        val canvas = compose.onNodeWithTag("canvas")
        val canvasBounds = canvas.fetchSemanticsNode().boundsInRoot
        val painted = compose.onNodeWithTag("close_visual", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue(painted.width < target.fetchSemanticsNode().boundsInRoot.width)
        compose.mainClock.autoAdvance = false
        val before = canvas.captureToImage().toPixelMap()
        // Press the touch target beyond the visual's left edge, then hold long enough for ripple.
        target.performTouchInput { down(Offset(1f, centerY)) }
        compose.mainClock.advanceTimeBy(200)
        val pressed = canvas.captureToImage().toPixelMap()
        var changedInside = false
        for (y in 0 until before.height) for (x in 0 until before.width) {
            val screen = Offset(canvasBounds.left + x + 0.5f, canvasBounds.top + y + 0.5f)
            val outside = screen.x < painted.left - 1f || screen.x > painted.right + 1f ||
                screen.y < painted.top - 1f || screen.y > painted.bottom + 1f
            if (outside) assertEquals("Press leaked into hit-box pixel $x,$y", before[x,y].toArgb(), pressed[x,y].toArgb())
            else if (before[x,y].toArgb() != pressed[x,y].toArgb()) changedInside = true
        }
        assertTrue("The visual button must retain visible press feedback", changedInside)
        target.performTouchInput { up() }
        compose.mainClock.autoAdvance = true
        compose.runOnIdle { assertEquals(1, clicks) }
    }
}
