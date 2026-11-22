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
        compose.setContent { TrainPilotTheme(fontScaleMultiplier = 1.6f) {
            Box(Modifier.width(320.dp)) {
                TrainPilotGlobalTopHeaderAndNav(selected, SubScreen.NONE, language, 1,
                    TrainPilotStrings.forLanguage(language), { selected = it }, { _, _ -> }, {}, {}, {})
            }
        } }
        val tabs = listOf("home" to MainTab.HOME, "programs" to MainTab.PROGRAMS,
            "workout" to MainTab.WORKOUT, "progress" to MainTab.PROGRESS, "health" to MainTab.HEALTH)
        for (lang in AppLanguage.entries) {
            compose.runOnIdle { language = lang }
            val journal = compose.onNodeWithTag("top_bar_journal_button")
            val brand = compose.onNodeWithTag("top_bar_brand_home").fetchSemanticsNode().boundsInRoot
            val actions = listOf("top_bar_calendar_button", "top_bar_coach_button",
                "top_bar_journal_button", "top_bar_settings_button").map { tag ->
                compose.onNodeWithTag(tag).assertIsDisplayed().assertWidthIsEqualTo(48.dp)
                    .assertHeightIsEqualTo(48.dp).fetchSemanticsNode().boundsInRoot
            }
            assertTrue(brand.right <= actions.first().left)
            assertTrue(actions.zipWithNext().all { (left, right) -> left.right <= right.left })
            compose.onNodeWithText("TrainPilot", useUnmergedTree = true).assertIsDisplayed()
            journal.performClick().assertIsSelected()
            compose.onNodeWithTag("top_nav_progress").assertIsNotSelected()
            compose.runOnIdle { assertEquals(MainTab.LOG, selected) }
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
