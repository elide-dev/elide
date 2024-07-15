#!/bin/env bash

#
# Copyright (c) 2024 Elide Technologies, Inc.
#
# Licensed under the MIT license (the "License"); you may not use this file except in compliance
# with the License. You may obtain a copy of the License at
#
# https://opensource.org/license/mit/
#
# Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
# an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations under the License.
#

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
