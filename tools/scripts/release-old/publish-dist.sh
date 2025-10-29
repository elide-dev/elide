#!/usr/bin/env bash

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

set -euo pipefail
source tools/scripts/release/commons.sh

which rclone || {
  echo "rclone is not installed. Please install rclone to publish the distribution."
  exit 1
}
which gh || {
  echo "GitHub CLI (gh) is not installed. Please install gh to publish the distribution."
  exit 1
}

echo "- Publishing build/dist/pub..."
set -x
cd build/dist/pub && rclone copy --progress . "elide:elide-tools/cli/v1/snapshot/$platform/$version/"
set +x
echo "- Published build/dist/pub."

echo "- Publishing build/dist/github..."
set -x
cd build/dist/github && gh release upload "$version" .*
set +x

echo "Dists published (github, pub)."
