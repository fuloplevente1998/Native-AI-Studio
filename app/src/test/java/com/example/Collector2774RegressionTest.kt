package com.example

import com.example.model.ExerciseItem
import com.example.ui.components.ExerciseKind
import com.example.ui.components.exerciseKind
import com.repforge.app.data.db.NativeBuiltinCatalog
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Collector2774RegressionTest {
    private fun item(e: com.repforge.app.data.db.ExerciseLibraryEntity) = ExerciseItem(
        id = e.id, nameHu = e.nameHu, equipmentHu = "", loadType = e.loadType, repUnit = e.repUnit,
        measurementType = e.measurementType,
        isTimed = e.measurementType.contains("time", true) || e.repUnit.contains("mp", true))

    @Test fun everyLibraryExerciseGetsAnIconAndBodyweightOrTimedAreNotDumbbells() {
        // 2774: one rule covers the whole built-in library (and custom exercises).
        val kinds = NativeBuiltinCatalog.exercises.associate { it.nameHu to exerciseKind(item(it)) }
        assertTrue(kinds.size >= 100)
        assertTrue("not everything may fall back to a dumbbell", kinds.values.toSet().size >= 3)
        NativeBuiltinCatalog.exercises.filter { it.measurementType.contains("time", true) }.forEach {
            assertEquals(it.nameHu, ExerciseKind.TIMED, kinds[it.nameHu])
        }
        NativeBuiltinCatalog.exercises.filter { it.loadType == "bodyweight" && !it.measurementType.contains("time", true) &&
            it.measurementType !in setOf("distance", "gps") }.forEach {
            assertEquals(it.nameHu, ExerciseKind.BODYWEIGHT, kinds[it.nameHu])
        }
        assertEquals(ExerciseKind.BODYWEIGHT, exerciseKind(ExerciseItem("x", "Hasprés", equipmentHu = "Testsúly")))
        assertEquals(ExerciseKind.MACHINE, exerciseKind(ExerciseItem("y", "Csigás lenyomás", equipmentHu = "Csiga")))
        assertEquals(ExerciseKind.TIMED, exerciseKind(ExerciseItem("z", "Plank", isTimed = true)))
    }
}
