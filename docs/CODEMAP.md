# Kódtérkép (funkció → fájl)

Előbb ezt olvasd, utána csak a megnevezett fájlt. Nagy fájlban: `rg -n "fun Név" fájl` és célzott
sortartomány. Gyökerek: telefon `app/src/main/java/`, óra `wear/src/main/java/com/repforge/app/wear/`,
közös szerződések `shared/` (`com.repforge.shared`). Rövidítések: `ex/` = `com/example/`,
`rf/` = `com/repforge/app/`. Új funkció/fájl → új sor ide. A szétszedés (docs/REFACTOR_PLAN.md) után a
nagy fájlok sorai frissülnek.

## Belépés, keret
| Funkció | Fájl |
|---|---|
| Indító Activity (manifest LAUNCHER) → `TrainPilotAppRoot` | `rf/MainActivity.kt` |
| App gyökér, fül/képernyő-váltás, lapok, health/settings VM bekötés | `ex/MainActivity.kt` (`TrainPilotAppRoot`) |
| Állapot (UI state), navigáció enumok | `ex/ui/viewmodel/TrainPilotUiState.kt`, `ex/model/TrainPilotModels.kt` |
| Fő ViewModel mag (állapotmezők, init, natív kötés/újratöltés, előzmény, perzisztencia, szünet) | `ex/ui/viewmodel/TrainPilotViewModel.kt` (1.4k) |
| VM kiterjesztések (R6a): navigáció/vissza; téma, nyelv, célok, gyors edzés lap; programok (könyvtár-mutáció, átnevezés, másolás, törlés, aktiválás); programszerkesztő (napok, gyakorlatok, sorrend, új program); naptár (törlés, újratervezés, esemény, visszavonás); gyakorlatkönyvtár + saját gyakorlat | `ex/ui/viewmodel/VmNavigation.kt`, `VmAppearance.kt`, `VmPrograms.kt`, `VmProgramEditor.kt`, `VmCalendar.kt`, `VmExerciseLibrary.kt` |
| VM aktív edzés (R6b): sorozat-szerkesztés/visszavonás; kezdő profil + ajánlott program; pihenő, intervall, összegző/gyakorlat-visszajelzés; edzésindítás (tervezett, coach, gyors, sor, kültéri/GPS); gyakorlatváltás/léptetés, kültéri metrika; befejezés + mentés | `ex/ui/viewmodel/VmActiveSets.kt`, `VmStarterProfile.kt`, `VmRestAndIntervals.kt`, `VmWorkoutStart.kt`, `VmExerciseSwitch.kt`, `VmFinishWorkout.kt` |
| VM napló/haladás/health/beállítások (R6c): naplókeresés, edzésrészlet, útvonal, PR; haladás/statisztika, izomterhelés, videó; health lap, testsúly, privát fotó, forrás, health-napló újratöltés; óra vezérlés/eszközinfó; backup, felhő-frissítés, platform/adatvédelmi hozzáférés, banner, natív beállítás; Studio módok + időzítő/hang/rezgés; entitás-leképezők (gyakorlat, program, PR, testsúly) | `ex/ui/viewmodel/VmLog.kt`, `VmProgress.kt`, `VmHealthWeight.kt`, `VmHealthJournal.kt`, `VmWatch.kt`, `VmBackupSettings.kt`, `VmStudioModes.kt`, `VmMappers.kt` |
| Felső fejléc, alsó nav, piros X | `ex/ui/components/common/TopHeaderAndNav.kt` |
| Palettaválasztó lap, céldialógus | `ex/ui/components/common/PalettePickerSheet.kt`, `ex/ui/components/common/GoalsEditDialog.kt` |
| Gyakorlatvideó gomb, görgetés-segédek | `ex/ui/components/common/ExerciseVideoButton.kt`, `ex/ui/components/common/ScrollHelpers.kt` |
| Alulról nyíló lapok, útvonalak | `ex/ui/components/TrainPilotSheetRoutes.kt`, `TrainPilotScreenSheet.kt`, `TrainPilotModalBottomSheet.kt` |
| Gombok, chipek, nyomásvisszajelzés, keretek | `ex/ui/components/TrainPilotControls.kt`, `rf/ui/theme/TrainPilotPressFeedback.kt`, `TrainPilotFrames.kt`, `TrainPilotButton.kt` |
| Témák/paletták, színek | `ex/ui/theme/TrainPilotPalettes.kt`, `Theme.kt`, `rf/ui/theme/NativeThemeTokens.kt` |
| Szövegek (4 nyelv) | `ex/localization/TrainPilotStrings.kt`; képernyőkben `tr(lang, hu, en, de, ro)` (`ex/ui/screens/ScreenText.kt`) |

