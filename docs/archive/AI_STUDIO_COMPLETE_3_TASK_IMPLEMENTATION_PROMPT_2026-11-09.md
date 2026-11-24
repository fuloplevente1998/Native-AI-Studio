# AI STUDIO — TRAINPILOT COMPLETE IMPLEMENTATION PROMPT
## Scope: PR #215 + Issues #214 and #216 (2026-11-09)

You are implementing a production-quality Android + Wear OS TrainPilot change. Work in repository `fuloplevente1998/Native-AI-Studio`, based on current `main`. Inspect AGENTS.md and existing architecture before changing code. **Do not simply report an audit; implement, test, and deliver a reviewable PR.**

### Mandatory authoritative sources
1. Full approved exercise expansion spec (34 additions, 140 total):
https://raw.githubusercontent.com/fuloplevente1998/Native-AI-Studio/feat/exercise-catalog-expansion-spec-20261109/docs/EXERCISE_CATALOG_EXPANSION_AI_STUDIO_HANDOFF_2026-11-09.md
2. Draft specification PR #215: https://github.com/fuloplevente1998/Native-AI-Studio/pull/215
3. Wear/phone sync bug #214: https://github.com/fuloplevente1998/Native-AI-Studio/issues/214
4. Background timer and layout bug #216: https://github.com/fuloplevente1998/Native-AI-Studio/issues/216
5. Current catalog: `app/src/main/resources/native-catalog-main-7473.json`
6. Current planner: `app/src/main/resources/native-planner-main-7473.json`
7. User device-test baseline: successful signed Android/Wear APK pair Actions run #51, commit `307bc9ed`.

### Implementation A — 34 new exercises
- Existing catalog: **106 exercises, 13 programs**. Add all 34 approved exercise proposals from the full raw spec (18 first pass + 16 second pass); expected total **140**, provided no duplicates discovered on the latest main.
- Use stable unique IDs, HU/EN names, actual equipment, muscle groups, movement type, locations, difficulty, setup, technique, safety, defaults, logging/measurement semantics and complete video metadata. Never fabricate demonstration URLs; verify actual exercise/equipment match, availability and attribution, and provide a graceful fallback when none verified.
- Keep all existing IDs and historical training logs stable; never overwrite dumbbell exercise metadata with barbell variants.
- Add `barbell-floor-press` distinct from `db-floor-press`. **Only in the built-in Otthoni A/B A-day template**, replace `db-floor-press` with `barbell-floor-press`, preserving other exercises/order, user customizations, saved workouts and history. Ensure safe equipment requirement and correct kg-total versus kg-per-arm handling. Do not force this substitution for users without a barbell; preserve equipment-filtered alternatives.
- Ensure catalog, all 13 built-in programs, program editor, quick workout, custom workouts, Coach, exercise search/filter, planner, progress/history, phone and Wear references, exercise-detail and video links consistently support all 140 IDs.
- Review the 13 program names and exercise IDs and all planner roles/needs/substitutions; do not introduce invalid equipment recommendations or hidden duplicates. For carries, distance/time tracking must be supported; unilateral exercises need per-side semantics. Check rest periods, RIR, default reps, sets and weight units.

### Implementation B — #214 Wear ↔ phone synchronization
- Reproduce: complete a set on Wear; Wear rest starts, phone rest does not. Operate workout on phone; Wear becomes one exercise behind.
- Trace actual event/data transport in both directions. Identify root cause rather than guessing; consider event ordering, idempotence, snapshot versioning, acknowledgements, reconnect, stale state overwrites and lifecycle.
- Establish single consistent session state: current exercise ID/index, set index, completion, rest deadline, pause/resume and active controller. Apply updates idempotently, prevent stale snapshots from regressing progress, reconcile after reconnect/background.
- Test watch→phone and phone→watch exercise advance, set completion, rest start/stop, rapid events, duplicate/out-of-order delivery, disconnect/reconnect, foreground/background and app restart as applicable. Avoid creating two independent countdown truths.

### Implementation C — #216 reliable rest timer + responsive UI
- Reproduce: countdown freezes after app is backgrounded even ~2 seconds; screenshot shows narrow bottom rest toolbar with `Kihagy` wrapping into multiple lines and potential timer digit/label layout breaks.
- Replace UI-tick-as-source-of-truth logic with an elapsed-realtime/monotonic deadline for active process, persist/recover wall-clock-compatible deadline where needed for process death/reboot, with explicit paused duration handling. Remaining time must reflect elapsed time on foreground resume, without premature completion or reset.
- Preserve correct semantics for pause, resume, ±15 seconds, skip, exercise transitions, and cross-device rest state. Avoid leaks or repeated start events.
- Make bottom bar responsive for narrow widths, large font sizes and locale; prevent clipped/torn countdown digits and unintended split words. Test 00:59→00:58, 09:59→10:00, varying digit widths, long translations, 2/30/90-second background, lock screen, rotation and accessibility scaling.
- Do not silently conflate #214 sync failures with #216 local countdown failures; add independent regression tests.

### Delivery and quality gates
1. Start from latest main and branch for implementation. Keep #215 as spec/reference; create actual code PR(s) linked to #215, #214, #216. If separate PRs are necessary for clean review, use feature and bugfix PRs with explicit dependencies; never call an issue a PR.
2. Audit existing code, identify exact files/data flows and explain root cause for each confirmed bug.
3. Implement with minimal architecture disruption, preserve accepted UI design except necessary responsive fixes, and protect existing user data.
4. Add JVM/unit/integration tests for catalog IDs/counts, 13 programs, planner, Coach and custom/quick workout resolution, rest deadlines and synchronization event ordering; run available Android/Wear builds and tests.
5. Verify video links by real sources, not placeholder links. If external verification is unavailable, explicitly mark unverified links and do not invent them.
6. Run CI, resolve failures, produce signed phone + Wear APK pair only using the repo's existing secure signing workflow; never expose keys or secrets. Physical device verification remains necessary and must not be claimed without actual test.
7. Deliver: PR URL(s), commit SHAs, exact changed files, full list of 34 new exercises, catalog count, video verification report, test counts and CI links, outstanding risks, signed artifact links if produced, and clear manual phone+watch acceptance checklist.
8. Do not merge to main or deploy automatically without explicit user approval. Do not rewrite historical data or modify unrelated functionality.

### User-visible acceptance
- 140 unique catalog exercises, 13 valid programs, new barbell floor press on Otthoni A/B A day, existing dumbbell variant still selectable.
- Coach, program selection/editing, quick/custom workouts, video and search all work.
- Completing a set on watch starts the matching phone rest timer; operating phone advances watch to same exercise without lag.
- Backgrounding phone for 2 seconds does not stop the rest countdown; controls and digits do not wrap/clamp on narrow screens.
- Tests and builds pass; signed APK pair ready for real device testing.
