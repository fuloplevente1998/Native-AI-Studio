#!/usr/bin/env bash
# Install the exact stable SDK used by the app, Wear, shared and release verifier.
# Do not hide unavailable packages, silently downgrade, or switch to preview channels.
set -euo pipefail

sdk_root="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [[ -z "$sdk_root" ]]; then
    echo "ANDROID_HOME or ANDROID_SDK_ROOT is required" >&2
    exit 1
fi
manager="$sdk_root/cmdline-tools/latest/bin/sdkmanager"
if [[ ! -x "$manager" ]]; then
    manager=$(find "$sdk_root/cmdline-tools" -type f -name sdkmanager | sort -V | tail -n 1)
fi
test -x "$manager"
echo "Android SDK manager: $manager"
"$manager" --sdk_root="$sdk_root" --version
# Accept official SDK licenses. yes may exit on a closed pipe; package installation
# below must succeed and the exact installed platform/tool files must exist.
yes | "$manager" --sdk_root="$sdk_root" --licenses > /dev/null || true
# sdkmanager 20.0 did not list android-37 on the hosted runner. Inspect/update the
# official stable command-line tooling before requesting the correct 37.1 package.
"$manager" --sdk_root="$sdk_root" --channel=0 --install 'cmdline-tools;latest'
manager="$sdk_root/cmdline-tools/latest/bin/sdkmanager"
test -x "$manager"
"$manager" --sdk_root="$sdk_root" --version
echo "Stable platform/build-tools entries for Android 37:"
"$manager" --sdk_root="$sdk_root" --channel=0 --list | grep -E 'platforms;android-37([.[:digit:]]*)[[:space:]]*\||build-tools;37[.[:digit:]]*[[:space:]]*\|' || true
"$manager" --sdk_root="$sdk_root" --channel=0 --install 'platforms;android-37.1' 'build-tools;37.0.0'
test -f "$sdk_root/platforms/android-37.1/android.jar"
test -x "$sdk_root/build-tools/37.0.0/apksigner"
echo "Verified Android compile SDK 37.1 and build-tools 37.0.0."
