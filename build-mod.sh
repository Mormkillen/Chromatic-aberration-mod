#!/usr/bin/env bash
export JAVA_HOME="/tmp/jdk21dir/jdk-21.0.12+8"
cd "$(dirname "$0")"
GRADLE_BIN="${GRADLE_BIN:-/tmp/gradle-8.8/bin/gradle}"
"$GRADLE_BIN" build --no-daemon 2>&1 | tail -20
