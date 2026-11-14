package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.AppLanguage
import com.example.model.ExerciseItem
import com.example.model.ExerciseSetItem
import com.example.ui.components.StudioCompactRestControls
import com.example.ui.components.StudioExerciseSequenceStrip
import com.example.ui.theme.TrainPilotTheme
import com.example.ui.viewmodel.TrainPilotUiState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w800dp-h1000dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class ActiveWorkoutDockNavigationRegressionTest {
    @get:Rule val compose = createComposeRule()

    private fun showRest(width: Dp, font: Float, language: AppLanguage, enabled: Boolean = true,
        adjust: (Int) -> Unit = {}, toggle: () -> Unit = {}, skip: () -> Unit = {}, expand: () -> Unit = {}) {
        compose.setContent { TrainPilotTheme {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, font)) {
                Box(Modifier.width(width)) {
                    StudioCompactRestControls(language, 90, false, enabled, adjust, toggle, skip, expand)
                }
            }
        } }
    }

    private fun assertRestFits() {
        val root = compose.onNodeWithTag("compact_rest_controls").getUnclippedBoundsInRoot()
        val title = compose.onNodeWithTag("compact_rest_time").getUnclippedBoundsInRoot()
        val actions = compose.onNodeWithTag("compact_rest_actions").getUnclippedBoundsInRoot()
        assertTrue(title.bottom <= actions.top || title.right <= actions.left)
        val tags = listOf("minus", "toggle", "plus", "skip", "expand")
        val bounds = tags.map { tag ->
            compose.onNodeWithTag("compact_rest_$tag").assertIsDisplayed()
                .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).getUnclippedBoundsInRoot()
        }
        bounds.forEach { b ->
            assertTrue(b.left >= root.left && b.right <= root.right)
            assertTrue(b.top >= title.bottom || b.left >= title.right)
            assertTrue(b.bottom <= root.bottom)
        }
        bounds.forEachIndexed { i, a -> bounds.drop(i + 1).forEach { b ->
            assertTrue(a.right <= b.left || b.right <= a.left || a.bottom <= b.top || b.bottom <= a.top)
        } }
        compose.onAllNodes(hasText("15", substring = true) or hasText("Indít") or hasText("Start") or
            hasText("Kihagy") or hasText("Überspr.") or hasText("01:30", substring = true))
            .fetchSemanticsNodes().forEach { node ->
                val layouts = mutableListOf<TextLayoutResult>()
                node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
                layouts.forEach { assertFalse("Clipped rest label: ${it.layoutInput.text.text}; size=${it.size}; paragraph=${it.multiParagraph.width}x${it.multiParagraph.height}; constraints=${it.layoutInput.constraints}; width=${it.didOverflowWidth}; height=${it.didOverflowHeight}", it.hasVisualOverflow) }
            }
    }

    @Test fun narrowRestKeepsLabelsApartAndAllControlsOperate() {
        val adjustments = mutableListOf<Int>()
        var toggles = 0; var skips = 0; var expansions = 0
        showRest(320.dp, 1.3f, AppLanguage.HU,
            adjust = { adjustments += it }, toggle = { toggles++ }, skip = { skips++ }, expand = { expansions++ })
        assertRestFits()
        compose.onNodeWithTag("compact_rest_minus").performClick()
        compose.onNodeWithTag("compact_rest_toggle").performClick()
        compose.onNodeWithTag("compact_rest_plus").performClick()
        compose.onNodeWithTag("compact_rest_skip").performClick()
        compose.onNodeWithTag("compact_rest_expand").performClick()
        compose.runOnIdle {
            assertEquals(listOf(-15, 15), adjustments)
            assertEquals(1, toggles); assertEquals(1, skips); assertEquals(1, expansions)
        }
    }

    @Test fun largeGermanFontWrapsControlsWithoutClippingOrOverlap() {
        showRest(280.dp, 1.6f, AppLanguage.DE)
        assertRestFits()
    }

    @Test fun wideRestStaysInlineAndOwnershipStillDisablesActions() {
        var mutations = 0
        showRest(740.dp, 1f, AppLanguage.HU, enabled = false,
            adjust = { mutations++ }, toggle = { mutations++ }, skip = { mutations++ })
        assertRestFits()
        val time = compose.onNodeWithTag("compact_rest_time").getUnclippedBoundsInRoot()
        val actions = compose.onNodeWithTag("compact_rest_actions").getUnclippedBoundsInRoot()
        assertTrue(time.top < actions.bottom && time.bottom > actions.top)
        listOf("minus", "toggle", "plus", "skip").forEach {
            compose.onNodeWithTag("compact_rest_$it").assertIsNotEnabled().performTouchInput { click() }
        }
        compose.runOnIdle { assertEquals(0, mutations) }
    }

    @Test fun activeExerciseAutomaticallyReachesLeadingEdgeInBothDirectionsIncludingLast() {
        val exercises = (0..5).map { ExerciseItem("exercise-$it", "Hosszabb nevű gyakorlat $it") }
        val pending = listOf(ExerciseSetItem(1, 10f, 12, 2, false, id = "pending"))
        var state by mutableStateOf(TrainPilotUiState(activeWorkoutId = "workout", activeWorkoutGeneration = 7,
            activeWorkoutExercises = exercises, activeSets = pending,
            activeSetsByExerciseId = exercises.take(2).associate { it.id to pending.map { set -> set.copy(completed = true) } }))
        compose.setContent { TrainPilotTheme {
            Box(Modifier.width(300.dp)) {
                StudioExerciseSequenceStrip(state, exercises, state.activeExerciseIndex) { selected ->
                    state = state.copy(activeExerciseIndex = exercises.indexOf(selected))
                }
            }
        } }
        fun leading(index: Int) {
            compose.waitForIdle()
            val strip = compose.onNodeWithTag("active_workout_exercise_sequence").getUnclippedBoundsInRoot()
            val chip = compose.onNodeWithTag("active_exercise_chip_$index").assertIsDisplayed().getUnclippedBoundsInRoot()
            assertEquals(strip.left.value, chip.left.value, 1f)
        }
        leading(0)
        compose.runOnIdle { state = state.copy(activeExerciseIndex = 2) }
        leading(2)
        compose.runOnIdle { state = state.copy(activeExerciseIndex = 5) }
        leading(5)
        compose.runOnIdle { state = state.copy(activeExerciseIndex = 0) }
        leading(0)
        compose.onNodeWithTag("active_exercise_chip_0").performClick()
        compose.runOnIdle { assertEquals(pending, state.activeSets); assertFalse(state.activeSets.single().completed) }
    }
}
