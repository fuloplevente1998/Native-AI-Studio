package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.example.ui.theme.TrainPilotPalettes
import com.example.ui.theme.TrainPilotTheme
import com.repforge.app.ui.theme.TrainPilotButton
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
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
class ButtonGlowContourTest {
    @get:Rule val compose = createComposeRule()

    @Test fun filledButtonGlowHugsPaintedPillAndLeaves48DpTouchTarget() {
        var paletteId by mutableIntStateOf(23)
        var closes = 0
        var host: View? = null
        compose.setContent {
            val palette = TrainPilotPalettes.getById(paletteId)
            TrainPilotTheme(palette = palette) {
                val view = LocalView.current
                SideEffect { host = view }
                Box(Modifier.background(palette.background).padding(20.dp)) {
                    TrainPilotButton(
                        onClick = { closes++ },
                        modifier = Modifier.testTag("glow_close"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.primary,
                            contentColor = palette.onPrimary
                        )
                    ) { Text("Bezárás") }
                }
            }
        }
        // View.draw is deterministic with Robolectric's paused looper; PixelCopy is not.
        fun pixels() = compose.runOnIdle {
            val view = requireNotNull(host)
            Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also {
                view.draw(Canvas(it))
            }.asImageBitmap().toPixelMap()
        }

        for (id in listOf(23, 18, 1)) {
            compose.runOnIdle { paletteId = id }
            val button = compose.onNodeWithTag("glow_close")
            button.assertHeightIsEqualTo(48.dp)
            val bounds = button.fetchSemanticsNode().boundsInRoot
            val x = bounds.center.x.roundToInt()
            val top = bounds.top.roundToInt()
            val insideX = (bounds.left + 12f).roundToInt()
            val insideY = bounds.center.y.roundToInt()
            val before = pixels()
            val nearPaint = before[x, top + 3]
            val fartherAway = before[x, top]
            val palette = TrainPilotPalettes.getById(id)
            assertEquals("The visible fill must keep its theme color",
                palette.primary.toArgb(), before[x, top + 6].toArgb())
            // 40 dp painted height centered in the 48 dp reserved slot: the glow belongs
            // next to the painted edge at 4 dp, not the slot's outer edge.
            if (palette.hasGlow) {
                assertTrue("Glow $id must hug the painted outline, not the hit box",
                    brightness(nearPaint) > brightness(fartherAway) + 0.005f)
            } else {
                assertTrue("Matte palette must not draw a glow",
                    kotlin.math.abs(brightness(nearPaint) - brightness(fartherAway)) < 0.005f)
            }
            button.performTouchInput { down(center) }
            val held = pixels()
            assertNotEquals("Press feedback must remain visible on the actual surface",
                before[insideX, insideY].toArgb(), held[insideX, insideY].toArgb())
            assertEquals("Press must not change the static outer glow",
                before[x, top + 3].toArgb(), held[x, top + 3].toArgb())
            button.performTouchInput { up() }
            // Even the transparent top of the 48 dp accessible slot must click.
            button.performTouchInput { click(Offset(centerX, 1f)) }
        }
        assertEquals(6, closes)
    }

    private fun brightness(color: Color): Float = color.luminance() * color.alpha
}
