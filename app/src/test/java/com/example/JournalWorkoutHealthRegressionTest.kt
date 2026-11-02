package com.example

import android.content.Context
import android.os.Looper
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.localization.TrainPilotStrings
import com.example.model.AppLanguage
import com.example.model.LoggedWorkoutEntry
import com.example.ui.screens.LogWorkoutDetailScreen
import com.example.ui.theme.TrainPilotTheme
import com.example.ui.viewmodel.TrainPilotUiState
import com.repforge.app.*
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.db.WorkoutHistoryWithDetails
import com.repforge.app.data.migration.LegacyWebViewMigrator
import com.repforge.app.data.store.TrainPilotSettingsStore
import com.repforge.app.ui.journal.JournalHealthPanel
import com.repforge.app.ui.text.nativeResources
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w480dp-h960dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class JournalWorkoutHealthRegressionTest {
    @get:Rule val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: TrainPilotDatabase
    private val models = androidx.lifecycle.ViewModelStore()
    private val start = "2026-11-01T10:00:00Z"
    private val end = "2026-11-01T16:20:00Z"
    private fun intervals() = JSONArray("""[
        {"start":"2026-11-01T10:00:00Z","end":"2026-11-01T10:25:00Z"},
        {"start":"2026-11-01T16:00:00Z","end":"2026-11-01T16:20:00Z"}]
    """)

    @Before fun open() {
        val field = PhoneWorkoutExecution::class.java.getDeclaredField("store").apply { isAccessible = true }
        (field.get(null) as? PhoneWorkoutExecutionStore)?.close(); field.set(null, null)
        val roomField = TrainPilotDatabase::class.java.getDeclaredField("INSTANCE").apply { isAccessible = true }
        (roomField.get(null) as? TrainPilotDatabase)?.close()
        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
        roomField.set(null, db)
    }
    @After fun close() {
        models.clear()
        db.close()
        TrainPilotDatabase::class.java.getDeclaredField("INSTANCE").apply { isAccessible = true }.set(null, null)
    }
    private fun <T> awaitNative(operation: suspend () -> T): T {
        val worker = Executors.newSingleThreadExecutor()
        val future = worker.submit<T> { runBlocking { operation() } }
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        try {
            while (true) {
                shadowOf(Looper.getMainLooper()).idle()
                try { return future.get(10, TimeUnit.MILLISECONDS) }
                catch (_: TimeoutException) { check(System.nanoTime() < deadline) { "Health operation timed out" } }
            }
        } finally { future.cancel(true); worker.shutdownNow() }
    }
    private fun seed(id: String = "fresh", health: JSONObject? = null): WorkoutHistoryWithDetails = awaitNative {
        val raw = JSONObject().put("id", id).put("key", id).put("started", start).put("finished", end)
            .put("active", false).put("activeIntervals", intervals()).put("exercises", JSONArray())
            .put("programName", "Edzés").put("wear", JSONObject().put("averageHeartRate", 130).put("totalCalories", 200))
        if (health != null) raw.put("health240", health)
        LegacyWebViewMigrator(context, db, TrainPilotSettingsStore(context)).saveWorkoutJsonToRoom(raw)
        db.workoutHistoryDao().getWorkoutWithDetailsById(id)!!
    }
    private fun response() = runBlocking {
        val workout = JSONObject().put("started", start).put("finished", end).put("activeIntervals", intervals())
        val queried = mutableListOf<NativeWorkoutActiveWindows.Window>()
        NativeWorkoutActiveWindows.health(workout) { window ->
            queried.add(window)
            JSONObject().put("start", window.start.toString()).put("end", window.end.toString())
                .put("averageHeartRate", if (queried.size == 1) 100 else 160)
                .put("heartRateSamples", if (queried.size == 1) 2 else 1).put("activeCalories", 50)
                .put("averageSpeedMps", if (queried.size == 1) 2 else 4).put("speedSamples", if (queried.size == 1) 3 else 1)
                .put("workoutEnergyProrated", true).put("sources", JSONArray().put("com.example.provider"))
                .put("sourceLabels", JSONObject().put("com.example.provider", "Measured provider"))
        }.also { assertEquals(2, queried.size); assertEquals("2026-11-01T10:25:00Z", queried[0].end.toString()) }
    }

    @Test fun freshWorkoutHealthAttachesToRoomExcludesLongPauseAndKeepsWearSeparate() {
        val before = seed()
        assertEquals(45 * 60, before.workout.durationSeconds)
        val native = NativeWorkoutHealth(context, db)
        val request = awaitNative { native.prepare(before.workout) }
        awaitNative { native.attach(request, response()) }
        val saved = awaitNative { db.workoutHistoryDao().getWorkoutWithDetailsById("fresh")!! }
        val data = JSONObject(saved.workout.health240Json!!)
        assertTrue(NativeWorkoutHealth.matchesWindow(data, saved.workout))
        assertEquals(120.0, data.getDouble("averageHeartRate"), .001)
        assertEquals(100.0, data.getDouble("activeCalories"), .001)
        assertEquals(2.5, data.getDouble("averageSpeedMps"), .001)
        assertTrue(data.getBoolean("workoutEnergyProrated"))
        assertEquals("Measured provider", data.getJSONObject("sourceLabels").getString("com.example.provider"))
        assertEquals(before.workout.wearMetricsJson, saved.workout.wearMetricsJson)
        assertEquals(before.workout.activeIntervalsJson, saved.workout.activeIntervalsJson)
        val restored = seed("restored", data)
        assertTrue(NativeWorkoutHealth.matchesWindow(JSONObject(restored.workout.health240Json!!), restored.workout))
        assertEquals(data.toString(), JSONObject(restored.workout.health240Json!!).toString())
    }

    @Test fun samsungSnapshotPersistsAlongsideHealthConnectAndProviderChipsSwitchActualValues() {
        val before = seed(health = response().put("windowStart", start).put("windowEnd", end))
        val native = NativeWorkoutHealth(context, db)
        val request = awaitNative { native.prepare(before.workout) }
        awaitNative { native.attach(request, response().put("averageHeartRate", 155), HealthJournalStore.SAMSUNG) }
        val saved = awaitNative { db.workoutHistoryDao().getWorkoutWithDetailsById("fresh")!! }
        val health = JSONObject(saved.workout.health240Json!!)
        assertEquals(120.0, NativeWorkoutHealthSource.snapshot(health, HealthJournalStore.HC)!!.getDouble("averageHeartRate"), .01)
        assertEquals(155.0, NativeWorkoutHealthSource.snapshot(health, HealthJournalStore.SAMSUNG)!!.getDouble("averageHeartRate"), .01)
        assertEquals(before.workout.wearMetricsJson, saved.workout.wearMetricsJson)
        compose.setContent {
            var provider by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(HealthJournalStore.HC) }
            TrainPilotTheme {
                androidx.compose.foundation.lazy.LazyColumn(Modifier.testTag("provider_list")) {
                    item {
                        JournalHealthPanel(saved, nativeResources(context, "hu"), "hu", false, null, true,
                            provider = provider, selectProvider = { provider = it }) { }
                    }
                }
            }
        }
        val list = compose.onNodeWithTag("provider_list")
        list.performScrollToNode(hasTestTag("journal_hc_averageHeartRate_value"))
        compose.onNodeWithTag("journal_hc_averageHeartRate_value").assertTextEquals("120 bpm")
        list.performScrollToNode(hasTestTag("journal_provider_samsung_health"))
        compose.onNodeWithTag("journal_provider_samsung_health").performClick()
        list.performScrollToNode(hasTestTag("journal_samsung_averageHeartRate_value"))
        compose.onNodeWithTag("journal_samsung_averageHeartRate_value").assertTextEquals("155 bpm")
        compose.onNodeWithTag("journal_hc_averageHeartRate_value").assertDoesNotExist()
    }

    @Test fun sameOuterDatesDoNotValidateHealthThatIncludesThePause() {
        val row = seed().workout
        val fullWindow = JSONObject().put("windowStart", start).put("windowEnd", end).put("averageHeartRate", 150)
        assertFalse(NativeWorkoutHealth.matchesWindow(fullWindow, row))
        fullWindow.put("activeIntervals", intervals())
        assertTrue(NativeWorkoutHealth.matchesWindow(fullWindow, row))
        fullWindow.getJSONArray("activeIntervals").getJSONObject(1).put("start", "2026-11-01T15:00:00Z")
        assertFalse(NativeWorkoutHealth.matchesWindow(fullWindow, row))
        assertTrue(NativeWorkoutHealth.matchesWindow(JSONObject().put("windowStart", start).put("windowEnd", end),
            row.copy(activeIntervalsJson = null)))
    }

    @Test fun mismatchedActiveWindowOrDeletedWorkoutCannotReceiveLateHealth() {
        val native = NativeWorkoutHealth(context, db)
        val row = seed().workout
        val request = awaitNative { native.prepare(row) }
        val invalid = response().apply { remove("activeIntervals") }
        try { awaitNative { native.attach(request, invalid) }; fail("Pause-inclusive data accepted") }
        catch (error: java.util.concurrent.ExecutionException) { assertTrue(error.cause is IllegalStateException) }
        assertNull(awaitNative { db.workoutHistoryDao().getWorkoutWithDetailsById(row.id)!!.workout.health240Json })
        awaitNative { db.workoutHistoryDao().deleteAllWorkouts() }
        try { awaitNative { native.attach(request, response()) }; fail("Deleted workout resurrected") }
        catch (error: java.util.concurrent.ExecutionException) { assertTrue(error.cause is IllegalStateException) }
        assertNull(awaitNative { db.workoutHistoryDao().getWorkoutWithDetailsById(row.id) })
    }

    @Test fun studioJournalShowsActualSavedHealthAndWearValuesInWorkoutDetails() {
        val data = response().put("windowStart", start).put("windowEnd", end).put("syncedAt", end)
        val detail = seed(health = data)
        val entry = LoggedWorkoutEntry("fresh", "2026.11.01", "Edzés", 45, "45:00", "—", 1, 150, "Futás", isOutdoorRun = true)
        val model = com.repforge.app.ui.journal.JournalViewModel(context as android.app.Application)
        models.put("journal", model)
        var canonicalSources = false
        compose.setContent {
            TrainPilotTheme {
                val health = com.example.ui.components.studioWorkoutHealthContent("fresh", "hu", {},
                    { id, wear, hc -> canonicalSources = id == "fresh" && wear && hc }, model)
                LogWorkoutDetailScreen(TrainPilotUiState(loggedWorkouts = listOf(entry), selectedLogWorkoutId = entry.id,
                    logDetailTabIndex = 1), TrainPilotStrings.forLanguage(AppLanguage.HU), {}, {}, {}, {}, {}, {}, {}, {},
                    workoutHealth = health)
            }
        }
        compose.waitUntil(10_000) { model.uiState.value.detail != null && canonicalSources }
        assertFalse(model.uiState.value.healthRefreshing)
        assertNull(model.uiState.value.healthError)
        val list = compose.onNodeWithTag("log_workout_detail_screen")
        val header = compose.onNodeWithTag("journal_detail_header").getUnclippedBoundsInRoot()
        assertTrue("Compact header must not consume a large empty top section", header.bottom - header.top <= 160.dp)
        assertTrue("No duplicate top spacer", header.top - list.getUnclippedBoundsInRoot().top <= 10.dp)
        compose.onNodeWithTag("journal_detail_location").assertTextEquals("Fejlődés · Napló")
        compose.onNodeWithText("Edzés részletei").assertDoesNotExist()
        list.performScrollToNode(hasTestTag("journal_hc_averageHeartRate_value"))
        compose.onNodeWithTag("journal_hc_averageHeartRate_value").assertTextEquals("120 bpm")
        list.performScrollToNode(hasTestTag("journal_hc_activeCalories_value"))
        compose.onNodeWithTag("journal_hc_activeCalories_value").assertTextEquals("100 kcal")
        list.performScrollToNode(hasTestTag("journal_wear_totalCalories_value"))
        compose.onNodeWithTag("journal_wear_totalCalories_value").assertTextEquals("200 kcal")
    }
    @Test fun emptyCachedResponseRetriesOnOpenAndNeverAdvertisesMissingMeasurements() {
        // The foreground-only refresh contract needs an explicitly resumed host and
        // denied grants; neither state is an implicit Robolectric fixture guarantee.
        compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        shadowOf(context as android.app.Application).denyPermissions(*listOf(
            "READ_STEPS", "READ_HEART_RATE", "READ_ACTIVE_CALORIES_BURNED", "READ_TOTAL_CALORIES_BURNED",
            "READ_EXERCISE", "READ_DISTANCE", "READ_SPEED"
        ).map { "android.permission.health.$it" }.toTypedArray())
        assertEquals(androidx.lifecycle.Lifecycle.State.RESUMED, compose.activity.lifecycle.currentState)
        val empty = JSONObject().put("windowStart", start).put("windowEnd", end).put("activeIntervals", intervals())
        val detail = seed(health = empty)
        assertTrue(NativeWorkoutHealth.matchesWindow(empty, detail.workout))
        val model = com.repforge.app.ui.journal.JournalViewModel(context as android.app.Application)
        models.put("empty-journal", model)
        var healthAvailable = true
        compose.setContent {
            TrainPilotTheme {
                val content = com.example.ui.components.studioWorkoutHealthContent("fresh", "hu", {},
                    { _, _, hc -> healthAvailable = hc }, model)
                content()
            }
        }
        // Robolectric has no granted provider permissions. A read must be attempted,
        // fail truthfully, and retain the cached row instead of inventing a pulse/zero.
        try {
            compose.waitUntil(10_000) { model.uiState.value.healthError != null }
        } catch (timeout: androidx.compose.ui.test.ComposeTimeoutException) {
            throw AssertionError("Empty-cache foreground read did not report an error: ${model.uiState.value}; host=${compose.activity.lifecycle.currentState}", timeout)
        }
        assertFalse(healthAvailable)
        compose.onNodeWithTag("journal_hc_averageHeartRate_value").assertDoesNotExist()
        compose.onNodeWithTag("journal_hc_activeCalories_value").assertDoesNotExist()
        assertEquals(detail.workout.health240Json,
            awaitNative { db.workoutHistoryDao().getWorkoutWithDetailsById("fresh")!!.workout.health240Json })
    }

    @Test fun journalDetailsAndRouteKeepProgressSelectedAndBackRestoresTheOrigin() {
        val model = com.example.ui.viewmodel.TrainPilotViewModel()
        models.put("studio-navigation", model)
        model.openLogWorkoutDetail("fresh")
        assertEquals(com.example.model.MainTab.PROGRESS, model.uiState.value.currentTab)
        assertEquals(1, model.uiState.value.progressMainSection)
        compose.setContent {
            val state by model.uiState.collectAsState()
            TrainPilotTheme {
                com.example.ui.components.TrainPilotGlobalTopHeaderAndNav(
                    currentTab = state.currentTab, currentSubScreen = state.currentSubScreen,
                    currentLanguage = state.language, selectedPaletteId = state.selectedPaletteId,
                    strings = TrainPilotStrings.forLanguage(state.language), onSelectTab = model::selectTab,
                    onNavigateToSubScreen = { _, _ -> }, onSelectLanguage = {}, onOpenPalettePicker = {}, onOpenSettings = {})
            }
        }
        compose.onNodeWithTag("top_nav_progress").assertIsSelected()
        compose.runOnIdle { model.navigateToSubScreen(com.example.model.SubScreen.LOG_ROUTE_MAP) }
        compose.onNodeWithTag("top_nav_progress").assertIsSelected()
        compose.runOnIdle { model.navigateBack() }
        compose.onNodeWithTag("top_nav_progress").assertIsSelected()
        compose.runOnIdle { model.navigateBack() }
        compose.onNodeWithTag("top_nav_home").assertIsSelected()
    }

}
