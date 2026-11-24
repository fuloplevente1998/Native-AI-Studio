package com.example

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * REFACTOR_PLAN.md: keeps Kotlin files small so an edit reads one focused file.
 * New files may not exceed [LIMIT] lines. Files that were already larger are listed with a cap
 * (size when the plan started + small headroom); a split lowers or removes their row – never raise it.
 */
class SourceSizeGuardTest {
    private val LIMIT = 900

    private val legacyCaps = mapOf(
        "app/src/main/java/com/example/ui/viewmodel/TrainPilotViewModel.kt" to 5887,
        "app/src/main/java/com/example/ui/screens/WorkoutAndPlanningScreens.kt" to 4095,
        "app/src/main/java/com/example/ui/screens/ActiveMeasurementScreens.kt" to 3181,
        "wear/src/main/java/com/repforge/app/wear/WearScreens.kt" to 2949,
        "app/src/main/java/com/example/ui/screens/LogAndProgressScreens.kt" to 2355,
        "app/src/main/java/com/example/ui/components/TrainPilotCommonComponents.kt" to 2302,
        "app/src/main/java/com/example/ui/components/AnatomicalBodyAndMapComponents.kt" to 1918,
        "app/src/main/java/com/repforge/app/GoogleSyncRepository.kt" to 1751,
        "app/src/main/java/com/repforge/app/HealthJournalStore.kt" to 1492,
        "app/src/main/java/com/example/MainActivity.kt" to 1393,
        "app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutViewModel.kt" to 1384,
        "app/src/main/java/com/repforge/app/ui/workout/ActiveWorkoutScreen.kt" to 1361,
        "app/src/main/java/com/repforge/app/PhoneWorkoutExecutionStore.kt" to 1353,
        "app/src/main/java/com/example/model/TrainPilotModels.kt" to 1334,
        "app/src/main/java/com/repforge/app/HealthConnectRepository.kt" to 1288,
        "app/src/main/java/com/example/localization/TrainPilotStrings.kt" to 1273,
        "app/src/main/java/com/repforge/app/WearSyncRepository.kt" to 1196,
        "app/src/main/java/com/repforge/app/BleDiscoveryRepository.kt" to 1121,
        "app/src/main/java/com/repforge/app/IntervalTimerService.kt" to 1100,
        "wear/src/main/java/com/repforge/app/wear/MainActivity.kt" to 1094,
        "app/src/main/java/com/repforge/app/PhoneWorkoutEngine.kt" to 1085,
    )

    @Test fun kotlinSourcesStayWithinTheirSizeLimit() {
        val root = File("..").canonicalFile
        val tooBig = listOf("app/src/main", "wear/src/main", "shared/src").flatMap { dir ->
            File(root, dir).walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        }.mapNotNull { file ->
            val path = file.relativeTo(root).invariantSeparatorsPath
            val lines = file.useLines { it.count() }
            val cap = legacyCaps[path] ?: LIMIT
            if (lines > cap) "$path: $lines lines (limit $cap) – split it by feature (docs/REFACTOR_PLAN.md)" else null
        }
        assertTrue(tooBig.joinToString("\n"), tooBig.isEmpty())
    }

    @Test fun legacyCapsOnlyNameExistingFiles() {
        val root = File("..").canonicalFile
        val stale = legacyCaps.keys.filterNot { File(root, it).isFile }
        assertTrue("Remove split/renamed files from legacyCaps: $stale", stale.isEmpty())
    }
}
