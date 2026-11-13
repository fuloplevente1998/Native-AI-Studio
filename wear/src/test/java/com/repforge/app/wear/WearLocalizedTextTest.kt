package com.repforge.app.wear

import org.junit.Assert.*
import org.junit.Test

class WearLocalizedTextTest {
    @Test fun primaryActionsAndFeedbackAreTranslatedWithoutChangingUnknownExerciseNames() {
        assertEquals("Save",wearText("Mentés","en"))
        assertEquals("Löschen",wearText("Törlés","de"))
        assertEquals("Jurnal",wearText("Napló","ro"))
        assertEquals("Mentés",wearText("Mentés","hu"))
        assertEquals("Save",wearText("Mentés","fr"))
        assertEquals("Goblet squat",wearText("Goblet squat","de"))
    }
}
