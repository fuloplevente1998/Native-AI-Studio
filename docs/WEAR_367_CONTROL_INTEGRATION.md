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
