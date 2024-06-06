#env bash

source ./tools/scripts/bench-common.sh

# Resolved Elide binary
ELIDE="$1"

HYPERFINE_ARGS="--shell none"

# run `--help`
hyperfine $HYPERFINE_ARGS "$ELIDE --help"

# run some hello scripts
hyperfine $HYPERFINE_ARGS -n hello-js "$ELIDE run --javascript ./tools/scripts/hello.js" -n hello-ts "$ELIDE run --typescript ./tools/scripts/hello.ts"

# run some sqlite stuff
hyperfine $HYPERFINE_ARGS -n sqlite "$ELIDE run --javascript ./tools/scripts/sqlite.js"

echo "CLI bench done."
