"""Collect exact-checkout, mutually exclusive JVM shard XML without overwriting tests."""
import os
import pathlib
import shutil
import subprocess
import xml.etree.ElementTree as ET

ROOT = pathlib.Path("ci-jvm-shards")
CHECKOUT = subprocess.check_output(["git", "rev-parse", "HEAD"], text=True).strip()
TARGETED = os.environ.get("TARGETED") == "true"
SUITES = ("phone", "wear-shared") if TARGETED else ("phone-a", "phone-b", "wear-shared")
GROUP_A = frozenset("AGHILPST")
evidence = pathlib.Path("ci-evidence")
evidence.mkdir(exist_ok=True)
seen = set()
for suite in SUITES:
    source = ROOT / f"trainpilot-jvm-shard-{suite}"
    marker = source / "ci-evidence" / f"{suite}.sha"
    if marker.read_text().strip() != CHECKOUT:
        raise SystemExit(f"Wrong checkout: {suite}")
    shutil.copyfile(marker, evidence / marker.name)
    modules = ("app",) if suite.startswith("phone") else ("wear", "shared")
    for module in modules:
        relative = pathlib.Path(module) / "build/test-results/testDebugUnitTest"
        files = sorted((source / relative).glob("TEST-*.xml"))
        if not files:
            raise SystemExit(f"Missing raw XML: {suite}/{module}")
        destination = relative
        destination.mkdir(parents=True, exist_ok=True)
        for file in files:
            parsed = ET.parse(file).getroot()
            name = parsed.attrib["name"]
            if parsed.tag != "testsuite":
                raise SystemExit(f"Invalid XML: {file}")
            if suite in ("phone-a", "phone-b"):
                initial = name.rsplit(".", 1)[-1][:1]
                if (initial in GROUP_A) != (suite == "phone-a"):
                    raise SystemExit(f"Wrong test partition: {suite}: {name}")
            key = (module, name)
            if key in seen or (destination / file.name).exists():
                raise SystemExit(f"Duplicate raw suite: {key}")
            seen.add(key)
            shutil.copyfile(file, destination / file.name)
        print(f"{suite}/{module}: {len(files)} distinct XML suites from {CHECKOUT}")
