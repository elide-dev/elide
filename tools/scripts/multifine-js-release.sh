#!/bin/bash

WARMUPS=10
RUNS=50
PROFILE=nativeOptimizedCompile

hyperfine \
  --shell=none --warmup "$WARMUPS" --runs "$RUNS" \
  -n 'node' "node $@" \
  -n 'deno' "deno run $@" \
  -n 'bun' "bun run $@" \
  -n 'elide' "elide $ELIDE_ARGS $@"
