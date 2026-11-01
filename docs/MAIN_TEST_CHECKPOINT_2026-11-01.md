> Historical PR #2 checkpoint. The current authoritative continuation is [AI_STUDIO_HANDOFF_2026-11-01.md](AI_STUDIO_HANDOFF_2026-11-01.md) and its linked PR #10 evidence.

# Main test checkpoint – 2026-11-01

The owner explicitly requested merging the validated phone repair to main for
physical testing and AI Studio continuation. This overrides the earlier draft-only
restriction for PR#2. Audit PR#1 stays open as the requirements record.

## Proven build and tests

- Application source a36528ad04b365a2936c8bbd03dcb2373a9fad4f.
- CI41284190084 SUCCESS; checkout5749e64bf9fdfc45a22385587f769c870a4ccb1a
  (PR merge commit). Both trees10690fd5220ddec34f76f9c6b273c75d69350ad9 match.
- :app:assembleDebug and :app:testDebugUnitTest SUCCESS.
- Raw3JUnit XML files:10tests PASS,0failures/errors/skips, including8regressions:
  45active minutes across a long pause/duplicate commands/JSON restore; overlapping
  interval union; Health per-window reads and sample-weighted pulse; missing Health
  stays unknown; persisted-profile completion policy; MM:SS/HH:MM:SS formatting;
  DST/year-boundary dates; native stopwatch pause/checkpoint/resume.
- App identity independently extracted from APK:com.TrainPilot.com,
  version1.3.71-native-test/2736. APK SHA256:
  252539830d0c13228785ef25dadad741d4610818da9fe305312ff1ede445515b
- [Debug APK ZIP](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/41284190084/artifacts/13074790214)
- [Raw tests](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/41284190084/artifacts/13073639553)
- [CI run](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/41284190084)
- Machine-readable record:MAIN_TEST_BUILD_2026-11-01.json.

The artifact is signed with a temporary CI DEBUG key. Release signing settings
were not changed. It cannot be assumed to update a differently signed installed
app; use the original AI Studio signing path for that, preserving user data.

## Implemented code in main

- Full planner profile persisted via existing validated DataStore; fields restored.
  Home setup card depends on persisted profile completion plus an active custom
  plan with real days. Existing editing UI and Room engine remain.
- Studio edits use NativeWorkoutEditing rather than overwriting pause/timer state.
  Measurement navigation/background pauses the real native session; explicit
  return resumes. Running GPS/watch-owned sessions have separate owner rules.
- Single/bilateral UI reads the existing ActiveWorkoutViewModel/NativeSetStopwatch.
  Pause does not complete a set; explicit recording uses canonical commands.
  Timers have durable native checkpoints, single-line tabular labels and hours.
- Finalization uses the canonical native snapshot/finalizer and verifies Room.
  Double-submit guarded; no invented start timestamp or outdoor done=true set.
  Only positive, explicitly completed native measurements may finalize.
- History duration is the union of active intervals; Room duration drives UI.
  HC workout queries use those same windows and weighted heart-rate counts.
- Calendar has full ISO dates, real local-zone starts/duration-based ends and
  transactional schedule+undo persistence; coalesced serialized writes prevent
  stale snapshots from re-inserting a deleted local event. Synced rows protected.
- Weight deletion is DB-first, reloads persisted data, and reports errors.
- Health screen uses native foreground provider/permission/sync handling. Removed
  fixed BMR/HR-range/sync-date and workout-route metrics; cloud actions do not
  manufacture success. Daily calories are not substituted for workout calories.
- GPS uses native target/service binding and actual active receipt. Indoor mode
  uses canonical stopwatch, not a Compose sec++ loop. Removed duplicate location
  listener. Google Maps renders real private route segments, including journal
  references; no synthetic Margitsziget route. Empty/keyless state is explicit.
- Configure TRAINPILOT_MAPS_API_KEY externally for actual map tiles. No key stored
  in GitHub. Opening/closing a route map does not control GPS.

Main modified files:app/build.gradle.kts;app/src/main/AndroidManifest.xml;
com/example/MainActivity.kt;model/{TrainPilotModels,StudioProfileCompletion,
StudioCalendarDates}.kt;ui/screens/{HomeScreen,ActiveMeasurementScreens,
StudioTimerReadout,WorkoutAndPlanningScreens,HealthCoachAndSettingsScreens,
LogAndProgressScreens}.kt;ui/components/AnatomicalBodyAndMapComponents.kt;
ui/viewmodel/TrainPilotViewModel.kt;com/repforge/app/{MainActivity,StudioWorkoutBridge,
NativeWorkoutActiveWindows,NativeWorkoutHealth}.kt;ui/workout/ActiveWorkoutViewModel.kt;
data/migration/LegacyWebViewMigrator.kt;StudioNativeRegressionTest.kt;canonicalCI.
Native references reused:settings/profile validator, NativeWorkoutSession,
PhoneWorkoutExecution/ActiveWorkoutStore, NativeWorkoutEditing/NativeSetStopwatch,
TrainPilotWorkoutFinalization, Room/legacy mapper, HealthConnectRepository,
HealthDashboardViewModel and private WorkoutRouteCoordinates. No second phone
engine/database, WebView or release signer change.

## Open work (do not claim full audit completion)

P0:
- Physical/instrumented profile+plan cold restart, set edits, long pause and actual
  process-death recovery; current JVM JSON roundtrips are not device restart proof.
- Calendar DAO failure/race/undo/restore concurrency and legacy month migration;
  date/DST unit tests alone do not prove all persistence races. Verify month/year
  selection/replan across boundaries and existing/synced data on device.
- Samsung workout-window wrapper and Wear pause-window telemetry evidence remain
  incomplete; HC active-window handling is implemented and unit tested.
- Paired-device/background/lockscreen/ownership/finalization behavior needs actual
  instrumentation and physical acceptance. Keep audit#1 checkboxes open until
  complete item-level evidence exists; this is a testable increment.

P1:
- Wear module + ACK control continuation is saved separately at
  wip/native-wear-integration-20261101 /b380bbc, NOT main or build-validated yet.
  It reuses exact migration source648e971034201511d69585cd5deecaa27eddf942;
  shared source matched byte-for-byte before unchanged extraction to :shared.
- Maps tiles require an external API key. GPS permissions/fix loss/background,
  real route persistence and map loading are not device tested.
- Google Drive/Calendar OAuth configuration and real cloud operations remain open;
  local backup works through existing paths, but no cloud success is claimed.
- Broaden Room deletion failures, provider permissions, health provenance and
  cross-source dedup tests. Actual Samsung/Wear metrics require device evidence.

## Suggested owner test

Use an AI Studio build with the existing signing key, or the CI debug APK on a
compatible test installation. Test profile+plan save/reopen; Plank and left/right
pause/record; navigate away and explicit Continue; the exact10:00/10:25/16:00/16:20
case (45minutes); save once and double-tap Save; calendar across months/DST,
delete/Undo/reopen; weight delete; indoor mode and permission-denied/actual GPS;
route map empty/key-configured cases. Report the screen/action and observed
result. Full UI/device acceptance remains with the owner.
