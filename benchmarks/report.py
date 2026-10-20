#!/usr/bin/env python3
"""Compare JMH output from identical workloads and emit Markdown and CSV."""
import argparse
import csv
import json
import math
from pathlib import Path


def number(value):
    if isinstance(value, bool) or not isinstance(value, (int, float)) or not math.isfinite(value):
        raise ValueError(f"Expected a finite measurement, got {value!r}")
    return value


def load(path):
    data = json.loads(path.read_text())
    if not isinstance(data, list) or not data:
        raise ValueError(f"No benchmark measurements in {path}")
    result = {}
    for item in data:
        key = (item["benchmark"], tuple(sorted(item.get("params", {}).items())))
        if key in result:
            raise ValueError(f"Duplicate benchmark: {key}")
        metric = item["primaryMetric"]
        allocation = item["secondaryMetrics"]["gc.alloc.rate.norm"]
        if item["mode"] != "avgt" or metric["scoreUnit"] != "ns/op" or allocation["scoreUnit"] != "B/op":
            raise ValueError("Expected average-time ns/op and GC allocation B/op measurements")
        if number(metric["score"]) <= 0 or number(allocation["score"]) < 0:
            raise ValueError("Time must be positive and allocation must be nonnegative")
        result[key] = item
    return result


def change(before, after):
    return None if before == 0 else 100 * (after / before - 1)


def compare(before, after):
    if before.keys() != after.keys():
        raise ValueError("Base/head benchmark workloads or parameters do not match")
    settings = ("mode", "threads", "forks", "warmupIterations", "warmupTime", "measurementIterations",
                "measurementTime", "jvmArgs", "jdkVersion", "jmhVersion")
    rows = []
    for key, old in sorted(before.items()):
        new = after[key]
        for setting in settings:
            if old.get(setting) != new.get(setting):
                raise ValueError(f"Mismatched {setting} for {key[0]}")
        old_time, new_time = old["primaryMetric"]["score"], new["primaryMetric"]["score"]
        old_bytes = old["secondaryMetrics"]["gc.alloc.rate.norm"]["score"]
        new_bytes = new["secondaryMetrics"]["gc.alloc.rate.norm"]["score"]
        rows.append({"benchmark": key[0], "params": json.dumps(dict(key[1]), sort_keys=True),
                     "before_ns_per_op": old_time, "after_ns_per_op": new_time,
                     "time_change_percent": change(old_time, new_time),
                     "before_bytes_per_op": old_bytes, "after_bytes_per_op": new_bytes,
                     "allocation_change_percent": change(old_bytes, new_bytes)})
    return rows


def percent(value):
    return "n/a (zero baseline)" if value is None else f"{value:+.1f}%"


def markdown(rows, base, head, metadata):
    lines = ["# PR benchmark comparison", "", f"Base: `{base}`  ", f"Head: `{head}`", "",
             "Negative changes mean less time or fewer allocated bytes; positive changes mean more.", "",
             "| Workload | Base ns/op | Head ns/op | Time change | Allocated B/op (base → head) | Allocation change |",
             "|---|---:|---:|---:|---:|---:|"]
    for row in rows:
        label = row["benchmark"].split(".")[-1]
        if row["params"] != "{}":
            label += " " + row["params"]
        label = label.replace("|", "\\|").replace("\n", " ")
        lines.append(f"| {label} | {row['before_ns_per_op']:,.1f} | {row['after_ns_per_op']:,.1f} | "
                     f"{percent(row['time_change_percent'])} | {row['before_bytes_per_op']:,.0f} → "
                     f"{row['after_bytes_per_op']:,.0f} | {percent(row['allocation_change_percent'])} |")
    lines += ["", f"JMH {metadata['jmhVersion']}; {metadata['forks']} JVM forks; {metadata['threads']} thread(s); "
              f"{metadata['warmupIterations']} × {metadata['warmupTime']} warmup and "
              f"{metadata['measurementIterations']} × {metadata['measurementTime']} measurement per fork. "
              "Both versions run with the same harness and dependencies.", "",
              f"JDK {metadata['jdkVersion']}; JVM flags: `{' '.join(metadata['jvmArgs'])}`.", "",
              "These microbenchmarks measure framework overhead using synthetic senders/players. "
              "Allocated bytes per operation are not total RAM usage. Hosted-runner noise can affect timings; "
              "no performance threshold is enforced. Raw JMH data includes measurement uncertainty."]
    return "\n".join(lines) + "\n"


def generate(directory, base, head):
    before = load(directory / "before.json")
    rows = compare(before, load(directory / "after.json"))
    text = markdown(rows, base, head, next(iter(before.values())))
    memory_paths = [directory / f"{version}-memory.json" for version in ("before", "after")]
    if all(path.exists() for path in memory_paths):
        memory = [json.loads(path.read_text()) for path in memory_paths]
        sizes = [number(item["total_owned_bytes"]) for item in memory]
        text += (f"\nBundle and known parameter-cache objects (three user arguments plus context): "
                 f"**{sizes[0]:,.0f} → {sizes[1]:,.0f} bytes**. "
                 "This probe excludes shared reflection, converter, and annotation objects, "
                 "and is not a whole-heap measurement.\n")
    (directory / "summary.md").write_text(text)
    with (directory / "comparison.csv").open("w", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=list(rows[0]))
        writer.writeheader()
        writer.writerows(rows)
    return text


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("directory", type=Path)
    parser.add_argument("--base", required=True)
    parser.add_argument("--head", required=True)
    args = parser.parse_args()
    generate(args.directory, args.base, args.head)
