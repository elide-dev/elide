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

HYPERFINE_ARGS="--shell none"

# run `--help`
hyperfine $HYPERFINE_ARGS $ELIDE --help

# run some hello scripts
hyperfine $HYPERFINE_ARGS -n hello-js "$ELIDE run --javascript ./tools/scripts/hello.js" -n hello-ts "$ELIDE run --typescript ./tools/scripts/hello.ts"

# run some sqlite stuff
hyperfine $HYPERFINE_ARGS -n sqlite "$ELIDE run --javascript ./tools/scripts/sqlite.js"

echo "CLI bench done."
