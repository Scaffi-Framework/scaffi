# Optional PR benchmarks

Add the **`benchmark`** label to a pull request to compare its base commit with its head commit. The benchmark runs again when the labeled PR is updated; remove the label to stop opting in. Create the label in the repository if it does not exist.

Alternatively, open **Actions → Optional PR benchmark → Run workflow** and enter the PR number. GitHub only shows manual dispatch after the workflow exists on the default branch. The PR must contain this benchmark tooling. Fork PRs use the normal `pull_request` event and GitHub's usual workflow approval rules.

The workflow is independent of publishing. It adds a comparison table to the **Actions run summary** and uploads a `benchmark-pr-…` artifact containing raw JMH JSON, normalized allocation measurements, CSV, bundle/cache sizes, commit IDs, JVM details, and diagnostic logs. Results are retained for 14 days. It takes approximately four minutes plus dependency/build time. Do not make this optional job a required branch check.

## Local comparison

Requires Java 21, Maven, Python 3, Bash, and a Git checkout containing both commits:

```bash
bash benchmarks/run.sh BASE_REF HEAD_REF
# Example: compare the PR branch with its target branch
bash benchmarks/run.sh origin/version/0.2.0 HEAD
```

Results go to `benchmark-results/` (ignored by Git). A third argument selects another output directory. Both revisions are built from temporary Git archives; the checkout is not modified. The benchmark harness comes from the current checkout and is compiled once against the baseline API. Main JAR filenames are detected, so changing the framework version does not require editing the script. Failed builds or incompatible APIs produce a failed run with diagnostic artifacts.

## Measurements

- Context-only commands; typed String/Integer/Boolean arguments; Vector3 conversion; omitted optional arguments.
- Tab completion from 100 command candidates and 200 synthetic players, with ten matches each.
- JMH 1.37, two forks per workload/version, one thread, 3 × 1s warmup and 5 × 1s measurement per fork.
- Fixed 512 MiB heap and G1 GC; elapsed time and allocated bytes per operation.
- A Java agent measures the shallow size of a bundle plus the known parameter-cache array/records. Shared objects are excluded.

The comparison uses a common Spigot dependency, configurable through `spigot.version` in this directory's POM. If a framework API or supported Spigot version changes incompatibly, update the shared harness before comparing those revisions. Command fixtures return a reusable empty response to isolate framework work. Both versions' fixture outputs are checked before measurement.

These are microbenchmarks, not a Minecraft server TPS or total-heap test. Negative percentage changes indicate less time/allocation; positive changes indicate more. Hosted-runner contention and measurement order can influence timings. Inspect raw measurements and rerun uncertain results. The workflow reports regressions without enforcing a threshold.

## Tooling checks

```bash
python3 -m unittest discover -s benchmarks/tests -v
bash -n benchmarks/run.sh
```
