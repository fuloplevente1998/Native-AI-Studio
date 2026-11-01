# Backup review after PRs #3–#6 (2026-11-01)

Baseline: Native-AI-Studio/main `ef4aa24791f5041e750d4e4ed69f00e855ab7e8c`.
At the initial review, PRs #3–#5 were merged and PR #6 was still a separate draft.
Following explicit user authorization, PR #6 merged at `8e2aaacb07b2f1d9d0fcd2093404ccbd22405057`
and PR #7 at `2558adc2fb487c6c0553a6b9a4cab1466120b266`. Both fixes are now in main.
The combined run [41306853276](https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/41306853276)
passed 31/31 tests with zero failures/errors/skips. Its APK source tree equals that
main tree (`fccb0050041aa07e69daad2d7560046ec10ce76c`). The APK and SHA256 are in PR #7.

## Implemented in this branch

- Unknown JSON (including `{}`) or a malformed/missing history is rejected before
  the replacement lease and redo receipt. Previously normalization supplied an
  empty history, which allowed the replacement importer to delete existing workouts.
- Explicit canonical empty history remains valid. Supported legacy wrappers,
  prefixed Capacitor keys and `workouts` / `loggedWorkouts` arrays remain accepted.
- Invalid live photo IDs fail validation instead of being silently skipped.
  This restores the strict behavior of the native migration reference.
- The ordinary import entry reads the file signature, rather than trusting SAF
  URI extensions or provider MIME types, to select the existing JSON/ZIP importer.
- Regression tests seed a real Room workout and verify failed imports preserve
  it and the selected backup, with no replacement receipt or blocked flag.

## Native reference

TrainPilot `feat/native-android-migration` was examined at
`648e971034201511d69585cd5deecaa27eddf942`. Its replacement fence, redo journal and
health backup gate already match this app. No second database/engine was introduced;
archive checksum verification, replay and Room mapping remain in the existing manager.

## Verification and remaining work

CI must run `:app:assembleDebug :app:testDebugUnitTest` for this exact branch.
Do not infer success from the existence of an APK artifact alone.
PR #6's previous run 41293213069 had one failing recovery test (`Execution requires
main looper`). Its subsequent test blocked Main with `runBlocking`; commit 9669a07
keeps the real Main looper available and also checks preservation of files and Room.
That change was originally verified independently in PR #6 and is now included in main.

Still required: complete JSON and ZIP export/import/restart on a real device,
including photo, profile, weight, schedule and Health Journal preservation;
process-death replay at each restore checkpoint; Drive import authentication and
failure behavior; and the user's original interrupted restore reproduction.
Never delete user data, backup files or recovery receipts as a workaround.
Application ID, version, release signing and Compose design are unchanged.
