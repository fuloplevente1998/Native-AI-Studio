package com.example

import android.content.Context
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.ui.viewmodel.TrainPilotViewModel
import com.repforge.app.*
import com.repforge.app.data.db.*
import com.repforge.app.data.migration.LegacyWebViewMigrator
import com.repforge.app.data.store.TrainPilotSettingsStore
import com.repforge.app.ui.coach.NativeSelectedCoachReadings
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.PAUSED)
class StudioPrefillBindingRegressionTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: TrainPilotDatabase
    private lateinit var settings: TrainPilotSettingsStore
    private lateinit var exercise: ExerciseLibraryEntity
    private val parent = SupervisorJob()
    private val scope = CoroutineScope(parent + Dispatchers.Main.immediate)

    @Before fun setup() {
        val field = PhoneWorkoutExecution::class.java.getDeclaredField("store").apply { isAccessible = true }
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close(); field.set(null, null)
        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
        settings = TrainPilotSettingsStore(context)
        exercise = TrainPilotDatabase.defaultExercises().first {
            it.loadType in setOf("per_hand", "single_dumbbell", "total") &&
                it.repUnit in setOf("", "ism.", "reps", "/kar", "/láb") && it.measurementType !in setOf("distance", "time", "gps")
        }
        native {
            db.exerciseLibraryDao().upsertExercises(TrainPilotDatabase.defaultExercises())
            val template = TrainPilotDatabase.defaultPrograms().first.first()
            val program = template.copy(id = "studio-test", nameHu = "Tesztprogram", nameEn = "Test",
                builtin = false, generated = false, sourceType = "imported", sourceTemplateId = "original-template", templateVersion = 7,
                rawJson = JSONObject().put("id", "studio-test").put("customUnknown", "keep-raw").toString())
            val dayA = ProgramDayEntity("studio-test:a", "studio-test", "a", "A nap", "A", "A", "A", 0,
                JSONArray().put(JSONObject().put("id", exercise.id).put("sets", 2).put("reps", "8–12").put("weightKg", 33)).toString())
            val dayB = dayA.copy(compositeId = "studio-test:b", dayId = "b", nameHu = "B nap", sortOrder = 1)
            db.programDao().saveProgramWithDays(program, listOf(dayA, dayB))
            settings.setActiveProgramId("studio-test")
        }
    }
    @After fun close() { parent.cancel(); shadowOf(Looper.getMainLooper()).idle(); db.close() }

    private fun <T> native(operation: suspend () -> T): T {
        val worker = Executors.newSingleThreadExecutor()
        val future = worker.submit<T> { runBlocking { operation() } }
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        try {
            while (true) {
                shadowOf(Looper.getMainLooper()).idle()
                try { return future.get(10, TimeUnit.MILLISECONDS) }
                catch (_: TimeoutException) { check(System.nanoTime() < deadline) { "Native operation timed out" } }
            }
        } finally { future.cancel(true); worker.shutdownNow() }
    }
    private fun await(condition: () -> Boolean) {
        val end = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        while (!condition()) {
            shadowOf(Looper.getMainLooper()).idle()
            check(System.nanoTime() < end) { "Studio state timed out" }
            Thread.sleep(5)
        }
    }
    private fun model(): TrainPilotViewModel = TrainPilotViewModel().also {
        it.bindNativeEnvironment(context, settings, db, scope)
        await { !it.uiState.value.savedRecordsLoading && it.uiState.value.programs.any { p -> p.id == "studio-test" } }
    }
    private fun seedPrevious(): String = native {
        val now = Instant.now()
        val sets = JSONArray().put(JSONObject().put("done", true).put("weight", 17.5).put("reps", "12").put("actualRir", 1))
            .put(JSONObject().put("done", true).put("weight", 19.7).put("reps", "9").put("actualRir", 0))
        val raw = JSONObject().put("id", "previous-different-program").put("programId", "another-program")
            .put("dayId", "other-day").put("programName", "Előző edzés")
            .put("started", now.minusSeconds(7200).toString()).put("finished", now.minusSeconds(3600).toString())
            .put("exercises", JSONArray().put(JSONObject().put("id", exercise.id).put("loadType", exercise.loadType)
                .put("repUnit", exercise.repUnit).put("measurementType", exercise.measurementType).put("sets", sets)))
        LegacyWebViewMigrator(context, db, settings).saveWorkoutJsonToRoom(raw)
        requireNotNull(db.workoutHistoryDao().getWorkoutWithDetailsById("previous-different-program")).workout.rawJson
    }

    @Test fun actualCoachDayUsesPerSetHistoryAndKeepsIdsManualValuesAndMissingRir() {
        val before = seedPrevious()
        val model = model()
        model.startCoachWorkout("studio-test", "b", null)
        await { model.uiState.value.isWorkoutSessionActive }
        val frame = requireNotNull(ActiveWorkoutStore.current(context))
        assertEquals("b", frame.getString("dayId"))
        val rows = frame.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets")
        assertEquals(17.5, rows.getJSONObject(0).getDouble("weight"), 0.001)
        assertEquals("12", rows.getJSONObject(0).getString("reps"))
        assertEquals(19.7, rows.getJSONObject(1).getDouble("weight"), 0.001)
        assertEquals("9", rows.getJSONObject(1).getString("reps"))
        assertFalse(rows.getJSONObject(0).has("actualRir")); assertFalse(rows.getJSONObject(0).getBoolean("done"))
        val id = rows.getJSONObject(0).getString("setId")
        model.updateSetValuesExact(1, 22f, 10, null)
        await { ActiveWorkoutStore.current(context)?.getJSONArray("exercises")?.getJSONObject(0)
            ?.getJSONArray("sets")?.getJSONObject(0)?.optDouble("weight") == 22.0 }
        val edited = requireNotNull(ActiveWorkoutStore.current(context)).getJSONArray("exercises").getJSONObject(0)
        assertEquals(exercise.loadType, edited.getString("loadType"))
        assertEquals(id, edited.getJSONArray("sets").getJSONObject(0).getString("setId"))
        assertFalse(edited.getJSONArray("sets").getJSONObject(0).has("actualRir"))
        assertTrue(edited.getJSONArray("sets").getJSONObject(0).has("previousValues228"))
        val untouched = edited.getJSONArray("sets").getJSONObject(1)
        assertEquals(untouched.getJSONObject("previousValues228").getDouble("weight"), untouched.getDouble("weight"), 0.0)
        val saved = native { db.workoutHistoryDao().getWorkoutWithDetailsById("previous-different-program")!! }
        assertEquals(JSONObject(before).getJSONArray("exercises").toString(), JSONObject(saved.workout.rawJson).getJSONArray("exercises").toString())
        model.startPlannedWorkout("studio-test", 0)
        assertEquals(frame.getString("workoutId"), ActiveWorkoutStore.current(context)!!.getString("workoutId"))
    }

    @Test fun quickStartPrefillsButAnExplicitZeroRirAndClearingItRemainDistinct() {
        seedPrevious()
        val model = model()
        model.startWorkoutWithExercises(listOf(model.uiState.value.exercises.first { it.id == exercise.id }))
        await { model.uiState.value.isWorkoutSessionActive }
        val frame = requireNotNull(ActiveWorkoutStore.current(context))
        assertTrue(frame.isNull("programId")); assertTrue(frame.isNull("dayId"))
        assertEquals(17.5f, model.uiState.value.activeSets.first().weightKg)
        assertFalse(model.uiState.value.activeSets.first().actualRirProvided)
        model.updateSetValuesExact(1, 17.5f, 12, 0)
        await { ActiveWorkoutStore.current(context)?.getJSONArray("exercises")?.getJSONObject(0)?.getJSONArray("sets")?.getJSONObject(0)?.has("actualRir") == true }
        assertEquals(0, ActiveWorkoutStore.current(context)!!.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(0).getInt("actualRir"))
        model.updateSetValuesExact(1, 17.5f, 12, null)
        await { ActiveWorkoutStore.current(context)?.getJSONArray("exercises")?.getJSONObject(0)?.getJSONArray("sets")?.getJSONObject(0)?.has("actualRir") == false }
    }

    @Test fun noHistoryKeepsPrescriptionsAndProgramMetadataWithoutInventingMeasurements() {
        val model = model()
        val first = model.uiState.value.programs.first { it.id == "studio-test" }.days.first().exercises.first()
        assertEquals(33f, first.defaultWeightKg)
        assertEquals("8–12", first.targetRepsText)
        model.activateProgram("studio-test")
        model.startPlannedWorkout("studio-test", 1)
        await { model.uiState.value.isWorkoutSessionActive }
        val frame = requireNotNull(ActiveWorkoutStore.current(context))
        assertEquals("b", frame.getString("dayId"))
        val ex = frame.getJSONArray("exercises").getJSONObject(0)
        assertEquals("8–12", ex.getJSONObject("prescription").getString("reps"))
        assertEquals(2, ex.getJSONArray("sets").length())
        val set = ex.getJSONArray("sets").getJSONObject(0)
        assertEquals(33.0, set.getDouble("weight"), 0.001)
        assertEquals("", set.getString("reps")); assertFalse(set.getBoolean("done")); assertFalse(set.has("actualRir"))
        val saved = native { db.programDao().getProgramWithDaysById("studio-test")!!.program }
        assertEquals("keep-raw", JSONObject(saved.rawJson).getString("customUnknown"))
        assertEquals("imported", saved.sourceType); assertEquals("original-template", saved.sourceTemplateId)
        assertEquals(7, saved.templateVersion)
    }

    @Test fun coachReadsOnlyTheSelectedProviderAndTreatsVerifiedSamsungZeroAsData() {
        val store = HealthJournalStore.get(context)
        val now = Instant.parse("2026-11-03T10:00:00Z")
        native {
            store.samsungDay("2026-11-03", JSONObject().put("energyScore", 0)
                .put("energyScoreSource", "samsung_health").put("energyScoreTime", "2026-11-03T09:00:00Z"), JSONArray(), store.generation())
            store.healthDay("2026-11-03", JSONObject().put("source", "@aggregate").put("energyScore", 95)
                .put("energyScoreSource", "samsung_health").put("energyScoreTime", "2026-11-03T09:00:00Z"), store.generation())
            store.setPreferences(JSONObject().put("provider", HealthJournalStore.HC))
        }
        val hc = native { NativeSelectedCoachReadings.load(context, db, now, ZoneOffset.UTC) }
        assertEquals(HealthJournalStore.HC, hc.provider); assertNull(hc.readiness.samsungScore); assertNull(hc.readiness.score)
        native { store.setPreferences(JSONObject().put("provider", HealthJournalStore.SAMSUNG)) }
        val samsung = native { NativeSelectedCoachReadings.load(context, db, now, ZoneOffset.UTC) }
        assertEquals(0.0, requireNotNull(samsung.readiness.samsungScore), 0.001)
        assertEquals(0.0, requireNotNull(samsung.readiness.score), 0.001)
    }
}
