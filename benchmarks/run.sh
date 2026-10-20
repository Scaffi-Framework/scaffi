#!/usr/bin/env bash
set -euo pipefail
if (( $# < 2 || $# > 3 )); then
    echo 'Usage: bash benchmarks/run.sh BASE_REF HEAD_REF [RESULTS_DIRECTORY]' >&2
    exit 2
fi
source_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_dir="$(git -C "$source_dir" rev-parse --show-toplevel)"
base_sha="$(git -C "$repo_dir" rev-parse --verify --end-of-options "${1}^{commit}")"
head_sha="$(git -C "$repo_dir" rev-parse --verify --end-of-options "${2}^{commit}")"
results_dir="${3:-$repo_dir/benchmark-results}"
mkdir -p "$results_dir"
results_dir="$(cd "$results_dir" && pwd)"
# A failed rerun must not publish measurements or a summary from an earlier run.
rm -f "$results_dir/summary.md" "$results_dir/comparison.csv" "$results_dir/harness-build.log"
for variant in before after; do
    rm -f "$results_dir/$variant.json" "$results_dir/$variant-memory.json" \
        "$results_dir/$variant-build.log" "$results_dir/$variant-smoke.log" "$results_dir/$variant-jmh.log"
done
work_dir="$(mktemp -d "${TMPDIR:-/tmp}/scaffi-benchmark.XXXXXX")"
trap 'rm -rf "$work_dir"' EXIT
printf '%s\n' "$base_sha" > "$results_dir/base-sha.txt"
printf '%s\n' "$head_sha" > "$results_dir/head-sha.txt"
java -version > "$results_dir/java-version.txt" 2>&1
mvn -version > "$results_dir/maven-version.txt" 2>&1

for variant in before after; do
    if [[ "$variant" == before ]]; then revision="$base_sha"; else revision="$head_sha"; fi
    mkdir -p "$work_dir/$variant"
    git -C "$repo_dir" archive "$revision" | tar -x -C "$work_dir/$variant"
    echo "Building $variant ($revision)"
    if ! mvn --batch-mode --no-transfer-progress -f "$work_dir/$variant/pom.xml" \
        -DskipTests -Dmaven.javadoc.skip=true package > "$results_dir/$variant-build.log" 2>&1; then
        echo "Build failed; see $results_dir/$variant-build.log" >&2
        exit 1
    fi
    jars=()
    for candidate in "$work_dir/$variant"/target/*.jar; do
        [[ -f "$candidate" ]] || continue
        case "$(basename "$candidate")" in
            *-sources.jar|*-javadoc.jar|original-*) continue ;;
        esac
        jars+=("$candidate")
    done
    if (( ${#jars[@]} != 1 )); then
        echo "Expected exactly one main JAR for $variant, found ${#jars[@]}." >&2
        exit 1
    fi
    cp "${jars[0]}" "$work_dir/$variant.jar"
done

mkdir -p "$work_dir/harness"
cp "$source_dir/pom.xml" "$work_dir/harness/pom.xml"
cp -R "$source_dir/src" "$work_dir/harness/src"
if ! mvn --batch-mode --no-transfer-progress -f "$work_dir/harness/pom.xml" \
    "-Dscaffi.jar=$work_dir/before.jar" package > "$results_dir/harness-build.log" 2>&1; then
    echo "Harness build failed; see $results_dir/harness-build.log" >&2
    exit 1
fi
harness_jar="$work_dir/harness/target/benchmarks.jar"
for variant in before after; do
    benchmark_cp="$harness_jar:$work_dir/$variant.jar"
    echo "Checking workload outputs for $variant"
    java -cp "$benchmark_cp" bench.FrameworkBenchmark > "$results_dir/$variant-smoke.log" 2>&1
    echo "Benchmarking $variant (about two minutes)"
    java -cp "$benchmark_cp" org.openjdk.jmh.Main bench.FrameworkBenchmark \
        -prof gc -rf json -rff "$results_dir/$variant.json" -foe true \
        > "$results_dir/$variant-jmh.log" 2>&1
    # Checkpoint the known bundle/cache footprint; shared objects are excluded.
    if [[ ! -f "$work_dir/memory-probe.jar" ]]; then
        printf 'Premain-Class: bench.MemoryProbe\n\n' > "$work_dir/agent.mf"
        jar --create --file "$work_dir/memory-probe.jar" --manifest "$work_dir/agent.mf" \
            -C "$work_dir/harness/target/classes" bench/MemoryProbe.class
    fi
    java -Xms512m -Xmx512m -XX:+UseG1GC "-javaagent:$work_dir/memory-probe.jar" \
        -cp "$work_dir/memory-probe.jar:$benchmark_cp" bench.MemoryProbe > "$results_dir/$variant-memory.json"
done
python3 "$source_dir/report.py" "$results_dir" --base "$base_sha" --head "$head_sha"
echo "Comparison saved to $results_dir/summary.md"
