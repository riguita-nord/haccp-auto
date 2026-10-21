#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p out data
javac -encoding UTF-8 -d out src/haccp/Main.java
java -cp out haccp.Main
