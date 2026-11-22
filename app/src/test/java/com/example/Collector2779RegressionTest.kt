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

    @Test fun homeBasicLosesOnlyExercisesAppendedAfterTheStockListAndKeepsOwnValues() {
        val stock = com.repforge.app.data.db.TrainPilotDatabase.defaultPrograms()
        val program = stock.first.first { it.id == "home-basic" }
        val dayA = stock.second.first { it.programId == "home-basic" && it.dayId == "A" }
        val dayB = stock.second.first { it.programId == "home-basic" && it.dayId == "B" }
        val ids = JSONArray(dayA.exercisesJson).let { rows -> (0 until rows.length()).map { rows.optJSONObject(it)?.getString("id") ?: rows.getString(it) } }
        assertEquals(7, ids.size)
        // Persisted by the old queue path: object rows with the user's own kg, plus three appended exercises.
        val grown = JSONArray().apply {
            ids.forEach { put(JSONObject().put("id", it).put("sets", 4).put("reps", "10").put("weight", 12.5)) }
            listOf("pushup", "plank", "lateral-raise").forEach { put(JSONObject().put("id", it).put("sets", 3)) }
        }
        val current = com.repforge.app.data.db.ProgramWithDays(program, listOf(dayA.copy(exercisesJson = grown.toString()), dayB))
        val trimmed = requireNotNull(com.repforge.app.data.db.NativeBuiltinCatalog.trimAppendedHomeBasic(current))
        val rows = JSONArray(trimmed.days.first { it.dayId == "A" }.exercisesJson)
        assertEquals(7, rows.length())
        assertEquals(ids, (0 until rows.length()).map { rows.getJSONObject(it).getString("id") })
        assertEquals(12.5, rows.getJSONObject(0).getDouble("weight"), 0.0) // own prescription kept
        assertEquals(dayB, trimmed.days.first { it.dayId == "B" })

        // A reordered/edited day, a personal copy or a stock day is never touched.
        val reordered = JSONArray(grown.toString()).apply { val first = get(0); put(0, get(1)); put(1, first) }
        assertNull(com.repforge.app.data.db.NativeBuiltinCatalog.trimAppendedHomeBasic(current.copy(days = listOf(dayA.copy(exercisesJson = reordered.toString()), dayB))))
        assertNull(com.repforge.app.data.db.NativeBuiltinCatalog.trimAppendedHomeBasic(current.copy(program = program.copy(builtin = false))))
        assertNull(com.repforge.app.data.db.NativeBuiltinCatalog.trimAppendedHomeBasic(com.repforge.app.data.db.ProgramWithDays(program, listOf(dayA, dayB))))
    }

    @Test fun quickWorkoutQueueNoLongerWritesIntoAProgramDay() {
        val main = java.io.File("src/main/java/com/example/MainActivity.kt").readText()
        val vm = java.io.File("src/main/java/com/example/ui/viewmodel/TrainPilotViewModel.kt").readText()
        // Only the editor entry arms the program-day write; every other library entry is queue-only.
        assertEquals(2, Regex("openExerciseLibraryForProgramDay\\(\\)").findAll(main).count())
        assertTrue(vm.contains("if (_uiState.value.libraryMutationBusy || !libraryAddsToProgramDay) return"))
        val quickSheet = main.substringAfter("if (uiState.showQuickWorkoutSheet)").substringBefore("onRemoveExerciseFromQueue")
        assertFalse(quickSheet.contains("addExerciseToActiveProgramDay"))
    }

    @Test fun finalCoachUsesItsOwnSignalsAndWeightsVerifiedSamsungEnergy() {
        val now = java.time.Instant.parse("2026-11-03T10:00:00Z")
        val zone = java.time.ZoneOffset.UTC
        val day = "2026-11-03"
        val sleep = JSONObject().put("day", day).put("sleepMinutes", 450)
        val energy = JSONObject().put("energyScore", 86).put("energyScoreSource", "samsung_health")
            .put("energyScoreTime", "2026-11-03T09:00:00Z").put("day", day)
        fun workout() = JSONObject().put("started", "2026-11-03T07:00:00Z").put("finished", "2026-11-03T08:00:00Z")
            .put("exercises", JSONArray().put(JSONObject().put("effort", "hard")).put(JSONObject().put("effort", "hard")))
        fun read(recovery: JSONObject, dailyEnergy: JSONObject, workouts: List<JSONObject> = emptyList()) =
            com.repforge.app.ui.coach.NativeCoachReadiness.calculate(recovery, dailyEnergy, workouts, now, zone)

        val noSamsung = read(sleep, JSONObject())
        assertEquals(82.0, requireNotNull(noSamsung.score), 0.0)
        val both = read(sleep, energy)
        assertEquals(82, both.trainpilotScore)
        assertEquals(86.0, requireNotNull(both.samsungScore), 0.0)
        assertEquals(83.0, requireNotNull(both.score), 0.0)
        val hard = read(sleep, energy, List(3) { workout() })
        assertEquals(76, hard.trainpilotScore)
        assertEquals(78.0, requireNotNull(hard.score), 0.0)
        val samsungOnly = read(JSONObject(), energy)
        assertEquals(86.0, requireNotNull(samsungOnly.samsungScore), 0.0)
        assertNull(samsungOnly.score)
        assertFalse(samsungOnly.hasRecoverySignal)
    }
}
