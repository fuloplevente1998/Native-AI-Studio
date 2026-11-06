package com.example

import android.content.Context
import android.app.Application
import android.os.Looper
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.repforge.app.data.db.*
import com.repforge.app.data.migration.LegacyWebViewMigrator
import com.repforge.app.data.store.TrainPilotSettingsStore
import com.repforge.app.data.store.TrainPilotUserSettings
import com.repforge.app.ui.coach.*
import com.repforge.app.ui.workout.NativeTrainingModeProposal
import com.repforge.app.ActiveWorkoutStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
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
import java.util.TimeZone
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.PAUSED)
class WorkoutCoachAdviceRegressionTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: TrainPilotDatabase
    private lateinit var migrator: LegacyWebViewMigrator
    private lateinit var originalZone: TimeZone
    private val now = Instant.parse("2026-11-05T12:00:00Z")
    private val prefs = TrainPilotUserSettings(language = "en")
    private val readiness get() = NativeCoachReadiness.calculate(JSONObject(), JSONObject(), emptyList(), now, ZoneOffset.UTC)
    private fun <T> native(block: suspend () -> T): T = runBlocking(Dispatchers.IO) { block() }

    @Before fun open() {
        originalZone = TimeZone.getDefault(); TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
        migrator = LegacyWebViewMigrator(context, db, TrainPilotSettingsStore(context))
        native {
            val exercise = TrainPilotDatabase.defaultExercises().first().copy(id = "coach-press", nameEn = "Actual press",
                loadType = "total", repUnit = "reps", measurementType = "strength", defaultSets = 2,
                defaultReps = "8-12", defaultWeight = 10.0, custom = false, rawJson = "{}")
            db.exerciseLibraryDao().upsertExercises(listOf(exercise, exercise.copy(id = "programmed-press")))
            val rx = JSONObject().put("coach-press", JSONObject().put("sets", 2).put("reps", "5"))
            val program = TrainPilotDatabase.defaultPrograms().first.first().copy(id = "coach-program",
                rawJson = JSONObject().put("prescriptions", rx).toString())
            // The active exercise may be a substitution absent from the saved day.
            val day = ProgramDayEntity("coach-program:b", "coach-program", "b", "B", "B", "B", "B", 0,
                JSONArray().put("programmed-press").toString())
            db.programDao().saveProgramWithDays(program, listOf(day))
        }
    }
    @After fun close() { db.close(); TimeZone.setDefault(originalZone) }

    private fun frame(quick: Boolean = false): JSONObject = JSONObject().put("active", true)
        .put("workoutId", "live").put("generation", 3).put("currentExercise", 0)
        .put("programId", if (quick) JSONObject.NULL else "coach-program")
        .put("dayId", if (quick) JSONObject.NULL else "b").put("quickWorkout", quick)
        .put("exercises", JSONArray().put(JSONObject().put("id", "coach-press").put("loadType", "total")
            .put("repUnit", "reps").put("measurementType", "strength").put("targetReps", "8-12")
            .put("prescription", JSONObject().put("sets", 2).put("reps", "8-12"))
            .put("sets", JSONArray().put(JSONObject().put("setId", "live-set").put("reps", "").put("weight", 0)))))

    private fun history(id: String, day: Int = 4, quick: Boolean = false, done: Boolean? = true,
        reps: String = "12", effort: String = "good"): JSONObject {
        val sets = JSONArray()
        for (number in 1..2) {
            val set = JSONObject().put("setId", "$id-$number").put("set", number).put("weight", 20).put("reps", reps)
            if (number == 1) set.put("actualRir", 0)
            if (done != null) set.put("done", done)
            sets.put(set)
        }
        return JSONObject().put("id", id).put("key", id)
            .put("started", "2026-11-0${day}T09:00:00Z").put("finished", "2026-11-0${day}T10:00:00Z")
            .put("programId", if (quick) JSONObject.NULL else "coach-program").put("quickWorkout", quick).put("dayId", "b")
            .put("exercises", JSONArray().put(JSONObject().put("id", "coach-press").put("loadType", "total")
                .put("repUnit", "reps").put("effort", effort).put("sets", sets)))
    }
    private suspend fun read(frame: JSONObject = frame(), settings: TrainPilotUserSettings = prefs) =
        NativeCoachAdviceRepository(db).loadActiveExercise(frame, "coach-press", settings, readiness, now)

    @Test fun repeatedCoachReloadsKeepPriorSnapshotAndCoalesceWithoutCancellingReads() {
        val reads = java.util.concurrent.atomic.AtomicInteger()
        val cancellations = java.util.concurrent.atomic.AtomicInteger()
        val gates = List(3) { kotlinx.coroutines.CompletableDeferred<Unit>() }
        val owner = ViewModelStore()
        val provider = com.repforge.app.HealthJournalStore.get(context).preferences()
            .optString("provider", com.repforge.app.HealthJournalStore.HC)
        val model = CoachViewModel(context as Application, db) {
            val index = reads.getAndIncrement()
            try { gates[index].await() }
            catch (cancelled: kotlinx.coroutines.CancellationException) { cancellations.incrementAndGet(); throw cancelled }
            CoachUiState(loading = false, provider = provider,
                target = CoachTarget("coach-program", "b", "Read $index"))
        }
        owner.put("reload-coach", model)
        fun await(condition: () -> Boolean) {
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
            while (true) {
                shadowOf(Looper.getMainLooper()).idle()
                if (condition()) return
                check(System.nanoTime() < deadline) { "Coach reload timed out" }
                Thread.sleep(5)
            }
        }
        try {
            await { reads.get() == 1 }
            repeat(5) { model.reload() }
            assertEquals(1, reads.get())
            gates[0].complete(Unit)
            await { reads.get() == 2 && model.state.value.target?.title == "Read 0" }
            assertTrue(model.state.value.loading)
            assertFalse(model.state.value.initialLoading)
            gates[1].complete(Unit)
            await { !model.state.value.loading && model.state.value.target?.title == "Read 1" }
            assertEquals(0, cancellations.get())
            assertEquals(2, reads.get())
            model.reload()
            await { reads.get() == 3 }
            assertEquals("Read 1", model.state.value.target?.title)
            assertFalse(model.state.value.initialLoading)
            gates[2].complete(Unit)
            await { !model.state.value.loading && model.state.value.target?.title == "Read 2" }
        } finally { gates.forEach { it.complete(Unit) }; owner.clear() }
    }

    @Test fun unselectedModeStillReturnsCanonicalAdviceForActualSubstitutedExerciseAndTarget() = native {
        for (day in 2..4) migrator.saveWorkoutJsonToRoom(history("valid-$day", day))
        val frame = frame(); val before = frame.toString()
        val row = read(frame)
        assertEquals("coach-press", row.id); assertEquals("Actual press", row.name)
        assertEquals("8-12", row.advice.target.text)
        assertEquals("increase", row.advice.action); assertEquals("stable", row.advice.reason)
        assertEquals(3, row.advice.historyCount); assertTrue(row.advice.weight > 20)
        assertNull(row.modePreview); assertFalse(row.advice.autoApply)
        assertEquals(before, frame.toString())
        assertEquals(0, frame.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(0).getInt("weight"))
    }

    @Test fun quickWorkoutNeedsNoProgramOrDayAndUsesOnlyComparableQuickHistory() = native {
        for (day in 2..4) migrator.saveWorkoutJsonToRoom(history("quick-$day", day, quick = true))
        migrator.saveWorkoutJsonToRoom(history("program-history"))
        val row = read(frame(quick = true))
        assertEquals(3, row.advice.historyCount); assertEquals("increase", row.advice.action)
        assertFalse(row.advice.autoApply)
    }

    @Test fun noHistoryIsCanonicalStartWhileWrongIdentityAndMissingDayAreErrors() = native {
        val row = read()
        assertEquals(0, row.advice.historyCount); assertEquals("start", row.advice.action)
        for (bad in listOf(frame().put("dayId", "missing"), frame().put("programId", "missing"),
            frame().apply { getJSONArray("exercises").getJSONObject(0).put("id", "different") })) {
            try { read(bad); fail("Invalid context must not look like empty history") }
            catch (_: IllegalArgumentException) { }
        }
    }

    @Test fun legacyDatesAndPersistedCompletionAreReadWithoutRewritingOriginalValues() = native {
        val rows = listOf(
            history("date", 2, done = null).put("started", "2026-11-02").put("finished", "2026-11-02"),
            history("local", 3, done = null).put("started", "2026-11-03T09:00:00").put("finished", "2026-11-03T10:00:00"),
            // The old importer falls back to the valid start epoch, outside the 90-day window.
            history("fallback", 4, done = null).put("started", "2026-06-01T09:00:00Z").put("finished", "2026-11-04T10:00:00")
        )
        for (raw in rows) migrator.saveWorkoutJsonToRoom(raw)
        val originals = rows.associate { raw -> raw.getString("id") to
            db.workoutHistoryDao().getWorkoutWithDetailsById(raw.getString("id"))!!.workout.rawJson }
        assertEquals(0L, db.workoutHistoryDao().getWorkoutWithDetailsById("date")!!.workout.finishedEpochMs)
        val row = read()
        assertEquals(3, row.advice.historyCount); assertEquals("increase", row.advice.action)
        for ((id, original) in originals) {
            val saved = db.workoutHistoryDao().getWorkoutWithDetailsById(id)!!
            assertEquals(original, saved.workout.rawJson)
            val projected = NativeCoachHistoryProjection(db).read(saved.workout)
                .getJSONArray("exercises").getJSONObject(0).getJSONArray("sets")
            assertTrue(projected.getJSONObject(0).getBoolean("done"))
            assertEquals(0, projected.getJSONObject(0).getInt("actualRir"))
            assertFalse(projected.getJSONObject(1).has("actualRir"))
            assertFalse(JSONObject(original).getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(0).has("done"))
        }
    }

    @Test fun explicitIncompleteSetStaysIncompleteAndMissingMeasurementsStayMissing() = native {
        val raw = history("partial", done = null)
        val sets = raw.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets")
        sets.getJSONObject(0).put("done", false)
        sets.getJSONObject(1).remove("weight"); sets.getJSONObject(1).remove("reps")
        migrator.saveWorkoutJsonToRoom(raw)
        val saved = db.workoutHistoryDao().getWorkoutWithDetailsById("partial")!!
        val projected = NativeCoachHistoryProjection(db).read(saved.workout)
            .getJSONArray("exercises").getJSONObject(0).getJSONArray("sets")
        assertFalse(projected.getJSONObject(0).getBoolean("done"))
        assertTrue(projected.getJSONObject(1).getBoolean("done"))
        assertFalse(projected.getJSONObject(1).has("weight")); assertFalse(projected.getJSONObject(1).has("reps"))
        assertEquals("partial", read().advice.reason)
    }

    @Test fun completionProjectionRequiresThePersistedExerciseAndSetIdentity() = native {
        val raw = history("identity", done = null)
        // The legacy no-setId form is matched only within its exercise/order and set number.
        raw.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(1).remove("setId")
        migrator.saveWorkoutJsonToRoom(raw)
        val stored = db.workoutHistoryDao().getWorkoutWithDetailsById("identity")!!.workout
        val projection = NativeCoachHistoryProjection(db)
        val projected = projection.read(stored).getJSONArray("exercises").getJSONObject(0).getJSONArray("sets")
        assertTrue(projected.getJSONObject(1).getBoolean("done"))
        val changed = JSONObject(stored.rawJson)
        val exercise = changed.getJSONArray("exercises").getJSONObject(0)
        exercise.getJSONArray("sets").getJSONObject(0).put("setId", "different")
        val mismatched = projection.read(stored.copy(rawJson = changed.toString()))
        assertFalse(mismatched.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(0).has("done"))
        exercise.put("id", "different-exercise")
        val otherExercise = projection.read(stored.copy(rawJson = changed.toString()))
        assertFalse(otherExercise.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(1).has("done"))
    }

    @Test fun keysetPagesSkipUnrelatedHistoryAndTombstonesAndSortLegacyByRealDate() = native {
        for (i in 0..34) migrator.saveWorkoutJsonToRoom(history("zz-unrelated-$i")
            .put("programId", "other-program"))
        for (day in 2..4) migrator.saveWorkoutJsonToRoom(history("aa-valid-$day", day))
        val legacy = history("legacy-newest").put("started", "2026-11-05T09:00:00").put("finished", "2026-11-05T10:00:00")
        migrator.saveWorkoutJsonToRoom(legacy)
        val tombstoned = history("deleted").put("started", "2026-11-05").put("finished", "2026-11-05")
        migrator.saveWorkoutJsonToRoom(tombstoned)
        db.workoutHistoryDao().upsertTombstone(HistoryTombstoneEntity("deleted", "deleted", "[\"deleted\"]", 1))
        val lib = JSONObject().put("id", "coach-press").put("loadType", "total").put("repUnit", "reps")
        val rows = NativeCoachAdviceRepository(db).recent("coach-press", "coach-program", lib, JSONObject(), JSONObject(), now)
        assertEquals(listOf("legacy-newest", "aa-valid-4", "aa-valid-3", "aa-valid-2"), rows.map { it.first.getString("id") })
        assertEquals(4, read().advice.historyCount)
    }

    @Test fun painAndMissingVersusZeroRirStillUseTheCanonicalGuards() = native {
        migrator.saveWorkoutJsonToRoom(history("indexed-good", day = 3))
        migrator.saveWorkoutJsonToRoom(history("legacy-pain", effort = "pain")
            .put("started", "2026-11-04T09:00:00").put("finished", "2026-11-04T10:00:00"))
        assertEquals("pain", read().advice.action)
        val explicit = JSONObject().put("schemaVersion", 1).put("explicitChoice", true)
            .put("defaults", JSONObject().put("trainingStyle", "normal").put("progressionMode", "double"))
        val modeAdvice = read(settings = prefs.copy(trainingModes197Json = explicit.toString()))
        assertEquals("hold", modeAdvice.advice.action)
        assertTrue(modeAdvice.advice.reason.endsWith(":pain"))
        assertFalse(modeAdvice.advice.autoApply)
        val exercise = history("rir").getJSONArray("exercises").getJSONObject(0)
        assertEquals(0.0, NativeTrainingModeProgression.observedRir(exercise)!!, 0.0)
        exercise.getJSONArray("sets").getJSONObject(0).remove("actualRir")
        assertNull(NativeTrainingModeProgression.observedRir(exercise))
    }

    @Test fun readOnlyTargetIgnoresTicksAndTracksExercisePrescriptionAndSessionIdentity() {
        val frame = frame(); val key = WorkoutCoachAdviceViewModel.targetFrame(frame)
        frame.put("executionRevision", 999).put("elapsedSeconds", 123).put("stopwatchTime", 999)
        assertEquals(key, WorkoutCoachAdviceViewModel.targetFrame(frame))
        frame.getJSONArray("exercises").getJSONObject(0).getJSONObject("prescription").put("reps", "6-8")
        assertNotEquals(key, WorkoutCoachAdviceViewModel.targetFrame(frame))
        val changedTarget = WorkoutCoachAdviceViewModel.targetFrame(frame)
        frame.put("generation", 4)
        assertNotEquals(changedTarget, WorkoutCoachAdviceViewModel.targetFrame(frame))
        frame.put("active", false)
        assertNull(WorkoutCoachAdviceViewModel.targetFrame(frame))
    }

    @Test fun observerLoadsAdviceWithoutExplicitModeAndRefreshesSameCountHistoryEdit() {
        native { TrainPilotSettingsStore(context).importPortableSettings(JSONObject()
            .put("settings", JSONObject().put("trainingModes197", JSONObject.NULL))) }
        ActiveWorkoutStore.saveOwned(context, frame())
        val owner = ViewModelStore()
        val model = WorkoutCoachAdviceViewModel(context as Application, db)
        owner.put("coach", model)
        fun await(condition: () -> Boolean) {
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
            while (!condition()) {
                shadowOf(Looper.getMainLooper()).idle()
                check(System.nanoTime() < deadline) { "Coach observer timed out: ${model.state.value}" }
                Thread.sleep(5)
            }
        }
        try {
            model.selectExercise("coach-press")
            await { !model.state.value.loading && model.state.value.row?.advice?.action == "start" }
            val raw = history("live-edit").put("started", Instant.now().minusSeconds(7200).toString())
                .put("finished", Instant.now().minusSeconds(3600).toString())
            native { migrator.saveWorkoutJsonToRoom(raw) }
            await { model.state.value.row?.advice?.action == "increase" }
            assertNull(model.state.value.row!!.modePreview)
            assertEquals(1, model.state.value.row!!.advice.historyCount)
            // Same ID and row count: a journal edit must invalidate advice without manual reload.
            val sets = raw.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets")
            for (index in 0 until sets.length()) sets.getJSONObject(index).put("reps", "9")
            native { migrator.saveWorkoutJsonToRoom(raw) }
            await { model.state.value.row?.advice?.action == "reps" }
            assertEquals(listOf(10.0, 10.0), model.state.value.row!!.advice.nextReps)
            assertEquals(1, native { db.workoutHistoryDao().getWorkoutCount() })
            assertEquals(1, model.state.value.row!!.advice.historyCount)
        } finally {
            owner.clear()
            ActiveWorkoutStore.clear(context)
            shadowOf(Looper.getMainLooper()).idle()
        }
    }

    @Test fun readonlyAdviceDoesNotRelaxApprovalOwnershipOrTouchedSetGuards() {
        val frame = frame().put("timerOwner", "phone").put("nativeEditorProjection", 1)
        val ticket = requireNotNull(NativeTrainingModeProposal.ticket(frame, "coach-press", 22.5, "source", null))
        assertTrue(NativeTrainingModeProposal.permitted(frame, ticket))
        frame.put("timerOwner", "watch")
        assertFalse(NativeTrainingModeProposal.permitted(frame, ticket))
        frame.put("timerOwner", "phone")
        frame.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(0).put("actualRir", 0)
        assertFalse(NativeTrainingModeProposal.permitted(frame, ticket))
    }
}
