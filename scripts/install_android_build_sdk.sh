#!/usr/bin/env bash
# Install only the exact stable SDK needed by the app, Wear, shared and release verifier.
set -euo pipefail

sdk_root="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [[ -z "$sdk_root" ]]; then
    echo "ANDROID_HOME or ANDROID_SDK_ROOT is required" >&2
    exit 1
fi
platform='platforms;android-37.1'
build_tools='build-tools;37.0.0'

sdk_ready() {
    [[ -s "$sdk_root/platforms/android-37.1/android.jar" && -s "$sdk_root/licenses/android-sdk-license" ]] || return 1
    local tool
    for tool in aapt2 d8 zipalign apksigner; do
        [[ -x "$sdk_root/build-tools/37.0.0/$tool" ]] || return 1
    done
}

if sdk_ready; then
    echo "Reusing verified Android compile SDK 37.1 and build-tools 37.0.0."
    exit 0
fi

manager="$sdk_root/cmdline-tools/latest/bin/sdkmanager"
if [[ ! -x "$manager" ]]; then
    manager=$(find "$sdk_root/cmdline-tools" -type f -path '*/bin/sdkmanager' | sort -V | tail -n 1)
fi
test -x "$manager"

# yes may receive SIGPIPE when sdkmanager exits. Check the manager's exit status,
# not the producer's. Accept the official licenses, never fake license markers.
accept() (
    set +o pipefail
    yes 2>/dev/null | "$manager" --sdk_root="$sdk_root" "$@"
)
install_exact() {
    if [[ ! -s "$sdk_root/licenses/android-sdk-license" ]]; then
        accept --licenses >/dev/null || return 1
    fi
    accept --channel=0 --install "$platform" "$build_tools" && sdk_ready
}

echo "Trying installed Android SDK manager: $manager"
if ! install_exact; then
    # Only update the stable manager when the current one cannot install the exact SDK.
    echo "Exact SDK installation failed; refreshing stable command-line tools once." >&2
    accept --channel=0 --install 'cmdline-tools;latest'
    # sdkmanager can install alongside an occupied latest directory as latest-2, etc.
    updated=$(find "$sdk_root/cmdline-tools" -type f -path '*/latest-*/bin/sdkmanager' | sort -V | tail -n 1)
    if [[ -n "$updated" && -x "$updated" ]]; then
        manager="$updated"
    elif [[ -x "$sdk_root/cmdline-tools/latest/bin/sdkmanager" ]]; then
        manager="$sdk_root/cmdline-tools/latest/bin/sdkmanager"
    fi
    echo "Retrying with Android SDK manager: $manager"
    install_exact || { echo "Exact stable Android SDK 37.1 / build-tools 37.0.0 unavailable or incomplete" >&2; exit 1; }
fi
sdk_ready
echo "Verified Android compile SDK 37.1 and build-tools 37.0.0."
