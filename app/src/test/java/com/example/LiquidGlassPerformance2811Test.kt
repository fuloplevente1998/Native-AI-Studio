package com.example

import com.example.model.SubScreen
import com.example.ui.theme.*
import org.junit.Assert.*
import org.junit.Test

/**
 * 2811 G5: the cheap paths. Active measurement screens draw every surface with the light Performance
 * material on a static backdrop (no blur, refraction, shadow or drift clock); battery saver freezes the drift;
 * shader programs are compiled once and only uniforms change.
 */
class LiquidGlassPerformance2811Test {
    private val measuring = listOf(SubScreen.ACTIVE_STRENGTH, SubScreen.ACTIVE_TIMED, SubScreen.ACTIVE_BILATERAL,
        SubScreen.ACTIVE_OUTDOOR_GPS)

    @Test fun activeWorkoutUsesTheLightPerformanceMaterialOnAStaticBackdrop() {
        for (screen in measuring) {
            val enabled = glassOpticsEnabledFor(screen)
            assertFalse(enabled)
            for (role in GlassRole.values()) {
                val drawn = GlassRole.resolve(role, enabled)
                val optics = GlassOptics.from(65, drawn)
                assertEquals("$role", 0f, optics.elevation, 0f)
                // The page-matched header keeps its role (no step), but on this screen it only has the static backdrop.
                if (!role.matchesPage) {
                    assertEquals(0f, optics.bend, 0f)
                    assertEquals(0f, optics.dispersion, 0f)
                    assertEquals(GlassRole.Performance, drawn)
                    assertEquals(0f, optics.blurDp, 0f)
                }
            }
            assertEquals(GlassBackdropMode.OFF, GlassScene.mode(true, enabled, motionEnabled = true))
            assertFalse(GlassScene.ticks(GlassScene.mode(true, enabled, true), visible = true))
        }
    }

    @Test fun batterySaverFreezesTheDriftButKeepsTheGlass() {
        val saver = GlassScene.mode(true, opticsEnabled = true, motionEnabled = true, powerSave = true)
        assertEquals(GlassBackdropMode.STATIC, saver)
        assertFalse(GlassScene.ticks(saver, visible = true))
        assertEquals(GlassBackdropMode.LIVING, GlassScene.mode(true, true, true, powerSave = false))
        assertEquals(GlassBackdropMode.OFF, GlassScene.mode(false, true, true, powerSave = true))
    }

    @Test fun programsCompileOnceAndRolesAreCached() {
        val shaders = java.io.File("src/main/java/com/example/ui/theme/GlassShaders.kt").readText()
        assertTrue(shaders.contains("onFailure { failed = true }"))
        assertTrue(shaders.contains("sceneLensProgram ?: compile("))
        for (role in GlassRole.values()) for (step in listOf(0, 65, 100))
            assertSame("$role $step cached", GlassOptics.from(step, role), GlassOptics.from(step, role))
        // The header and nav share one recorded backdrop; quick buttons no longer open a second lens.
        val header = java.io.File("src/main/java/com/example/ui/components/common/TopHeaderAndNav.kt").readText()
        assertTrue(header.contains("val ownGlass = !hosted && selected && !quick"))
    }
}
