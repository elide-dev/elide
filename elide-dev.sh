#!/bin/bash

#
# Copyright (c) 2024 Elide Technologies, Inc.
#
# Licensed under the MIT license (the "License"); you may not use this file except in compliance
# with the License. You may obtain a copy of the License at
#
#   https://opensource.org/license/mit/
#
# Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
# an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations under the License.
#

#
#  Elide Entrypoint
#

set -e -o pipefail -o nounset -o errexit

# Present working directory.
PWD=$(pwd)

# Resolve the OS name and lowercase it.
OS=$(uname -s | tr '[:upper:]' '[:lower:]')

# Resolve the arch and lowercase it.
ARCH=$(uname -m | tr '[:upper:]' '[:lower:]')

# Turn `arm64` into `aarch64`.
if [ "$ARCH" = "arm64" ]; then
    ARCH="aarch64"
fi

# Location of the stage 0 binary.
STAGE_ZERO="$PWD/packages/cli/build/install/elide-jvm-$OS-$ARCH/bin/elide"

# If the stage0 binary doesn't exist (at `$STAGE_ZERO`), then we build it; this happens via an external script which is
# sourced in; this script will fail as needed if the user is missing Java or other dependencies.
if [ ! -f "$STAGE_ZERO" ]; then
    ./gradlew :packages:cli:installDist
fi

# Run the stage 0 script with provided arguments.
exec bash "$STAGE_ZERO" "$@"
