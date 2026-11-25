package com.example

import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.example.ui.components.workoutEditorFill
import com.example.ui.components.workoutEditorFrame
import com.example.ui.components.workoutFeedbackPaint
import com.example.ui.theme.*
import com.repforge.app.StudioWorkoutFeedback
import com.repforge.app.ui.theme.NativeThemeTokens
import org.junit.Assert.*
import org.junit.Test
import androidx.compose.ui.unit.dp

class WorkoutEditorAppearance2814Test {
    @Test fun legacyChoicesAreDistinctOpaqueAndReadable() {
        for (p in TrainPilotPalettes.allPalettes.filterNot { it.isLiquidGlass }) {
            val paints = StudioWorkoutFeedback.efforts.map { workoutFeedbackPaint(p, it, false) }
            assertEquals(6, paints.map { it.fill }.distinct().size)
            for (key in StudioWorkoutFeedback.efforts) {
                val idle = workoutFeedbackPaint(p, key, false)
                val chosen = workoutFeedbackPaint(p, key, true)
                assertNotEquals(idle.fill, chosen.fill)
                for (paint in listOf(idle, chosen)) {
                    assertEquals(1f, paint.fill.alpha, .001f)
                    assertTrue(NativeThemeTokens.contrast(paint.text, paint.fill) >= 4.5f)
                }
            }
            assertEquals(1.dp, workoutEditorFrame(p).width)
            assertEquals(1f, workoutEditorFill(p, p.surfaceElevated).alpha, .001f)
        }
    }

    @Test fun glassChoicesStayBelowContrastCeilingAndHaveDistinctSelectedPaint() {
        for (seed in LiquidGlassPalettes.all) for (clarity in listOf(0, 65, 100)) {
            val p = seed.withGlassClarity(clarity)
            assertEquals(6, StudioWorkoutFeedback.efforts.map { workoutFeedbackPaint(p, it, false).fill }.distinct().size)
            for (key in StudioWorkoutFeedback.efforts) {
                val idle = workoutFeedbackPaint(p, key, false)
                val chosen = workoutFeedbackPaint(p, key, true)
                assertTrue(chosen.fill.alpha > idle.fill.alpha)
                for (paint in listOf(idle, chosen)) {
                    val composite = paint.fill.compositeOver(GlassOptics.lensWorstCase)
                    assertTrue(composite.luminance() <= GlassOptics.SURFACE_LUMA_MAX + .003f)
                    assertTrue(NativeThemeTokens.contrast(paint.text, composite) >= 4.5f)
                }
            }
            val fallback = workoutEditorFill(p, p.surfaceElevated)
            assertEquals(1f, fallback.alpha, .001f)
            assertTrue(NativeThemeTokens.contrast(p.textPrimary, fallback) >= 4.5f)
            assertEquals(1.dp, workoutEditorFrame(p).width)
        }
    }
}
