package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.example.ui.theme.TrainPilotPalettes
import com.example.ui.theme.TrainPilotTheme
import com.repforge.app.ui.theme.TrainPilotButton
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w480dp-h960dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class ButtonGlowContourTest {
    @get:Rule val compose = createComposeRule()

    @Test fun filledButtonGlowFollowsVisiblePillNot48DpTouchBounds() {
        var paletteId by mutableIntStateOf(17)
        var closes = 0
        compose.setContent {
            val palette = TrainPilotPalettes.getById(paletteId)
            TrainPilotTheme(palette = palette) {
                Box(Modifier.background(palette.background)) {
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
        for (id in listOf(17, 18, 1)) {
            compose.runOnIdle { paletteId = id }
            val button = compose.onNodeWithTag("glow_close")
            // Material's visible default button is 40 dp tall. Its accessible slot stays 48 dp.
            button.assertHeightIsEqualTo(48.dp)
            val pixels = button.captureToImage().toPixelMap()
            val x = pixels.width / 2
            val nearPaint = pixels[x, 3]
            val fartherAway = pixels[x, 0]
            // In glow themes the strongest halo is adjacent to the painted edge at y=4,
            // not on the outside edge of the 48 dp touch slot at y=0.
            if (TrainPilotPalettes.getById(id).hasGlow) {
                assertTrue("Glow $id must hug the painted surface, not its hit bounds",
                    brightness(nearPaint) > brightness(fartherAway) + 0.005f)
            } else {
                assertTrue("Matte palette must not paint a glow",
                    kotlin.math.abs(brightness(nearPaint) - brightness(fartherAway)) < 0.005f)
            }
            button.performClick()
        }
        assertEquals(3, closes)
    }

    private fun brightness(color: Color): Float = color.luminance() * color.alpha
}
