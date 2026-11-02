"""Verify actual APK signatures and pair identity without exposing embedded Maps keys."""
import hashlib
import json
import os
import pathlib
import re
import shutil
import subprocess
import xml.etree.ElementTree as ET

release = os.environ.get("TRAINPILOT_VERIFY_RELEASE") == "true"
phone = pathlib.Path("app/build/outputs/apk/release/app-release.apk" if release else "app/build/outputs/apk/debug/app-debug.apk")
if not phone.is_file():
    print("No debug phone APK produced; configuration evidence unavailable.")
    raise SystemExit(1 if release else 0)
sdk = pathlib.Path(os.environ.get("ANDROID_HOME") or os.environ["ANDROID_SDK_ROOT"])
analyzer = shutil.which("apkanalyzer") or str(next(sdk.glob("cmdline-tools/*/bin/apkanalyzer")))
signer_tool = sdk / "build-tools/36.0.0/apksigner"
android = "{http://schemas.android.com/apk/res/android}"


def inspect(apk):
    manifest = ET.fromstring(subprocess.check_output([analyzer, "manifest", "print", str(apk)], text=True))
    verified = subprocess.check_output([str(signer_tool), "verify", "--print-certs", str(apk)], text=True)
    sha1 = re.search(r"Signer #1 certificate SHA-1 digest: ([0-9a-fA-F]+)", verified)
    sha256 = re.search(r"Signer #1 certificate SHA-256 digest: ([0-9a-fA-F]+)", verified)
    assert sha1 and sha256, "APK signer evidence unavailable"
    return manifest, {
        "applicationId": manifest.get("package"),
        "versionCode": manifest.get(android + "versionCode"),
        "versionName": manifest.get(android + "versionName"),
        "apkSha256": hashlib.sha256(apk.read_bytes()).hexdigest(),
        "signerSha1": sha1.group(1),
        "signerSha256": sha256.group(1),
    }


manifest, result = inspect(phone)
key = next((n.get(android + "value", "") for n in manifest.findall("application/meta-data")
            if n.get(android + "name") == "com.google.android.geo.API_KEY"), "")
result["mapsApiKeyConfigured"] = (
    bool(key.strip())
    and not key.startswith("${")
    and not key.startswith("@string/0x")
    and key not in {"YOUR_API_KEY", "DEFAULT_API_KEY"}
)
result["ciCheckout"] = os.environ.get("GITHUB_SHA")
wear_debug = pathlib.Path("wear/build/outputs/apk/debug/wear-debug.apk")
wear = pathlib.Path("wear/build/outputs/apk/release/wear-release.apk" if release else "wear/build/outputs/apk/optimized/wear-optimized.apk")
optimized = wear.is_file()
if not optimized and not release:
    wear = wear_debug
requires_wear = os.environ.get("TRAINPILOT_REQUIRE_WEAR_PAIR") == "true"
if wear.is_file():
    wear_manifest, result["wear"] = inspect(wear)
    result["wear"]["variant"] = "release" if release else "optimized-test" if optimized else "debug"
    result["wear"]["bytes"] = wear.stat().st_size
    if wear_debug.is_file():
        result["wearDebugBytes"] = wear_debug.stat().st_size
    with __import__("zipfile").ZipFile(wear) as archive:
        resources = archive.read("resources.arsc")
        result["wearCompanionCapabilityRetained"] = (
            b"android_wear_capabilities" in resources or
            "android_wear_capabilities".encode("utf-16le") in resources
        )
    result["dataLayerPairIdentityVerified"] = (
        result["applicationId"] == result["wear"]["applicationId"] == "com.TrainPilot.com"
        and result["signerSha256"] == result["wear"]["signerSha256"]
    )
    result["wearHardwareRequired"] = any(n.get(android + "name") == "android.hardware.type.watch"
        and n.get(android + "required") == "true" for n in wear_manifest.findall("uses-feature"))
else:
    result["dataLayerPairIdentityVerified"] = False
pathlib.Path("app/build/outputs/apk-verification.json").write_text(json.dumps(result, indent=2) + "\n")
print(json.dumps(result, indent=2))
if requires_wear:
    assert wear.is_file(), "Paired Wear APK missing"
    assert result["dataLayerPairIdentityVerified"], "Phone/Wear package or signer differs"
    assert result["wearHardwareRequired"], "Wear hardware declaration missing"
    assert optimized, "Optimized/release paired Wear APK missing"
    assert result["wearCompanionCapabilityRetained"], "Wear capability resource was removed"
    if not release:
        assert result["wear"]["bytes"] < result["wearDebugBytes"], "Wear optimization did not reduce APK size"

if release:
    expected = os.environ.get("TRAINPILOT_RELEASE_CERT_SHA256", "").replace(":", "").lower().strip()
    assert len(expected) == 64, "Pinned release certificate missing"
    assert result["signerSha256"].lower() == expected, "Release signer differs from configured persistent key"
    assert result["applicationId"] == "com.TrainPilot.com", "Application identity changed"
    assert result["versionCode"] == result["wear"]["versionCode"] == "2740", "Package versions differ"
    assert result["versionName"] == result["wear"]["versionName"] == "0.1.0-beta", "Package versions differ"
    assert result["mapsApiKeyConfigured"], "Maps key not embedded"
