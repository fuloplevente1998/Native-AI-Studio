# TrainPilot – native AI Studio app

**Continue from the latest `main`, using the current handoff:**

- [Native implementation / verification handoff](docs/AI_STUDIO_HANDOFF_2026-11-02.md)
- [Exact Hungarian AI Studio continuation prompt](docs/AI_STUDIO_CONTINUE_PROMPT.md)
- [Current validated source, signed APKs and device acceptance: PR #15](https://github.com/fuloplevente1998/Native-AI-Studio/pull/15)
- [Original audit / open task list: PR #1](https://github.com/fuloplevente1998/Native-AI-Studio/pull/1)

The existing Compose UI, original logo, native anatomy, top navigation,
`com.TrainPilot.com` identity, Room/native engine, privacy, migrations and release
signing configuration must remain intact. No fake Health/Wear/GPS data or silent
user-data reset. The native watch companion is now `:wear`; the unchanged shared
native contracts live in `:shared`.

CI uses Gradle 9.3.1 / JDK 17 / Android 36.1:

```
:app:assembleDebug :app:testDebugUnitTest
:wear:assembleDebug :wear:assembleOptimized :wear:testDebugUnitTest :shared:testDebugUnitTest
```

Download phone and Wear APKs from the same successful run. Actual manifests and
signatures are checked together; `trainpilot-apk-metadata` records package/version,
APK hashes, signer fingerprints and Maps key presence without exposing the key.
Ordinary CI debug signers are temporary. Device APKs now use the owner-managed persistent
key through `trainpilot-persistent-signing.yml` / `android-signing`; the last validated
main source is `977cdd5`, with **248/248 tests passing**. Existing Gradle signing configurations remain intact.

`TRAINPILOT_MAPS_API_KEY` is configured externally and embedded in the latest signed pair. Presence is not proof of
live Cloud authorization. Physical map tiles, watch ACK/sensors and real device
backup/restart acceptance remain separate from JVM/Compose fixture test results.

[Earlier PR #2 checkpoint (historical)](docs/MAIN_TEST_CHECKPOINT_2026-11-01.md).

Future APK uploads go to the **[TrainPilot Native Drive folder](https://drive.google.com/drive/folders/1ywHgrManynjV9uNvZfVncOmCr3SbHIb9)**.
The current handoff distinguishes proven automated results from pending device checks and preserves the existing data/privacy rules.
