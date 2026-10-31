#!/usr/bin/env bash
set -eu
rm -rf out
mkdir -p out
javac -d out src/*.java
java -ea -cp out SnapshotDecisionTest
java -cp out NightlySnapshotService
