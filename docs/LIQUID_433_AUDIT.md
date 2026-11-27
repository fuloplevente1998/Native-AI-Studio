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
