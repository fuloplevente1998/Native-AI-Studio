# TrainPilot Native Constitution

Version: 1.0.1 · Ratified: 2026-11-02 · Amended: 2026-11-02

## Existing architecture and identity

Preserve the existing Kotlin/Compose phone and Wear applications, native workout engine,
Room/DataStore and native Health/GPS stores. Reuse their canonical calculations and commands.
Do not introduce a parallel workout engine or database. Both application IDs remain
`com.TrainPilot.com`; retain the owner-managed persistent signer and monotonic versionCode.
Never request or expose signing keys, passwords or API secrets in chat, logs or commits.

## Data preservation and ownership

Retain ownership epochs, handover/installed ACK, explicit resume, finalization/discard
fences, receipts, tombstones, replacement/restore generations and backup/privacy guards.
Do not delete user data or recovery state to work around a bug. A phone receipt must
follow its durable operation. Transport delivery alone does not prove workout completion.
Protect other active workouts and already saved history when reconciling stale commands.

## Measured data and privacy

Display real measurements from the explicitly selected provider. Unknown is not zero.
Do not add overlapping phone/watch/provider totals or silently switch providers.
Use actual timestamps, local dates and deduplicated intervals. Preserve default-off local
health backup consent and its existing export scope. Remote data cannot enable consent.

## Phone presentation contract

Follow the agreed frame hierarchy across phone screens: the page or related group gets
one stronger outer frame, inner panels use softer/thinner frames, and outlined controls
use the lightest frame. Reuse `TrainPilotFrames` and `TrainPilotOutlinedButton` instead
of introducing independent heavy borders. The three workout health-source panels are
mutually exclusive and keep their provider data separate. See `docs/FRAME_DESIGN_SYSTEM.md`.
This is a presentation contract; it does not change stored data or workout ownership.

## Evidence and verification

For each change record the observed symptom, expected behavior, affected scope, preserved
contracts and concrete acceptance criteria. Reuse existing regression tests; add meaningful
coverage for failure cases. Run Android builds/tests through the existing GitHub Actions.
Check actual source SHA, raw XML results, package/version, APK hashes and permanent signer.
JVM/Compose tests do not establish physical Samsung/HC/Wear acceptance. Label implementation,
CI verification, APK delivery and device acceptance separately. Do not claim an unfinished
build passed, weaken checks or mark a device observation verified from a fixture.

## Delivery and collaboration

Use isolated branches and reviewable PRs; preserve unrelated local work. Existing handoffs,
issue #19 and current user instructions are authoritative context, not requests to rebuild
completed features. Communicate in Hungarian. Do not spawn sub-agents unless the user
explicitly requests them. Do not publish Play/GitHub releases without explicit authorization.
Verified unpacked phone/Wear APKs go to the existing TrainPilot Native Drive folder
`1ywHgrManynjV9uNvZfVncOmCr3SbHIb9`; preserve older files and verify size/parent readback.

## Maintenance

This constitution records existing project contracts. Update it when an agreed contract
changes; identify the reason and migration impact in the PR. Keep feature specs bounded,
and reconcile changes to specs/tasks with their implementation and evidence. Generated
Spec Kit agent skills are maintained through the pinned CLI, not manual rewrites.
