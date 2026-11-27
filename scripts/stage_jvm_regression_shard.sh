#!/usr/bin/env bash
set -euo pipefail

: "${SUITE:?JVM shard suite must be set}"
case "$SUITE" in phone-a|phone-b|wear-shared) ;; *) echo "Invalid JVM shard suite: $SUITE" >&2; exit 2 ;; esac
mkdir -p ci-jvm-results/ci-evidence
if [[ "$SUITE" == phone-* ]]; then modules=(app); else modules=(wear shared); fi
for module in "${modules[@]}"; do
  for dir in "$module/build/test-results" "$module/build/reports/tests"; do
    if [[ -d "$dir" ]]; then cp -a --parents "$dir" ci-jvm-results/; fi
  done
done
git rev-parse HEAD > "ci-jvm-results/ci-evidence/$SUITE.sha"
