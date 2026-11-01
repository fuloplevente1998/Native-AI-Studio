# Unvalidated Wear continuation

Branch wip/native-wear-integration-20261101, based on validated phone candidate
a36528ad04b365a2936c8bbd03dcb2373a9fad4f (PR#2). This increment is NOT built or
runtime validated yet. Do not call Wear/device behavior complete.

Reference: TrainPilot feat/native-android-migration exact648e971034201511d69585cd5deecaa27eddf942.
The ten existing shared source files are byte-identical to that reference. They
are moved unchanged into a single :shared library, used by phone and imported
:wear source/tests. This does not introduce a second phone engine/database.
Wear applicationId matches com.TrainPilot.com; debug/release signing configs copy
the existing phone configuration rather than introducing a release test key.
No credentials or release artifacts were imported. Release has not been built.

Phone ownership controls now request the native remote watch activity, then wait
for actual ownership ACK/epoch state. Takeback uses OwnershipJournal193.request
and the existing transport/resume/install protocol on the main thread. Pending
transfers lock controls; timeout is not fabricated success. These changes are
UNVALIDATED and require CI and physical paired-device testing.

Next: check module Gradle/AGP9 compatibility and imports, run canonical phone+Wear
assembleDebug/testDebugUnitTest tasks, inspect all raw XML and both APK identities.
Do not merge this increment until those checks pass. The owner requested the
already validated phone fixes on main for AI Studio/device testing; keep this
separate until reviewed.
