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
        exercise("glute-bridge"), exercise("side-plank"), exercise("dead-bug"), exercise("bird-dog"),
        exercise("lateral-raise"), exercise("db-curl"), exercise("hammer-curl"),
        exercise("concentration-curl"), exercise("oh-triceps"), exercise("db-triceps-kickback"),
        exercise("incline-pushup"), exercise("crunch")
    )

    private fun exercise(id: String, load: Float = 5f) =
        ExerciseItem(id, id, defaultWeightKg = load, loadType = "total")

    private fun planner(
        level: String,
        gear: Set<String>,
        minutes: Int = 60,
        excluded: Set<String> = emptySet(),
        excludedMuscles: Set<String> = emptySet(),
        gentlerStart: Boolean = false
    ) = StudioStarterPlan(gear, true, "Otthon", level, minutes, excludedMuscles, excluded, gentlerStart)

    private fun patternOf(ex: ExerciseItem) =
        com.repforge.app.NativePlannerPolicy.exerciseMetadata(ex.id).optString("movementPattern")

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
        val planner = planner("Kezdő", emptySet(), minutes = 30, excluded = setOf("pushup", "modified-pushup", "incline-pushup"))
        val days = planner.build(catalog, "Izomépítés")
        assertTrue(days.all { it.isNotEmpty() && it.size <= 4 })
        assertTrue(days.flatten().all { it.id in setOf("bodyweight-squat", "glute-bridge", "side-plank", "dead-bug", "bird-dog", "crunch") })
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

    @Test fun sixExerciseHomeMusclePlanIncludesDirectBicepsOnDayAAndDirectTricepsOnDayBWithFatigueOrdering() {
        val gear = setOf("dumbbell", "dumbbells", "support", "bench")
        val days = planner("Középhaladó", gear, minutes = 58).build(catalog, "Izomépítés")
        assertEquals(2, days.size)
        val (dayA, dayB) = days
        assertEquals(6, dayA.size)
        assertEquals(6, dayB.size)
        assertTrue(days.all { it.size <= 6 })

        assertTrue(dayA.any { patternOf(it) == "elbow-flexion" && it.id == "db-curl" })
        assertTrue(dayB.any { patternOf(it) == "elbow-extension" && it.id == "oh-triceps" })

        for (day in days) {
            val patterns = day.map(::patternOf)
            assertTrue(patterns.any { it in setOf("squat", "lunge") })
            assertTrue(patterns.contains("horizontal-push"))
            assertTrue(patterns.any { it in setOf("horizontal-pull", "vertical-pull") })
            assertTrue(patterns.contains("hinge"))
        }

        val pushIdxB = dayB.indexOfFirst { it.id == "pushup" }
        val tricepsIdxB = dayB.indexOfFirst { patternOf(it) == "elbow-extension" }
        assertTrue("pushup should be at 2nd or 3rd place (0-based 1..2), was $pushIdxB", pushIdxB in 1..2)
        assertTrue("direct triceps should come after pushup and main compounds", tricepsIdxB > pushIdxB && tricepsIdxB >= 4)
    }

    @Test fun sixtyMinuteHomeMusclePlanAllowsSevenExercisesWhenTimePermitsAndDropsSeventhWhenTimeExceeded() {
        val gear = setOf("dumbbell", "dumbbells", "support", "bench")
        val fitsSeven = planner("Középhaladó", gear, minutes = 60).build(catalog, "Izomépítés")
        val (dayA7, dayB7) = fitsSeven
        assertEquals(7, dayA7.size)
        assertEquals(7, dayB7.size)
        assertTrue(dayA7.any { it.id == "db-ohp" } && dayA7.any { it.id == "db-curl" })
        assertTrue(dayB7.any { it.id == "lateral-raise" } && dayB7.any { it.id == "oh-triceps" })

        val pushIdxB7 = dayB7.indexOfFirst { it.id == "pushup" }
        val lateralIdxB7 = dayB7.indexOfFirst { it.id == "lateral-raise" }
        val tricepsIdxB7 = dayB7.indexOfFirst { it.id == "oh-triceps" }
        val coreIdxB7 = dayB7.indexOfFirst { patternOf(it) in setOf("core-stability", "core-flexion", "core-bracing") }
        assertTrue("pushup should stay at 2nd or 3rd place in 7-exercise plan", pushIdxB7 in 1..2)
        assertTrue("accessories and core should follow main compounds", lateralIdxB7 >= 4 && tricepsIdxB7 > pushIdxB7 && coreIdxB7 >= 4)

        val heavyCatalog = catalog.map {
            it.copy(defaultSets = 4)
        }
        val cappedByTime = planner("Középhaladó", gear, minutes = 60).build(heavyCatalog, "Izomépítés")
        val (dayA6, dayB6) = cappedByTime
        assertEquals(6, dayA6.size)
        assertEquals(6, dayB6.size)
        assertTrue(dayA6.any { it.id == "db-curl" } && dayA6.none { it.id == "db-ohp" })
        assertTrue(dayB6.any { it.id == "oh-triceps" } && dayB6.none { it.id == "lateral-raise" })
        val pushIdxB6 = dayB6.indexOfFirst { it.id == "pushup" }
        val tricepsIdxB6 = dayB6.indexOfFirst { it.id == "oh-triceps" }
        assertTrue(pushIdxB6 in 1..2 && tricepsIdxB6 > pushIdxB6)
    }

    @Test fun armExclusionRemovesBothDirectArmSlotsAndSingleExerciseExclusionSubstitutesInPlace() {
        val gear = setOf("dumbbell", "dumbbells", "support", "bench")
        for (mins in listOf(58, 60)) {
            val noArms = planner("Középhaladó", gear, minutes = mins, excludedMuscles = setOf("Kar"))
                .build(catalog, "Izomépítés")
            assertTrue(noArms.flatten().none { patternOf(it) in setOf("elbow-flexion", "elbow-extension") })
            assertTrue(noArms.all { it.size <= 6 })
        }

        val substituted = planner(
            "Középhaladó",
            gear,
            minutes = 60,
            excluded = setOf("db-curl", "oh-triceps", "pushup")
        ).build(catalog, "Izomépítés")
        val (dayA, dayB) = substituted
        assertTrue(dayA.none { it.id == "db-curl" })
        assertTrue(dayA.any { it.id in setOf("hammer-curl", "concentration-curl") && patternOf(it) == "elbow-flexion" })
        assertTrue(dayB.none { it.id == "oh-triceps" } && dayB.none { it.id == "pushup" })
        assertTrue(dayB.any { it.id == "db-triceps-kickback" && patternOf(it) == "elbow-extension" })

        val pushSubIdx = dayB.indexOfFirst { it.id in setOf("modified-pushup", "incline-pushup", "db-floor-press") }
        val triSubIdx = dayB.indexOfFirst { it.id == "db-triceps-kickback" }
        assertTrue("pushup substitute must remain at 2nd or 3rd position (0-based 1..2)", pushSubIdx in 1..2)
        assertTrue("direct triceps substitute must follow pushup substitute", triSubIdx > pushSubIdx)

        val noDumbbellArms = planner("Kezdő", emptySet(), minutes = 60).build(catalog, "Izomépítés")
        assertTrue(noDumbbellArms.flatten().none { patternOf(it) in setOf("elbow-flexion", "elbow-extension") })
    }
}

