#!/bin/env bash

source ./tools/scripts/bench-common.sh

# Resolved Elide binary
ELIDE="$1"

if [ ! -f "$ELIDE" ]; then
  echo "Elide binary not found"
  exit 2
fi

$ELIDE --version
echo "Starting server bench for Elide..."

OHA_ARGS="-c 2048 -z 60s --http-version 1.1"
WRK_ARGS="-c 2048 -d 60s -t 16 -L --timeout 15s -R 700000"
HYPERFINE_ARGS="--warmup 5000 --runs 1000 --shell none"

hyperfine $HYPERFINE_ARGS "curl http://localhost:3000/plaintext"
oha $OHA_ARGS http://localhost:3000/plaintext
wrk $WRK_ARGS http://localhost:3000/plaintext

echo "Server bench done."
