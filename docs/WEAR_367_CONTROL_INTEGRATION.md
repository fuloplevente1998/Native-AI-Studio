# #367 – Control integration status

## Implemented: watch-owned workout (protocol 367)
- Phone home snapshot advertises controlProtocol367=1. Legacy phones do not enable these buttons.
- Manual rest ±15 seconds, 0–600 seconds, absolute deadline on the existing owner clock.
- Full pause reuses WearOwnership193.freeze, including active intervals, automatic timer,
  stopwatch, health and GPS; resume uses existing durable resumeWorkout.
- watchControl367 is a separately tracked, non-coalesced owner snapshot command. The watch
  persists the mutation and exact command together in its local snapshot before outbox enqueue.
  Local-only recovery marker never appears in a wire snapshot. The same ID is retried after
  process death or reconnect; a pending marker blocks other edits until durable enqueue.
- Phone accepts only the same active watch-owned workout, epoch, generation, valid transition,
  sequence and target. New state is persisted before applied ACK; exact replay is recognized.
  Tombstoned/saved drafts cannot be resurrected. No second timer is started.
- Out-of-order obsolete commands are superseded, not reported as saved.
- Only a matching canonical receipt counts as synced. New pure JVM tests cover deadlines,
  invalid deltas, wrong owners/epochs, replay, and stale pause revisions.

## Still open: phone-owned session
- UI retains explicit phone-control shortcut. No watch-side local mutation is permitted.
- To enable direct remote controls, route an idempotent command to the canonical phone
  timer key/phase (NativeWorkoutEditing.controlRest / native engine) or full pause path.
  Must preserve generation, pause revision, owner epoch, current target and durable
  duplicate handling. Do not reuse watchControl367 to mutate a phone-owned snapshot.
- Automatic active rest ± requires the automatic timer's own transition support; the
  implemented ± is for manual rest only.
- Device tests pending: offline replay, process death, reconnect, simultaneous phone/watch,
  transferred ownership, 0/max, haptics and actual Health/GPS pause/resume.

## Native phone-owner controls
- Protocol 2: explicit pause/resume on canonical PhoneWorkoutExecution, matching generation/epoch/pause revision, durable transaction proof, no optimistic watch mutation.
- Protocol 3: phone-owned rest-only phase adjustment via existing IntervalTimerService.adjustRest, fenced by key, phase number and configured duration; durable checkpoint proof before ACK. No parallel timer. The watch queues a request and uses the phone mirror. Automatic multi-step rest adjustment remains unsupported. Physical device verification pending.

## Protocol 4: native watch automatic rest
- Current rest only, using the existing monotonic tick and absolute remaining time. Configured rest for later rounds stays unchanged. Actual elapsed rest is credited before adjustment. At zero, existing skip transition. Durable owner snapshot uses watchControl367/automaticRest with phase identity and remaining-time proof. Transferred timer checkpoints and multi-phase phone timelines stay disabled for this operation until their shared plan can represent per-phase overrides. Physical device test pending.

## Shared per-phase override groundwork
- Optional restOverridesMs367 map in native interval checkpoints; `initial`, `only`, or step-index:round keys identify one rest, not all rounds. Shared timeline validates identities, integral durations and total <=24h. No change to existing schedules with absent map. Both phone and transferred-watch readers reconstruct the same timeline; phone persists the map in checkpoints and matching plan. Actual multi-phase mutation remains disabled pending owner command integration and CI. Tests cover initial-vs-round, independent later rounds, 0, 600, ledger accounting and JSON round-trip.

## Protocol 5: phone multi-phase native rest
- Phone remote rest now carries exact current phase duration and revision. Existing timeline clock samples actual elapsed time, changes only one rest override, commits checkpoint and proof, then advances normally at zero. Wear reads the new clock with phase/revision fencing. Legacy rest-only requests remain supported without overrides. Transferred plans can read the persisted override map; watch-side mutation of transferred plans is still disabled. Device and CI verification required.

## Protocol 6: transferred timer on watch
- Watch owner adjusts the current shared timeline override at a sampled monotonic position, preserves measured ledger, then uses the existing advance path at zero. Absolute checkpoint + proof is persisted in the existing durable watch control intent. Phone validates the exact before/after phase override by reconstructing the old timeline. Future rests and original plan unchanged. Both checkpoint readers preserve overrides for subsequent handoff. Device verification and CI pending.
