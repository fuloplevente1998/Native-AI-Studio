package com.repforge.app.localization

import com.example.localization.CalendarFeedback
import com.example.model.AppLanguage
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarFeedbackTest {
    @Test fun feedbackRetainsDatesBadgesAndTimesInEveryLanguage() {
        for (language in AppLanguage.values()) {
            assertTrue(CalendarFeedback.modified(language, "A/B", "09:30").contains("A/B"))
            assertTrue(CalendarFeedback.modified(language, "A/B", "09:30").contains("09:30"))
            assertTrue(CalendarFeedback.moved(language, "2026-11-17").contains("2026-11-17"))
            assertTrue(CalendarFeedback.added(language, "2026-11-17", "A/B").contains("A/B"))
            assertTrue(CalendarFeedback.deleted(language).isNotBlank())
            assertTrue(CalendarFeedback.undoEmpty(language).isNotBlank())
            assertTrue(CalendarFeedback.undone(language).isNotBlank())
        }
    }
}
