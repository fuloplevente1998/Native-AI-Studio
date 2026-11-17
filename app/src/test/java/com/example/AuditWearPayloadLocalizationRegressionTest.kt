package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.repforge.app.WearSyncRepository
import com.repforge.app.data.db.ProgramDayEntity
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.migration.LegacyWebViewMigrator
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuditWearPayloadLocalizationRegressionTest {
    private fun JSONArray.byId(id: String): JSONObject = (0 until length())
        .map { getJSONObject(it) }.first { it.optString("id") == id }

    @Test fun realHomeAndStartPayloadsUsePhoneLanguageAndPreserveHistoricalNames() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).allowMainThreadQueries().build()
        val settings = TrainPilotSettingsStore(context)
        try {
            TrainPilotDatabase.ensureBuiltinCatalogSeeded(db, context)
            val builtin = TrainPilotDatabase.defaultExercises().first().copy(
                nameHu = "Guggolás", nameEn = "Squat", nameDe = "Kniebeuge", nameRo = "Genuflexiune", custom = false)
            val custom = builtin.copy(id = "audit-custom", nameHu = "My custom lift", nameEn = "My custom lift",
                nameDe = "", nameRo = "", custom = true)
            val missing = builtin.copy(id = "audit-missing", nameHu = "Tartalék", nameEn = "Fallback",
                nameDe = "", nameRo = "", custom = false)
            db.exerciseLibraryDao().upsertExercises(listOf(builtin, custom, missing))
            val program = TrainPilotDatabase.defaultPrograms().first.first().copy(id = "audit-program",
                nameHu = "Program HU", nameEn = "Program EN", nameDe = "Programm DE", nameRo = "Program RO")
            val day = ProgramDayEntity("audit-program:day", "audit-program", "day", "Nap HU", "Day EN", "Tag DE", "Zi RO", 0,
                JSONArray().put(builtin.id).put(custom.id).put(missing.id).toString())
            db.programDao().saveProgramWithDays(program, listOf(day))
            settings.setActiveProgramId(program.id)
            fun historyExercise(id: String, hu: String, en: String) = JSONObject().put("id", id).put("hu", hu).put("en", en)
                .put("sets", JSONArray().put(JSONObject().put("done", true).put("reps", "10")))
            val history = JSONObject().put("id", "audit-history").put("started", "2026-11-15T10:00:00Z")
                .put("finished", "2026-11-15T11:00:00Z").put("dayName", "My historic day")
                .put("exercises", JSONArray().put(historyExercise(builtin.id, builtin.nameHu, builtin.nameEn))
                    .put(historyExercise(custom.id, "My custom lift", "My custom lift"))
                    .put(historyExercise(builtin.id, "My renamed lift", "Squat")))
            LegacyWebViewMigrator(context, db, settings).saveWorkoutJsonToRoom(history)
            data class Names(val program: String, val day: String, val exercise: String)
            val languages = mapOf("hu" to Names("Program HU", "Nap HU", "Guggolás"),
                "en" to Names("Program EN", "Day EN", "Squat"),
                "de" to Names("Programm DE", "Tag DE", "Kniebeuge"),
                "ro" to Names("Program RO", "Zi RO", "Genuflexiune"))
            languages.forEach { (language, names) ->
                settings.setLanguage(language)
                val home = WearSyncRepository.buildWatchHomeSnapshot(context, database = db)
                assertEquals(names.program, home.getString("activeProgramName"))
                val watchDay = home.getJSONArray("days").byId("day")
                assertEquals(names.program, watchDay.getString("programName"))
                assertEquals(names.day, watchDay.getString("name"))
                val calendarDay = home.getJSONArray("calendarDays").byId("day")
                assertEquals(names.program, calendarDay.getString("programName"))
                assertEquals(names.day, calendarDay.getString("name"))
                assertEquals(names.exercise, watchDay.getJSONArray("exercises").byId(builtin.id).getString("name"))
                val quick = home.getJSONArray("quickExercises")
                assertEquals(names.exercise, quick.byId(builtin.id).getString("name"))
                assertEquals("My custom lift", quick.byId(custom.id).getString("name"))
                assertEquals(if (language == "hu") "Tartalék" else "Fallback", quick.byId(missing.id).getString("name"))
                assertEquals(names.exercise, WearSyncRepository.watchExerciseForStart(db, builtin, settings.current()).getString("name"))
                val recent = home.getJSONArray("recentWorkouts").getJSONObject(0)
                assertEquals("My historic day", recent.getString("dayName"))
                val exercises = recent.getJSONArray("exercises")
                assertEquals(names.exercise, exercises.getJSONObject(0).getString("name"))
                assertEquals("My custom lift", exercises.getJSONObject(1).getString("name"))
                assertEquals("My renamed lift", exercises.getJSONObject(2).getString("name"))
            }
        } finally { db.close() }
    }
}
