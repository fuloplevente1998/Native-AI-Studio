"""Stage and reassemble only the signed release outputs of one trusted workflow run."""
import hashlib
import json
import os
from pathlib import Path
import shutil
import sys
import unittest
import tempfile

MODULES = ("app", "wear")


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def outputs(module, build_aab):
    if module not in MODULES:
        raise ValueError("Invalid signed module")
    paths = [f"{module}/build/outputs/apk/release/{module}-release.apk"]
    if build_aab:
        paths.append(f"{module}/build/outputs/bundle/release/{module}-release.aab")
    return paths


def require(condition, message):
    if not condition:
        raise ValueError(message)


def stage(root, target, module, source, run, attempt, build_aab):
    target.mkdir(parents=True, exist_ok=False)
    files = {}
    for rel in outputs(module, build_aab):
        src = root / rel
        require(src.is_file() and src.stat().st_size > 0, f"Missing signed output: {rel}")
        dest = target / rel
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(src, dest)
        files[rel] = digest(dest)
    (target / "signed-module.json").write_text(json.dumps({
        "module": module, "source": source, "run": run, "attempt": attempt,
        "buildAab": build_aab, "files": files,
    }, indent=2) + "\n")


def assemble(staging, root, source, run, attempt, build_aab):
    # A successful prior-attempt job may be reused by GitHub's failed-job rerun.
    # Never accept a different run, source, module, output mode or future attempt.
    verified = []
    for module in MODULES:
        folder = staging / module
        evidence = json.loads((folder / "signed-module.json").read_text())
        require(evidence["module"] == module and evidence["source"] == source
                and evidence["run"] == run and evidence["buildAab"] is build_aab
                and 1 <= evidence["attempt"] <= attempt, "Signed module provenance mismatch")
        expected = set(outputs(module, build_aab))
        require(set(evidence["files"]) == expected, "Signed module output set mismatch")
        actual = {p.relative_to(folder).as_posix() for p in folder.rglob("*") if p.is_file()}
        require(actual == expected | {"signed-module.json"}, "Unexpected signed artifact content")
        for rel in sorted(expected):
            src = folder / rel
            require(src.stat().st_size > 0 and digest(src) == evidence["files"][rel],
                    f"Signed output digest mismatch: {rel}")
            require(not (root / rel).exists(), f"Refusing to overwrite existing output: {rel}")
            verified.append((src, root / rel))
    for src, dest in verified:
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(src, dest)


class ModuleEvidenceTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.staging = self.root / "staging"
        self.source = "a" * 40
        self.run = "42"

    def prepare(self, build_aab=True):
        for module in MODULES:
            for rel in outputs(module, build_aab):
                path = self.root / "src" / rel
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes(rel.encode())
            stage(self.root / "src", self.staging / module, module,
                  self.source, self.run, 1, build_aab)

    def verify(self, source=None, run=None, attempt=1, build_aab=True):
        assemble(self.staging, self.root / "dest", source or self.source,
                 run or self.run, attempt, build_aab)

    def test_four_outputs_and_retry(self):
        self.prepare()
        self.verify(attempt=2)
        for module in MODULES:
            for rel in outputs(module, True):
                self.assertEqual((self.root / "dest" / rel).read_bytes(), rel.encode())

    def test_apk_only(self):
        self.prepare(False)
        self.verify(build_aab=False)
        self.assertEqual(len(list((self.root / "dest").rglob("*.apk"))), 2)
        self.assertEqual(list((self.root / "dest").rglob("*.aab")), [])

    def test_wrong_source_run_or_mode(self):
        self.prepare()
        for kwargs in ({"source": "b" * 40}, {"run": "43"}, {"build_aab": False}, {"attempt": 0}):
            with self.subTest(kwargs=kwargs), self.assertRaises(ValueError):
                self.verify(**kwargs)

    def test_missing_module(self):
        self.prepare()
        shutil.rmtree(self.staging / "wear")
        with self.assertRaises(FileNotFoundError):
            self.verify()
        self.assertFalse((self.root / "dest").exists())

    def test_tampered_output(self):
        self.prepare()
        (self.staging / "wear" / outputs("wear", True)[0]).write_bytes(b"tampered")
        with self.assertRaises(ValueError):
            self.verify()
        self.assertFalse((self.root / "dest").exists())

    def test_extra_file(self):
        self.prepare()
        (self.staging / "app" / "unexpected.txt").write_text("extra")
        with self.assertRaises(ValueError):
            self.verify()


if __name__ == "__main__":
    if sys.argv[1:] == ["--self-test"]:
        unittest.main(argv=[__file__])
    else:
        source = os.environ["TRAINPILOT_SOURCE_SHA"]
        run = os.environ["GITHUB_RUN_ID"]
        attempt = int(os.environ["GITHUB_RUN_ATTEMPT"])
        mode = os.environ["BUILD_AAB"]
        require(mode in ("true", "false"), "Invalid AAB mode")
        build_aab = mode == "true"
        if sys.argv[1:] == ["stage"]:
            stage(Path("."), Path("signed-module"), os.environ["MODULE"], source, run, attempt, build_aab)
        elif sys.argv[1:] == ["assemble"]:
            assemble(Path("signed-modules"), Path("."), source, run, attempt, build_aab)
        else:
            raise SystemExit("Expected stage, assemble or --self-test")
