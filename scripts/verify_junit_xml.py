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
report = {"commit": os.environ.get("TRAINPILOT_SOURCE_SHA") or os.environ.get("GITHUB_SHA", ""), "modules": {}, "total": {
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
    source_dir = pathlib.Path("app" if module == "phone" else module) / "src" / "test"
    has_test_sources = any(file.suffix in {".kt", ".java"} for file in source_dir.rglob("*")) if source_dir.is_dir() else False
    counts["noSource"] = not has_test_sources
    report["modules"][module] = counts

destination = pathlib.Path("app/build/outputs/junit-verification.json")
destination.parent.mkdir(parents=True, exist_ok=True)
destination.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
print(json.dumps(report, indent=2, ensure_ascii=False))
if any(row["tests"] == 0 or row["files"] == 0 for row in report["modules"].values() if not row["noSource"]):
    print("Missing or empty raw JUnit XML from a module with JVM test sources", file=sys.stderr)
    sys.exit(1)
if any(row["files"] or row["tests"] for row in report["modules"].values() if row["noSource"]):
    print("Unexpected JUnit XML for a NO-SOURCE module", file=sys.stderr)
    sys.exit(1)
if report["total"]["failures"] or report["total"]["errors"]:
    print("JUnit XML contains failing or error tests", file=sys.stderr)
    sys.exit(1)
