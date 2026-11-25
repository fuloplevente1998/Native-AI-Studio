package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-mdpi")
class GlassScrollChrome2813Test {
    @get:Rule val compose = createComposeRule()

    @Test fun firstAndLastRowsClearBothFixedBarsAndMiddleRowsUnderlap() {
        compose.setContent {
            TrainPilotTheme {
                val scene = rememberKyantScene(rememberLayerBackdrop())
                CompositionLocalProvider(LocalKyantScene provides scene, LocalGlassChromeUnderlap provides true) {
                    GlassScrollChrome(rootInset = 60.dp,
                        top = { Box(Modifier.fillMaxWidth().height(48.dp).testTag("chrome_top")) },
                        bottom = { Box(Modifier.fillMaxWidth().height(72.dp).testTag("chrome_bottom")) }
                    ) { modifier, top, bottom ->
                        LazyColumn(modifier.testTag("chrome_list"), contentPadding = PaddingValues(top = top, bottom = bottom)) {
                            items(40) { index ->
                                Box(Modifier.fillMaxWidth().height(48.dp).testTag("chrome_row_$index")) { Text("row $index") }
                            }
                        }
                    }
                }
            }
        }
        val upper = compose.onNodeWithTag("chrome_top").getUnclippedBoundsInRoot()
        val lower = compose.onNodeWithTag("chrome_bottom").getUnclippedBoundsInRoot()
        val first = compose.onNodeWithTag("chrome_row_0").getUnclippedBoundsInRoot()
        assertEquals(upper.bottom, first.top)
        compose.onNodeWithTag("chrome_list").performScrollToIndex(8)
        assertEquals(upper, compose.onNodeWithTag("chrome_top").getUnclippedBoundsInRoot())
        assertEquals(lower, compose.onNodeWithTag("chrome_bottom").getUnclippedBoundsInRoot())
        val row = compose.onNodeWithTag("chrome_row_7").getUnclippedBoundsInRoot()
        assertTrue("The previous row extends behind the upper bar", row.top < upper.bottom && row.bottom > upper.top)
        compose.onNodeWithTag("chrome_list").performScrollToIndex(39)
        assertTrue(compose.onNodeWithTag("chrome_row_39").getUnclippedBoundsInRoot().bottom <= lower.top)
    }
}
