#!/bin/bash

WARMUPS=200
RUNS=200

hyperfine \
  --shell=none --warmup "$WARMUPS" --runs "$RUNS" \
  -n 'node' "node $@" \
  -n 'deno' "deno run $@" \
  -n 'bun' "bun run $@" \
  -n 'elide' "./packages/cli/build/native/nativeOptimizedCompile/elide run $ELIDE_ARGS $@"
