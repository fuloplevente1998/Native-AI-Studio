package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.example.model.AppLanguage
import com.example.model.ExerciseSetItem
import com.example.ui.components.*
import com.example.ui.theme.TrainPilotTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h1000dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class ResponsiveControlsRegressionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun adjustmentGroupsReflowWithoutSplittingWordsAndDispatchEachAction() {
        var scale by mutableStateOf(1f)
        var language by mutableStateOf(AppLanguage.HU)
        var width by mutableStateOf(300)
        val calls = IntArray(3)
        fun labels() = when (language) {
            AppLanguage.HU -> listOf("Nulláz", "+5 mp", "+15 mp")
            AppLanguage.EN -> listOf("Reset", "+5 s", "+15 s")
            AppLanguage.DE -> listOf("Reset", "+5 Sek.", "+15 Sek.")
            AppLanguage.RO -> listOf("Reset", "+5 sec", "+15 sec")
        }
        compose.setContent { TrainPilotTheme(fontScaleMultiplier = scale) {
            Box(Modifier.width(width.dp)) {
                StudioCompactActionRow(labels().mapIndexed { index, label -> StudioCompactAction(label) { calls[index]++ } },
                    Modifier.testTag("adjustments"))
            }
        } }
        for (font in listOf(1f, 1.5f, 2f)) for (lang in AppLanguage.entries) for (dp in listOf(260, 320)) {
            compose.runOnIdle { scale = font; language = lang; width = dp }
            val nodes = compose.onAllNodes(hasClickAction()).fetchSemanticsNodes()
            assertEquals(3, nodes.size)
            for (label in labels()) {
                val node = compose.onNodeWithText(label).assertIsDisplayed()
                readable(node)
                val bounds = node.fetchSemanticsNode().boundsInRoot
                assertTrue(bounds.width > bounds.height)
                node.performClick()
            }
        }
        compose.runOnIdle { assertArrayEquals(intArrayOf(24,24,24), calls) }
    }

    @Test fun sideFieldsStayEqualAndKeepEditRirAndCompletionActionsAcrossLocales() {
        var scale by mutableStateOf(1f)
        var language by mutableStateOf(AppLanguage.HU)
        var width by mutableStateOf(300)
        var edits = 0; var rirEdits = 0; var toggles = 0
        compose.setContent { TrainPilotTheme(fontScaleMultiplier = scale) {
            Box(Modifier.width(width.dp)) {
                StudioBilateralSetControls(ExerciseSetItem(1,0f,0,2,false,leftSec=5,rightSec=125),true,language,
                    onEdit={ edits++ }, onRir={ rirEdits++ }, onToggle={ toggles++ })
            }
        } }
        for (font in listOf(1f,1.5f,2f)) for (lang in AppLanguage.entries) for (dp in listOf(260,320)) {
            compose.runOnIdle { scale=font;language=lang;width=dp }
            val left=compose.onNodeWithTag("bilateral_set_1_left_value").assertIsDisplayed()
            val right=compose.onNodeWithTag("bilateral_set_1_right_value").assertIsDisplayed()
            val l=left.fetchSemanticsNode().boundsInRoot;val r=right.fetchSemanticsNode().boundsInRoot
            assertEquals(l.height,r.height,1f);assertEquals(l.width,r.width,1f);assertEquals(l.top,r.top,1f)
            val texts=compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text),useUnmergedTree=true)
            for(i in 0 until texts.fetchSemanticsNodes().size) readable(texts[i])
            left.performClick();right.performClick()
            compose.onNodeWithTag("bilateral_set_1_rir").performClick()
            compose.onNodeWithTag("bilateral_set_1_complete").performClick()
        }
        compose.runOnIdle { assertEquals(48,edits);assertEquals(24,rirEdits);assertEquals(24,toggles) }
    }

    private fun readable(node: SemanticsNodeInteraction) {
        val results=mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        for(layout in results) {
            assertEquals("A compact label must not split into letter fragments",1,layout.lineCount)
            assertFalse("The label must not be truncated",layout.isLineEllipsized(0))
            assertTrue("Text exceeds its available width",layout.getLineRight(0)-layout.getLineLeft(0)<=layout.size.width+1f)
            assertFalse("Text is vertically clipped",layout.didOverflowHeight)
        }
    }
}
