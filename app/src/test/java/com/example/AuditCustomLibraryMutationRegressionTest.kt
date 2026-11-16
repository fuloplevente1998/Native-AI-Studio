package com.example

import android.content.Context
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.repforge.app.*
import com.repforge.app.data.db.*
import com.repforge.app.data.store.TrainPilotSettingsStore
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
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.PAUSED)
class AuditCustomLibraryMutationRegressionTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: TrainPilotDatabase
    private lateinit var settings: TrainPilotSettingsStore
    private lateinit var exercise: ExerciseLibraryEntity

    @Before fun setup() {
        val field = PhoneWorkoutExecution::class.java.getDeclaredField("store").apply { isAccessible = true }
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close(); field.set(null, null)
        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
        settings = TrainPilotSettingsStore(context)
        exercise = TrainPilotDatabase.defaultExercises().first().copy(id = "custom-test", custom = true)
        native {
            db.exerciseLibraryDao().upsertExercise(exercise.copy(id = "old"))
            val template = TrainPilotDatabase.defaultPrograms().first.first()
            val program = template.copy(id = "test-program", builtin = false,
                rawJson = JSONObject().put("id", "test-program").put("prescriptions", JSONObject().put("old", JSONObject().put("sets", 4))).toString())
            val day = ProgramDayEntity("test-program:A", "test-program", "A", "A", "A", "A", "A", 0,
                JSONArray().put(JSONObject().put("id", "old").put("sets", 5).put("custom", "keep")).toString())
            db.programDao().saveProgramWithDays(program, listOf(day))
        }
    }
    @After fun close() { db.close() }

    private fun <T> native(operation: suspend () -> T): T {
        val worker = Executors.newSingleThreadExecutor()
        val future = worker.submit<T> { runBlocking { operation() } }
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        try {
            while (true) {
                shadowOf(Looper.getMainLooper()).idle()
                try { return future.get(10, TimeUnit.MILLISECONDS) }
                catch (_: TimeoutException) { check(System.nanoTime() < deadline) }
            }
        } finally { future.cancel(true); worker.shutdownNow() }
    }
    private fun ticket() = NativeDataReplacement.continuationForCurrentData(context)

    @Test fun addsRowAndLinkWithoutChangingInlinePrescription() {
        val before = native { db.programDao().getProgramWithDaysById("test-program")!! }
        val id = native { NativeCustomLibraryMutation.addExercise(context, db, exercise, "test-program", "test-program:A", "hu", ticket()) }
        val after = native { db.programDao().getProgramWithDaysById(id!!)!! }
        assertEquals(exercise, native { db.exerciseLibraryDao().getExerciseById(exercise.id) })
        val oldEntry = JSONArray(before.days.single().exercisesJson).getJSONObject(0)
        val rows = JSONArray(after.days.single().exercisesJson)
        assertEquals(oldEntry.toString(), rows.getJSONObject(0).toString())
        assertEquals(exercise.id, rows.getString(1))
        assertEquals(JSONObject(before.program.rawJson).getJSONObject("prescriptions").toString(),
            JSONObject(after.program.rawJson).getJSONObject("prescriptions").toString())
    }

    @Test fun missingProgramOrDayRollsBackExercise() {
        for ((program, day) in listOf("missing" to "A", "test-program" to "missing")) {
            try {
                native { NativeCustomLibraryMutation.addExercise(context, db, exercise, program, day, "hu", ticket()) }
                fail("Expected invalid target to fail")
            } catch (_: java.util.concurrent.ExecutionException) { }
            assertNull(native { db.exerciseLibraryDao().getExerciseById(exercise.id) })
        }
    }

    @Test fun staleDatasetCannotWrite() {
        val stale = NativeDataReplacement.Continuation(-1)
        try {
            native { NativeCustomLibraryMutation.addExercise(context, db, exercise, null, null, "hu", stale) }
            fail("Expected stale continuation to fail")
        } catch (_: java.util.concurrent.ExecutionException) { }
        assertNull(native { db.exerciseLibraryDao().getExerciseById(exercise.id) })
    }

    @Test fun invalidExistingEntryRollsBackExerciseInsertedEarlierInTransaction() {
        native {
            val opened = db.programDao().getProgramWithDaysById("test-program")!!
            val invalid = opened.days.single().copy(exercisesJson = JSONArray().put(JSONObject()).toString())
            db.programDao().saveProgramWithDays(opened.program, listOf(invalid))
        }
        try {
            native { NativeCustomLibraryMutation.addExercise(context, db, exercise, "test-program", "A", "hu", ticket()) }
            fail("Expected invalid existing entry to fail")
        } catch (_: java.util.concurrent.ExecutionException) { }
        assertNull(native { db.exerciseLibraryDao().getExerciseById(exercise.id) })
        val saved = native { db.programDao().getProgramWithDaysById("test-program")!! }
        assertEquals("[{}]", saved.days.single().exercisesJson)
    }

    @Test fun createdProgramPersistsBothDaysAndActivatesAfterSave() {
        val second = exercise.copy(id = "custom-second")
        native { db.exerciseLibraryDao().upsertExercises(listOf(exercise, second)) }
        val id = native { NativeCustomLibraryMutation.createProgram(context, db, settings, "My program", "Description", "en", listOf(exercise.id), listOf(second.id), ticket()) }
        val saved = native { db.programDao().getProgramWithDaysById(id)!! }
        assertEquals(listOf("A", "B"), saved.days.sortedBy { it.sortOrder }.map { it.dayId })
        assertEquals(listOf(exercise.id, second.id), saved.days.sortedBy { it.sortOrder }.map { JSONArray(it.exercisesJson).getString(0) })
        assertEquals(id, native { settings.current().activeProgramId })
    }
}