## Kezdőlap, gyors edzés
| Funkció | Fájl |
|---|---|
| Kezdőlap | `ex/ui/screens/HomeScreen.kt` |
| Mai állapot csempék és lenyíló lap | `ex/ui/screens/StudioTodayStatusGrid.kt`, `ex/ui/screens/health/HealthTodayStatusSheet.kt`, `ex/ui/screens/health/HealthTodayTileExtras.kt`, `ex/ui/screens/health/HealthTiles.kt` |
| Heti célok | `ex/ui/components/WeeklyGoals.kt`, `ex/ui/screens/health/HealthWeightAndGoals.kt` (`HealthWeeklyGoalsPanel`) |
| Gyors edzés lap | `ex/ui/components/QuickWorkoutHubSheet.kt` |
| Coach tanács kártya | `ex/ui/components/HomeCoachAdviceCard.kt`, `rf/ui/coach/*` |

## Edzés fül, programok, naptár, könyvtár
| Funkció | Fájl |
|---|---|
| Edzés fül, módcsempék, napkártyák | `ex/ui/screens/workout/WorkoutTabScreen.kt`, `ex/ui/screens/WorkoutTabDayCards.kt` |
| Programlista, programkártya, szerkesztő | `ex/ui/screens/workout/ProgramsListScreen.kt`, `ex/ui/screens/workout/ProgramCard.kt`, `ex/ui/screens/workout/ProgramEditorScreen.kt`; mentés `rf/StudioProgramSave.kt`, `rf/NativeProgramEditor.kt` |
| Naptár tervező | `ex/ui/screens/workout/CalendarPlannerScreen.kt`, `ex/ui/screens/workout/PlannerControls.kt`; logika `ex/model/StudioCalendarPlanning.kt`, `rf/ui/calendar/*` |
| Gyakorlatkönyvtár, részletek, saját gyakorlat | `ex/ui/screens/workout/ExerciseLibraryScreen.kt`, `ex/ui/screens/workout/ExerciseDetailScreen.kt`, `ex/ui/screens/workout/AddCustomExerciseDialog.kt`; `trW` a `ex/ui/screens/workout/WorkoutScreenText.kt` |
| Személyes tervező (első profil) | `ex/ui/components/StudioPersonalPlannerDialog.kt`, `ex/ui/components/common/PersonalPlannerModal.kt` |

## Aktív edzés, mérés
| Funkció | Fájl |
|---|---|
| Erő / időzített / kétoldalas / kültéri GPS képernyő | `ex/ui/screens/measure/Active{Strength,Timed,Bilateral}WorkoutScreen.kt`, `ex/ui/screens/measure/ActiveOutdoorGpsScreen.kt`, közös: `ex/ui/screens/measure/MeasurementShared.kt` |
| Felső sáv, „Legutóbb” sor | `ex/ui/components/StudioActiveWorkoutTop.kt`, `StudioWorkoutHandoverBar.kt` |
| Pihenő dokk / kompakt pihenő | `ex/ui/components/StudioRestTimerDock.kt`, `StudioCompactRestControls.kt`, `rf/StudioRestTimer.kt` |
| Sorozatkártya | `ex/ui/components/StudioStrengthSetCard.kt` |
| Intervallum | `ex/ui/screens/measure/IntervalSettingsScreen.kt`, `rf/IntervalTimerService.kt` |
| GPS műszerfal, útvonal | `ex/ui/screens/OutdoorGpsDashboard.kt`, `rf/DistanceTrackingService.kt`, `rf/GpsRouteStore.kt` |
| Edzésösszegzés | `ex/ui/screens/measure/WorkoutSummaryScreen.kt` |
| Motor, időzítő-tulajdonos, mentés/lezárás | `rf/PhoneWorkoutEngine.kt`, `rf/PhoneWorkoutExecutionStore.kt`, `rf/TrainPilotWorkoutFinalization.kt`, `rf/ActiveWorkoutStore.kt` |

