package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.performClick
import com.example.ui.components.TrainPilotIconButton
import com.example.ui.components.TrainPilotIconToggleButton
import com.example.ui.components.TrainPilotMenuItem
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*
import com.repforge.app.ui.theme.trainPilotPressIndication
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
@Config(sdk = [34], qualifiers = "w480dp-h960dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class LiquidGlass433PressTest {
    @get:Rule val compose = createComposeRule()

    @Test fun nativeLiquidControlsKeepClickToggleAndDisabledSemantics() {
        var clicks = 0
        var menuClicks = 0
        var toggles = 0
        val palette = LiquidGlassPalettes.all.first()
        compose.setContent {
            TrainPilotTheme(palette = palette) {
                var checked by remember { mutableStateOf(false) }
                Column {
                    TrainPilotIconButton(onClick = { clicks++ },
                        modifier = Modifier.size(48.dp).testTag("icon_action")) { Text("A") }
                    TrainPilotIconButton(onClick = { clicks++ }, enabled = false,
                        modifier = Modifier.size(48.dp).testTag("icon_disabled")) { Text("D") }
                    TrainPilotIconToggleButton(checked, { checked = it; toggles++ },
                        Modifier.size(48.dp).testTag("icon_toggle")) { Text("T") }
                    TrainPilotMenuItem(text = { Text("Menu") }, onClick = { menuClicks++ },
                        modifier = Modifier.testTag("menu_action"))
                    TrainPilotMenuItem(text = { Text("Disabled") }, onClick = { menuClicks++ },
                        enabled = false, modifier = Modifier.testTag("menu_disabled"))
                }
            }
        }
        compose.onNodeWithTag("icon_action").assertIsEnabled().performClick()
        compose.onNodeWithTag("icon_disabled").assertIsNotEnabled()
        compose.onNodeWithTag("icon_toggle").assertIsOff().performClick()
        compose.onNodeWithTag("icon_toggle").assertIsOn()
        compose.onNodeWithTag("menu_action").assertIsEnabled().performClick()
        compose.onNodeWithTag("menu_disabled").assertIsNotEnabled()
        compose.runOnIdle {
            assertEquals(1, clicks)
            assertEquals(1, toggles)
            assertEquals(1, menuClicks)
        }
    }

    @Test fun shortWideControlPressIsLocalEvenWithOpticsDisabled() {
        val source = MutableInteractionSource()
        val p = LiquidGlassPalettes.all.first()
        val shape = RoundedCornerShape(12.dp)
        var host: View? = null
        var clicks = 0
        compose.setContent {
            TrainPilotTheme(palette = p) {
                CompositionLocalProvider(LocalGlassOpticsEnabled provides false) {
                    val view = LocalView.current
                    SideEffect { host = view }
                    Box(Modifier.background(p.background)) {
                        Box(Modifier.width(280.dp).height(48.dp).testTag("wide_local_press")
                            .trainPilotPressIndication(shape, source)
                            .clickable(interactionSource = source, indication = null) { clicks++ }
                            .background(Color(0xFF243746), shape))
                    }
                }
            }
        }
        val node = compose.onNodeWithTag("wide_local_press")
        val bounds = node.fetchSemanticsNode().boundsInRoot
        fun pixel(x: Int): Int = compose.runOnIdle {
            val view = requireNotNull(host)
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            val color = bitmap.asImageBitmap().toPixelMap()[
                bounds.left.roundToInt() + x, bounds.center.y.roundToInt()].toArgb()
            bitmap.recycle()
            color
        }
        val points = listOf(25, 140, 255)
        val rest = points.map(::pixel)
        compose.mainClock.autoAdvance = false
        for (x in points) {
            compose.runOnIdle {
                val press = PressInteraction.Press(Offset(x.toFloat(), bounds.height / 2f))
                assertTrue(source.tryEmit(press))
                assertTrue(source.tryEmit(PressInteraction.Release(press)))
            }
            compose.mainClock.advanceTimeBy(64L)
            val after = points.map(::pixel)
            for (i in points.indices) {
                if (points[i] == x) assertNotEquals("local short press $x", rest[i], after[i])
                else assertEquals("remote surface must not flash", rest[i], after[i])
            }
            compose.mainClock.advanceTimeBy(1500L)
            assertEquals(rest, points.map(::pixel))
        }
        compose.mainClock.autoAdvance = true
        node.performTouchInput { down(center); up() }
        assertEquals(1, clicks)
    }
}
