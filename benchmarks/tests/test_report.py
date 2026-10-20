import csv
import json
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import report


def measurement(name="bench.FrameworkBenchmark.commandNumeric", time=200, allocation=400):
    return {"benchmark": name, "mode": "avgt", "threads": 1, "forks": 2,
            "warmupIterations": 3, "warmupTime": "1 s", "measurementIterations": 5,
            "measurementTime": "1 s", "jvmArgs": ["-Xms512m", "-Xmx512m", "-XX:+UseG1GC"],
            "jdkVersion": "21", "jmhVersion": "1.37",
            "primaryMetric": {"score": time, "scoreUnit": "ns/op"},
            "secondaryMetrics": {"gc.alloc.rate.norm": {"score": allocation, "scoreUnit": "B/op"}}}


class ReportTest(unittest.TestCase):
    def write(self, path, values):
        path.write_text(json.dumps(values))
        return report.load(path)

    def test_improvements_and_regressions_have_correct_sign(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory)
            before = self.write(path / "before.json", [measurement()])
            after = self.write(path / "after.json", [measurement(time=100, allocation=600)])
            row = report.compare(before, after)[0]
            self.assertEqual(-50, row["time_change_percent"])
            self.assertEqual(50, row["allocation_change_percent"])

    def test_zero_allocation_baseline_does_not_divide_by_zero(self):
        self.assertIsNone(report.change(0, 10))
        self.assertEqual("n/a (zero baseline)", report.percent(None))

    def test_missing_workload_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "workloads"):
            report.compare({("one", ()): measurement()}, {})

    def test_duplicate_workload_is_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            with self.assertRaisesRegex(ValueError, "Duplicate"):
                self.write(Path(directory) / "data.json", [measurement(), measurement()])

    def test_distinct_parameters_are_preserved(self):
        values = [measurement(), measurement()]
        values[0]["params"] = {"players": "10"}
        values[1]["params"] = {"players": "200"}
        with tempfile.TemporaryDirectory() as directory:
            parsed = self.write(Path(directory) / "data.json", values)
            self.assertEqual(2, len(report.compare(parsed, parsed)))

    def test_wrong_units_are_rejected(self):
        value = measurement()
        value["primaryMetric"]["scoreUnit"] = "ops/s"
        with tempfile.TemporaryDirectory() as directory:
            with self.assertRaisesRegex(ValueError, "ns/op"):
                self.write(Path(directory) / "data.json", [value])

    def test_mismatched_runtime_or_measurement_settings_are_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory)
            before = self.write(path / "before.json", [measurement()])
            for field, value in [("forks", 1), ("jdkVersion", "25"), ("jvmArgs", ["-Xmx1g"])]:
                item = measurement()
                item[field] = value
                after = self.write(path / "after.json", [item])
                with self.subTest(field=field), self.assertRaisesRegex(ValueError, field):
                    report.compare(before, after)

    def test_nonfinite_negative_and_boolean_measurements_are_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            for value in [float("nan"), float("inf"), -1, True]:
                with self.subTest(value=value), self.assertRaises(ValueError):
                    self.write(Path(directory) / "data.json", [measurement(time=value)])

    def test_empty_results_are_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            with self.assertRaisesRegex(ValueError, "No benchmark"):
                self.write(Path(directory) / "data.json", [])

    def test_summary_and_csv_contain_comparison_and_memory_tradeoff(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory)
            self.write(path / "before.json", [measurement()])
            self.write(path / "after.json", [measurement(time=100, allocation=96)])
            for variant, size in [("before", 24), ("after", 192)]:
                (path / f"{variant}-memory.json").write_text(json.dumps({"total_owned_bytes": size}))
            text = report.generate(path, "base-sha", "head-sha")
            self.assertIn("base-sha", text)
            self.assertIn("head-sha", text)
            self.assertIn("-50.0%", text)
            self.assertIn("24 → 192 bytes", text)
            self.assertIn("not a whole-heap", text)
            with (path / "comparison.csv").open() as stream:
                rows = list(csv.DictReader(stream))
            self.assertEqual("96", rows[0]["after_bytes_per_op"])
            self.assertEqual(text, (path / "summary.md").read_text())

    def test_memory_measurements_are_optional(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory)
            self.write(path / "before.json", [measurement()])
            self.write(path / "after.json", [measurement()])
            self.assertNotIn("parameter-cache objects", report.generate(path, "base", "head"))


if __name__ == "__main__":
    unittest.main()
