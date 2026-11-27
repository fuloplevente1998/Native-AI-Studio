# Build modernization – 2026-11-27

Branch: `chore/build-modernization`, baseline `b14dc497448b2e3b4df0f9715ea65cb675a01775`. Independent of #433 / PR #434; main and released version 1.5.7 (281800/281802) unchanged. This branch is a candidate, not an accepted upgrade.

## Phase M1 – build tooling (implemented, CI pending)

| Component | Baseline | Candidate | Reason |
|---|---|---|---|
| Temurin runner JDK | 17 | 25 LTS | Gradle supports JDK 25 from 9.1; AGP 9.5 includes a JDK25 warning fix (#553121473). Kyant 1.0.6 requires at least Java 21. One JDK for CI/build/signing; Android Java target remains 17. Exact-head full CI must verify compatibility. |
| Gradle | 9.3.1 | 9.9.3 | Latest stable GitHub release; wrapper and all three workflows aligned. AGP 9.5 requires at least Gradle 9.6.0; actual integration must pass. |
| AGP | 9.1.1 | 9.5.1 | Stable release per official Android release notes; leave compileSdk 36.1 / targetSdk 36 / minSdk unchanged. |
| Kotlin/Compose compiler plugin | 2.3.10 | 2.4.21 | Latest stable; D8/R8 must catch any inline/dex regression. |
| KSP | 2.3.5 | 2.3.14 | Latest stable; fixes concurrent executions regression #3286. Not a guarantee that the reported KSP IntelliJ cleanup NPE disappears. |
| Robolectric | 4.16.1 | 4.18.1 | Latest stable; DefaultSdkProvider at tag robolectric-4.18.1 still uses PREINSTRUMENTED_VERSION=7 and identical API28/API34 artifact IDs, so keep existing checksum pins. |
| checkout/setup-java/gradle actions/cache/upload/download/github-script | v4/v4/v4/v4/v4/v4/v7 | v7.0.1/v6.3.0/v6.4.1/v6.1.0/v7.0.2/v8.0.2/v9.0.0 | Stable release tags; modern action runtimes, no forced old Node20. |

Official references:
- https://developer.android.com/build/releases/agp-9-5-0-release-notes
- https://docs.gradle.org/current/userguide/compatibility.html
- https://github.com/gradle/gradle/releases/tag/v9.9.3
- https://kotlinlang.org/docs/releases.html
- https://github.com/google/ksp/releases/tag/2.3.14
- https://github.com/robolectric/robolectric/releases/tag/robolectric-4.18.1
- https://github.com/robolectric/robolectric/blob/robolectric-4.18.1/robolectric/src/main/java/org/robolectric/plugins/DefaultSdkProvider.java
- GitHub Actions official release tags listed above.

Three existing workflows updated, no new signing path. The historical Play-internal AAB workflow is manual-only to avoid an extra signing build just because its YAML changed on main; the normal persistent-signing workflow still automatically stages and publishes the signed APK/AAB pair only after full regression on trusted main. No Play upload. Modernization PR uses existing DEX, phone+Wear R8, and no-skips raw XML gate; #433 branch gate is preserved for integration. No runtime application source changed.

## M2 – runtime dependency audit (not yet implemented)

- JDK25 LTS is now the selected build runner in M1; only retain a lower runner version if an actual incompatibility is demonstrated and documented. Android Java target stays 17.
- Phone Compose BOM 2026.02.00 -> 2026.11.01 candidate; current wear BOM 2024.09.00 stays untouched for M1. Kyant 1.0.6 -> 2.0.1 is a major API migration, not a blind version substitution. 2.0.1 release notes specify Compose 1.12.0, while BOM 2026.11.01 maps UI 1.13.0; choose and verify compatible pairing, then actual renderer source migration with #433 merged. Preserve local touch, bounded palette paint, active measurement/Tile/Performance no capture, all 32 standard palettes and custom themes.
- Inventory other direct dependencies (AndroidX lifecycle/activity/navigation/Room/DataStore/WorkManager/CameraX/Credentials, Play Services/Firebase, networking/Moshi/Coil, etc.) and transitive constraints. Major updates need migration and behavioral tests, especially Room schemas, persisted workout/session ownership and Wear sync. No blanket version bump.
- Keep existing library checks, no alpha/beta/RC, no disabling tests, no application version or signer changes. No local Gradle/emulator.

## Acceptance and integration

1. Check M1 exact-head full CI: real phone/Wear/shared raw JUnit XML, zero failures/errors/skips; D8 and both R8. Diagnose logs; do not infer success from annotations or silence.
2. #433 is separately blocked on its CI and source review. Reconcile workflow/HANDOFF changes after #433 integration; preserve all #433 tests and gates. Avoid overwriting its renderer implementation.
3. M2 source migrations and dependency updates in scoped commits, full CI after completed batch; no intermediate signing.
4. Only approved integrated main SHA with green full CI triggers persistent signed phone+Wear APK+AAB; verify signer/package/version/hash and artifact digest. Physical A57 acceptance remains separate. No Play upload.

## JDK25 candidate – user clarification
All three workflow files now use Temurin JDK25. Gradle official compatibility table supports running Gradle on Java25 from 9.1; AGP 9.5 fixed issue #553121473; Robolectric 4.18 documentation lists Gradle 9.8.2/AGP 9.5.0 as its build environment (not a blanket guarantee of every JDK combination). Sources: https://docs.gradle.org/current/userguide/compatibility.html ; https://developer.android.com/build/releases/agp-9-5-0-release-notes ; https://robolectric.org/compatibility_table/ . Exact new head must pass. JDK21 run 46071048835 was in progress when inspected and is superseded; no success claim. Runtime dependency updates remain M2, not blindly forcing incompatible Compose/Kyant majors. No change to Android JVM target, minSdk, signer, package, versions or #433 branch.
