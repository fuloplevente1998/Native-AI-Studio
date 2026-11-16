package com.example

import android.content.Context
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.model.ExerciseCategory
import com.example.ui.viewmodel.TrainPilotViewModel
import com.repforge.app.PhoneWorkoutExecution
import com.repforge.app.PhoneWorkoutExecutionStore
import com.repforge.app.data.db.*
import com.repforge.app.data.store.TrainPilotSettingsStore
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
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.PAUSED)
class AuditCustomLibraryViewModelRegressionTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: TrainPilotDatabase
    private lateinit var settings: TrainPilotSettingsStore
    private val parent = SupervisorJob()
    private val scope = CoroutineScope(parent + Dispatchers.Main.immediate)

    @Before fun setup() {
        val field = PhoneWorkoutExecution::class.java.getDeclaredField("store").apply { isAccessible = true }
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close(); field.set(null, null)
        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
        settings = TrainPilotSettingsStore(context)
        native {
            val exercises = TrainPilotDatabase.defaultExercises()
            db.exerciseLibraryDao().upsertExercises(exercises)
            val template = TrainPilotDatabase.defaultPrograms().first.first()
            val program = template.copy(id = "audit-program", builtin = false,
                rawJson = JSONObject().put("id", "audit-program").toString())
            val days = listOf("A", "B").mapIndexed { i, day ->
                ProgramDayEntity("audit-program:$day", "audit-program", day, day, day, day, day, i,
                    JSONArray().put(exercises.first().id).toString())
            }
            db.programDao().saveProgramWithDays(program, days)
            settings.setActiveProgramId(program.id)
        }
    }
    @After fun close() { parent.cancel(); await { parent.isCompleted }; db.close() }

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
    private fun await(condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        while (!condition()) {
            shadowOf(Looper.getMainLooper()).idle()
            check(System.nanoTime() < deadline) { "ViewModel operation timed out" }
            Thread.sleep(5)
        }
    }
    private fun model() = TrainPilotViewModel().also {
        it.bindNativeEnvironment(context, settings, db, scope)
        await { !it.uiState.value.savedRecordsLoading && it.uiState.value.programs.any { p -> p.id == "audit-program" } }
    }
    private fun add(vm: TrainPilotViewModel, name: String, link: Boolean) {
        vm.addCustomExercise(name, ExerciseCategory.CORE, "bodyweight", "Instructions", addToCurrentProgramDay = link)
    }

    @Test fun exerciseIsCommittedToSelectedDayAndSurvivesReload() {
        val vm = model()
        vm.openProgramEditor("audit-program", 1)
        vm.toggleAddCustomExerciseDialog(true)
        val before = vm.uiState.value.exercises
        add(vm, "Audit linked exercise", true)
        assertEquals(before, vm.uiState.value.exercises)
        assertTrue(vm.uiState.value.showAddCustomExerciseDialog)
        await { !vm.uiState.value.libraryMutationBusy }
        assertFalse(vm.uiState.value.showAddCustomExerciseDialog)
        val id = vm.uiState.value.exercises.single { it.nameHu == "Audit linked exercise" }.id
        assertEquals(id, vm.uiState.value.selectedExerciseId)
        val saved = native { db.programDao().getProgramWithDaysById("audit-program")!! }
        assertFalse(saved.days.single { it.dayId == "A" }.exercisesJson.contains(id))
        assertTrue(saved.days.single { it.dayId == "B" }.exercisesJson.contains(id))
        val reloaded = model()
        val day = reloaded.uiState.value.programs.single { it.id == "audit-program" }.days[1]
        assertTrue(day.exercises.any { it.id == id })
    }

    @Test fun standaloneExerciseDoesNotChangeProgram() {
        val vm = model()
        vm.openProgramEditor("audit-program", 0)
        val before = native { db.programDao().getProgramWithDaysById("audit-program")!! }
        add(vm, "Audit standalone exercise", false)
        await { !vm.uiState.value.libraryMutationBusy }
        assertTrue(vm.uiState.value.exercises.any { it.nameHu == "Audit standalone exercise" })
        assertEquals(before, native { db.programDao().getProgramWithDaysById("audit-program")!! })
    }

    @Test fun closedDatabaseKeepsDialogAndCatalogVisible() {
        val vm = model()
        vm.toggleAddCustomExerciseDialog(true)
        val before = vm.uiState.value
        db.close()
        add(vm, "Cannot save exercise", false)
        await { !vm.uiState.value.libraryMutationBusy }
        assertTrue(vm.uiState.value.showAddCustomExerciseDialog)
        assertEquals(before.exercises, vm.uiState.value.exercises)
        assertEquals(before.programs, vm.uiState.value.programs)
        vm.createNewCustomProgram("Cannot save program", "Description")
        await { !vm.uiState.value.libraryMutationBusy }
        assertEquals(before.programs, vm.uiState.value.programs)
        assertEquals(before.editingProgramId, vm.uiState.value.editingProgramId)
    }

    @Test fun unavailableDatabaseDoesNotPublishSuccess() {
        val vm = TrainPilotViewModel()
        vm.toggleAddCustomExerciseDialog(true)
        val before = vm.uiState.value
        add(vm, "Offline exercise", false)
        assertEquals(before.exercises, vm.uiState.value.exercises)
        assertTrue(vm.uiState.value.showAddCustomExerciseDialog)
        vm.createNewCustomProgram("Offline program", "Description")
        assertEquals(before.programs, vm.uiState.value.programs)
        assertEquals(before.editingProgramId, vm.uiState.value.editingProgramId)
        assertEquals(before.currentSubScreen, vm.uiState.value.currentSubScreen)
    }
}
