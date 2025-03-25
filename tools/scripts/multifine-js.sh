#!/bin/bash

WARMUPS=200
RUNS=200
PROFILE=nativeOptimizedCompile

hyperfine \
  --shell=none --warmup "$WARMUPS" --runs "$RUNS" \
  -n 'node' "node $@" \
  -n 'deno' "deno run $@" \
  -n 'bun' "bun run $@" \
  -n 'elide' "./packages/cli/build/native/$PROFILE/elide run --host:allow-io $ELIDE_ARGS $@"

