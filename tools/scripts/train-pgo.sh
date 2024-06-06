#env bash

# Resolved Elide binary
ELIDE="$1"

# Parameters
SERVER_JS="./tools/scripts/server.js"
SERVER_PY="./tools/scripts/serve-docs.py"
HYPERFINE_ARGS="--shell none"
SERVER_WARMUPS=5000
SERVER_RUNS=10000
EXEC_WARMUPS=0
EXEC_RUNS=25

if [ ! -f "$ELIDE" ]; then
  echo "Elide binary not found"
  exit 2
fi

echo "Elide version:"
$ELIDE --version

echo "Starting PGO warmup for Elide at version $VERSION"

set -o xtrace

# run the server
$ELIDE serve --javascript $SERVER_JS &
set +o xtrace
ELIDE_JS_SERVER_PID=$!

echo "Using JS server at PID $ELIDE_JS_SERVER_PID"
echo "Beginning server warmup..."

set -o xtrace

# server warmup
hyperfine --command-name "server-plaintext" --warmup "$SERVER_WARMUPS" --runs "$SERVER_RUNS" $HYPERFINE_ARGS "curl http://localhost:3000/plaintext"
hyperfine --command-name "server-json" --warmup "$SERVER_WARMUPS" --runs "$SERVER_RUNS" $HYPERFINE_ARGS "curl http://localhost:3000/json"

set +o xtrace

echo "Halting JS server at PID $ELIDE_JS_SERVER_PID"
kill -SIGINT "$ELIDE_JS_SERVER_PID"
echo "Finished server warmup. Beginning execution warmup..."

# javascript execution
echo "- Warming up JavaScript..."
set -o xtrace
hyperfine --command-name "execute-js" --warmup "$EXEC_WARMUPS" --runs "$EXEC_RUNS" $HYPERFINE_ARGS "$ELIDE run --javascript ./tools/scripts/hello.js"
set +o xtrace
echo "- Warming up JS (sqlite)..."
set -o xtrace
hyperfine --command-name "execute-js-sqlite" --warmup "$EXEC_WARMUPS" --runs "$EXEC_RUNS" $HYPERFINE_ARGS "$ELIDE run --javascript ./tools/scripts/sqlite.js"
set +o xtrace

# typescript execution
echo "- Warming up TypeScript..."
set -o xtrace
hyperfine --command-name "execute-ts" --warmup "$EXEC_WARMUPS" --runs "$EXEC_RUNS" $HYPERFINE_ARGS "$ELIDE run --typescript ./tools/scripts/hello.ts"
set +o xtrace

# python execution
hyperfine --command-name "execute-py" --warmup "$EXEC_WARMUPS" --runs "$EXEC_RUNS" $HYPERFINE_ARGS "$ELIDE run --python ./tools/scripts/hello.py"

# ruby execution
hyperfine --command-name "execute-rb" --warmup "$EXEC_WARMUPS" --runs "$EXEC_RUNS" $HYPERFINE_ARGS "$ELIDE run --ruby ./tools/scripts/hello.rb"

echo "Done."
