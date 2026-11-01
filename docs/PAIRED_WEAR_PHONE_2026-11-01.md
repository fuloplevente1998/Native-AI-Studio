# Native TrainPilot phone / Wear OS companion

## What the connection screen proves

A NodeClient connection proves that Wear OS transport reaches a system node. It does
not prove that the TrainPilot watch app is installed or that workout ownership moved.
The new companion advertises `trainpilot_workout_companion193`; the phone requires
that capability on its single reachable node before handoff. The screen distinguishes
system reachability, companion reachability and the actual committed ownership ACK.

Use the **TrainPilot Wear OS APK** from the same build as the phone APK. Galaxy Wearable
provides system pairing; it is not the TrainPilot companion. An unrelated/older watch
APK cannot be assumed compatible with the current CI phone signer.

## Source and safety

The Wear source is imported from the prepared native migration integration at pinned
TrainPilot commit `648e971034201511d69585cd5deecaa27eddf942`. Ten existing shared native
files move byte-for-byte from the phone into `:shared`, used by both apps. No parallel
workout engine, ownership journal or phone database is created. Existing native Wear
UI, sensor permissions, local workout storage and protocol tests are retained.

Both application IDs are `com.TrainPilot.com`, version 2736 / `1.3.71-native-test`.
Wear signing copies the existing phone signing configuration; release settings and
keys are unchanged. Actions creates one existing-style temporary debug keystore for
both modules. APK verification calls apksigner and rejects differing package or
signer identities before a pair can be reported as verified.

Phone handoff opens the visible watch activity for the expected workout/revision.
The watch requests ownership only for a fresh, visible, matching explicit launch;
merely opening the watch to view measurements does not request ownership. Existing
native journals perform request/offer/commit/ACK. The phone waits for a matching
native receipt, not merely an owner flag: after sending to the watch, the source
journal must reach `settled` (the watch's installed acknowledgement received).
On return, the phone's target journal must reach `installed`, with matching workout,
transfer ID and epoch before native resume installation. A source-side `committed`
snapshot alone does not prove installation on the peer. Uncertain transfers stay
locked, and the foreground refresh rechecks late receipts without inventing an ACK.
Only an expired request that never reached an offered/accepted native lease may be
retried after the protocol's existing 30-second admission window. A pending native
frame or durable offered/accepted/committed journal never unlocks merely on timeout. Both devices require
explicit Continue after transfer; this does not start sensors from a background
receiver. Active phone GPS recording is rejected with an explicit explanation,
matching the existing native transfer admission rule. Takeback uses the same native
journal and imports the returned paused frame on the required Main looper.

## CI and device test

Run phone and Wear `assembleDebug` and `testDebugUnitTest`, plus shared unit tests.
Download phone APK, Wear APK, raw test reports and `trainpilot-apk-metadata` from one
successful run. Metadata records source checkout, package/version, SHA256 of each
APK, signer SHA1/SHA256, required-watch declaration and pair-identity result.
It also reports Maps key presence without printing its value.

A matching pair within a CI run is not proof of signer continuity with a previously
installed release or a previous temporary CI signer. An existing installation signed
with another key requires an update built with its original authorized key.

For the physical test:
1. Install the phone APK on the phone and the Wear APK on Galaxy Watch7 using the
   watch's normal development installation route (wireless ADB or Android Studio).
   Pair the watch through Galaxy Wearable and open TrainPilot on both devices.
2. Confirm the phone reports **TrainPilot Wear available**, not only Wear OS system
   transport. Battery stays unavailable until actually measured/published.
3. Start a non-GPS phone workout, open Connected devices and request transfer. The
   watch must receive the same workout; the phone must wait for the committed ACK.
4. Confirm the transferred workout remains paused. Explicit Continue on the watch
   should record only real sensor data after actual OS permissions/measurement consent.
5. Request return to the phone; confirm the same workout and measured sets return,
   with only active time credited. Disconnect/reconnect and verify safe recovery.

Compilation/unit tests cannot prove Bluetooth delivery, installed-device signatures,
real sensor values or physical round-screen layout. Mark those device checks pending
until observed. Do not claim a transport node alone is a successful workout handoff.
