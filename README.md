# TrainPilot – native AI Studio app

Current validated phone increment and AI Studio continuation:
[main checkpoint](docs/MAIN_TEST_CHECKPOINT_2026-11-01.md).
[Implementation PR#2](https://github.com/fuloplevente1998/Native-AI-Studio/pull/2) ·
[Audit/task list PR#1](https://github.com/fuloplevente1998/Native-AI-Studio/pull/1).

Keep the existing Compose UI, com.TrainPilot.com identity, Room/native engine and
release signing configuration. Use a repair branch; do not replace this app or
silently reset user data. The checkpoint distinguishes verified CI from remaining
instrumented/device/provider work. Wear integration is separately saved and not
part of the validated phone build yet.

CI uses Gradle9.3.1/JDK17/Android36.1 and runs
`:app:assembleDebug :app:testDebugUnitTest`, uploading debug APK and raw XML.
The CI debug key is temporary; original AI Studio/release signing is unchanged.
Configure `TRAINPILOT_MAPS_API_KEY` externally for Google Maps, never in Git.
