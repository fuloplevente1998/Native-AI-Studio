# TrainPilot – native AI Studio app

**Agent workflow and model/thinking rules: [AGENTS.md](AGENTS.md). Short continuation handoff: [WORK_HANDOFF](docs/WORK_HANDOFF.md).**

**Current source, verified APKs and pending device checks: [CURRENT_STATUS](docs/CURRENT_STATUS.md).**

**Continue from the latest `main`, using the current handoff:**

- [Native implementation / verification handoff](docs/AI_STUDIO_HANDOFF_2026-11-02.md)
- [Exact Hungarian AI Studio continuation prompt](docs/AI_STUDIO_CONTINUE_PROMPT.md)
- [Current beta fixes, source and verification status: PR #20](https://github.com/fuloplevente1998/Native-AI-Studio/pull/20)
- [Original audit / open task list: PR #1](https://github.com/fuloplevente1998/Native-AI-Studio/pull/1)

The existing Compose UI, original logo, native anatomy, top navigation,
`com.TrainPilot.com` identity, Room/native engine, privacy, migrations and release
signing configuration must remain intact. No fake Health/Wear/GPS data or silent
user-data reset. The native watch companion is now `:wear`; the unchanged shared
native contracts live in `:shared`.

CI uses Gradle 9.3.1 / JDK 17 / Android 36.1 and compiles the real phone/Wear sources for the complete JVM regressions:

```
:app:testDebugUnitTest :wear:testDebugUnitTest :shared:testDebugUnitTest
```

The debug variant here is only the existing test harness (including the Compose test Activity); CI no longer packages debug or extra optimized test APKs. Device installers come only from `trainpilot-persistent-signing.yml` / `android-signing` on main:

```
:app:assembleRelease :wear:assembleRelease
```

The phone artifact is release; Wear release already uses R8 and resource shrinking. Download phone and Wear APKs from the same successful signed run. The release workflow also runs all JVM tests and verifies actual manifests, package/version, APK hashes, pinned persistent signer, Maps configuration and phone/Wear capability/identity. `trainpilot-apk-metadata` from older debug CI runs is historical; new verification JSON is bundled with the signed pair.

Historical 2738 checkpoint: application
main source was `b96982d`: **0.1.0-beta / 2738**, after **50/50 targeted regressions**.
**277/277 full tests pass** in signed Actions `41492179428` (108 phone + 169 Wear; no failures/errors/skips). Signed APKs and source/signer proof
are recorded in the handoff and PR #22. Existing Gradle signing configurations remain intact.

`TRAINPILOT_MAPS_API_KEY` is configured externally and embedded in the latest signed pair. Presence is not proof of
live Cloud authorization. Physical map tiles, watch ACK/sensors and real device
backup/restart acceptance remain separate from JVM/Compose fixture test results.

[Earlier PR #2 checkpoint (historical)](docs/MAIN_TEST_CHECKPOINT_2026-11-01.md).

Future APK uploads go to the **[TrainPilot Native Drive folder](https://drive.google.com/drive/folders/1ywHgrManynjV9uNvZfVncOmCr3SbHIb9)**.
The current handoff distinguishes proven automated results from pending device checks and preserves the existing data/privacy rules.

Device-reported progress/Health/workout acceptance checklist: [tracker](docs/PROGRESS_HEALTH_BUG_TRACKER_2026-11-02.md), [GitHub #19](https://github.com/fuloplevente1998/Native-AI-Studio/issues/19).