## Napló, fejlődés, statisztika
| Funkció | Fájl |
|---|---|
| Napló lista, edzés részletei, térkép | `ex/ui/screens/log/LogTabScreen.kt`, `ex/ui/screens/log/LogWorkoutDetailScreen.kt`, `ex/ui/screens/log/LogRouteMapScreen.kt` |
| Fejlődés, volumen, izomtérkép | `ex/ui/screens/progress/ProgressTabScreen.kt`, `ex/ui/screens/progress/ProgressVolumeAndMuscle.kt`, `ex/ui/components/visual/MuscleHeatmapCard.kt`, `visual/AnatomicalFigure.kt`, `ex/ui/screens/StudioVolumeChart.kt` |
| Statisztikák, kardió | `ex/ui/screens/StudioStatisticsScreen.kt`, `rf/ui/progress/NativeCardio*` |
| PR rekordok | `ex/ui/screens/LoggedPrRecords.kt`, `rf/PersonalRecordCalculator226.kt` |
| Diagram koppintás/kijelölés | `ex/ui/components/TrainPilotChartSelection.kt` |
| Útvonaltérkép, gyakorlat-illusztráció, programtípus embléma, felszerelés-összegzés, óra/HC jelvény | `ex/ui/components/visual/` (`WorkoutRouteMapBox.kt`, `ExercisePostureIllustration.kt`, `ProgramTypeEmblem.kt`, `ProgramEquipmentSummary.kt`, `IntegrationBadges.kt`) |

## Egészség, súly, Coach
| Funkció | Fájl |
|---|---|
| Egészség fül (+ közös segédek `health/HealthScreenShared.kt`) | `ex/ui/screens/health/HealthTabScreen.kt` |
| Forrásválasztás, szinkron részletei, intervallum chipek | `ex/ui/screens/health/HealthSourcesContent.kt` |
| Testsúlynapló, fotó | `ex/ui/screens/health/HealthWeightAndGoals.kt` (`WeightLogBody`), `rf/ui/progress/NativeWeightJournalRepository.kt`, `rf/NativeWeightPhotos.kt` |
| Health VM, előtéri frissítés, kapu | `rf/ui/health/HealthDashboardViewModel.kt`, `NativeHealthAutoRefreshGate.kt`, `NativeHealthAppRefreshPolicy.kt` |
| Szinkron ütemezés (előtér/háttér percek, Google auto) | `rf/NativeSyncSchedule.kt` |
| HC háttér job, BLE óra szolgáltatás | `rf/HealthBackgroundServices.kt` |
| Health Connect / Samsung olvasás, napló tár | `rf/HealthConnectRepository.kt`, `rf/SamsungHealthRepository.kt`, `rf/NativeHealthSync.kt` · napló tár: `rf/HealthJournalStore.kt` (séma, meta, tranzakció, beállítás, sync-állapot), `rf/HealthJournalIngest.kt` (rekord/nap írás), `rf/HealthJournalQueries.kt` (projection, page, forrásnap), `rf/HealthJournalBackup.kt` (export, restore, erase) |
| Külső edzés #359 (HC session modell, Samsung jelöltek, Room import, kézi + opt-in előtéri automatikus import, saját forrású metrikák) | `rf/ExternalWorkout.kt`, `rf/ExternalWorkoutImport.kt`, `rf/ExternalWorkoutAutoImport.kt`, `rf/ExternalWorkoutMetrics.kt`, `rf/ExternalWorkoutSourceDeletion.kt`, `rf/ExternalWorkoutAssociation.kt`, `rf/ui/journal/JournalExternalWorkoutMetrics.kt`, `rf/HealthConnectJournal.kt`, `rf/HealthConnectLegacyBridge.kt`, `rf/HealthConnectRepository.kt`, `rf/SamsungSdkReader.java` |
| Külső edzés import panel, automatikus import kapcsoló, négy nyelv | `ex/ui/screens/health/ExternalWorkoutImportPanel.kt` (meglévő szinkronból tárolt jelöltek) |
| Kalória/energia | `rf/HealthProjectionsAndEnergy.kt` |
| Coach képernyő, készenlét | `ex/ui/screens/health/CoachDetailScreen.kt`, `rf/ui/coach/NativeCoachReadiness.kt` |

