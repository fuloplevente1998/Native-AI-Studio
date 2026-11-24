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
import kotlinx.coroutines.sync.Mutex
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
import java.util.concurrent.ConcurrentLinkedQueue
import com.example.ui.viewmodel.addCustomExercise
import com.example.ui.viewmodel.clearBannerMessage
import com.example.ui.viewmodel.createNewCustomProgram
import com.example.ui.viewmodel.customExerciseProgramTargetLabel
import com.example.ui.viewmodel.deleteStudioCustomExercise
import com.example.ui.viewmodel.openProgramEditor
import com.example.ui.viewmodel.toggleAddCustomExerciseDialog

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
    private fun assertFailure(message: String?) {
        val text = message.orEmpty().lowercase()
        assertTrue("Expected an error banner, got: $message",
            listOf("nem sikerült", "nem menthető", "hiba", "nem elérhető").any { it in text })
    }
    private data class MutationJobs(val causes: ConcurrentLinkedQueue<String>, val count: Int)
    private fun startMutation(vm: TrainPilotViewModel, action: () -> Unit): MutationJobs {
        vm.clearBannerMessage()
        assertTrue("Parent scope is inactive before mutation", parent.isActive)
        val previous = parent.children.toSet()
        val causes = ConcurrentLinkedQueue<String>()
        action()
        val jobs = parent.children.toSet() - previous
        jobs.forEach { job ->
            job.invokeOnCompletion { cause ->
                causes.add(cause?.stackTraceToString() ?: "Completed normally")
            }
        }
        if (!vm.uiState.value.libraryMutationBusy && vm.uiState.value.statusBannerMessage == null) {
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
            while (causes.size < jobs.size && System.nanoTime() < deadline) {
                shadowOf(Looper.getMainLooper()).idle()
                Thread.sleep(5)
            }
        }
        assertTrue("Mutation was not admitted: busy=${vm.uiState.value.libraryMutationBusy}, " +
            "banner=${vm.uiState.value.statusBannerMessage}, job causes=$causes",
            vm.uiState.value.libraryMutationBusy || vm.uiState.value.statusBannerMessage != null)
        return MutationJobs(causes, jobs.size)
    }
    private fun awaitMutation(vm: TrainPilotViewModel, captured: MutationJobs) {
        val causes = captured.causes
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        while (vm.uiState.value.libraryMutationBusy) {
            shadowOf(Looper.getMainLooper()).idle()
            check(System.nanoTime() < deadline) {
                "Mutation did not report an outcome: busy=${vm.uiState.value.libraryMutationBusy}, " +
                    "banner=${vm.uiState.value.statusBannerMessage}, scopeActive=${parent.isActive}, job causes=$causes"
            }
            Thread.sleep(5)
        }
        while (causes.size < captured.count && System.nanoTime() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(5)
        }
        assertNotNull("Mutation finished without an outcome; scopeActive=${parent.isActive}, job causes=$causes",
            vm.uiState.value.statusBannerMessage)
    }

    @Test fun exerciseIsCommittedToSelectedDayAndSurvivesReload() {
        val vm = model()
        vm.openProgramEditor("audit-program", 1)
        vm.toggleAddCustomExerciseDialog(true, fromProgramEditor = true)
        assertTrue(vm.customExerciseProgramTargetLabel().orEmpty().endsWith(" · B nap"))
        val before = vm.uiState.value.exercises
        val causes = startMutation(vm) { add(vm, "Audit linked exercise", true) }
        assertEquals(before, vm.uiState.value.exercises)
        assertTrue(vm.uiState.value.showAddCustomExerciseDialog)
        awaitMutation(vm, causes)
        assertFalse("Expected committed exercise; banner=${vm.uiState.value.statusBannerMessage}", vm.uiState.value.showAddCustomExerciseDialog)
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
        vm.toggleAddCustomExerciseDialog(true)
        assertNull(vm.customExerciseProgramTargetLabel())
        val causes = startMutation(vm) {
            vm.addCustomExercise("Audit standalone exercise", ExerciseCategory.CORE, "bodyweight", "Instructions")
        }
        awaitMutation(vm, causes)
        val exercise = vm.uiState.value.exercises.single { it.nameHu == "Audit standalone exercise" }
        assertEquals(before, native { db.programDao().getProgramWithDaysById("audit-program")!! })
        awaitMutation(vm, startMutation(vm) { vm.deleteStudioCustomExercise(exercise.id) })
        assertNull(native { db.exerciseLibraryDao().getExerciseById(exercise.id) })
        assertFalse(vm.uiState.value.exercises.any { it.id == exercise.id })
    }

    @Test fun createProgramAppearsOnlyAfterCommitAndReload() {
        val vm = model()
        val before = vm.uiState.value.programs
        val causes = startMutation(vm) { vm.createNewCustomProgram("Audit created program", "Description") }
        assertEquals(before, vm.uiState.value.programs)
        awaitMutation(vm, causes)
        val created = requireNotNull(vm.uiState.value.programs.singleOrNull { it.nameHu == "Audit created program" }) {
            "Expected committed program; banner=${vm.uiState.value.statusBannerMessage}"
        }
        val saved = native { db.programDao().getProgramWithDaysById(created.id)!! }
        assertEquals(listOf("A", "B"), saved.days.sortedBy { it.sortOrder }.map { it.dayId })
        assertEquals(created.id, native { settings.current().activeProgramId })
        assertTrue(model().uiState.value.programs.any { it.id == created.id })
    }

    @Test fun closedDatabaseKeepsDialogAndCatalogVisible() {
        val vm = model()
        vm.toggleAddCustomExerciseDialog(true)
        val before = vm.uiState.value
        db.close()
        val exerciseCauses = startMutation(vm) { add(vm, "Cannot save exercise", false) }
        awaitMutation(vm, exerciseCauses)
        assertTrue(vm.uiState.value.showAddCustomExerciseDialog)
        assertEquals(before.exercises, vm.uiState.value.exercises)
        assertEquals(before.programs, vm.uiState.value.programs)
        assertFailure(vm.uiState.value.statusBannerMessage)
        val programCauses = startMutation(vm) { vm.createNewCustomProgram("Cannot save program", "Description") }
        awaitMutation(vm, programCauses)
        assertEquals(before.programs, vm.uiState.value.programs)
        assertEquals(before.editingProgramId, vm.uiState.value.editingProgramId)
        assertFailure(vm.uiState.value.statusBannerMessage)
    }

    @Test fun cancelledCallerCannotCommitAQueuedMutationOrPublishAnError() {
        val vm = model()
        vm.openProgramEditor("audit-program", 1)
        vm.toggleAddCustomExerciseDialog(true, fromProgramEditor = true)
        val before = vm.uiState.value
        val programBefore = native { db.programDao().getProgramWithDaysById("audit-program")!! }
        val mutex = TrainPilotViewModel::class.java.getDeclaredField("programWriteMutex")
            .apply { isAccessible = true }.get(vm) as Mutex
        runBlocking { mutex.lock() }
        try {
            startMutation(vm) { add(vm, "Cancelled linked exercise", true) }
            assertTrue(vm.uiState.value.libraryMutationBusy)
            parent.cancel()
            await { parent.isCompleted }
            assertFalse(vm.uiState.value.libraryMutationBusy)
            assertNull(vm.uiState.value.statusBannerMessage)
            assertTrue(vm.uiState.value.showAddCustomExerciseDialog)
            assertEquals(before.exercises, vm.uiState.value.exercises)
            assertEquals(before.programs, vm.uiState.value.programs)
            assertFalse(native { db.exerciseLibraryDao().getAllExercises() }
                .any { it.nameHu == "Cancelled linked exercise" })
            assertEquals(programBefore, native { db.programDao().getProgramWithDaysById("audit-program")!! })
        } finally { mutex.unlock() }
    }

    @Test fun unavailableDatabaseDoesNotPublishSuccess() {
        val vm = TrainPilotViewModel()
        vm.toggleAddCustomExerciseDialog(true)
        val before = vm.uiState.value
        val exerciseCauses = startMutation(vm) { add(vm, "Offline exercise", false) }
        awaitMutation(vm, exerciseCauses)
        assertEquals(before.exercises, vm.uiState.value.exercises)
        assertTrue(vm.uiState.value.showAddCustomExerciseDialog)
        assertFailure(vm.uiState.value.statusBannerMessage)
        val programCauses = startMutation(vm) { vm.createNewCustomProgram("Offline program", "Description") }
        awaitMutation(vm, programCauses)
        assertEquals(before.programs, vm.uiState.value.programs)
        assertEquals(before.editingProgramId, vm.uiState.value.editingProgramId)
        assertEquals(before.currentSubScreen, vm.uiState.value.currentSubScreen)
        assertFailure(vm.uiState.value.statusBannerMessage)
    }
}
