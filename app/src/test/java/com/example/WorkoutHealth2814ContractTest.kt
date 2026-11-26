package com.example

import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Adapter wiring regression. Device SDK responses and timestamp coverage still require real tests. */
class WorkoutHealth2814ContractTest {
    @Test fun samsungWorkoutReadsBothEnergyDefinitionsWithinRequestedWindow() {
        val src = File("src/main/java/com/repforge/app/SamsungSdkReader.java").readText()
        val workout = src.substringAfter("JSONObject workout(Instant start, Instant end)")
        assertTrue(workout.contains("TOTAL_ACTIVE_CALORIES_BURNED, start, end"))
        assertTrue(workout.contains("TOTAL_CALORIES_BURNED, start, end"))
        assertTrue(workout.contains("AMBIGUOUS_LOCAL_TIME_WINDOW"))
        assertTrue(workout.contains("HealthWorkoutEnergy.match("))
        val repo = File("src/main/java/com/repforge/app/SamsungHealthRepository.kt").readText()
            .substringAfter("suspend fun readTrainingWindowSuspend").substringBefore("suspend fun readHealthDay")
        assertTrue(repo.contains("\"READ_ACTIVE_CALORIES_BURNED\" to listOf(\"activeCalories\")"))
        assertTrue(repo.contains("\"READ_TOTAL_CALORIES_BURNED\" to listOf(\"totalCalories\")"))
        assertTrue(repo.substringBefore("val result = reader.workout").contains("\"READ_ACTIVE_CALORIES_BURNED\""))
    }
}