## Beállítások, felhő, mentés
| Funkció | Fájl |
|---|---|
| Beállítások főoldal | `ex/ui/screens/settings/SettingsMainScreen.kt` |
| Óra és szinkron oldal | `ex/ui/screens/settings/WatchAndSyncScreen.kt` |
| Google Cloud kártya (állapot, auto, „Szinkron most”, leválasztás lap) | `ex/ui/screens/settings/GoogleCloudSyncCard.kt` (`GoogleDisconnectSheet`) |
| Google szinkron motor, Drive-összefésülés | `rf/GoogleSyncRepository.kt` (auth, Drive/Naptár szinkron, karbantartás), `rf/GoogleSyncMerge18.kt` (#18 összefésülő motor `*18`), `rf/GoogleSyncTransport.kt` (HTTP, Drive-fájl/fotó, naptár, hibaüzenet), `rf/GoogleAutoSyncWorker.kt`, `rf/ui/settings/SettingsViewModel.kt`, `rf/ui/settings/NativeGoogleMaintenance*.kt` |
| Mentés / visszaállítás | `ex/ui/screens/settings/BackupRestoreScreen.kt`, `rf/BackupRestoreManager.kt`, `rf/BackupJsonIO.kt` |
| Engedélyek, privát fotók | `ex/ui/screens/settings/PermissionsScreen.kt`, `rf/NativePrivacyControls.kt` |
| Adatbázis, migráció, DataStore | `rf/data/db/*`, `rf/data/migration/*`, `rf/data/store/TrainPilotSettingsStore.kt` |

## Telefon ↔ óra
| Funkció | Fájl |
|---|---|
| Telefon oldali szinkron, távoli mérésindítás | `rf/WearSyncRepository.kt`, `rf/WearSyncStoresAndHandoff.kt`, `rf/WearCommandListenerService.kt` |
| Közös szerződések (ownership/epoch/ACK, GPS csomag) | `shared/.../com/repforge/shared/*` |
| Óra UI (menü, napló, naptár, egészség, edzés, összegzés) | `wear/.../ui/`: `WearAppRoot.kt` (gyökér, navigáció), `WearMenuScreens.kt`, `WearJournalScreens.kt`, `WearCalendarScreens.kt`, `WearHealthScreen.kt`, `WearWorkoutScreens.kt`, `WearTimedSetScreens.kt`, `WearValueControls.kt`, `WearWorkoutMenuAndSummary.kt`, `WearUiPrimitives.kt`/`WearUiTokens.kt`; `WearHomeDashboard.kt`, `WearMenu216.kt` |
| Óra adatfogadás, tulajdon, GPS | `wear/.../WearDataListenerService.kt`, `WearOwnership193.kt`, `WearGps193.kt` |

## Teszt, CI, kiadás
| Funkció | Fájl |
|---|---|
| Gyűjtő regressziós tesztek | `app/src/test/java/com/example/Collector*RegressionTest.kt` |
| Fájlméret-őr, áthelyezés-ellenőrzés | `app/src/test/java/com/example/SourceSizeGuardTest.kt`, `scripts/verify_move.py` |
| CI, aláírt kiadás, Play | `.github/workflows/trainpilot-android-ci.yml`, `trainpilot-persistent-signing.yml`, `trainpilot-play-internal-aab.yml` |
| Verzió | `app/build.gradle.kts`, `wear/build.gradle.kts` |
| Play kiadási megjegyzés | `docs/PLAY_LISTING.md` |

Megjegyzés: a `rf/ui/{home,journal,calendar,programs,workout,settings}/` alatti `*Screen.kt` fájlok egy része
régi natív képernyő; mielőtt módosítod, `rg` -vel nézd meg, hívja-e valami (az élő UI a `com.example` fa).
