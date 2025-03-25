#!/bin/bash

WARMUPS=200
RUNS=200
PROFILE=nativeOptimizedCompile

hyperfine \
  --shell=none --warmup "$WARMUPS" --runs "$RUNS" \
  -n 'node' "node $@" \
  -n 'deno' "deno $@" \
  -n 'bun' "bun $@" \
  -n 'elide' "./packages/cli/build/native/$PROFILE/elide $ELIDE_ARGS $@"
