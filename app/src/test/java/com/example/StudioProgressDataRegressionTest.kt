package com.example

import android.content.Context
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.ui.viewmodel.TrainPilotViewModel
import com.repforge.app.*
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.migration.LegacyWebViewMigrator
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.PAUSED)
class StudioProgressDataRegressionTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
    private val store = TrainPilotSettingsStore(context)
    private val parent = SupervisorJob()
    private val scope = CoroutineScope(parent + Dispatchers.Main.immediate)
    private val model = TrainPilotViewModel()
    private fun awaitCondition(condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        while (true) {
            shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            check(System.nanoTime() < deadline) { "Native projection timed out: ${model.uiState.value}" }
            Thread.sleep(10)
        }
    }
    private fun <T> native(block: suspend () -> T): T {
        val executor = Executors.newSingleThreadExecutor()
        val future = executor.submit<T> { runBlocking { block() } }
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        try {
            while (true) {
                shadowOf(Looper.getMainLooper()).idle()
                try { return future.get(10, TimeUnit.MILLISECONDS) }
                catch (_: TimeoutException) { check(System.nanoTime() < deadline) }
            }
        } finally { future.cancel(true); executor.shutdownNow() }
    }
    @After fun close() {
        parent.cancel(); awaitCondition { parent.isCompleted }; db.close()
        val field = HealthJournalStore::class.java.getDeclaredField("instance").apply { isAccessible = true }
        (field.get(null) as? HealthJournalStore)?.close(); field.set(null, null)
    }
    private fun seed(id: String, offset: Int, muscle: String, count: Int) = native {
        val exercise = TrainPilotDatabase.defaultExercises().first { it.primaryMuscle == if (muscle == "arms") "biceps" else muscle }
        val start = LocalDate.now().minusDays(offset.toLong()).atTime(0, 10).atZone(ZoneId.systemDefault()).toInstant()
        val sets = JSONArray()
        repeat(count) { n -> sets.put(JSONObject().put("setId", "$id-$n").put("set", n + 1)
            .put("weight", 10).put("reps", "5").put("done", true)) }
        sets.put(JSONObject().put("setId", "$id-undone").put("set", count + 1)
            .put("weight", 1000).put("reps", "99").put("done", false))
        LegacyWebViewMigrator(context, db, store).saveWorkoutJsonToRoom(JSONObject()
            .put("id", id).put("key", id).put("syncId", id).put("started", start.toString())
            .put("finished", start.plusSeconds(60).toString()).put("programName", id)
            .put("exercises", JSONArray().put(JSONObject().put("id", exercise.id).put("name", exercise.nameHu)
                .put("sets", sets))))
    }
    @Test fun periodsAndSingleWorkoutHaveDistinctRealCompletedMuscleLoads() {
        native { TrainPilotDatabase.ensureBuiltinCatalogSeeded(db, context) }
        seed("today-chest", 0, "chest", 1); seed("week-legs", 3, "legs", 2)
        seed("month-back", 20, "back", 3); seed("quarter-arms", 60, "arms", 4)
        seed("outside", 120, "shoulders", 5)
        model.bindNativeEnvironment(context, store, db, scope)
        awaitCondition { model.uiState.value.realMuscleLoads.any { it.id == "arms" && it.setsCount == 4 } }
        val expected = listOf(setOf("chest"), setOf("chest", "legs"), setOf("chest", "legs", "back"), setOf("chest", "legs", "back", "arms"))
        for (index in 0..3) {
            model.selectProgressRange(index)
            awaitCondition { model.uiState.value.realMuscleLoads.isNotEmpty() }
            assertEquals(expected[index], model.uiState.value.realMuscleLoads.filter { it.setsCount > 0 }.map { it.id }.toSet())
        }
        model.selectProgressRange(3); model.selectProgressRange(0)
        awaitCondition { model.uiState.value.realMuscleLoads.isNotEmpty() }
        assertEquals(setOf("chest"), model.uiState.value.realMuscleLoads.filter { it.setsCount > 0 }.map { it.id }.toSet())
        model.openLogWorkoutDetail("week-legs")
        awaitCondition { model.uiState.value.logWorkoutMuscleLoads.isNotEmpty() }
        assertEquals(setOf("legs"), model.uiState.value.logWorkoutMuscleLoads.filter { it.setsCount > 0 }.map { it.id }.toSet())
        assertEquals(2, model.uiState.value.logWorkoutMuscleLoads.first { it.id == "legs" }.setsCount)
        model.openLogWorkoutDetail("month-back"); model.openLogWorkoutDetail("today-chest")
        awaitCondition { model.uiState.value.logWorkoutMuscleLoads.isNotEmpty() }
        assertEquals(setOf("chest"), model.uiState.value.logWorkoutMuscleLoads.filter { it.setsCount > 0 }.map { it.id }.toSet())
    }
    @Test fun todayAndWeekUseOneProviderFullDayValuesWithoutWorkoutStepAdditionOrFallback() {
        val health = HealthJournalStore.get(context)
        health.erase()
        val day = LocalDate.now().toString()
        health.healthDay(day, JSONObject().put("steps", 8000).put("activeCalories", 1200)
            .put("activityOrigin", "test.hc"), health.generation())
        health.samsungDay(day, JSONObject().put("steps", 7000).put("activeCalories", 1000)
            .put("totalCalories", 1500), JSONArray(), health.generation())
        health.syncMeta(JSONObject().put("lastSyncAt_samsung_health", "2026-11-02T08:00:00Z"))
        health.setPreferences(JSONObject().put("provider", HealthJournalStore.SAMSUNG))
        model.bindNativeEnvironment(context, store, db, scope)
        awaitCondition { model.uiState.value.dailySteps == 7000 && model.uiState.value.dailyHealthDate == day }
        assertEquals(7000, model.uiState.value.weeklyTotalSteps)
        assertEquals(1000, model.uiState.value.activeCaloriesKcal)
        assertEquals(1500, model.uiState.value.totalCaloriesKcal)
        assertTrue("steps" in model.uiState.value.dailyHealthAvailableFields)
        assertEquals(1, model.uiState.value.weeklyHealthDays.count { it.stepsAvailable })
        assertEquals("2026-11-02T08:00:00Z", model.uiState.value.dailyHealthLastSyncIso)
        health.setPreferences(JSONObject().put("provider", HealthJournalStore.HC))
        model.reloadHealthJournal()
        awaitCondition { model.uiState.value.dailySteps == 8000 }
        assertEquals(8000, model.uiState.value.weeklyTotalSteps)
        assertEquals("—", model.uiState.value.dailyHealthLastSyncIso)
        health.erase()
        health.samsungDay(day, JSONObject().put("steps", 7000), JSONArray(), health.generation())
        model.reloadHealthJournal()
        awaitCondition { model.uiState.value.dailyHealthSourceLabel == "Health Connect" }
        assertTrue(model.uiState.value.dailyHealthAvailableFields.isEmpty())
        assertEquals(0, model.uiState.value.dailySteps)
    }
}
