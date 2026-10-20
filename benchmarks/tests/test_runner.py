import os
from pathlib import Path
import subprocess
import tempfile
import unittest

RUNNER = Path(__file__).resolve().parents[1] / "run.sh"


class RunnerTest(unittest.TestCase):
    def test_missing_arguments_show_usage(self):
        result = subprocess.run(["bash", str(RUNNER)], capture_output=True, text=True)
        self.assertEqual(2, result.returncode)
        self.assertIn("Usage:", result.stderr)

    def test_unknown_revision_is_rejected_before_creating_results(self):
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "results"
            result = subprocess.run(["bash", str(RUNNER), "refs/heads/__missing_benchmark_ref__", "HEAD", str(output)],
                                    capture_output=True, text=True)
            self.assertNotEqual(0, result.returncode)
            self.assertFalse(output.exists())

    def test_failed_build_cleans_stale_results_and_keeps_diagnostics(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            tools = root / "bin"
            tools.mkdir()
            scripts = {
                "java": "#!/bin/sh\nprintf 'Fake Java for failure-path test\\n'\n",
                "mvn": "#!/bin/sh\nif [ \"$1\" = -version ]; then printf 'Fake Maven\\n'; exit 0; fi\nprintf 'Expected fixture build failure\\n'; exit 23\n",
            }
            for name, text in scripts.items():
                tool = tools / name
                tool.write_text(text)
                tool.chmod(0o755)
            output = root / "results with spaces"
            output.mkdir()
            stale = ["summary.md", "comparison.csv", "harness-build.log", "before.json", "after.json",
                     "before-memory.json", "after-memory.json", "after-build.log", "after-jmh.log", "after-smoke.log"]
            for name in stale:
                (output / name).write_text("old result")
            (output / "unrelated.txt").write_text("keep")
            environment = os.environ.copy()
            environment["PATH"] = str(tools) + os.pathsep + environment["PATH"]
            result = subprocess.run(["bash", str(RUNNER), "HEAD", "HEAD", str(output)],
                                    env=environment, capture_output=True, text=True)
            self.assertEqual(1, result.returncode)
            self.assertIn("Build failed", result.stderr)
            self.assertIn("Expected fixture build failure", (output / "before-build.log").read_text())
            self.assertEqual("keep", (output / "unrelated.txt").read_text())
            for name in stale:
                self.assertFalse((output / name).exists(), name)


if __name__ == "__main__":
    unittest.main()
