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
| Közös kijelölés és telt gomb feliratkontraszt (#349) | `ex/ui/theme/ThemeSelectionColors.kt`, teszt `app/src/test/java/com/example/ThemeSelection349Test.kt` |
| Saját téma (#350): modell, kontrasztfeloldás, hordozható JSON | `ex/ui/theme/CustomThemeDraft.kt`, `CustomThemeResolver.kt`, `CustomThemeCodec.kt` |
| Kyant Liquid Glass – jóváhagyott main-alapú K0–K5 integráció | `docs/KYANT_GLASS_PLAN.md`; K0: `gradle/libs.versions.toml`, `app/build.gradle.kts`, `rf/ui/journal/ExternalRoutePreview.kt`, `app/src/main/assets/third-party-licenses/kyant-backdrop-NOTICE.txt`, `.github/workflows/trainpilot-android-ci.yml` |
| Kyant központi anyagszerepek / ablakhoz kötött rétegek (K1) | `ex/ui/theme/KyantMaterialSpec.kt` (Panel, Tile, Navigation, Selection, Button, Input, Overlay; 0–100), `KyantGlass.kt` (public Backdrop API, scene/content guard), `GlassAwareScaffold.kt`, `TrainPilotGlassEngine.kt`, `GlassContentBackdrop.kt`, `ex/ui/components/TrainPilotModalBottomSheet.kt`; `app/src/test/java/com/example/KyantMaterialSpecTest.kt` |
| Ötödik Liquid Glass család (#350) | `ex/ui/theme/LiquidGlassPalettes.kt`, `TrainPilotGlassEngine.kt`, `GlassContentBackdrop.kt`, `GlassAwareScaffold.kt`, `ex/ui/components/common/LiquidGlassControl.kt`, `StudioPaletteCategories.kt`; terv és aktuális hatókör: `docs/LIQUID_GLASS_THEMES.md` |
| Liquid Glass optika/motor (#373): csúszka→paraméter leképezés, kapu, mozgásszabályok; AGSL forrás + egyszer fordított programok; lap/kártya anyag; élő tartalom-lencse; csúszó nav pill; nyomás-rugó | `ex/ui/theme/GlassOptics.kt`, `GlassShaders.kt`, `TrainPilotGlassEngine.kt`, `GlassContentBackdrop.kt`, `GlassMotionState.kt`, `ex/ui/components/common/GlassSelectionPill.kt`, `rf/ui/theme/TrainPilotPressFeedback.kt` (`glassPressMotion`); érintési lencse-buborék `ex/ui/theme/GlassTouchBubble.kt`; teszt `app/src/test/java/com/example/LiquidGlassOptics373Test.kt`, `LiquidGlassNav373Test.kt` |
| Liquid Glass élő háttér + saját háttérkép (2809/1): foltok, őr, órajel, gyökér-háttér, lap-háttér segédek; kép import/tár; csempe-festék (2809/3); keretkapu (2809/2) | `ex/ui/theme/GlassScene.kt`, `GlassShaders.kt` (`SCENE`, `SCENE_PHOTO`), `GlassAwareScaffold.kt` (`trainPilotPageBackdrop`, `trainPilotScreenBackdrop`), `rf/GlassBackdropImages.kt`, `ex/ui/components/common/LiquidGlassControl.kt` (választó), `ex/ui/theme/LiquidGlassPalettes.kt` (`glassTile`), `rf/ui/theme/TrainPilotFrames.kt` (`trainPilotFrame`, `trainPilotFrameStroke`, `liquidGlassDropsStroke`); teszt `LiquidGlass2809Test.kt`, `GlassBackdropStore2809Test.kt` |
| Liquid Glass iOS-szerű anyag (2810): fehéres tónus, háttér-fénysűrűség korlát és kontrasztmodell, kártya/csempe út, fejléc alatti fülek | `ex/ui/theme/GlassContrast.kt`, `GlassOptics.kt`, `GlassShaders.kt`, `LiquidGlassPalettes.kt` (`glassCardFill`, `glassTile`, `glassFallbackPaint`), `GlassAwareScaffold.kt` (`glassUnderlapRoute`); teszt `LiquidGlass2810Test.kt` |
| Saját téma lista / szerkesztő / előnézet / színvezérlők | `ex/ui/components/common/CustomThemeSection.kt`, `CustomThemeEditorSheet.kt`, `CustomThemePreview.kt`, `CustomThemeColorControls.kt`, `CustomThemeColorWheel.kt`; wireframe `docs/CUSTOM_THEME_EDITOR_350.md` |
| Saját téma tartós CRUD / alkalmazás / Wear színek / backup | `rf/data/store/TrainPilotSettingsStore.kt`, `rf/MainActivity.kt`, `rf/data/migration/LegacyWebViewMigrator.kt`, `wear/.../WearHomeTheme.kt` (`forWatch()`: sötét paletta → sötét óraalap; 2809: világos paletta → világos óraalap, a szöveg/vezérlő sötétedik; kontrasztőr 4,5:1 / 3:1, a tárolt paletta változatlan) |
| Saját téma teszt | `app/src/test/java/com/example/CustomTheme{Draft,Resolver,Store,Editor}350Test.kt`, `LiquidGlass350Test.kt`, `wear/src/test/java/com/repforge/app/wear/WearThemePersistenceRegressionTest.kt`, `Wear2808Test.kt` (óra kontraszt, pulzussor, lépés-frissesség, lünetta-ívek) |
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
| Pihenő dokk / kompakt pihenő | `ex/ui/components/StudioRestTimerDock.kt`, `StudioCompactRestControls.kt`, `rf/StudioRestTimer.kt`; görgetésfüggő nagy/kompakt (csak felhasználói görgetés): `ex/ui/components/RestDockScrollSizing.kt` |
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
| Külső edzés #359 (HC session modell, Samsung jelöltek, Room import, kézi + opt-in előtéri automatikus import, saját forrású metrikák) | `rf/ExternalWorkout.kt`, `rf/ExternalWorkoutImport.kt`, `rf/ExternalWorkoutAutoImport.kt`, `rf/ExternalWorkoutMetrics.kt`, `rf/ExternalSessionEvidence.kt`, `rf/ExternalWorkoutSourceDeletion.kt`, `rf/ExternalWorkoutAssociation.kt`, `rf/ui/progress/ExternalProgressSummary.kt`, `rf/ui/journal/JournalExternalWorkoutMetrics.kt`, `rf/ui/journal/ExternalHeartRateZones.kt`, `rf/ui/journal/ExternalRoutePreview.kt`, `rf/ExternalRouteStore.kt`, `rf/HealthConnectJournal.kt`, `rf/HealthConnectLegacyBridge.kt`, `rf/HealthConnectRepository.kt`, `rf/SamsungSdkReader.java` |
| Importált edzés neve (#359, `userTitle359` a forrás-pillanatképen kívül, szinkron nem írja felül) és forrás csak a Forrás fülön | `rf/NativeJournalEditor.kt` (`USER_TITLE`, `savedTitle`), `rf/ui/journal/JournalEditorDialog.kt` (`journal_editor_title`), `ex/model/TrainPilotModels.kt` (`externalUserTitle`, `externalSourceLabel`), `ex/ui/screens/log/LogWorkoutDetailScreen.kt`, `LogTabScreen.kt`; teszt `app/src/test/java/com/repforge/app/ExternalWorkoutName359Test.kt` |
| Alvásösszesítő telefon + óra (#367/9): közös unió, telefon Today-csempe értéke a home snapshotban, órán éjszakai szakaszok összege | `shared/.../SleepNightTotal.kt`, `rf/NativeDailyHealthDetails.kt` (`sleepMinutes`), `rf/WatchSleepSummary367.kt`, `rf/WearSyncRepository.kt` (`sleepSummary367`), `wear/.../WatchHomeStore.kt`, `wear/.../WearDailyHealth.kt` (`wearSleepDisplay` friss `computedAt` ≤`WEAR_PHONE_SLEEP_FRESH_MS`, `wearSleepSpansAfter`); teszt `SleepNightTotalTest`, `WatchSleepSummary367Test`, `WearDailyHealthTest` |
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
| #351 automatikus órás mérés: telefonos indítási kérés, élő mérési ACK, előtéri fallback | `ex/ui/viewmodel/VmWatchMeasurement.kt`, `ex/ui/components/StudioWorkoutHandoverBar.kt`, `rf/WearSyncStoresAndHandoff.kt`, `wear/.../WearHealthService.kt`, `WearBackgroundMeasurement.kt`, `WearBackgroundSensorAccess.kt`, shared `WearMeasurementRequest351.kt` |
| Telefon oldali szinkron, távoli mérésindítás | `rf/WearSyncRepository.kt`, `rf/WearSyncStoresAndHandoff.kt`, `rf/WearCommandListenerService.kt` |
| Közös szerződések (ownership/epoch/ACK, GPS csomag) | `shared/.../com/repforge/shared/*` |
| Óra UI (menü, napló, naptár, egészség, edzés, összegzés) | `wear/.../ui/`: `WearAppRoot.kt` (gyökér, navigáció), `WearMenuScreens.kt`, `WearJournalScreens.kt`, `WearCalendarScreens.kt`, `WearHealthScreen.kt`, `WearWorkoutScreens.kt`, `WearWorkoutControlOverlay.kt` (ideiglenes vezérlők és peremvédett edzésgesztus), `WearWorkoutV2.kt` (sorozatérték, korábbi teljesítmény, pihenő), `WearTimedSetScreens.kt`, `WearValueControls.kt`, `WearWorkoutMenuAndSummary.kt`, `WearFinishPreview.kt` (mentés előtti összegzés, meglévő lezárási pillanatkép), `WearControlTargets367.kt` (célazonosított UI-callbackok), `wear/.../WearWatchControls367.kt` (óratulajdonú pihenő/pause tartós parancs), `app/.../NativeWatchControlAdmission367.kt`, `NativeTransferredRestAdmission367.kt` (telefonos validáció és ACK), `WearPhoneControls367.kt`, `PhoneRemotePause367.kt`, `PhoneRemoteRest367.kt` (natív telefon-tulajdonú távoli vezérlés), `shared/.../IntervalTimeline193.kt` (fázisonkénti opcionális pihenőfelülírás, átadható checkpoint), `WearUiPrimitives.kt`/`WearUiTokens.kt`; `WearMainNavigation.kt` (főlapok, szélső visszagesztus megőrzése), `WearDailyHealthScreen.kt` (napi helyi adatok, engedély), `WearDailyPulse.kt` (előtéri passzív pulzus + napi összeg callback), `wear/.../WearDailyHealth.kt` (független passzív mérési cache, `WearDailyHealthRestoreReceiver` újraindítás/frissítés utáni feliratkozás), `wear/.../WearHomeDashboard.kt` + `WearHomeDashboardState.kt` (stabil pulzussor `wearHomePulse`, lünetta-ívek `wearHomeArcLayout`), `WearMenu216.kt` |
| Óra edzésképernyő #367 2. fázis (3.1–3.8): egy elsődleges alsó gomb minden képernyőtípuson (állapotgép), rögzített pulzussor a gomb felett, stabil stopper-arc, vezérlő-overlay felirattal, érintészár 3 mp, lapozási küszöbök | `wear/.../WearWorkoutFooter367.kt` (`wearFooterState`, `wearWorkoutPulse`, `wearLiveMovementRow`, `wearShowPreviousRow`, stopper-konstansok), `ui/WearWorkoutFooterUi367.kt` (`WearPrimaryFooter`, `WearPinnedPulse`, `WearTimerFace`), `WearActionLayout.kt` (`wearWorkoutPageLayout`), `ui/WearUiPrimitives.kt` (`FixedRoundPanel` pinned/footerSize), `ui/WearWorkoutScreens.kt` (`WorkoutPanel`, `WearWorkoutFooter`, `WearQuickUndo`), `ui/WearTimedSetScreens.kt`, `ui/WearWorkoutControlOverlay.kt`, `WearPageSwipe367.kt` (`wearSwipeMayStart`, `wearSwipeCommits`, `wearSwipePageDirection`, küszöbök), `ui/WearMainNavigation.kt`, `WearTouchLock189.kt` + `WearTouchLockedScreen189.kt` (`wearUnlockHoldInside`, `wearUnlockSecondsLeft`); teszt `wear/src/test/.../Wear367Phase2Test.kt` |
| Óra adatfogadás, tulajdon, GPS | `wear/.../WearDataListenerService.kt`, `WearOwnership193.kt`, `WearGps193.kt` |
| 2809 fantom edzés ellen: telefonos lezárás tombstone, elakadt telefonos tükör elejtése, explicit telefonos elvetés (marker + órás ACK), újraindítás utáni automatikus mérés kapuja; telefonon reconnect csak terminális vetület / élesített mérés | `wear/.../WearPhoneClosure2809.kt`, `WorkoutSnapshotStore.kt` (`save`), `rf/PhoneMeasurementReconnect351.kt`, `rf/WearCommandListenerService.kt`, `rf/TrainPilotWorkoutFinalization.kt` (`discardWorkout`), `rf/ActiveWorkoutStore.kt` (`clear`), shared `WorkoutOwnership193.PHONE_DISCARD_2809`; teszt `wear/.../Wear2809PhantomWorkoutTest.kt`, `app/.../WatchPhantomWorkout2809RegressionTest.kt` |

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

| Kyant K2 közös vezérlők és integráció | `ex/ui/theme/GlassSurfaces.kt` (state tint), `LiquidGlassPalettes.kt` (glassControlPaint), `TrainPilotGlassEngine.kt` (cheap Button/Input/Performance), `rf/ui/theme/TrainPilotButton.kt` (audited primary action), `ex/ui/components/TrainPilotControls.kt`, `GlassContentBackdrop.kt` (nested header controls), `KyantK2IntegrationTest.kt` |

| K2 overlay and switches | `ex/ui/components/TrainPilotControls.kt` (`TrainPilotSwitch`), `TrainPilotAlertDialog.kt` (own-window glass overlay), `TrainPilotModalBottomSheet.kt` (default handle), `ex/ui/screens/StudioWorkoutModesDialog.kt`, `ex/ui/screens/settings/SettingsMainScreen.kt` |

| Kyant dialog window isolation | `ex/ui/theme/KyantWindowOverlay.kt` (Android-only backdrop types behind glass/API/optics gate; common alert dialog remains JVM-compatible candidate) |

| K2 popup and sheet finish | `ex/ui/components/TrainPilotDropdownMenu.kt`, `ex/ui/theme/GlassSurfaces.kt` (`glassOverlayFill`), `KyantWindowOverlay.kt` (közös Overlay-lencse, 2812/2), `GlassSurfaces.kt` (`sheetUsesGlass`, `LocalGlassOverlay`, ablakon belüli szerepfesték/perem), `app/src/test/java/com/example/LiquidGlassOverlay2812Test.kt`, `TrainPilotModalBottomSheet.kt`, status banner in `ex/MainActivity.kt` |

| K3 scene paint cache | `ex/ui/theme/GlassScenePaintCache.kt` (bounded last-value slots), `GlassScene.kt` (host-owned cache), `KyantK3CacheTest.kt` (reuse/invalidation) |

| K3 single root scene | `ex/ui/theme/KyantGlass.kt` (`KyantSceneBackground`, reuse same captured layer), `GlassAwareScaffold.kt` (empty sibling before scaffold), `KyantK3CacheTest.kt` (source contract) |

| K3 battery saver | `ex/ui/theme/GlassPowerSave.kt` (host-only power mode observer), `GlassScene.kt` (static drift gate), `KyantK3CacheTest.kt` (mode matrix) |

| 2813 görgethető tartalom és mért fix glass sávok | `ex/ui/theme/GlassScrollChrome.kt`, `MeasuredScrollChrome.kt`, `GlassAwareScaffold.kt`, `GlassContentBackdrop.kt` (külön chrome gate, list capture, in-scroll top/bottom padding); teszt `GlassScrollChrome2813Test.kt` |

| 2814 edzés-prompt és értékmenü | `ex/ui/components/WorkoutFeedbackPopup.kt`, `WorkoutEditorAppearance.kt`, `StudioActiveWorkoutPresentation.kt`, `TrainPilotAlertDialog.kt`, `TrainPilotDropdownMenu.kt`, `StudioStrengthSetCard.kt`; teszt `WorkoutEditorAppearance2814Test.kt` |

| Liquid Drop 2814 (L1–L6) | `ex/ui/theme/LiquidDropMaterial.kt` (policy, bounded paint, same-window lens), `KyantMaterialSpec.kt`, `KyantGlass.kt`, `TrainPilotGlassEngine.kt` (cached bevel); common `TrainPilotControls.kt`, `rf/ui/theme/TrainPilot{Button,OutlinedButton,ActionButtons}.kt`; teszt `LiquidDrop2814Test.kt` |

| 2814/4 edzés-egészségadat audit | `docs/WORKOUT_HEALTH_2814.md`; `rf/NativeLiveWorkoutHealth.kt`, `StudioWorkoutSummaryPresentation.kt`, `NativeWorkoutHealth.kt`, `NativeWorkoutActiveWindows.kt`, `NativeWorkoutHealthSummary.kt`, `SamsungSdkReader.java`, `SamsungHealthRepository.kt`; teszt `WorkoutHealth2814ContractTest.kt` |
