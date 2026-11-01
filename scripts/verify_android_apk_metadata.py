"""Verify actual APK signatures and pair identity without exposing embedded Maps keys."""
import hashlib
import json
import os
import pathlib
import re
import shutil
import subprocess
import xml.etree.ElementTree as ET

phone = pathlib.Path("app/build/outputs/apk/debug/app-debug.apk")
if not phone.is_file():
    print("No debug phone APK produced; configuration evidence unavailable.")
    raise SystemExit(0)
sdk = pathlib.Path(os.environ["ANDROID_HOME"])
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
result["mapsApiKeyConfigured"] = bool(key.strip()) and not key.startswith("${") and key not in {"YOUR_API_KEY", "DEFAULT_API_KEY"}
result["ciCheckout"] = os.environ.get("GITHUB_SHA")
wear = pathlib.Path("wear/build/outputs/apk/debug/wear-debug.apk")
requires_wear = os.environ.get("TRAINPILOT_REQUIRE_WEAR_PAIR") == "true"
if wear.is_file():
    wear_manifest, result["wear"] = inspect(wear)
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
