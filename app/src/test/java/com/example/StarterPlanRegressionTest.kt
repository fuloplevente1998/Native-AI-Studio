package com.example

import com.example.model.ExerciseItem
import com.example.model.StudioStarterPlan
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StarterPlanRegressionTest {
    private val catalog = listOf(
        exercise("barbell-hip-thrust", load = 50f), exercise("bench-press", load = 20f),
        exercise("incline-bench-press"), exercise("barbell-floor-press"),
        exercise("goblet-squat"), exercise("db-floor-press"), exercise("one-arm-row"),
        exercise("db-rdl"), exercise("db-ohp"), exercise("reverse-lunge"),
        exercise("bulgarian-split-squat"), exercise("barbell-row"), exercise("lat-pulldown"),
        exercise("bodyweight-squat"), exercise("pushup"), exercise("modified-pushup"),
        exercise("glute-bridge"), exercise("side-plank"), exercise("dead-bug"), exercise("bird-dog")
    )

    private fun exercise(id: String, load: Float = 5f) =
        ExerciseItem(id, id, defaultWeightKg = load, loadType = "total")

    private fun planner(level: String, gear: Set<String>, minutes: Int = 60,
        excluded: Set<String> = emptySet()) =
        StudioStarterPlan(gear, true, "Otthon", level, minutes, emptySet(), excluded)

    @Test fun homeRestartRetainsCoreMovementPatternsAndNeverCopiesCatalogWeights() {
        val gear = setOf("dumbbell", "dumbbells", "barbell", "bench", "support")
        val days = planner("Újrakezdő", gear).build(catalog, "Izomépítés")
        assertEquals(listOf("goblet-squat", "db-floor-press", "one-arm-row", "db-rdl", "side-plank"),
            days.first().map { it.id })
        assertTrue(days.all { it.size <= 5 })
        assertTrue(days.flatten().all { it.defaultWeightKg == 0f && it.defaultSets == 2 })
        assertTrue(days.flatten().none { it.id in setOf("bench-press", "incline-bench-press", "lat-pulldown", "barbell-hip-thrust") })
    }

    @Test fun advancedCanUseMoreDemandingMovementsAndSetsWithoutInventingLoad() {
        val gear = setOf("dumbbell", "dumbbells", "barbell", "bench", "support")
        val beginner = planner("Kezdő", gear).build(catalog, "Izomépítés")
        val advanced = planner("Haladó", gear).build(catalog, "Izomépítés")
        assertTrue(beginner[1].none { it.id in setOf("bulgarian-split-squat", "barbell-row") })
        assertTrue(advanced[1].any { it.id == "barbell-row" })
        assertTrue(advanced.flatten().any { it.defaultSets == 3 })
        assertTrue(advanced.flatten().all { it.defaultWeightKg == 0f })
    }

    @Test fun exclusionsAndEquipmentStillApplyWhenAnEntireSlotHasNoChoice() {
        val planner = planner("Kezdő", emptySet(), minutes = 30, excluded = setOf("pushup", "modified-pushup"))
        val days = planner.build(catalog, "Izomépítés")
        assertTrue(days.all { it.isNotEmpty() && it.size <= 4 })
        assertTrue(days.flatten().all { it.id in setOf("bodyweight-squat", "glute-bridge", "side-plank", "dead-bug", "bird-dog") })
        val noBodyweight = StudioStarterPlan(emptySet(), false, "Otthon", "Kezdő", 30, emptySet(), emptySet())
        assertTrue(noBodyweight.build(catalog, "Izomépítés").all { it.isEmpty() })
    }

    @Test fun genericSupportDoesNotQualifyAsBenchPressSafetyEquipment() {
        val ordinaryGear = setOf("barbell", "bench", "support")
        val flat = catalog.first { it.id == "bench-press" }
        val incline = catalog.first { it.id == "incline-bench-press" }
        assertFalse(planner("Haladó", ordinaryGear).eligible(flat))
        val safetyGear = ordinaryGear + setOf("lifting-bench", "rack")
        assertTrue(planner("Haladó", safetyGear).eligible(flat))
        assertFalse(planner("Haladó", safetyGear).eligible(incline))
        assertTrue(planner("Haladó", safetyGear + "incline-bench").eligible(incline))
    }
}
