#env bash

source ./tools/scripts/bench-common.sh

# Resolved Elide binary
ELIDE="$1"

if [ ! -f "$ELIDE" ]; then
    echo "Elide binary not found"
    exit 2;
fi

if [[ -x "$file" ]]
then
    # nothing
else
    echo "Elide binary is not executable"
    exit 2;
fi

VERSION=$("$ELIDE --version")

echo "Starting server bench for Elide at version $VERSION";

# run the server
"$ELIDE serve --javascript $SERVER_JS" &
ELIDE_JS_SERVER_PID=$!

OHA_ARGS="-c 1024 -z 60s --http-version 1.1"
WRK_ARGS="-c 1024 -d 60s -t 12 -L --timeout 10s -R 500000"
HYPERFINE_ARGS="--warmup 5000 --runs 1000 --shell none"

hyperfine $HYPERFINE_ARGS "curl http://localhost:3000/plaintext"
oha $OHA_ARGS http://localhost:3000/plaintext
wrk $WRK_ARGS http://localhost:3000/plaintext

echo "Halting JS server at PID $ELIDE_JS_SERVER_PID";
kill -SIGINT "$ELIDE_JS_SERVER_PID";
echo "Server bench done.";
