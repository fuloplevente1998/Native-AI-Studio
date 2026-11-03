"""Verify raw Gradle JUnit XML from phone, Wear and shared JVM test tasks."""
import json
import os
import pathlib
import sys
import xml.etree.ElementTree as ET

sources = {
    "phone": pathlib.Path("app/build/test-results/testDebugUnitTest"),
    "wear": pathlib.Path("wear/build/test-results/testDebugUnitTest"),
    "shared": pathlib.Path("shared/build/test-results/testDebugUnitTest"),
}
report = {"commit": os.environ.get("GITHUB_SHA", ""), "modules": {}, "total": {
    "tests": 0, "failures": 0, "errors": 0, "skipped": 0
}}
for module, directory in sources.items():
    counts = {"files": 0, "tests": 0, "failures": 0, "errors": 0, "skipped": 0}
    for file in sorted(directory.glob("TEST-*.xml")):
        try:
            suite = ET.parse(file).getroot()
            if suite.tag != "testsuite":
                raise ValueError("Expected a raw JUnit testsuite root")
            numbers = {key: int(suite.attrib.get(key, "0")) for key in (
                "tests", "failures", "errors", "skipped")}
            if any(value < 0 for value in numbers.values()):
                raise ValueError("Negative JUnit count")
        except (ValueError, ET.ParseError) as exc:
            raise SystemExit(f"Invalid raw JUnit XML {file}: {exc}")
        counts["files"] += 1
        for key, value in numbers.items():
            counts[key] += value
            report["total"][key] += value
    report["modules"][module] = counts

destination = pathlib.Path("app/build/outputs/junit-verification.json")
destination.parent.mkdir(parents=True, exist_ok=True)
destination.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
print(json.dumps(report, indent=2, ensure_ascii=False))
if not all(row["files"] and row["tests"] for row in report["modules"].values()):
    print("Missing or empty raw JUnit XML from a required module", file=sys.stderr)
    sys.exit(1)
if report["total"]["failures"] or report["total"]["errors"]:
    print("JUnit XML contains failing or error tests", file=sys.stderr)
    sys.exit(1)
