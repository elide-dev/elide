#!/bin/bash
set -euo pipefail

set +x
echo "Compressing local build state...";
set -x

# shellcheck disable=SC2038
# shellcheck disable=SC2046
tar -czvf ./.buildstate.tar.gz $(find . -type d -name "build" | xargs);

set +x
echo "Build state saved at '.buildstate.tar.gz'";
set -x
