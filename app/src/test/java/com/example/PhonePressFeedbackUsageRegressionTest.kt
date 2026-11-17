package com.example

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** Architecture guard complements pixel tests: new phone actions must opt into shaped feedback. */
class PhonePressFeedbackUsageRegressionTest {
    @Test fun phoneActionsDoNotIntroduceIndependentRectangularRipples() {
        val root = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .map { File(it, "app/src/main/java") }.firstOrNull { it.isDirectory }
            ?: error("Phone sources required for the press-feedback usage audit")
        val exceptions = mapOf(
            // These are selectable plots, not painted buttons.
            "com/example/ui/screens/StudioVolumeChart.kt" to ".clickable { onSelect(index) }",
            "com/repforge/app/ui/progress/NativeMuscleLoadVisual.kt" to ".clickable(role = Role.Button, onClickLabel = label)",
            // Finishing overlay consumes input; it never navigates or invokes a button action.
            "com/repforge/app/ui/workout/ActiveWorkoutScreen.kt" to ".clickable { }"
        )
        val unexpected = mutableListOf<String>()
        root.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
            val relative = file.relativeTo(root).invariantSeparatorsPath
            file.readLines().forEachIndexed { index, line ->
                if (".clickable" in line && !line.trimStart().startsWith("import ") &&
                    // Enlarged hit targets explicitly suppress parent ripple and paint their visual child.
                    "indication = null" !in line &&
                    exceptions[relative]?.let { line.contains(it) } != true) {
                    unexpected += "$relative:${index + 1}"
                }
            }
        }
        assertTrue("Use shared shaped feedback or document a genuine non-button target: $unexpected", unexpected.isEmpty())
    }
}
