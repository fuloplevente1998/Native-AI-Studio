package com.example

import com.example.ui.theme.glassStateTile

import androidx.compose.ui.graphics.Color
import com.example.ui.screens.calendarDayAppearance
import com.example.ui.theme.LiquidGlassPalettes
import com.example.ui.theme.TrainPilotPalettes
import com.example.ui.theme.glassTile
import org.junit.Assert.*
import org.junit.Test

class CalendarGlass2Test {
    @Test fun glassDaysUseLightweightTilesExceptForTheSelectedLens() {
        for (palette in LiquidGlassPalettes.all) {
            for (today in listOf(false, true)) for (planned in listOf(false, true)) {
                val normal = calendarDayAppearance(palette, false, today, planned)
                val selected = calendarDayAppearance(palette, true, today, planned)
                val oldFill = if (planned) palette.primary.copy(alpha = .12f) else palette.surface
                assertEquals(if (planned) palette.glassStateTile(palette.primary, oldFill) else palette.glassTile(oldFill, elevated = today), normal.fill)
                assertTrue(normal.fill.alpha < 1f)
                assertTrue(normal.lightRim)
                assertFalse(normal.selectedLens)
                assertEquals(Color.Transparent, selected.fill)
                assertTrue(selected.selectedLens)
                assertFalse(selected.lightRim)
                assertEquals(today, normal.underlineToday)
                assertEquals(today, selected.underlineToday)
            }
        }
    }

    @Test fun legacyDayPaintIsUnchangedForEveryState() {
        for (palette in TrainPilotPalettes.allPalettes) {
            for (today in listOf(false, true)) for (planned in listOf(false, true))
                for (selected in listOf(false, true)) {
                    val appearance = calendarDayAppearance(palette, selected, today, planned)
                    assertEquals(if (planned) palette.primary.copy(alpha = .12f) else palette.surface, appearance.fill)
                    assertFalse(appearance.selectedLens)
                    assertFalse(appearance.lightRim)
                    assertFalse(appearance.underlineToday)
                }
        }
    }
}
