# Fájltulajdon – kötelező ütközésmentes kiosztás

| Chat | Meglévő saját fájlok röviden | Új saját névtér | Közös bekötés |
|---|---|---|---|
| 01 Videó | ExerciseVideoDialog, ExerciseVideoResolver, resolver teszt | inappvideo | Manifest/dependency/új caller csak integrátor |
| 02 Óra | WearDeviceInfo, WatchDeviceInfoRegressionTest | watchinfo | VM és HealthCoachAndSettingsScreens csak integrátor |
| 03 Egészség | NativeDailyHealthDetails, SamsungSdkReader/Repository, NativeCoachReadiness/Signals, DailyHealthDetails teszt | healthreadings | napi/heti VM-modell, TodayGrid és Health képernyő csak integrátor |
| 04 Térkép | OutdoorGpsDashboard, ActiveMeasurementScreens, AnatomicalBodyAndMapComponents, OutdoorGpsLayout teszt | mapcontrols | Main/VM/saved-map caller csak integrátor |
| 05 Integrátor | Minden fenntartott közös fájl, ld. saját feladatleírás | integráció | A négy PR átvétele után |

A teljes útvonalas listák a feladat-MD-kben szerepelnek. Nincs közösen írható fejlesztői fájl. Egy fájl másik függvényét sem szerkesztheti másik chat. Külön mappa és worktree önmagában nem akadályozza meg a Git-merge ütközést: ezért a tulajdonlás teljes fájlra vonatkozik.

Új komponensek/DTO-k önállóan fordulnak kompatibilis defaultokkal; a monolit képernyőbeli hívást és VM-mezőket az INTEGRACIO dokumentum adja át. Ne használd a másik chat még nem kész osztályát. A koordinátor összeköti az önálló kimeneteket.

#58 már kezeli a Coach-preview és a piros/listabeli törlés fájljait. Ezek nem új fejlesztői feladatok. A megőrzött vegyes WIP-stash nem megosztandó fejlesztési bázis.
