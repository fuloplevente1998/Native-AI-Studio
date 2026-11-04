package com.example

import com.example.localization.StudioProfileOptionLabels as Labels
import com.example.model.AppLanguage
import org.junit.Assert.*
import org.junit.Test

class ProfileOptionLabelsTest {
    @Test fun optionsTranslateWithoutRewritingUnknownUserText() {
        for (language in AppLanguage.entries) {
            assertEquals("My custom note őű", Labels.label("My custom note őű", language))
            assertEquals("Kettlebell", Labels.label("Kettlebell", language))
        }
        assertEquals("Izomépítés", Labels.label("Izomépítés", AppLanguage.HU))
        assertEquals("Muscle building", Labels.label("Izomépítés", AppLanguage.EN))
        assertEquals("Muskelaufbau", Labels.label("Izomépítés", AppLanguage.DE))
        assertEquals("Creștere musculară", Labels.label("Izomépítés", AppLanguage.RO))
        assertEquals("Legs • Dumbbell", Labels.description("Láb • Kézisúlyzó", AppLanguage.EN))
        assertEquals("Beine • Kurzhantel", Labels.description("Láb • Kézisúlyzó", AppLanguage.DE))
        assertEquals("Picioare • Ganteră", Labels.description("Láb • Kézisúlyzó", AppLanguage.RO))
    }
}
