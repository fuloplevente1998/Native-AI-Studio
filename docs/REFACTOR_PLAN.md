# Kód-szétszedési terv (token-takarékos szerkezet)

Cél: egy javításnál az agent 300–600 sort olvasson 3000–5700 helyett. Módszer: **tiszta áthelyezés**,
működésváltozás nélkül, ugyanabban a Kotlin-csomagban (a hívások nem változnak).

## Szabályok (minden lépésre)
- 1 lépés = 1 PR = 1 CI-kör. Funkció, javítás, átnevezés nem keveredik bele.
- Csak akkor indul, ha nincs más nyitott kód-PR (áthelyezés = garantált ütközés).
- Csomag és nyilvános/internal nevek változatlanok; fájlon átnyúló `private` → `internal`.
- Ellenőrzés: `scripts/verify_move.py` – a nem-import sorok multihalmaza előtte = utána
  (csak `package`/`import`/`private→internal` eltérés engedett). Plusz a teljes JVM regresszió.
- Célméret: fájl ≤ 600 sor (kivétel max. 900, indoklással). CI-őr: `SourceSizeGuardTest`
  – új fájl > 900 sor piros; a régi óriásfájlok listája csak csökkenhet.
- A 5 forrásszöveg-teszt (`File("src/main/...")`) útvonala az áthelyezéssel együtt frissül.

## Lépések

| # | Fájl (sor) | Új fájlok | Kockázat |
|---|---|---|---|
| R0 | – | `docs/CODEMAP.md` (funkció → fájl), CLAUDE.md szabály: „előbb CODEMAP, csak a megnevezett fájl”; ~147 történeti `docs/*` → `docs/archive/`; `verify_move.py`, `SourceSizeGuardTest` | alacsony |
| R1 ✅ | HealthCoachAndSettingsScreens (4331) → 14 fájl, legnagyobb 852 | `screens/health/`: HealthTabScreen, TodayStatusSheet (+ mai csempe segédek), HealthSources, HealthTiles, WeightLog, WeeklyGoalsPanel, CoachScreens · `screens/settings/`: SettingsMainScreen, WatchAndSyncScreen, GoogleCloudSyncCard, BackupRestoreScreen, PermissionsScreen · közös `tr()` → ScreenText.kt | alacsony |
| R2 ✅ | WorkoutAndPlanningScreens (3945) → 10 fájl (`workout/`); CalendarPlannerScreen 1068 (egy függvény, később) | WorkoutTabScreen, ProgramsListScreen, ProgramCard, ProgramEditorScreen, CalendarPlannerScreen (+ PlannerFields), ExerciseLibraryScreen, ExerciseDetailScreen, AddCustomExerciseDialog | alacsony |
| R3 ✅ | ActiveMeasurementScreens (3031) → 7 fájl (`measure/`) | ActiveStrength, ActiveTimed, ActiveBilateral, ActiveOutdoorGps, IntervalSettings, WorkoutSummaryScreen, MeasurementShared | közepes (5 forrásteszt) |
| R4 ✅ | LogAndProgressScreens (2205) → 6 fájl (`log/`, `progress/`) | LogTabScreen, LogWorkoutDetail, LogRouteMap, ProgressTabScreen, ProgressVolumeAndMuscle | alacsony |
| R5 ✅ | TrainPilotCommonComponents (2152) → 7 fájl (`components/common/`); PersonalPlannerModal 1013 (egy függvény, később) | TopHeaderAndNav, PalettePickerSheet, GoalsEditDialog, PersonalPlannerModal (~960 → 2 fájl), ScrollHelpers | alacsony |
| R6 ✅ (R6a, R6b, R6c) | TrainPilotViewModel (5737 → 4406 R6a → 3122 R6b → 1401 R6c után) | R6a: VmNavigation, VmAppearance, VmPrograms, VmProgramEditor, VmCalendar, VmExerciseLibrary (UI/programok/naptár/könyvtár). R6b: VmActiveSets, VmStarterProfile, VmRestAndIntervals, VmWorkoutStart, VmExerciseSwitch, VmFinishWorkout (aktív edzés). R6c: VmLog, VmProgress, VmHealthWeight, VmHealthJournal, VmWatch, VmBackupSettings, VmStudioModes, VmMappers (napló/haladás/health/óra/backup/beállítások/leképezők). Osztályban marad a mag: állapotmezők, init, bindNativeEnvironment/reloadNativeEnvironment, előzmény-olvasás, persist*Async, pause/onAppBackground, tr. Funkciók kiterjesztésekben ugyanabban a csomagban (állapotmezők `private` → `internal`) | közepes–magas |
| R7a ✅ | Wear: WearScreens (2799) → 13 fájl (`wear/ui/`); + AnatomicalBodyAndMapComponents (1768) → 7 fájl (`components/visual/`). MainActivity (R7b) az R6 után | WearShell, WearJournal, WearCalendar, WearHealth, WearWorkout, WearSetEditors, WearSummary · telefon NavHost kiemelése | közepes (páros verzió nem változik) |
| R8 ✅ | GoogleSyncRepository (1601 → 673), HealthJournalStore (1342 → 457) | GoogleSyncMerge18 (`*18` motor), GoogleSyncTransport (HTTP/Drive/Naptár) · HealthJournalIngest, HealthJournalQueries, HealthJournalBackup. Osztály marad; `internal` kiterjesztés/felső szintű függvény, companion-tagok importtal (`…Companion.LIMIT`), törzs változatlan | közepes |

Sorrend indoka: előbb a legtöbbet szerkesztett, alacsony kockázatú képernyők, a ViewModel csak
ha a képernyők már kicsik (kisebb ütközésfelület).

## Ráfordítás
- ~11 PR, mindegyik egy CI-kör (~7 perc). Az áthelyezés szkripttel, sortartomány alapján történik
  (nem kell a fájlokat végigolvasni), ezért maga a refaktor is olcsó.
- Kiadás nem kell hozzá; a következő verzióval megy ki.

## Várható eredmény
- Legnagyobb fájl ≤ 900 sor (ma 5737); kb. 60 új, funkció nevű fájl.
- Tipikus javítás: CODEMAP (≈100 sor) + 1–2 fájl (300–600 sor) olvasása.
