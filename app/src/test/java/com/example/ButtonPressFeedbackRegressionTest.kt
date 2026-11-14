package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.ui.theme.TrainPilotTheme
import com.repforge.app.ui.theme.TrainPilotButton
import com.repforge.app.ui.theme.TrainPilotTextButton
import androidx.compose.ui.focus.focusRequester
import com.repforge.app.ui.theme.TrainPilotOutlinedButton
import com.repforge.app.ui.theme.TrainPilotSaveButton
import com.repforge.app.ui.theme.TrainPilotNegativeButton
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import kotlin.math.roundToInt

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
@OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
class ButtonPressFeedbackRegressionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun aPressAndReleaseInOneFrameStillProduceVisibleWholeButtonFeedback() {
        val source = androidx.compose.foundation.interaction.MutableInteractionSource()
        var host: View? = null
        var clicks = 0
        compose.setContent { TrainPilotTheme {
            val view = LocalView.current
            SideEffect { host = view }
            Column(Modifier.padding(16.dp)) {
                TrainPilotButton(onClick = { clicks++ }, interactionSource = source,
                    modifier = Modifier.width(280.dp).height(48.dp).testTag("short_press")) { Text("") }
            }
        } }
        fun pixels() = compose.runOnIdle {
            val view = requireNotNull(host)
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            bitmap.asImageBitmap().toPixelMap()
        }
        val bounds = compose.onNodeWithTag("short_press").fetchSemanticsNode().boundsInRoot
        val before = pixels()
        compose.mainClock.autoAdvance = false
        try {
            compose.runOnIdle {
                val press = androidx.compose.foundation.interaction.PressInteraction.Press(bounds.center)
                assertTrue(source.tryEmit(press))
                assertTrue(source.tryEmit(androidx.compose.foundation.interaction.PressInteraction.Release(press)))
            }
            compose.mainClock.advanceTimeBy(96)
            val visible = pixels()
            val y = bounds.center.y.roundToInt()
            for (x in listOf((bounds.left + 12).roundToInt(), (bounds.right - 12).roundToInt()))
                assertNotEquals("A coalesced short tap must still light the button ends", before[x, y].toArgb(), visible[x, y].toArgb())
            compose.mainClock.advanceTimeBy(400)
            val after = pixels()
            val x = bounds.center.x.roundToInt()
            assertEquals(before[x, y].toArgb(), after[x, y].toArgb())
            compose.runOnIdle { assertEquals(0, clicks) }
        } finally { compose.mainClock.autoAdvance = true }
    }

    @Test fun textButtonsRetainVisibleKeyboardFocusAndNativeKeyboardActivation() {
        val requester = androidx.compose.ui.focus.FocusRequester()
        var focusManager: androidx.compose.ui.focus.FocusManager? = null
        var host: View? = null
        var clicks = 0
        compose.setContent { TrainPilotTheme {
            val view = LocalView.current
            val manager = androidx.compose.ui.platform.LocalFocusManager.current
            SideEffect { host = view; focusManager = manager }
            Column(Modifier.background(Color.Black).padding(16.dp)) {
                TrainPilotTextButton(onClick = { clicks++ },
                    modifier = Modifier.width(280.dp).height(48.dp)
                        .then(Modifier.focusRequester(requester)).testTag("focused_text")) { Text("Continue") }
            }
        } }
        fun pixels() = compose.runOnIdle {
            val view = requireNotNull(host)
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            bitmap.asImageBitmap().toPixelMap()
        }
        val node = compose.onNodeWithTag("focused_text")
        val bounds = node.fetchSemanticsNode().boundsInRoot
        val before = pixels()
        compose.runOnIdle { requester.requestFocus() }
        node.assertIsFocused()
        val focused = pixels()
        val x = (bounds.left + 12).roundToInt()
        val y = bounds.center.y.roundToInt()
        assertNotEquals(before[x, y].toArgb(), focused[x, y].toArgb())
        node.performKeyInput {
            keyDown(androidx.compose.ui.input.key.Key.Enter)
            keyUp(androidx.compose.ui.input.key.Key.Enter)
        }
        compose.runOnIdle { assertEquals(1, clicks); requireNotNull(focusManager).clearFocus(force = true) }
        val after = pixels()
        assertEquals(before[x, y].toArgb(), after[x, y].toArgb())
    }

    @Test fun disabledButtonDoesNotInvokeItsAction() {
        var clicks = 0
        compose.setContent { TrainPilotTheme {
            TrainPilotSaveButton(onClick = { clicks++ }, enabled = false, modifier = Modifier.testTag("disabled")) { Text("Save") }
        } }
        compose.onNodeWithTag("disabled").assertIsNotEnabled().performTouchInput { click() }
        compose.runOnIdle { assertEquals(0, clicks) }
    }

    @Test fun filledButtonsKeepTheirPaintedHeightAndCoverWideSurfaces() {
        var clicks = 0
        var view: View? = null
        val base = Color(0xFF34415A)
        compose.setContent { TrainPilotTheme {
            val host = LocalView.current
            SideEffect { view = host }
            Column(Modifier.background(Color.White).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TrainPilotButton(onClick = { clicks++ }, modifier = Modifier.width(280.dp).testTag("default"), colors = ButtonDefaults.buttonColors(containerColor = base)) { Text("") }
                TrainPilotButton(onClick = { clicks++ }, modifier = Modifier.width(280.dp).height(56.dp).testTag("56"), colors = ButtonDefaults.buttonColors(containerColor = base)) { Text("") }
                TrainPilotButton(onClick = { clicks++ }, modifier = Modifier.width(280.dp).height(72.dp).testTag("72"), colors = ButtonDefaults.buttonColors(containerColor = base)) { Text("") }
            }
        } }
        fun pixels() = compose.runOnIdle {
            val host = requireNotNull(view)
            val bitmap = Bitmap.createBitmap(host.width, host.height, Bitmap.Config.ARGB_8888)
            host.draw(Canvas(bitmap))
            bitmap.asImageBitmap().toPixelMap()
        }
        listOf("default", "56", "72").forEachIndexed { index, tag ->
            val node = compose.onNodeWithTag(tag)
            val bounds = node.fetchSemanticsNode().boundsInRoot
            val before = pixels()
            val cx = bounds.center.x.roundToInt()
            val start = bounds.top.roundToInt()
            val end = bounds.bottom.roundToInt()
            val paintedRows = (start until end).filter { before[cx, it].toArgb() == base.toArgb() }
            assertTrue("$tag must have a painted surface", paintedRows.isNotEmpty())
            if (tag == "default") assertTrue("default visual should remain shorter than its 48 dp target", paintedRows.size < end - start)
            node.performTouchInput { down(center) }
            val held = pixels()
            val midY = paintedRows[paintedRows.size / 2]
            assertNotEquals(before[(bounds.left + 20).roundToInt(), midY].toArgb(), held[(bounds.left + 20).roundToInt(), midY].toArgb())
            assertNotEquals(before[(bounds.right - 20).roundToInt(), midY].toArgb(), held[(bounds.right - 20).roundToInt(), midY].toArgb())
            for (y in start until paintedRows.first()) assertEquals("$tag painted above its visual", before[cx, y].toArgb(), held[cx, y].toArgb())
            for (y in paintedRows.last() + 1 until end) assertEquals("$tag painted below its visual", before[cx, y].toArgb(), held[cx, y].toArgb())
            if (index == 0) node.performTouchInput { cancel() } else node.performTouchInput { up() }
            val after = pixels()
            assertEquals(before[cx, midY].toArgb(), after[cx, midY].toArgb())
        }
        compose.runOnIdle { assertEquals(2, clicks) }
    }

    @Test fun wideButtonsShowHeldFeedbackAcrossTheRoundedVisualAndClearOnReleaseOrCancel() {
        var clicks = 0
        var view: View? = null
        compose.setContent { TrainPilotTheme {
            val host = LocalView.current
            SideEffect { view = host }
            Column(Modifier.background(Color.White).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TrainPilotOutlinedButton(onClick = { clicks++ }, modifier = Modifier.width(280.dp).height(48.dp).testTag("outlined"), shape = RoundedCornerShape(4.dp)) { Text("Outlined") }
                TrainPilotSaveButton(onClick = { clicks++ }, modifier = Modifier.width(280.dp).testTag("save"), shape = RoundedCornerShape(24.dp)) { Text("Save") }
                TrainPilotNegativeButton(onClick = { clicks++ }, modifier = Modifier.width(280.dp).testTag("negative"), shape = RoundedCornerShape(24.dp)) { Text("Delete") }
            }
        } }
        fun pixels() = compose.runOnIdle {
            val host = requireNotNull(view)
            val bitmap = Bitmap.createBitmap(host.width, host.height, Bitmap.Config.ARGB_8888)
            host.draw(Canvas(bitmap))
            bitmap.asImageBitmap().toPixelMap()
        }
        listOf("outlined", "save", "negative").forEachIndexed { index, tag ->
            val node = compose.onNodeWithTag(tag)
            val bounds = node.fetchSemanticsNode().boundsInRoot
            assertTrue(bounds.width >= 280f && bounds.height >= 48f)
            val before = pixels()
            val y = bounds.center.y.roundToInt()
            val left = (bounds.left + 12).roundToInt()
            val right = (bounds.right - 12).roundToInt()
            val cornerX = (bounds.left + 1).roundToInt()
            val cornerY = (bounds.top + 1).roundToInt()
            node.performTouchInput { down(center) }
            val held = pixels()
            assertNotEquals("$tag left side must react", before[left, y].toArgb(), held[left, y].toArgb())
            assertNotEquals("$tag right side must react", before[right, y].toArgb(), held[right, y].toArgb())
            if (index > 0) assertEquals("$tag feedback must follow the round corner", before[cornerX, cornerY].toArgb(), held[cornerX, cornerY].toArgb())
            if (index == 1) node.performTouchInput { cancel() } else node.performTouchInput { up() }
            val after = pixels()
            assertEquals(before[left, y].toArgb(), after[left, y].toArgb())
            assertEquals(before[right, y].toArgb(), after[right, y].toArgb())
        }
        compose.runOnIdle { assertEquals(2, clicks) }
    }
}
