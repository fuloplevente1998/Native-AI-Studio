package com.example

import com.example.model.LoggedWorkoutEntry
import com.example.ui.components.coachTargetText
import com.example.ui.screens.loggedPrRecords
import com.repforge.app.ui.coach.NativeCoachExerciseAdvice
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Collector2779RegressionTest {
    @Test fun prRecordsComeFromTheSavedWorkoutsNotAnEmptyList() {
        val prs = JSONArray().put(JSONObject().put("exerciseId", "goblet").put("exerciseName", "Goblet guggolás")
            .put("kind", "maxWeight").put("value", "14 kg").put("previous", "12,5 kg"))
        val workouts = listOf(
            LoggedWorkoutEntry(id = "w2", titleHu = "A", savedStartIso = "2026-11-19T10:00:00Z", hasPr = true, prRecordsJson = prs.toString()),
            LoggedWorkoutEntry(id = "w1", titleHu = "B", savedStartIso = "2026-11-10T10:00:00Z"))
        val records = loggedPrRecords(workouts)
        assertEquals(1, records.size)
        assertEquals("Goblet guggolás", records[0].exerciseNameHu)
        assertEquals("2026-11-19", records[0].dateLabel)
        assertEquals("12,5 kg", records[0].previousLabel)
        assertEquals("14 kg", records[0].newLabel)
        assertTrue(loggedPrRecords(listOf(LoggedWorkoutEntry(id = "x", titleHu = "x", prRecordsJson = "not json"))).isEmpty())
    }

    @Test fun coachTargetNamesItsUnit() {
        fun t(text: String, kind: String = "reps") = NativeCoachExerciseAdvice.Target(kind, text = text)
        assertEquals("25 mp", coachTargetText(t("25"), "mp", "hu"))
        assertEquals("25 mp/oldal", coachTargetText(t("25"), "mp/oldal", "hu"))
        assertEquals("8-12 ism.", coachTargetText(t("8-12"), "ism.", "hu"))
        assertEquals("10 ism.", coachTargetText(t("10"), "", "hu"))
        assertEquals("30 mp", coachTargetText(t("30", "time"), "", "hu"))
        assertEquals("2 RIR", coachTargetText(t("2 RIR", "rir"), "ism.", "hu")) // already has a unit
    }
}
