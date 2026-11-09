# TrainPilot AI Studio — Health #217 + #218 implementation handoff
Date: 2026-11-09
Repository: https://github.com/fuloplevente1998/Native-AI-Studio
Target branch: **main ONLY**

## IMPORTANT: AI Studio workflow limitations
AI Studio can only edit and push to `main`. **Do not create a branch, pull request, or attempt to merge a PR.** Do not stop because PR tools are unavailable. Implement and commit the fixes directly to `main` in logically grouped commits, then report commit SHA(s), changed files, tests, and limitations. Do not modify signing secrets, signing keys, or trigger a release build. A separate reviewer will verify the resulting `main` commit and trigger the signed phone/Wear build after acceptance.

## Mandatory source of truth
- Repo `AGENTS.md` and current `main` code, especially Health UI, Samsung Health / Health Connect integration, Room data, goals persistence, and navigation.
- Issue #217: https://github.com/fuloplevente1998/Native-AI-Studio/issues/217
- Issue #218: https://github.com/fuloplevente1998/Native-AI-Studio/issues/218
- Signed baseline: Actions run #54 https://github.com/fuloplevente1998/Native-AI-Studio/actions/runs/42615820938
- Screenshots attached to user conversation show Health tab “Mai állapot” and “Célok beállítása”. Treat screenshots as bug reports, not proof of data provenance.

## Task A — #217: six Health tiles and nonfunctional Health diary navigation
The six tiles currently show: sleep, active calories, heart rate, steps, today's workouts, and Samsung Health score. Sleep details show “Egészségnapló megnyitása lent” but tapping it does not open/scroll to the diary.

1. Find the composables, state flows, navigation targets, and data pipelines for all six tiles. Fix the “Egészségnapló megnyitása lent” action: it must be an actual accessible click target that reliably navigates or scrolls to the intended journal; if the journal is unavailable, explain why instead of doing nothing. Validate the other tile expansion/click behavior.
2. Audit each metric end-to-end. Document source (Samsung Health, Health Connect, Room, or TrainPilot-derived), permissions, query window and local timezone, aggregation, units, freshness and cache behavior. **Do not assume** all six are direct Samsung Health values.
3. Sleep: distinguish total duration from stage data; if stages were not provided, show a truthful “no stage data” message, not fabricated stages. Active calories: avoid conflating active with total calories. Heart rate: verify whether shown BPM is today's average, resting HR, or latest reading; label precisely. Steps: prevent duplicate-source aggregation. Workout count: define which completed sessions qualify as “today”. Score: confirm whether 85/100 is a real provider metric or an app-derived score and label its provenance clearly.
4. Handle missing permissions, disconnected sources, no records, stale cache, partial sync, and provider failures with explicit UI states; no fabricated values or silently misleading zeros. Preserve offline fallback only if visibly labeled as cached.
5. Ensure date rollover, timezone/DST changes and refresh events update tiles correctly. Preserve existing look and feel and user data.
6. Add unit/integration regression tests for navigation, mappings, aggregation, missing data, stale data, and boundary dates. Provide a device verification matrix.

## Task B — #218: coherent daily/weekly/monthly goals
The dialog “Célok beállítása” displays “Heti 25 edzés • Napi 10000 lépés” and “Edzések száma: 25”, although 25 workouts per week is unreasonable and the period is unclear.

1. Implement an explicit **weekly / monthly** selector for the workout-count target. A goal of 25 must be displayable as **25 edzés/hónap**, not incorrectly as a weekly target. Do not silently reinterpret an existing persisted value as monthly: migrate conservatively and retain existing data; when prior period is ambiguous, require or clearly surface period selection.
2. Keep steps as a **daily** goal; weekly step goal/progress can be derived and labeled accordingly. Make goal summary, progress tiles, dialog, labels and accessibility descriptions consistently reflect the selected units/period.
3. Compute workout progress from the correct completed-workout source with calendar week/month boundaries in the user's locale and timezone. Avoid duplicate workout counting and ensure changes of period refresh the progress correctly.
4. Make the dialog responsive on narrow Android screens: no clipped labels, overlapping slider thumbs, or horizontal overflow; provide an accessible precise numeric input alongside appropriate slider bounds and validation. Keep controls easy to use.
5. Choose sensible defaults for *new* users only. Never overwrite or reset an existing user's targets during migration. Add tests for weekly/monthly selection, persistence, migrations, progress, boundaries, and responsive UI.
6. Preserve Samsung Health integration, Room workout history, and existing other tabs.

## Implementation and verification requirements
- Inspect existing architecture before editing. Use native Kotlin/Compose and established persistence patterns. Avoid mock or hard-coded data in production.
- Fix actual root causes; do not merely change text to hide failures.
- Keep localized labels consistent in supported languages (HU/EN/DE/RO) where applicable.
- Add targeted regression tests and run full JVM suite for app + wear + shared where available. Build Android and Wear OS debug variants as appropriate; report exact commands and results.
- Do not claim live-device behavior is verified by unit tests. Include manual device checklist: all six metrics compared to their true providers for the same date, diary navigation, permissions revoked, data absent, offline/stale, weekly/monthly target persistence, 25/month, daily steps, small screen and accessibility.
- Protect signed release pipeline and existing data; **do not trigger signed APK release yourself**.
- Push finished implementation directly to `main`; no PR, no branch, no auto-merge workflow.
- Final report must include: commit SHA(s), files changed, source-of-truth table for six metrics, goal period/migration semantics, test counts, failing tests (if any), unresolved risks, and whether ready for reviewer to trigger signed APK pair.

## Acceptance criteria
- All six tiles accurately represent actual source or clearly identified derived/cached state, with honest labels and missing-data handling.
- Health diary action works.
- Workout goal explicitly supports weekly/monthly and 25 monthly is unambiguous.
- Step goal remains daily, goal progress matches period, old data survives.
- Full tests/build pass; no regression in watch/phone sync or rest timer.
