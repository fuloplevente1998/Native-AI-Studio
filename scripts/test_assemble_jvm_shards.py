"""Fast fixture verification for shard assembly, not Android test substitution."""
import pathlib
import tempfile
import unittest

from assemble_jvm_shards import assemble

SHA = "verified-checkout"


class ShardAssemblyTest(unittest.TestCase):
    def setUp(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.root = pathlib.Path(temp.name) / "artifacts"
        self.root.mkdir()
        self.dest = self.root.parent / "output"
        self.manifest = self.root.parent / "manifest"
        self.manifest.write_text("test.B\n")

    def shard(self, name, attempt, classes, checkout=SHA):
        folder = self.root / f"trainpilot-jvm-shard-{name}-attempt-{attempt}"
        evidence = folder / "ci-evidence"
        evidence.mkdir(parents=True)
        (evidence / f"{name}.sha").write_text(checkout)
        for module, suite in classes:
            directory = folder / module / "build/test-results/testDebugUnitTest"
            directory.mkdir(parents=True, exist_ok=True)
            (directory / f"TEST-{suite}.xml").write_text(
                f'<testsuite name="{suite}" tests="1" failures="0" errors="0" skipped="0">'
                f'<testcase classname="{suite}" name="test"/></testsuite>')
        return folder

    def valid(self, a="test.A", b="test.B"):
        self.shard("phone-a", 1, [("app", a)])
        self.shard("phone-b", 1, [("app", b)])
        self.shard("wear-shared", 1, [("wear", "test.W"), ("shared", "test.S")])

    def run_assembly(self, attempt=1, targeted=False):
        assemble(self.root, self.dest, SHA, attempt, targeted, self.manifest)

    def test_retry_uses_latest_phone_and_retains_other_modules(self):
        self.valid()
        self.shard("phone-a", 2, [("app", "test.NewA")])
        self.run_assembly(2)
        self.assertEqual(len(list(self.dest.rglob("TEST-*.xml"))), 4)
        self.assertFalse(any(self.dest.rglob("TEST-test.A.xml")))
        self.assertTrue(any(self.dest.rglob("TEST-test.NewA.xml")))
        self.assertTrue(any(self.dest.rglob("TEST-test.S.xml")))

    def test_reject_duplicate_classes(self):
        self.valid(a="test.B")
        with self.assertRaisesRegex(ValueError, "Duplicate JUnit suite"):
            self.run_assembly()
        self.assertFalse(self.dest.exists())

    def test_reject_missing_b_class(self):
        self.valid(b="test.Other")
        with self.assertRaisesRegex(ValueError, "manifest mismatch"):
            self.run_assembly()

    def test_reject_missing_shard(self):
        self.shard("phone-a", 1, [("app", "test.A")])
        with self.assertRaisesRegex(ValueError, "Missing JVM shards"):
            self.run_assembly()

    def test_reject_stale_checkout(self):
        self.valid()
        self.shard("phone-a", 2, [("app", "test.A")], checkout="old")
        with self.assertRaisesRegex(ValueError, "Checkout mismatch"):
            self.run_assembly(2)

    def test_targeted_mode_still_uses_phone_and_wear_shared(self):
        self.shard("phone-a", 1, [("app", "test.B")])
        self.shard("wear-shared", 1, [("wear", "test.W"), ("shared", "test.S")])
        self.run_assembly(targeted=True)
        self.assertEqual(len(list(self.dest.rglob("TEST-*.xml"))), 3)


if __name__ == "__main__":
    unittest.main()
