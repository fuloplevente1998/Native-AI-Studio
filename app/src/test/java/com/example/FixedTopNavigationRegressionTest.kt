package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.localization.TrainPilotStrings
import com.example.model.AppLanguage
import com.example.model.MainTab
import com.example.model.SubScreen
import com.example.ui.components.TrainPilotGlobalTopHeaderAndNav
import com.example.ui.theme.TrainPilotTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w480dp-h960dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class FixedTopNavigationRegressionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun allFiveTabsStayInsideNarrowHeaderAndDoNotMoveOnHorizontalSwipe() {
        var language by mutableStateOf(AppLanguage.HU)
        var selected by mutableStateOf(MainTab.HOME)
        compose.setContent { TrainPilotTheme {
            Box(Modifier.width(320.dp)) {
                TrainPilotGlobalTopHeaderAndNav(selected, SubScreen.NONE, language, 1,
                    TrainPilotStrings.forLanguage(language), { selected = it }, { _, _ -> }, {}, {}, {})
            }
        } }
        val tabs = listOf("home" to MainTab.HOME, "programs" to MainTab.PROGRAMS,
            "workout" to MainTab.WORKOUT, "progress" to MainTab.PROGRESS, "health" to MainTab.HEALTH)
        for (lang in AppLanguage.entries) {
            compose.runOnIdle { language = lang }
            val row = compose.onNodeWithTag("top_nav_row")
            val bounds = row.fetchSemanticsNode().boundsInRoot
            val before = tabs.map { (tag, _) ->
                compose.onNodeWithTag("top_nav_$tag").assertIsDisplayed().fetchSemanticsNode().boundsInRoot.also {
                    assertTrue(it.left >= bounds.left && it.right <= bounds.right)
                    assertTrue(it.width > 0 && it.height > 0)
                }
            }
            row.performTouchInput { swipeLeft() }
            tabs.forEachIndexed { index, (tag, _) ->
                val node = compose.onNodeWithTag("top_nav_$tag")
                assertEquals(before[index], node.fetchSemanticsNode().boundsInRoot)
            }
            tabs.forEach { (tag, tab) ->
                val node = compose.onNodeWithTag("top_nav_$tag")
                node.performClick()
                node.assertIsSelected()
                compose.runOnIdle { assertEquals(tab, selected) }
            }
        }
    }
}
