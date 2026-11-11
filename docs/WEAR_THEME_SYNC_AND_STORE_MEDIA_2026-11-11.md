# Wear OS theme sync and Play Store media follow-up

Date: 2026-11-11

## Current verified state

- The Wear OS app currently does **not** visually match the phone app theme.
- Phone -> Wear theme synchronization is not working reliably.
- The current Wear UI shown by the user is the real app UI and must be treated as the source of truth for Play Store screenshots.
- Do **not** use generated/fabricated Wear OS UI screenshots in the Play listing. Google Play requires Wear screenshots to reflect the actual app running on Wear OS.
- The supplied real Wear screenshot is 432 x 432. The source PNG contains alpha transparency, so the Play-ready export must be flattened to an opaque background without altering the actual UI.

## Code observations

- `WearSyncRepository` already documents that the phone publishes the watch-home snapshot including `theme`.
- `TrainPilotWearApp` receives `home.value?.theme` via `LocalWearHomeTheme`.
- Several Wear surfaces are still hard-coded, for example `Black = Color(0xFF0E1015)`, `Card`, `Secondary`, `White`, and `Muted`; only some accent usage is derived from `LocalWearHomeTheme`.
- This means a received theme can currently affect only part of the Wear UI and does not guarantee full phone-theme parity.

## Required fix

1. Make the phone's selected TrainPilot theme/palette part of the canonical watch-home state.
2. Publish it immediately when the phone theme changes and on normal watch-home refresh/reconnect.
3. Persist the last valid theme on Wear OS so the watch starts with the correct appearance even before a fresh phone sync.
4. Replace hard-coded Wear surface/text colors with a theme model derived from the synchronized phone palette where appropriate.
5. Preserve Wear-specific contrast/accessibility requirements; phone theme parity means the same palette identity, not blindly copying phone dimensions or components.
6. Verify theme changes while:
   - both devices are connected,
   - watch reconnects after being offline,
   - phone process restarts,
   - Wear app restarts,
   - active workout is running,
   - dark/gold default theme and every selectable phone palette.

## Acceptance criteria

- Changing the phone theme updates the Wear app without reinstalling either app.
- After reconnect/restart, Wear shows the last selected phone theme.
- No screen falls back to a fixed gold/black palette unless that is the selected phone theme.
- Text/background contrast remains readable on round Wear displays.
- Existing workout, timer, health, GPS and sync behavior is unchanged.
- Tests cover theme serialization, Data Layer delivery, persistence and Compose theme mapping.

## Play Store media rule

Until this fix is completed, only real screenshots from the current Wear build may be used in the Wear OS Play listing. Generated concept images may be used only as internal design references or marketing artwork where they are not presented as actual app screenshots.

The Play-ready Wear screenshot should be exported from the real 432 x 432 capture with a fully opaque background; do not redraw or redesign the UI for the listing.
