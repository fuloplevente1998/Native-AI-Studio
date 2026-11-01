"""Record APK identity/Maps readiness, without outputting the embedded API key."""
import hashlib
import json
import os
import pathlib
import re
import shutil
import subprocess
import xml.etree.ElementTree as ET

apk = pathlib.Path("app/build/outputs/apk/debug/app-debug.apk")
if not apk.is_file():
    print("No debug APK produced; configuration evidence unavailable.")
    raise SystemExit(0)
sdk = pathlib.Path(os.environ["ANDROID_HOME"])
analyzer = shutil.which("apkanalyzer")
if not analyzer:
    analyzer = str(next(sdk.glob("cmdline-tools/*/bin/apkanalyzer")))
manifest = ET.fromstring(subprocess.check_output([analyzer, "manifest", "print", str(apk)], text=True))
android = "{http://schemas.android.com/apk/res/android}"
key = next((node.get(android + "value", "") for node in manifest.findall("application/meta-data")
            if node.get(android + "name") == "com.google.android.geo.API_KEY"), "")
configured = bool(key.strip()) and not key.startswith("${") and key not in {"YOUR_API_KEY", "DEFAULT_API_KEY"}
signer_tool = sdk / "build-tools/36.0.0/apksigner"
verified = subprocess.check_output([str(signer_tool), "verify", "--print-certs", str(apk)], text=True)
sha1 = re.search(r"Signer #1 certificate SHA-1 digest: ([0-9a-fA-F]+)", verified)
sha256 = re.search(r"Signer #1 certificate SHA-256 digest: ([0-9a-fA-F]+)", verified)
assert sha1 and sha256, "APK signer evidence unavailable"
result = {
    "applicationId": manifest.get("package"),
    "versionCode": manifest.get(android + "versionCode"),
    "versionName": manifest.get(android + "versionName"),
    "mapsApiKeyConfigured": configured,
    "apkSha256": hashlib.sha256(apk.read_bytes()).hexdigest(),
    "signerSha1": sha1.group(1),
    "signerSha256": sha256.group(1),
    "ciCheckout": os.environ.get("GITHUB_SHA"),
}
pathlib.Path("app/build/outputs/apk-verification.json").write_text(json.dumps(result, indent=2) + "\n")
print(json.dumps(result, indent=2))
