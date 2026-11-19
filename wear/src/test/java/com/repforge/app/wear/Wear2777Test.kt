package com.repforge.app.wear

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.hypot
import kotlin.math.min

class Wear2777Test {
    @Test fun programDayAvatarUsesTheDayLetterNotTheWordNap() {
        assertEquals("A", wearDayInitial("A nap"))
        assertEquals("B", wearDayInitial("B nap"))
        assertEquals("Fe", wearDayInitial("Felsőtest – Erő"))
        assertEquals("?", wearDayInitial("  "))
    }

    @Test fun mainMenuHasNoHomeRowAndKeepsEveryDestination() {
        for (active in listOf(false, true)) for (quick in listOf(false, true)) {
            val keys = WearMenu216.entries(active, quick).map { it.first }
            assertFalse(keys.contains("home"))
            assertTrue(keys.containsAll(listOf("planned", "journal", "metrics", "settings")))
            // 2777 (A): "Planned" opens the calendar directly; program days are a button on it.
            assertEquals("calendar", WearMenu216.entries(active, quick).first { it.first == "planned" }.second)
        }
    }

    @Test fun twoButtonFooterLeavesRoomForCaptionsUnderTheCirclesInsideTheRound() {
        for (size in listOf(176f, 192f, 200f, 216f, 225f, 240f)) {
            val page = wearFixedPageLayout(size, size, false, 2)
            val layout = wearActionLayout(size, size, 2, 7f)
            val x = (layout.buttonSize + layout.gap) / 2f
            // caption line: 12 dp under the circle; a 44 dp wide caption ("Befejezés") centred under it
            val captionY = page.footerTop + layout.buttonSize + 12f - size / 2f
            val halfWidth = 22f
            assertTrue("caption fits at $size", hypot(x + halfWidth, captionY) <= min(size, size) / 2f)
            assertEquals(layout.buttonSize + 14f, page.footerHeight, .001f)
        }
    }

    @Test fun calendarBackAndProgramDaysRowFitsUnderTheDaysInsideTheRound() {
        for (size in listOf(176f, 192f, 200f, 216f, 225f, 240f)) {
            val layout = wearCalendarLayout(size, size)
            val rowTop = layout.menuTop - 4f
            val lastRowBottom = layout.days[4].y + layout.buttonSize / 2f + 6f // marker under the day
            assertTrue("row overlaps the days at $size", rowTop >= lastRowBottom)
            val rowCenter = rowTop + 13f - size / 2f
            // ‹ (26) + gap (6) + "Programnapok" pill (~96) centred, 26 dp tall with round ends
            assertTrue("row clipped at $size", kotlin.math.hypot(51f, rowCenter) + 13f <= size / 2f)
        }
    }
}
