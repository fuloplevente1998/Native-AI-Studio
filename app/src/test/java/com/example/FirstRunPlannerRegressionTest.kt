package com.example

import android.content.Context
import android.os.Looper
import androidx.compose.material3.Text
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.ui.theme.TrainPilotTheme
import com.example.ui.viewmodel.TrainPilotViewModel
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.store.TrainPilotSettingsStore
import com.repforge.app.ui.onboarding.NativeFirstLaunchGate
import com.repforge.app.ui.onboarding.NativeOnboardingPolicy
import kotlinx.coroutines.*
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class FirstRunPlannerRegressionTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var store: TrainPilotSettingsStore
    private lateinit var db: TrainPilotDatabase
    private val parent = SupervisorJob()
    private val scope = CoroutineScope(parent + Dispatchers.Main.immediate)

    @Before fun prepare() = runBlocking {
        store = TrainPilotSettingsStore(context)
        store.resetAllSettingsForEraseLocal()
        store.initializeOnboardingForMigration(false)
        store.markMigrationComplete()
        val field = TrainPilotDatabase::class.java.getDeclaredField("INSTANCE").apply { isAccessible = true }
        (field.get(null) as? TrainPilotDatabase)?.close()
        db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java)
            .allowMainThreadQueries().build()
        field.set(null, db)
    }

    @After fun close() {
        parent.cancel()
        await { parent.isCompleted }
        db.close()
        TrainPilotDatabase::class.java.getDeclaredField("INSTANCE").apply { isAccessible = true }.set(null, null)
    }

    private fun await(condition: () -> Boolean) = compose.waitUntil(30_000) {
        Snapshot.sendApplyNotifications()
        shadowOf(Looper.getMainLooper()).idle()
        condition()
    }

    private fun awaitTag(tag: String) = await { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }

    private fun show(bound: Boolean = true): TrainPilotViewModel {
        val model = TrainPilotViewModel()
        if (bound) {
            model.bindNativeEnvironment(context, store, db, scope)
            await { !model.uiState.value.savedRecordsLoading }
        }
        compose.setContent { TrainPilotTheme {
            NativeFirstLaunchGate(model) { Text("Home", Modifier.testTag("gated_home")) }
        } }
        return model
    }

    private fun beginAndEdit() {
        awaitTag("onboarding_welcome")
        compose.onNodeWithTag("onboarding_begin").performClick()
        awaitTag("onboarding_profile")
        compose.onNodeWithTag("onboarding_edit_profile").performClick()
        awaitTag("profile_height_box")
        compose.onNodeWithTag("full_height_form_footer").assertIsDisplayed()
        compose.onNodeWithTag("profile_field_height").assertDoesNotExist()
    }

    @Test fun continueUsesApprovedPlannerAndCommitsProfileAndPlanBeforeHome() {
        val model = show()
        beginAndEdit()
        compose.onNodeWithTag("profile_height_box").performClick()
        compose.onNodeWithText("181 cm").performScrollTo().performClick()
        compose.onNodeWithTag("profile_days_box").performScrollTo().performClick()
        compose.onNodeWithText("Heti 4 nap").performScrollTo().performClick()
        compose.onNodeWithTag("profile_duration_box").performScrollTo().performClick()
        compose.onNodeWithText("75 perc").performScrollTo().performClick()
        compose.onNodeWithTag("planner_save_button").assertIsDisplayed().performClick()
        awaitTag("gated_home")

        val saved = runBlocking { store.current() }
        assertEquals("complete", saved.onboarding128)
        val profile = JSONObject(saved.profileJson)
        assertEquals(181, profile.getInt("height"))
        assertEquals(4, profile.getInt("daysPerWeek"))
        assertEquals(75, profile.getInt("minutes"))
        assertTrue(saved.activeProgramId.startsWith("custom_plan_"))
        val program = runBlocking { db.programDao().getProgramWithDaysById(saved.activeProgramId) }!!
        assertEquals(2, program.days.size)
        assertTrue(program.days.all { org.json.JSONArray(it.exercisesJson).length() > 0 })

        model.bindNativeEnvironment(context, store, db, scope)
        await { model.uiState.value.profileComplete && model.uiState.value.userHeightCm == 181 }
        assertEquals(4, model.uiState.value.userScheduleDaysPerWeek)
        compose.onNodeWithTag("onboarding_profile").assertDoesNotExist()
    }

    @Test fun closingPlannerDoesNotCompleteOnboardingOrPersistUnconfirmedFields() {
        show()
        beginAndEdit()
        compose.onNodeWithTag("profile_height_box").performClick()
        compose.onNodeWithText("181 cm").performScrollTo().performClick()
        compose.onNodeWithTag("planner_skip_btn").performClick()
        await { compose.onAllNodesWithTag("profile_height_box").fetchSemanticsNodes().isEmpty() }
        assertEquals("in_progress", runBlocking { store.current().onboarding128 })
        assertEquals("{}", runBlocking { store.current().profileJson })
        compose.onNodeWithTag("gated_home").assertDoesNotExist()
        compose.onNodeWithTag("onboarding_edit_profile").performClick()
        awaitTag("profile_height_box")
        compose.onNodeWithTag("profile_height_box").assertTextContains("179 cm")
    }

    @Test fun interruptedFirstRunResumesIntoTheSamePlannerWithPersistedProfileValues() {
        runBlocking {
            val ticket = com.repforge.app.NativeDataReplacement.continuationForCurrentData(context)
            assertTrue(store.transitionOnboarding("welcome", NativeOnboardingPolicy.IN_PROGRESS, ticket))
            store.saveTrainingProfile(JSONObject().put("height", 185).put("weight", 74.5)
                .put("daysPerWeek", 4).put("minutes", 75).put("lineage", "keep"))
        }
        show()
        awaitTag("onboarding_profile")
        compose.onNodeWithTag("onboarding_welcome").assertDoesNotExist()
        compose.onNodeWithTag("onboarding_edit_profile").performClick()
        awaitTag("profile_height_box")
        compose.onNodeWithTag("profile_height_box").assertTextContains("185 cm")
        compose.onNodeWithTag("planner_save_button").performClick()
        awaitTag("gated_home")
        assertEquals("keep", JSONObject(runBlocking { store.current().profileJson }).getString("lineage"))
    }

    @Test fun unavailablePersistenceShowsErrorAndKeepsFirstRunIncomplete() {
        show(bound = false)
        beginAndEdit()
        compose.onNodeWithTag("planner_save_button").performClick()
        awaitTag("planner_save_error")
        compose.onNodeWithTag("planner_save_button").assertIsEnabled()
        compose.onNodeWithTag("gated_home").assertDoesNotExist()
        assertEquals("in_progress", runBlocking { store.current().onboarding128 })
        assertEquals("{}", runBlocking { store.current().profileJson })
    }
}
