#!/usr/bin/env bash
# Hermetic fixture: no network or real Android SDK required.
set -euo pipefail
script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
work=$(mktemp -d)
trap 'rm -rf -- "$work"' EXIT
export ANDROID_HOME="$work/sdk"
export SDK_LOG="$work/calls"
export SDK_TEST_MODE=direct

reset_sdk() {
    rm -rf -- "$ANDROID_HOME"
    : > "$SDK_LOG"
    mkdir -p "$ANDROID_HOME/cmdline-tools/latest/bin"
    cat > "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" <<'MOCK'
#!/usr/bin/env bash
set -euo pipefail
root=${1#--sdk_root=}
shift
printf '%s %s\n' "${0%/bin/sdkmanager}" "$*" >> "$SDK_LOG"
if [[ "$*" == '--licenses' ]]; then
    mkdir -p "$root/licenses"
    printf 'fixture license\n' > "$root/licenses/android-sdk-license"
elif [[ "$*" == '--channel=0 --install cmdline-tools;latest' ]]; then
    mkdir -p "$root/cmdline-tools/latest-2/bin"
    cp "$0" "$root/cmdline-tools/latest-2/bin/sdkmanager"
elif [[ "$*" == '--channel=0 --install platforms;android-37.1 build-tools;37.0.0' ]]; then
    if [[ "$SDK_TEST_MODE" == fail ]]; then exit 1; fi
    if [[ "$SDK_TEST_MODE" == fallback && "$0" != */latest-2/bin/sdkmanager ]]; then exit 1; fi
    if [[ "$SDK_TEST_MODE" == incomplete ]]; then exit 0; fi
    mkdir -p "$root/platforms/android-37.1" "$root/build-tools/37.0.0"
    printf 'fixture jar\n' > "$root/platforms/android-37.1/android.jar"
    for tool in aapt2 d8 zipalign apksigner; do
        printf '#!/usr/bin/env bash\n' > "$root/build-tools/37.0.0/$tool"
        chmod +x "$root/build-tools/37.0.0/$tool"
    done
else
    echo "Unexpected manager command: $*" >&2
    exit 1
fi
MOCK
    chmod +x "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
}
run_sdk() { bash "$script_dir/install_android_build_sdk.sh"; }

reset_sdk
run_sdk
grep -q -- '--licenses' "$SDK_LOG"
test "$(grep -c -- '--channel=0 --install platforms;android-37.1 build-tools;37.0.0' "$SDK_LOG")" -eq 1
! grep -q 'cmdline-tools;latest' "$SDK_LOG"
: > "$SDK_LOG"
run_sdk
test ! -s "$SDK_LOG"
rm "$ANDROID_HOME/build-tools/37.0.0/d8"
run_sdk
test -x "$ANDROID_HOME/build-tools/37.0.0/d8"
test "$(wc -l < "$SDK_LOG")" -eq 1

reset_sdk
export SDK_TEST_MODE=fallback
run_sdk
test "$(grep -c 'cmdline-tools;latest' "$SDK_LOG")" -eq 1
grep -q 'latest-2 --channel=0 --install platforms;android-37.1 build-tools;37.0.0' "$SDK_LOG"

for mode in fail incomplete; do
    reset_sdk
    export SDK_TEST_MODE="$mode"
    if run_sdk; then echo "Expected failure for $mode" >&2; exit 1; fi
    test "$(grep -c 'cmdline-tools;latest' "$SDK_LOG")" -eq 1
done

echo 'All SDK installer fixture checks passed.'
