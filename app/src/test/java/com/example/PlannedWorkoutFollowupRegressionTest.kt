package com.example

import android.content.Context
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.model.AppLanguage
import com.example.ui.viewmodel.TrainPilotViewModel
import com.repforge.app.ActiveWorkoutStore
import com.repforge.app.PhoneWorkoutExecution
import com.repforge.app.PhoneWorkoutExecutionStore
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.migration.LegacyWebViewMigrator
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.*
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.time.LocalDate
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.PAUSED)
class PlannedWorkoutFollowupRegressionTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: TrainPilotDatabase
    private lateinit var settings: TrainPilotSettingsStore
    private val parent = SupervisorJob()
    private val scope = CoroutineScope(parent + Dispatchers.Main.immediate)

    @Before fun prepare() {
        val execution = PhoneWorkoutExecution::class.java.getDeclaredField("store").apply { isAccessible = true }
        (execution.get(null) as? PhoneWorkoutExecutionStore)?.close(); execution.set(null, null)
        val field = TrainPilotDatabase::class.java.getDeclaredField("INSTANCE").apply { isAccessible = true }
        (field.get(null) as? TrainPilotDatabase)?.close()
        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
        field.set(null, db)
        settings = TrainPilotSettingsStore(context)
        native { settings.setActiveProgramId("home-basic") }
    }

    @After fun close() {
        parent.cancel(); await { parent.isCompleted }; db.close()
        TrainPilotDatabase::class.java.getDeclaredField("INSTANCE").apply { isAccessible = true }.set(null, null)
    }

    private fun model() = TrainPilotViewModel().also {
        it.bindNativeEnvironment(context, settings, db, scope)
        await { !it.uiState.value.savedRecordsLoading }
    }

    @Test fun homeFollowsReplannedBAndImmediateStartUsesTheSameCommittedSchedule() {
        val model = model()
        model.updatePlanningParameters(startDate = LocalDate.now().toString(), weeksCount = 1)
        model.applyCalendarReplan(LocalDate.now().toString(), "B nap", "home-basic")
        // No artificial wait before pressing Start: pending calendar/program writes are joined.
        model.startPlannedWorkout()
        await { model.uiState.value.isWorkoutSessionActive }
        val frame = ActiveWorkoutStore.current(context)!!
        assertEquals("home-basic", frame.getString("programId"))
        assertEquals("b", frame.getString("dayId"))
        val id = frame.getString("scheduleId")
        assertTrue(id.isNotBlank())
        await { model.uiState.value.homeWorkoutTarget?.scheduleId == id }
        assertEquals("b", model.uiState.value.homeWorkoutTarget?.dayId)
        assertEquals("b", native { db.calendarScheduleDao().getEntry(id) }!!.dayId)
    }

    @Test fun replanningAnotherProgramDoesNotReassignRetainedRowsAndSurvivesRebind() {
        val model = model()
        val start = LocalDate.now()
        model.updatePlanningParameters(startDate = start.toString(), weeksCount = 1)
        model.applyCalendarReplan(start.toString(), "A nap", "home-basic")
        val retainedId = model.uiState.value.calendarEvents.first().id
        await { model.uiState.value.homeWorkoutTarget?.scheduleId == retainedId }
        val before = native { db.calendarScheduleDao().getEntry(retainedId) }!!
        val other = model.uiState.value.programs.first { it.id != "home-basic" && it.days.size >= 2 }
        model.applyCalendarReplan(start.plusWeeks(2).toString(), "B nap", other.id)
        val newId = model.uiState.value.calendarEvents.first { it.programId == other.id }.id
        await { native { db.calendarScheduleDao().getEntry(newId) } != null }
        assertEquals(before.programId, native { db.calendarScheduleDao().getEntry(retainedId) }!!.programId)
        assertEquals(before.dayId, native { db.calendarScheduleDao().getEntry(retainedId) }!!.dayId)
        model.bindNativeEnvironment(context, settings, db, scope)
        await { model.uiState.value.calendarEvents.any { it.id == newId && it.programId == other.id } }
        assertEquals("home-basic", model.uiState.value.calendarEvents.single { it.id == retainedId }.programId)
    }

    @Test fun journalProjectionKeepsTheSavedDayEvenWhenTheActiveProgramDiffers() {
        native {
            LegacyWebViewMigrator(context, db, settings).saveWorkoutJsonToRoom(JSONObject()
                .put("id", "saved-b").put("key", "saved-b")
                .put("started", "2026-11-01T10:00:00Z").put("finished", "2026-11-01T10:30:00Z")
                .put("programId", "home-basic").put("programName", "Otthoni A/B – Alap")
                .put("dayId", "b").put("dayName", "B").put("quickWorkout", false))
        }
        val model = model()
        val entry = model.uiState.value.loggedWorkouts.single { it.id == "saved-b" }
        assertEquals("home-basic", entry.programId)
        assertEquals("b", entry.dayId)
        assertEquals("Otthoni A/B • B nap", entry.localizedTitle(AppLanguage.HU))
        assertEquals("Home A/B • Day B", entry.localizedTitle(AppLanguage.EN))
    }

    private fun await(condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        while (true) {
            shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            check(System.nanoTime() < deadline) { "Planned workout operation timed out" }
            Thread.sleep(5)
        }
    }
    private fun <T> native(block: suspend () -> T): T {
        val worker = Executors.newSingleThreadExecutor()
        val future = worker.submit<T> { runBlocking { block() } }
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        try { while (true) {
            shadowOf(Looper.getMainLooper()).idle()
            try { return future.get(10, TimeUnit.MILLISECONDS) }
            catch (_: TimeoutException) { check(System.nanoTime() < deadline) }
        } } finally { future.cancel(true); worker.shutdownNow() }
    }
}
