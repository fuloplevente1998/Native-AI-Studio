package com.example

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.localization.TrainPilotStrings
import com.example.model.AppLanguage
import com.example.ui.components.PageLayoutState
import com.example.ui.components.reconcilePageLayout
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.TrainPilotTheme
import com.example.ui.viewmodel.TrainPilotUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w393dp-h900dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TrainPilotPageLayoutRegressionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun savedOrderRejectsUnknownAndDuplicateBlocksAndIncludesNewBlocks() {
        assertEquals(listOf("health", "profile", "weekly"),
            reconcilePageLayout(listOf("obsolete", "health", "health"), listOf("profile", "health", "weekly")))
        val state = PageLayoutState("test", listOf("one", "two"), listOf("two", "one"))
        state.open()
        state.reset()
        assertEquals(listOf("one", "two"), state.draft)
        state.cancel()
        assertEquals(listOf("two", "one"), state.saved)
    }

    @Test fun homeSupportsLongPressAndFooterAndSavesNewOrderWithoutOpeningTheTile() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("trainpilot-page-layout", Context.MODE_PRIVATE)
        prefs.edit().remove("home").putString("health", "sources,today").apply()
        var profileOpened = 0
        compose.setContent {
            TrainPilotTheme {
                HomeScreen(TrainPilotUiState(), TrainPilotStrings.forLanguage(AppLanguage.HU),
                    onStartOrContinueWorkout = {}, onOpenProgramDayDetails = {},
                    onOpenQuickWorkout = {}, onOpenWatchAndSync = {}, onOpenHealthSleep = {},
                    onOpenHealthHeart = {}, onOpenHealthSteps = {}, onOpenCoachReadiness = {},
                    onOpenGoalsDialog = {}, onOpenPersonalPlanner = { profileOpened++ })
            }
        }
        val pageBlock = compose.onNodeWithTag("page_home_block_profile", useUnmergedTree = true)
        pageBlock.performTouchInput { longClick(Offset(center.x, 8f)) }
        compose.onNodeWithTag("page_layout_editor_home").assertExists()
        compose.runOnIdle { assertEquals(0, profileOpened) }
        compose.onNodeWithTag("page_layout_cancel").performClick()

        compose.onNodeWithTag("page_customize_home").performScrollTo().performClick()
        val moveUp = compose.onNodeWithTag("page_layout_handle_program").fetchSemanticsNode()
            .config[SemanticsActions.CustomActions].first { it.label == "Mozgatás fel" }
        compose.runOnIdle { assertTrue(moveUp.action()) }
        compose.onNodeWithTag("page_layout_save").performClick()
        compose.runOnIdle {
            assertEquals(listOf("program", "profile"), prefs.getString("home", "")!!.split(',').take(2))
            assertEquals("sources,today", prefs.getString("health", ""))
        }
    }
}
