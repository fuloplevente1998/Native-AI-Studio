package com.example

import android.content.Context
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.ui.viewmodel.TrainPilotViewModel
import com.repforge.app.PhoneWorkoutExecution
import com.repforge.app.PhoneWorkoutExecutionStore
import com.repforge.app.data.db.CalendarScheduleEntity
import com.repforge.app.data.db.ProgramWithDays
import com.repforge.app.data.db.TrainPilotDatabase
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
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import com.example.ui.viewmodel.copyEditorProgram
import com.example.ui.viewmodel.openProgramEditor
import com.example.ui.viewmodel.removeEditorExercise
import com.example.ui.viewmodel.saveEditorProgram

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.PAUSED)
class EditedProgramSaveRegressionTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: TrainPilotDatabase
    private lateinit var settings: TrainPilotSettingsStore
    private val parent = SupervisorJob()
    private val scope = CoroutineScope(parent + Dispatchers.Main.immediate)

    @Before fun prepare() {
        val execution = PhoneWorkoutExecution::class.java.getDeclaredField("store").apply { isAccessible = true }
        (execution.get(null) as? PhoneWorkoutExecutionStore)?.close()
        execution.set(null, null)
        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
        settings = TrainPilotSettingsStore(context)
        native {
            val exercises = TrainPilotDatabase.defaultExercises()
            db.exerciseLibraryDao().upsertExercises(exercises)
            val (programs, days) = TrainPilotDatabase.defaultPrograms()
            for (program in programs) {
                val programDays = days.filter { it.programId == program.id }.map { day ->
                    // Reproduce the reported ten-exercise day while retaining the real
                    // Home A/B program identity and seven original exercise references.
                    if (program.id != "home-basic" || day.dayId != "A") day else {
                        val original = exerciseIds(day.exercisesJson)
                        val extra = exercises.map { it.id }.filterNot { it in original }.take(3)
                        day.copy(exercisesJson = JSONArray(original + extra).toString())
                    }
                }
                db.programDao().saveProgramWithDays(program, programDays)
            }
            settings.setActiveProgramId("home-basic")
        }
    }

    @After fun close() {
        parent.cancel()
        await { parent.isCompleted }
        db.close()
    }

    @Test fun removingLastThreeAndSavingKeepsOneActiveCustomCopyAcrossReopening() {
        val vm = model()
        // 2779: the three appended exercises of the reported ten-exercise day are trimmed at start.
        await { exerciseIds(program("home-basic").days.single { it.dayId == "A" }.exercisesJson).size == 7 }
        val original = program("home-basic")
        val ids = exerciseIds(original.days.single { it.dayId == "A" }.exercisesJson)
        assertEquals(7, ids.size)
        val beforeIds = native { db.programDao().getProgramIds().toSet() }
        vm.openProgramEditor("home-basic")
        ids.takeLast(3).forEach { id -> mutate(vm) { vm.removeEditorExercise(id) } }
        val savedId = vm.uiState.value.editingProgramId
        assertNotEquals("home-basic", savedId)
        assertFalse(program(savedId).program.builtin)
        assertEquals(savedId, native { settings.current().activeProgramId })
        mutate(vm) { vm.saveEditorProgram("Otthoni A/B", emptyList()) }
        assertEquals(savedId, vm.uiState.value.editingProgramId)
        assertEquals(ids.dropLast(3), exerciseIds(program(savedId).days.single { it.dayId == "A" }.exercisesJson))
        assertEquals(original, program("home-basic"))

        val reopened = model()
        val active = reopened.uiState.value.programs.single { it.isActive }
        assertEquals(savedId, active.id)
        assertEquals(ids.dropLast(3), active.days.first().exercises.map { it.id })
        await { reopened.uiState.value.homeWorkoutTarget?.programId == savedId }
        reopened.openPlannedProgramDetails()
        assertEquals(savedId, reopened.uiState.value.editingProgramId)
        mutate(reopened) { reopened.saveEditorProgram("Otthoni A/B módosítva", emptyList()) }
        assertEquals(savedId, reopened.uiState.value.editingProgramId)
        assertEquals(beforeIds + savedId, native { db.programDao().getProgramIds().toSet() })
        assertEquals(savedId, native { settings.current().activeProgramId })
        assertEquals(original, program("home-basic"))
    }

    @Test fun firstSaveWithoutExerciseRemovalActivatesOneCopyAndProgramCardReopensIt() {
        val vm = model()
        val original = program("home-basic")
        val beforeIds = native { db.programDao().getProgramIds().toSet() }
        vm.openProgramEditor("home-basic")
        mutate(vm) { vm.saveEditorProgram("Otthoni A/B saját", emptyList()) }
        val savedId = vm.uiState.value.editingProgramId
        assertNotEquals("home-basic", savedId)
        assertEquals(savedId, native { settings.current().activeProgramId })
        val customCard = vm.uiState.value.programs.single { it.id == savedId }
        assertTrue(customCard.isCustom)
        assertTrue(customCard.isActive)
        vm.openProgramEditor(customCard.id)
        mutate(vm) { vm.saveEditorProgram("Otthoni A/B új név", emptyList()) }
        assertEquals(savedId, vm.uiState.value.editingProgramId)
        assertEquals(beforeIds + savedId, native { db.programDao().getProgramIds().toSet() })
        assertEquals(original, program("home-basic"))
    }

    @Test fun homeKeepsAnExistingCalendarTargetWhenAnotherProgramBecomesActive() {
        val start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
        val scheduled = CalendarScheduleEntity("existing-schedule", "", LocalDate.now().toString(),
            start.toString(), start.plusSeconds(3600).toString(), "gym-ab", "Gym A/B", "B", "B nap",
            "planned", System.currentTimeMillis(), "{}")
        native { db.calendarScheduleDao().upsertEntry(scheduled) }
        val vm = model()
        await { vm.uiState.value.homeWorkoutTarget?.scheduleId == scheduled.id }
        vm.openProgramEditor("home-basic")
        val removed = exerciseIds(program("home-basic").days.first().exercisesJson).last()
        mutate(vm) { vm.removeEditorExercise(removed) }
        val savedId = vm.uiState.value.editingProgramId
        assertNotEquals("home-basic", savedId)
        assertEquals(savedId, native { settings.current().activeProgramId })
        assertEquals(scheduled, native { db.calendarScheduleDao().getEntry(scheduled.id) })
        vm.openPlannedProgramDetails()
        assertEquals(scheduled.programId, vm.uiState.value.editingProgramId)
        assertEquals(1, vm.uiState.value.editingDayIndex)
    }

    @Test fun editingInactiveTemplatePreservesUnrelatedActiveProgram() {
        native { settings.setActiveProgramId("gym-ab") }
        val vm = model()
        val original = program("home-basic")
        vm.openProgramEditor("home-basic")
        val id = exerciseIds(original.days.first().exercisesJson).last()
        mutate(vm) { vm.removeEditorExercise(id) }
        val savedId = vm.uiState.value.editingProgramId
        assertNotEquals("home-basic", savedId)
        mutate(vm) { vm.saveEditorProgram("Otthoni A/B saját", emptyList()) }
        assertEquals(savedId, vm.uiState.value.editingProgramId)
        assertEquals("gym-ab", native { settings.current().activeProgramId })
        assertEquals("gym-ab", vm.uiState.value.programs.single { it.isActive }.id)
        assertEquals(original, program("home-basic"))
    }

    @Test fun customProgramRemovalAndRepeatedSaveUpdateSameId() {
        val source = program("home-basic")
        native {
            val id = "existing-home-copy"
            db.programDao().saveProgramWithDays(source.program.copy(id = id, builtin = false,
                rawJson = JSONObject(source.program.rawJson).put("id", id).put("builtin", false).toString()),
                source.days.map { it.copy(compositeId = "$id:${it.dayId}", programId = id) })
            settings.setActiveProgramId(id)
        }
        val beforeIds = native { db.programDao().getProgramIds().toSet() }
        val vm = model()
        // 2779: the built-in day is trimmed back to its 7 stock exercises; the personal copy keeps all 10.
        await { exerciseIds(program("home-basic").days.single { it.dayId == "A" }.exercisesJson).size == 7 }
        val repairedStock = program("home-basic")
        val id = "existing-home-copy"
        val originalIds = exerciseIds(source.days.first().exercisesJson)
        vm.openProgramEditor(id)
        originalIds.takeLast(3).forEach { exercise -> mutate(vm) { vm.removeEditorExercise(exercise) } }
        repeat(2) { mutate(vm) { vm.saveEditorProgram("Otthoni A/B saját", emptyList()) } }
        assertEquals(id, vm.uiState.value.editingProgramId)
        assertEquals(id, native { settings.current().activeProgramId })
        assertEquals(beforeIds, native { db.programDao().getProgramIds().toSet() })
        assertEquals(originalIds.dropLast(3), exerciseIds(program(id).days.first().exercisesJson))
        assertEquals(10, originalIds.size)
        assertEquals(repairedStock, program("home-basic"))
    }

    @Test fun explicitCopyStillRequiresExplicitActivation() {
        val vm = model()
        val original = program("home-basic")
        vm.openProgramEditor("home-basic")
        mutate(vm) { vm.copyEditorProgram() }
        val copyId = vm.uiState.value.editingProgramId
        assertNotEquals("home-basic", copyId)
        assertFalse(program(copyId).program.builtin)
        assertEquals("home-basic", native { settings.current().activeProgramId })
        assertEquals("home-basic", vm.uiState.value.programs.single { it.isActive }.id)
        assertEquals(original, program("home-basic"))
    }

    private fun program(id: String): ProgramWithDays = native {
        requireNotNull(db.programDao().getProgramWithDaysById(id)).let { it.copy(days = it.days.sortedBy { d -> d.sortOrder }) }
    }

    private fun exerciseIds(json: String): List<String> = JSONArray(json).let { rows ->
        (0 until rows.length()).map { rows.optJSONObject(it)?.getString("id") ?: rows.getString(it) }
    }

    private fun model() = TrainPilotViewModel().also { vm ->
        vm.bindNativeEnvironment(context, settings, db, scope)
        await { !vm.uiState.value.savedRecordsLoading && vm.uiState.value.programs.any { it.id == "home-basic" } }
    }

    private fun mutate(vm: TrainPilotViewModel, operation: () -> Unit) {
        assertFalse(vm.uiState.value.libraryMutationBusy)
        vm.clearBannerMessage()
        operation()
        await { !vm.uiState.value.libraryMutationBusy && vm.uiState.value.statusBannerMessage != null }
    }

    private fun await(condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        while (!condition()) {
            shadowOf(Looper.getMainLooper()).idle()
            check(System.nanoTime() < deadline) { "Program editor operation timed out" }
            Thread.sleep(5)
        }
    }

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
        } finally {
            future.cancel(true)
            worker.shutdownNow()
        }
    }
}
