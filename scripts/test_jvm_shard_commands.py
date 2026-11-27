"""Hermetic command/argument and stage fixtures for the shared JVM shard runner."""
import os
import pathlib
import subprocess
import tempfile
import unittest

SCRIPTS = pathlib.Path(__file__).resolve().parent


class SharedShardCommandsTest(unittest.TestCase):
    def setUp(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.root = pathlib.Path(temp.name)
        self.log = self.root / "gradle-arguments"
        tool = self.root / "gradle"
        tool.write_text('#!/usr/bin/env bash\nprintf "%s\\n" "$@" > "$GRADLE_ARGUMENTS"\n')
        tool.chmod(0o755)
        self.env = dict(os.environ, PATH=str(self.root) + os.pathsep + os.environ["PATH"],
                        GRADLE_ARGUMENTS=str(self.log),
                        WEAR_CLOSURE_REGRESSIONS="false", TARGETED_REGRESSIONS="false")

    def invoke(self, suite, closure=False, targeted=False, success=True):
        env = dict(self.env, SUITE=suite, WEAR_CLOSURE_REGRESSIONS=str(closure).lower(),
                   TARGETED_REGRESSIONS=str(targeted).lower())
        result = subprocess.run(["bash", str(SCRIPTS / "run_jvm_regression_shard.sh")],
                                env=env, cwd=self.root, capture_output=True, text=True)
        if not success:
            self.assertNotEqual(result.returncode, 0)
            self.assertFalse(self.log.exists())
            return []
        self.assertEqual(result.returncode, 0, result.stderr)
        args = self.log.read_text().splitlines()
        self.assertEqual(args[:4], ["--no-daemon", "--stacktrace", "--profile", "--continue"])
        return args[4:]

    def test_phone_shards_exact_complement(self):
        for suite in ("a", "b"):
            self.assertEqual(self.invoke("phone-" + suite),
                             ["-PtrainpilotPhoneTestShard=" + suite, ":app:testDebugUnitTest"])

    def test_wear_shared_unfiltered(self):
        self.assertEqual(self.invoke("wear-shared"), [":wear:testDebugUnitTest", ":shared:testDebugUnitTest"])

    def test_phone_targeted_filters_retained(self):
        args = self.invoke("phone-a", targeted=True)
        self.assertEqual(args[0], ":app:testDebugUnitTest")
        self.assertEqual(args[1::2], ["--tests"] * 10)
        self.assertEqual(args[2::2], [
            "com.example.ActiveWorkoutDockNavigationRegressionTest",
            "com.example.StudioProgress*",
            "com.example.StudioHealthPermissionActionTest",
            "com.example.StudioAppearanceRegressionTest",
            "com.example.StudioCloudRefreshRegressionTest",
            "com.example.JournalWorkoutHealthRegressionTest",
            "com.example.LegacyRestoreEvidenceRegressionTest",
            "com.example.PhoneReturnInstallationRegressionTest",
            "com.example.HealthProviderSelectionRegressionTest",
            "com.example.HealthSyncAdmissionRegressionTest",
        ])

    def test_closure_filters_retained(self):
        self.assertEqual(self.invoke("phone-a", closure=True), [
            ":app:testDebugUnitTest", "--tests", "com.example.WatchDiscardRecoveryRegressionTest",
            "--tests", "com.example.PhoneReturnInstallationRegressionTest",
            "--tests", "com.example.HealthSyncAdmissionRegressionTest",
        ])
        self.assertEqual(self.invoke("wear-shared", closure=True), [
            ":wear:testDebugUnitTest", "--tests", "com.repforge.app.wear.WearClosureRecoveryTest",
            "--tests", "com.repforge.app.wear.WearSaveSyncTest",
            "--tests", "com.repforge.app.wear.WorkoutOwnership193Test", ":shared:testDebugUnitTest",
        ])

    def test_invalid_or_duplicate_targeted_suite_rejected(self):
        self.invoke("bad", success=False)
        self.invoke("phone-b", targeted=True, success=False)

    def test_stage_retains_exact_checkout_and_module_paths(self):
        subprocess.run(["git", "init", "-q"], cwd=self.root, check=True)
        subprocess.run(["git", "-c", "user.name=Fixture", "-c", "user.email=fixture@example.invalid",
                        "commit", "--allow-empty", "-qm", "fixture"], cwd=self.root, check=True)
        expected = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=self.root, text=True).strip()
        for module in ("app", "wear", "shared"):
            p = self.root / module / "build/test-results/testDebugUnitTest/TEST-Fixture.xml"
            p.parent.mkdir(parents=True)
            p.write_text("<testsuite/>")
        subprocess.run(["bash", str(SCRIPTS / "stage_jvm_regression_shard.sh")],
                       cwd=self.root, env=dict(self.env, SUITE="wear-shared"), check=True)
        evidence = self.root / "ci-jvm-results"
        self.assertEqual((evidence / "ci-evidence/wear-shared.sha").read_text().strip(), expected)
        for module in ("wear", "shared"):
            self.assertTrue((evidence / module / "build/test-results/testDebugUnitTest/TEST-Fixture.xml").is_file())
        self.assertFalse((evidence / "app").exists())


if __name__ == "__main__":
    unittest.main()
