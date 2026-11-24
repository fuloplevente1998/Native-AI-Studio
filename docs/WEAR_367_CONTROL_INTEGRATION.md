# #367 – Rest adjustment and workout pause integration boundary

The #367 UI branch deliberately does not introduce a new ownership or timer wire command.
The UI exposes optional onRestAdjust367 and onPauseWorkout367 callbacks on TrainPilotWearApp.
Without a backend callback, the rest buttons remain disabled. Phone-owned sessions have an
explicit phone-control shortcut; automatic timer pause remains a separate labeled action.

## Shared integration to coordinate with #351 / #359

- Phone UI uses ±15 seconds, clamped to 0–600 seconds. Sources: StudioRestTimer.adjusted,
  StudioCompactRestControls, StudioRestTimerDock, NativeWorkoutEditing.controlRest.
- Rest target captures workout ID, generation, owner epoch, pause revision, owner,
  base revision, deadline and current exercise/set. Backend must additionally resolve
  the canonical timer key and phase where applicable, and reject stale targets.
- Only current owner adjusts its existing timer; do not start an independent countdown.
  A durable unique command ID, receipt, duplicate replay handling and offline recovery
  must be defined before enabling callbacks. A delayed command must not restart an expired rest.
- Existing watchAutomationState is a coalesced automatic snapshot, not a durable user
  command; do not repurpose it as an acknowledged rest/pause command. Existing resumeWorkout
  is not a pause action. WearOwnership193.freeze is used in handoff/closure, so a separate
  safe pause entrypoint is necessary.
- Phone-owned pause must update active intervals, pause revision, rest checkpoint, measurement,
  GPS and automatic timing via the existing canonical pause mechanism; watch-owned pause must
  preserve equivalent state and synchronize through the agreed transport. Resume uses existing path.
- Pending handoff, undo conflict, invalid or expired target and touch lock must disable mutation.
- Tests required: replay, out-of-order receipts, reconnect, paused/expired rest, epoch change,
  simultaneous phone/watch actions, process death, rest zero/max, feedback and closure.

UI tests in WearControlTargets367Test validate only UI target construction and numeric rules.
They do not assert a connected transport or end-to-end synchronization.
