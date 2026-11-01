# Native audit implementation

Baseline: Native-AI-Studio main960b4f733ab57b1e95281451f19c1b2f3263782d.
Audit: PR#1 /MAIN_OPEN_FIXES_AND_VERIFICATION_2026-10-31.md.
Branch fix/native-audit-stability-20261101; preserve com.TrainPilot.com,
AI Studio Compose design, existing Room/engine, user data and signing configuration.
No direct main writes, release or automatic merge. Work is performed by root alone.

## File plan and required evidence

- TrainPilotViewModel.kt /HomeScreen.kt: persist full profile in the existing
  settings store, derive completion independently from actual active custom plan;
  round-trip/restart tests and editing access.
- TrainPilotViewModel.kt /ActiveMeasurementScreens.kt: native session state owns
  pause, timers, measurements and resume; keep canonical engine identity/revision,
  active intervals and explicit continue. Small-font/large-font timer UI checks.
- TrainPilotWorkoutFinalization.kt /NativeWorkoutHealth.kt /provider repositories:
  one idempotent finalizer; active interval duration and provider queries, exclude
  pause samples, sample-weighted pulse and nonduplicated energy.
- CalendarDayEvent model /ViewModel/calendar DAO: full ISO date, local-zone start
  and duration-derived end, serialized transactional edits, exact delete/undo.
- DistanceTrackingService.kt /ActiveMeasurementScreens.kt: confirmed native GPS
  start, canonical indoor session, real route map with externally configured SDK key.
- ViewModel/Health UI: DB-first verified weight deletion and error recovery;
  remove demo health values and unverified integration success messages.
- WearSyncRepository/ownership/shared: reuse existing ACK/epoch/timeout protocol;
  no secondary engine/database or fabricated success when a watch is absent.
- Canonical Android CI: assembleDebug +testDebugUnitTest, raw XML/APK artifacts;
  instrumented/device gates remain explicitly separate.

## Status

All audit items are OPEN until implementation and evidence are recorded.
The imported CI configuration is from the existing reviewed CI branch and builds
this branch, rather than claiming the previous b0b3e02 run validated current main.
No runtime/build success is claimed at this initial checkpoint.

Required regression: 10:00–10:25 +16:00–16:20 =45active minutes, with pause-period
Health samples excluded; duplicate finish/pause/resume and restart retain identity.

## First validated source

62bd8f26da5bcf269352aab0fc88fe3bf115093d, PR#2.
Actions41282771615 SUCCESS (pull_request). Raw report artifact13074301878:
3JUnit XML files,7tests PASS,0failure/error/skip. Five regressions cover exact45
active minutes across duplicate pause/resume and JSON roundtrip, overlap union,
per-active-window Health queries with weighted pulse, unknown Health and profile
completion. Debug APK artifact13074172267; bytehash/manifest verification pending.
No phone/emulator/Health/Samsung/Wear/device success is inferred from these JVM tests.

Next increment (runtime not yet tested): calendar ISO dates, local-zone start and
duration-derived end; coalesced serialized Room transaction for schedule+undo,
exact previously managed local-id deletion, synced event protection and full-month
filtering. Timer labels now fit one line and add hours using existing typography.
Durable stopwatch control wiring remains open. Push/PR duplicate CI removed;
canonical builds now run once on PR changes.
