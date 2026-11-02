package com.example

import android.content.Context
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.ui.viewmodel.TrainPilotViewModel
import com.repforge.app.ActiveWorkoutStore
import com.repforge.app.data.db.KeyValueDocumentEntity
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.PAUSED)
class StudioCloudRefreshRegressionTest {
    private fun awaitCondition(condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        while (true) {
            shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            check(System.nanoTime() < deadline) { "Native refresh did not settle" }
            Thread.sleep(10)
        }
    }

    @Test fun cloudRefreshReloadsRoomWithoutAddingWatchObserversAndRebindReplacesThem() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, TrainPilotDatabase::class.java).build()
        val store = TrainPilotSettingsStore(context)
        val parent = SupervisorJob()
        val scope = CoroutineScope(parent + Dispatchers.Main.immediate)
        // Inspect actual live StateFlow subscriptions, rather than an implementation counter.
        val snapshots = ActiveWorkoutStore::class.java.getDeclaredField("_snapshotFlow").let {
            it.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            (it.get(ActiveWorkoutStore) as MutableStateFlow<JSONObject?>)
        }
        val baseline = snapshots.subscriptionCount.value
        val model = TrainPilotViewModel()
        fun saveGoal(goal: Int) = runBlocking {
            db.keyValueDocumentDao().putDocument(KeyValueDocumentEntity(
                "native:userGoals", JSONObject().put("weeklyGoalTarget", goal).toString(), goal.toLong()))
        }
        try {
            saveGoal(7)
            model.bindNativeEnvironment(context, store, db, scope)
            awaitCondition { model.uiState.value.weeklyGoalTarget == 7 }
            assertEquals(baseline + 1, snapshots.subscriptionCount.value)
            for (goal in 8..10) {
                saveGoal(goal)
                model.refreshAfterCloudSync(context)
                awaitCondition { model.uiState.value.weeklyGoalTarget == goal }
                assertEquals(baseline + 1, snapshots.subscriptionCount.value)
            }
            model.bindNativeEnvironment(context, store, db, scope)
            awaitCondition { snapshots.subscriptionCount.value == baseline + 1 }
            runBlocking { store.setHealthBackupConsent(true) }
            awaitCondition { model.uiState.value.includeHealthInBackup }
            runBlocking { store.setHealthBackupConsent(false) }
            awaitCondition { !model.uiState.value.includeHealthInBackup }
        } finally {
            parent.cancel()
            awaitCondition { parent.isCompleted }
            assertEquals(baseline, snapshots.subscriptionCount.value)
            db.close()
        }
    }
}
