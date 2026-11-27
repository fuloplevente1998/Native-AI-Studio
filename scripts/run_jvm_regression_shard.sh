#!/usr/bin/env bash
set -euo pipefail

: "${SUITE:?JVM shard suite must be set}"
case "$SUITE" in phone-a|phone-b|wear-shared) ;; *) echo "Invalid JVM shard suite: $SUITE" >&2; exit 2 ;; esac
WEAR_CLOSURE_REGRESSIONS="${WEAR_CLOSURE_REGRESSIONS:-false}"
TARGETED_REGRESSIONS="${TARGETED_REGRESSIONS:-false}"
for mode in "$WEAR_CLOSURE_REGRESSIONS" "$TARGETED_REGRESSIONS"; do
    case "$mode" in true|false) ;; *) echo "Invalid targeted test mode" >&2; exit 2 ;; esac
done
if [[ "$SUITE" == phone-b && ( "$WEAR_CLOSURE_REGRESSIONS" == true || "$TARGETED_REGRESSIONS" == true ) ]]; then
    echo "Targeted dispatch must not execute a second phone shard" >&2
    exit 2
fi
common=(--no-daemon --stacktrace --profile --continue)
if [[ "$SUITE" == phone-* ]]; then
  if [[ "$WEAR_CLOSURE_REGRESSIONS" == "true" ]]; then
    gradle "${common[@]}" :app:testDebugUnitTest --tests 'com.example.WatchDiscardRecoveryRegressionTest' \
      --tests 'com.example.PhoneReturnInstallationRegressionTest' --tests 'com.example.HealthSyncAdmissionRegressionTest'
  elif [[ "$TARGETED_REGRESSIONS" == "true" ]]; then
    gradle "${common[@]}" :app:testDebugUnitTest \
      --tests 'com.example.ActiveWorkoutDockNavigationRegressionTest' \
      --tests 'com.example.StudioProgress*' \
      --tests 'com.example.StudioHealthPermissionActionTest' \
      --tests 'com.example.StudioAppearanceRegressionTest' \
      --tests 'com.example.StudioCloudRefreshRegressionTest' \
      --tests 'com.example.JournalWorkoutHealthRegressionTest' \
      --tests 'com.example.LegacyRestoreEvidenceRegressionTest' \
      --tests 'com.example.PhoneReturnInstallationRegressionTest' \
      --tests 'com.example.HealthProviderSelectionRegressionTest' \
      --tests 'com.example.HealthSyncAdmissionRegressionTest'
  else
    gradle "${common[@]}" "-PtrainpilotPhoneTestShard=${SUITE#phone-}" :app:testDebugUnitTest
  fi
elif [[ "$WEAR_CLOSURE_REGRESSIONS" == "true" ]]; then
  gradle "${common[@]}" :wear:testDebugUnitTest --tests 'com.repforge.app.wear.WearClosureRecoveryTest' \
    --tests 'com.repforge.app.wear.WearSaveSyncTest' --tests 'com.repforge.app.wear.WorkoutOwnership193Test' :shared:testDebugUnitTest
else
  gradle "${common[@]}" :wear:testDebugUnitTest :shared:testDebugUnitTest
fi
