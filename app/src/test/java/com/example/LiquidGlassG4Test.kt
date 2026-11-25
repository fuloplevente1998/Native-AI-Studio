package com.example

import androidx.compose.ui.graphics.Color
import com.example.model.SubScreen
import com.example.ui.theme.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Integration contracts for the G4 screen paint migration; device acceptance is separate. */
class LiquidGlassG4Test {
    private fun source(path: String) = File("src/main/java/com/example/$path").readText()

    @Test fun migratedPanelAndTilePaintDoesNotChangeLegacyThemes() {
        val custom = requireNotNull(resolveCustomTheme(
            CustomThemeDraft.fromStyle(CustomThemeStyle.CONTRAST, "G4"), newCustomThemeKey())).palette
        for (p in TrainPilotPalettes.allPalettes + custom) {
            for (legacy in listOf(p.surfaceCard, p.surfaceElevated, p.background.copy(alpha = .55f), p.primary)) {
                assertEquals(legacy, p.glassCardFill(legacy))
                assertEquals(legacy, p.glassTile(legacy, elevated = true))
            }
        }
        for (p in LiquidGlassPalettes.all) {
            assertEquals(Color.Transparent, p.glassCardFill(p.surfaceCard))
            assertTrue(p.glassTile(p.surfaceCard, elevated = true).alpha < 1f)
        }
    }

    @Test fun measuringScreensUseSharedBackdropAndKeepExpensiveOpticsOff() {
        for ((file, screen) in listOf(
            "measure/ActiveStrengthWorkoutScreen.kt" to SubScreen.ACTIVE_STRENGTH,
            "measure/ActiveTimedWorkoutScreen.kt" to SubScreen.ACTIVE_TIMED,
            "measure/ActiveBilateralWorkoutScreen.kt" to SubScreen.ACTIVE_BILATERAL,
            "OutdoorGpsDashboard.kt" to SubScreen.ACTIVE_OUTDOOR_GPS
        )) {
            assertFalse(glassOpticsEnabledFor(screen))
            assertTrue(file, source("ui/screens/$file").contains("trainPilotScreenBackdrop(palette)"))
        }
        for (file in listOf("StudioRestTimerDock.kt", "StudioWorkoutHandoverBar.kt")) {
            val text = source("ui/components/$file")
            assertTrue(file, text.contains("GlassMaterialRole.Performance"))
            assertTrue(file, text.contains("glassTile("))
        }
        assertTrue(glassOpticsEnabledFor(SubScreen.NONE))
    }
}
