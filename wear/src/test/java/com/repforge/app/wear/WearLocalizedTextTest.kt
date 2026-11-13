package com.repforge.app.wear

import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class WearLocalizedTextTest {
    @Test fun primaryActionsAndFeedbackAreTranslatedWithoutChangingUnknownExerciseNames() {
        assertEquals("Save",wearText("Mentés","en"))
        assertEquals("Löschen",wearText("Törlés","de"))
        assertEquals("Jurnal",wearText("Napló","ro"))
        assertEquals("Mentés",wearText("Mentés","hu"))
        assertEquals("Save",wearText("Mentés","fr"))
        assertEquals("Goblet squat",wearText("Goblet squat","de"))
    }
    @Test fun dynamicStopwatchAndSetLabelsUseTheWatchLocale() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.ENGLISH)
            assertEquals("Left side · right: 12 s", wearFormat("Bal oldal · jobb: %s mp", 12))
            assertEquals("✓ Record right time", wearText("✓ Jobb idő rögzítése"))
            assertEquals("2/3 · Set 1/4", wearFormat("%d/%d · Sorozat %d/%d", 2, 3, 1, 4))
            Locale.setDefault(Locale.GERMAN)
            assertEquals(" · Runde 2/3", wearFormat(" · %d/%d. kör", 2, 3))
            assertEquals("Rechte Seite · links: 8 Sek.", wearFormat("Jobb oldal · bal: %s mp", 8))
            Locale.setDefault(Locale("ro"))
            assertEquals("✓ 2/4 serii înregistrate", wearFormat("✓ %d/%d sorozat rögzítve", 2, 4))
            Locale.setDefault(Locale("hu"))
            assertEquals("Bal oldal · jobb: 12 mp", wearFormat("Bal oldal · jobb: %s mp", 12))
        } finally {
            Locale.setDefault(original)
        }
    }
}
