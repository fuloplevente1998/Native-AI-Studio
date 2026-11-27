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
    @Test fun rangePickerShowsADistinctStartInteriorEndAndTodayWithoutLiveDayLenses() {
        for (palette in LiquidGlassPalettes.all + TrainPilotPalettes.allPalettes) {
            val normal = calendarDayAppearance(palette, false, false, false, rangePicker = true)
            val inside = calendarDayAppearance(palette, false, false, false, rangePicker = true, rangeInterior = true)
            val edge = calendarDayAppearance(palette, true, false, false, rangePicker = true)
            val todayEdge = calendarDayAppearance(palette, true, true, false, rangePicker = true)
            assertNotEquals(normal.fill, inside.fill)
            assertNotEquals(edge.fill, inside.fill)
            assertEquals(edge.fill, todayEdge.fill)
            assertTrue(todayEdge.underlineToday)
            assertFalse(edge.selectedLens)
            assertEquals(palette.isLiquidGlass, normal.lightRim)
            if (palette.isLiquidGlass) assertTrue(edge.fill.alpha > inside.fill.alpha && edge.fill.alpha < 1f)
            else assertEquals(palette.primary, edge.fill)
        }
    }

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
