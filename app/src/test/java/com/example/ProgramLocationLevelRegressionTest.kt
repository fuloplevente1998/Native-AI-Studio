package com.example

import com.example.model.resolveProgramLocationLevel
import com.repforge.app.data.db.TrainPilotDatabase
import org.junit.Assert.assertEquals
import org.junit.Test

/** #278: built-in program cards must not inherit the user's profile location/level. */
class ProgramLocationLevelRegressionTest {
    private val catalog = TrainPilotDatabase.defaultPrograms().first.associateBy { it.id }

    @Test fun builtInGymProgramsShowCatalogMetadataEvenWhenStoredRowWasPollutedByProfile() {
        for (id in listOf("gym-fullbody", "gym-ab", "gym-strength-abc")) {
            val template = catalog.getValue(id)
            // Older profile saves wrote the user's "Otthon"/"Kezdő" into every built-in row.
            val (location, level) = resolveProgramLocationLevel(
                builtin = true, storedLocation = "Otthon", storedLevel = "Kezdő",
                templateLocation = template.location, templateLevel = template.level
            )
            assertEquals(id, "Edzőterem", location)
            assertEquals(id, template.level, level)
        }
        assertEquals("Középhaladó", catalog.getValue("gym-ab").level)
    }

    @Test fun customAndGeneratedPlansKeepTheirOwnStoredValues() {
        val (location, level) = resolveProgramLocationLevel(
            builtin = false, storedLocation = "Otthon", storedLevel = "Haladó",
            templateLocation = "Edzőterem", templateLevel = "Kezdő"
        )
        assertEquals("Otthon", location)
        assertEquals("Haladó", level)
    }

    @Test fun blankValuesFallBackSafely() {
        assertEquals("Edzőterem" to "Középhaladó", resolveProgramLocationLevel(false, "", "", "Edzőterem", "Középhaladó"))
        assertEquals("Otthon / Edzőterem" to "Kezdő", resolveProgramLocationLevel(true, "", "", null, null))
    }
}
