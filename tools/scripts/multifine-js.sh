#!/bin/bash

WARMUPS=100
RUNS=100
PROFILE=nativeOptimizedCompile

hyperfine \
  --shell=none --warmup "$WARMUPS" --runs "$RUNS" \
  -n 'node' "node $@" \
  -n 'deno' "deno run $@" \
  -n 'bun' "bun run $@" \
  -n 'elide' "./packages/cli/build/native/$PROFILE/elide run --host:allow-io --js:strict --js:experimental-disable-polyfills $ELIDE_ARGS $@"

