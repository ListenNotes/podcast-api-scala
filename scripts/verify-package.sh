#!/usr/bin/env bash
set -euo pipefail
sdk_root=$(cd "$(dirname "$0")/.." && pwd)
java_source=${PODCAST_API_JAVA_SOURCE:-}
if [[ -n "$java_source" ]]; then java_source=$(cd "$java_source" && pwd); fi
standalone=$(mktemp -d)
trap 'rm -rf "$standalone"' EXIT
(cd "$sdk_root" && tar -cf - settings.gradle gradle.properties gradlew gradlew.bat gradle app/build.gradle app/gradle.lockfile app/src README.md LICENSE) | (cd "$standalone" && tar -xf -)
cd "$standalone"
args=()
if [[ -n "$java_source" ]]; then args+=(--include-build "$java_source"); fi
./gradlew --no-daemon "${args[@]}" check installDist
