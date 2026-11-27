# #433 – Liquid forrásaudit

Alap: `b14dc497448b2e3b4df0f9715ea65cb675a01775`, 281800. Csak dokumentáció; nincs implementáció vagy vizuális elfogadás. A nyolc Liquid téma a hatókör, a 32 standard paletta és Wear változatlan. A teljes fa 310 telefonos UI/VM/root Kotlin-fájlt tartalmaz; nem mind aktív vizuális komponens. A tényleges launcher, TrainPilotAppRoot, TrainPilotSheetRoutes és CODEMAP alapján kell a megkerülő útvonalakat besorolni. A táblázat teljes fájlokon végzett mintakeresés, nem készülékes teszt. A találat lehet szemantikus szín, import alias vagy standard ág; nem automatikusan hiba. A hiányzó találat nem igazol helyességet.

## Studio képernyők – 1. rész (12/49)

| Fájl | Sor | Közvetlen festék | Vezérlő | Közös üveg | Optika/lista | Közös press |
|---|---:|---|---|---|---|---|
| `com/example/ui/screens/HomeScreen.kt` | 538 | 297, 307, 386, 420 | 377 | 174, 223, 244, 316, 317, 470, 521, 522 | 10, 53, 154, 159 | 177, 242, 381, 524 |
| `com/example/ui/screens/LoggedPrRecords.kt` | 83 | — | — | — | — | — |
| `com/example/ui/screens/OutdoorGpsDashboard.kt` | 600 | 108, 240, 458, 564 | 249, 460, 476, 482, 484, 547, 562 | 304, 351, 352, 440, 497, 525, 544 | 29, 216, 236 | — |
| `com/example/ui/screens/ScreenText.kt` | 11 | — | — | — | — | — |
| `com/example/ui/screens/StatisticsExerciseTrend.kt` | 89 | — | — | — | — | — |
| `com/example/ui/screens/StudioBodyFitnessTiles.kt` | 181 | — | — | 148, 149 | — | 151 |
| `com/example/ui/screens/StudioStatisticsScreen.kt` | 563 | 210, 242, 522 | 168, 494, 512, 547 | 147, 400, 401, 470 | 7, 24, 28, 138, 159, 519 | 181, 215, 302, 451 |
| `com/example/ui/screens/StudioTimerReadout.kt` | 46 | — | — | — | — | — |
| `com/example/ui/screens/StudioTodayStatusGrid.kt` | 65 | — | — | — | — | — |
| `com/example/ui/screens/StudioVolumeChart.kt` | 103 | 69 | 78 | — | — | — |
| `com/example/ui/screens/StudioWorkoutModesDialog.kt` | 361 | 92, 148, 238, 293, 323 | 249, 258, 288, 313, 320 | 104, 107, 194, 236 | 16, 29, 99, 157 | 109, 237 |
| `com/example/ui/screens/WorkoutTabDayCards.kt` | 239 | 188 | — | 167, 169, 185 | — | 171, 210 |
| `com/example/ui/screens/health/CoachDetailScreen.kt` | 370 | 253, 326 | — | 196, 198, 274, 297, 317, 352 | 21, 39, 161, 175 | 353 |
| `com/example/ui/screens/health/ExternalWorkoutImportPanel.kt` | 215 | — | 45, 103, 106, 111, 128, 161, 166, 190, 195, 200 | — | 4, 118, 188 | — |
| `com/example/ui/screens/health/HealthScreenShared.kt` | 44 | — | — | — | — | — |
| `com/example/ui/screens/health/HealthSourcesContent.kt` | 263 | 95 | — | 114, 115, 138, 139 | — | 100, 119, 142, 221, 231 |
| `com/example/ui/screens/health/HealthTabScreen.kt` | 859 | 221, 340, 382, 401, 513, 644 | 369, 388, 518, 797 | 472, 555, 577, 592 | 3, 40, 44, 718, 722, 838 | 265, 278 |
| `com/example/ui/screens/health/HealthTiles.kt` | 296 | 169, 277 | — | 61, 63, 110, 112, 148, 152, 215 | — | 65, 154 |
| `com/example/ui/screens/health/HealthTodayStatusSheet.kt` | 539 | 264, 421, 524 | 269 | 101, 285, 309, 353, 426, 427, 507 | 34, 112 | 358, 479, 512 |
| `com/example/ui/screens/health/HealthTodayTileExtras.kt` | 270 | 130 | — | 123, 201 | — | 126, 161, 250 |
| `com/example/ui/screens/health/HealthWeightAndGoals.kt` | 450 | 165, 250, 262, 401, 413, 446 | 192, 221, 244, 417, 421, 423, 428, 434 | 135, 136, 307, 308, 440, 442 | 34, 235 | 302, 310, 444 |
| `com/example/ui/screens/log/LogRouteMapScreen.kt` | 144 | 132, 138 | — | 82, 85 | — | — |
| `com/example/ui/screens/log/LogScreenText.kt` | 9 | — | — | — | — | — |
| `com/example/ui/screens/log/LogTabScreen.kt` | 701 | 209, 231, 238, 689 | 114, 236, 652 | 321, 344, 364, 368, 407, 451, 547, 669 | 42, 309, 483 | 346, 370, 409, 455, 599, 671 |
| `com/example/ui/screens/log/LogWorkoutDetailScreen.kt` | 426 | 155, 165, 280 | — | 105, 110, 145, 146, 327, 328, 353, 354 | 37, 133, 217 | 210, 247 |
| `com/example/ui/screens/measure/ActiveBilateralWorkoutScreen.kt` | 838 | 166, 430, 455, 593, 618, 703, 711 | — | 276, 280, 281, 356, 431, 519, 594, 680, 760 | 10, 47, 260, 313 | 450, 613, 705, 728 |
| `com/example/ui/screens/measure/ActiveOutdoorGpsScreen.kt` | 583 | — | — | — | — | — |
| `com/example/ui/screens/measure/ActiveStrengthWorkoutScreen.kt` | 302 | 282 | — | 147, 151, 152 | 4, 32, 134, 193 | 290 |
| `com/example/ui/screens/measure/ActiveTimedWorkoutScreen.kt` | 558 | 367, 392, 523, 538, 544 | — | 198, 202, 203, 268, 470, 515 | 8, 46, 182, 230 | 387, 490 |
| `com/example/ui/screens/measure/IntervalSettingsScreen.kt` | 232 | 224 | 141, 149, 155, 208, 218 | 129, 130, 191, 192, 214 | 27, 80 | — |
| `com/example/ui/screens/measure/MeasurementShared.kt` | 122 | — | — | — | — | 63 |
| `com/example/ui/screens/measure/WorkoutSummaryScreen.kt` | 750 | 439, 508, 538, 620 | 491, 521 | 398, 399, 433, 586, 740, 741 | 34, 76, 239, 562 | 468, 641 |
| `com/example/ui/screens/progress/ProgressTabScreen.kt` | 248 | 227 | — | 117, 118, 145, 203 | 9, 11, 42, 106, 107, 184 | 134, 206 |
| `com/example/ui/screens/progress/ProgressVolumeAndMuscle.kt` | 835 | 420, 453, 470, 493, 510, 529, 546, 565, 621, 686, 732, 779 | — | 133, 137, 237, 251, 286, 387, 392, 437, 486, 522, 558, 598, 601, 761 | 41, 398, 682 | 149, 243, 418, 468, 487, 508, 523, 544, 559, 608, 768 |
| `com/example/ui/screens/settings/BackupRestoreScreen.kt` | 376 | 200 | 224, 269, 272 | 219, 220, 233, 256, 257, 366 | 37, 40, 181, 191, 371 | — |
| `com/example/ui/screens/settings/GoogleCloudSyncCard.kt` | 463 | 159, 214, 248, 302, 313, 334, 345, 348, 418 | 291, 330, 430 | 164, 165, 189, 192, 241, 243 | — | 207, 234, 287, 298, 428 |
