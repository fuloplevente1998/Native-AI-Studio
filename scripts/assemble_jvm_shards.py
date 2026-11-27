"""Assemble exact, disjoint current-run raw JVM reports; never overwrite a suite."""
import argparse
import pathlib
import re
import shutil
import xml.etree.ElementTree as ET

ARTIFACT = re.compile(r"trainpilot-jvm-shard-(phone-a|phone-b|wear-shared)-attempt-([1-9][0-9]*)$")
MANIFEST = pathlib.Path(__file__).with_name("ci_phone_test_shard_b.txt")
MODULES = {"phone-a": ("app",), "phone-b": ("app",), "wear-shared": ("wear", "shared")}
XML_DIR = pathlib.Path("build/test-results/testDebugUnitTest")


def require(ok, message):
    if not ok:
        raise ValueError(message)


def assemble(root, destination, checkout, attempt, targeted=False, manifest=MANIFEST):
    """Use latest available artifact per suite, including retained jobs on retry.

    The workflow independently requires all job conclusions to be success. A stale
    report may never hide a failed job; all reports must have the same git checkout.
    """
    required = ("phone-a", "wear-shared") if targeted else tuple(MODULES)
    latest = {}
    for folder in root.iterdir():
        if not folder.is_dir():
            continue
        match = ARTIFACT.fullmatch(folder.name)
        require(match is not None, f"Unexpected shard artifact: {folder.name}")
        suite, raw_attempt = match.groups()
        n = int(raw_attempt)
        require(suite in required and n <= attempt, f"Unexpected suite/attempt: {folder.name}")
        if suite not in latest or n > latest[suite][0]:
            latest[suite] = (n, folder)
    require(set(latest) == set(required), f"Missing JVM shards: {set(required) - set(latest)}")
    expected_b = {line.strip() for line in manifest.read_text().splitlines()
                  if line.strip() and not line.startswith("#")}
    require(bool(expected_b), "Empty phone shard B manifest")

    seen_suites, seen_cases, seen_paths = set(), set(), set()
    phone = {"phone-a": set(), "phone-b": set()}
    copies = []
    for suite in required:
        n, folder = latest[suite]
        marker = folder / "ci-evidence" / f"{suite}.sha"
        require(marker.is_file() and marker.read_text().strip() == checkout,
                f"Checkout mismatch for {suite}")
        require(not (destination / "ci-evidence" / marker.name).exists(), "Existing checkout marker")
        copies.append((marker, pathlib.Path("ci-evidence") / marker.name))
        for module in MODULES[suite]:
            relative = pathlib.Path(module) / XML_DIR
            xml = sorted((folder / relative).glob("TEST-*.xml"))
            require(bool(xml), f"Missing raw XML for {suite}/{module}")
            for source in xml:
                tree = ET.parse(source).getroot()
                name = tree.attrib.get("name", "")
                require(tree.tag == "testsuite" and bool(name), f"Invalid JUnit suite {source}")
                key = (module, name)
                require(key not in seen_suites, f"Duplicate JUnit suite {key}")
                seen_suites.add(key)
                if module == "app":
                    phone[suite].add(name)
                for case in tree.findall("testcase"):
                    key = (module, case.attrib.get("classname"), case.attrib.get("name"))
                    require(key not in seen_cases, f"Duplicate JUnit testcase {key}")
                    seen_cases.add(key)
                target = relative / source.name
                require(target not in seen_paths and not (destination / target).exists(),
                        f"Duplicate/existing report path {target}")
                seen_paths.add(target)
                copies.append((source, target))
        print(f"{suite}: selected artifact attempt {n}, checkout {checkout}")
    if not targeted:
        require(phone["phone-b"] == expected_b,
                f"Phone B manifest mismatch: missing={expected_b - phone['phone-b']}, "
                f"unexpected={phone['phone-b'] - expected_b}")
        require(not phone["phone-a"] & expected_b, "Phone A unexpectedly contains B test classes")
    # Validate everything before creating canonical copies. The existing all-module
    # verifier subsequently checks counts, zero errors/failures and zero full-run skips.
    for source, relative in copies:
        target = destination / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, target)
    print(f"Assembled {len(seen_suites)} distinct suites and {len(seen_cases)} distinct testcases.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=pathlib.Path, required=True)
    parser.add_argument("--checkout", required=True)
    parser.add_argument("--attempt", type=int, required=True)
    parser.add_argument("--targeted", choices=("true", "false"), required=True)
    args = parser.parse_args()
    assemble(args.root, pathlib.Path("."), args.checkout, args.attempt, args.targeted == "true")
