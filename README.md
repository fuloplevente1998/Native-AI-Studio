# TrainPilot – native AI Studio app

**Continue from the latest `main`, using the current handoff:**

- [Native implementation / verification handoff](docs/AI_STUDIO_HANDOFF_2026-11-01.md)
- [Exact Hungarian AI Studio continuation prompt](docs/AI_STUDIO_CONTINUE_PROMPT.md)
- [Integrated source, CI, APK hashes and remaining limitations: PR #10](https://github.com/fuloplevente1998/Native-AI-Studio/pull/10)
- [Original audit / open task list: PR #1](https://github.com/fuloplevente1998/Native-AI-Studio/pull/1)

The existing Compose UI, original logo, native anatomy, top navigation,
`com.TrainPilot.com` identity, Room/native engine, privacy, migrations and release
signing configuration must remain intact. No fake Health/Wear/GPS data or silent
user-data reset. The native watch companion is now `:wear`; the unchanged shared
native contracts live in `:shared`.

CI uses Gradle 9.3.1 / JDK 17 / Android 36.1:

```
:app:assembleDebug :app:testDebugUnitTest
:wear:assembleDebug :wear:testDebugUnitTest :shared:testDebugUnitTest
```

Download phone and Wear APKs from the same successful run. Actual manifests and
signatures are checked together; `trainpilot-apk-metadata` records package/version,
APK hashes, signer fingerprints and Maps key presence without exposing the key.
CI debug signers are temporary; they do not establish update continuity with a
previous installation. Original AI Studio/release signing remains unchanged.

Configure `TRAINPILOT_MAPS_API_KEY` externally for Maps. Presence is not proof of
live Cloud authorization. Physical map tiles, watch ACK/sensors and real device
backup/restart acceptance remain separate from JVM/Compose fixture test results.

[Earlier PR #2 checkpoint (historical)](docs/MAIN_TEST_CHECKPOINT_2026-11-01.md).
