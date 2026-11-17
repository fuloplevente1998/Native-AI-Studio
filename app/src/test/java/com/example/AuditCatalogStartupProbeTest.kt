package com.example

import com.example.ui.viewmodel.TrainPilotViewModel
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** JVM fixture timings are diagnostics, not a device startup benchmark. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuditCatalogStartupProbeTest {
    @Test fun constructorProjectsSameCatalogBeforeAndAfterWarmup() {
        fun construct(): Pair<TrainPilotViewModel, Long> {
            val start = System.nanoTime()
            val model = TrainPilotViewModel()
            return model to (System.nanoTime() - start)
        }
        val (cold, coldNs) = construct()
        val (warm, warmNs) = construct()
        val a = cold.uiState.value
        val b = warm.uiState.value
        assertTrue(a.exercises.isNotEmpty())
        assertTrue(a.programs.isNotEmpty())
        assertEquals(a.exercises.map { it.id }, b.exercises.map { it.id })
        assertEquals(a.programs.map { it.id }, b.programs.map { it.id })
        assertFalse(a.isWorkoutSessionActive)
        assertFalse(b.isWorkoutSessionActive)
        println("AUDIT_CATALOG_STARTUP_PROBE " + JSONObject().put("environment", "robolectric_jvm")
            .put("firstConstructorNs", coldNs).put("warmConstructorNs", warmNs)
            .put("exerciseCount", a.exercises.size).put("programCount", a.programs.size))
    }
}
