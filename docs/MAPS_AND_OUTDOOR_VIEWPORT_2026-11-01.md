# Maps configuration and outdoor viewport repair

Baseline main: `2558adc2fb487c6c0553a6b9a4cab1466120b266`.

## Runtime/UI changes

The outdoor dashboard uses its bounded parent height. The map yields space to
native status, measured metrics and pinned start/pause/lap/finish controls. Activity
selection remains available before starting through the compact selector. Fullscreen
uses safe drawing insets and keeps the map above the metric/control panels, including
Google's map attribution. Large system text and unusually short windows retain a
scrollable secondary region; primary controls remain outside it. Text scale is not
reduced to force a layout to fit. Existing palette, logo, top navigation and native
GPS/timer/finalization callbacks remain in use; no fake GPS/Health values are added.

Google Maps now renders when configured even before the first route fix. Camera
movement and polylines use recorded coordinates only, with segment gaps retained.
Missing key/Play services and map loading timeout are separate states; a blank map
is not treated as successful map loading. GPS recording remains independent of tile
availability. Runtime key presence is not proof of Cloud API authorization.

## Supply the Android Maps key

The build resolves the first nonblank key from:
1. `TRAINPILOT_MAPS_API_KEY` environment variable;
2. the same Gradle property;
3. ignored `local.properties` / `secrets.properties`, using
   `TRAINPILOT_MAPS_API_KEY` or `MAPS_API_KEY`.

Actions maps repository secret `TRAINPILOT_MAPS_API_KEY` (or `MAPS_API_KEY`) into
the environment. No key is committed or printed. `trainpilot-apk-metadata` reports
only `mapsApiKeyConfigured`, package/version, actual APK signer SHA1/SHA256 and APK
SHA256. This distinguishes absent configuration from an SDK/runtime failure.

If the metadata reports false, configure a key in the project's Google Cloud
Console, enable **Maps SDK for Android** and the required billing, then set the
repository Actions secret. Restrict the key to the Android application
`com.TrainPilot.com` and its actual signer SHA1, and to Maps SDK for Android.
Do not substitute a Gemini, JavaScript Maps or unrestricted server key.

The existing CI creates a temporary debug signer per run. Its SHA1 therefore changes
between builds; Android key restrictions must match the actual APK being tested.
Release signing configuration, application ID and version are unchanged. An existing
approved stable signer can be used through the authorized signing setup; no new key
or replacement release signer is created by this repair.

## Verification

Run `:app:assembleDebug :app:testDebugUnitTest` and inspect raw XML. New Compose
bounds tests cover a 360×480 content viewport without secondary scrolling,
fullscreen with reserved system-bar space, 320×420 with 2× system text, and activity
selection callbacks. Map configuration tests cover empty/unexpanded and string/
resource metadata. Test maps are placeholders only; they do not verify live tiles.

Still required: actual restricted API key/Cloud authorization if absent, real-device
map tile loading, GPS/background behavior and full system-window inset acceptance.
The separate Wear companion audit must verify matching package, signer and a real
ownership ACK before recommending an installed phone/watch pair.
