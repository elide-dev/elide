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

# This script is not meant to be run directly, but rather sourced by other release scripts.
# It must run after native build and sets the version of the binary
nativeBinTarget="${ELIDE_TARGET:-nativeOptimizedCompile}"
nativePrefix="packages/cli/build/native/$nativeBinTarget"
elideBin="$nativePrefix/elide"
# ensure the binary exists and is executable
if [ ! -x "$elideBin" ]; then
  echo "Error: The binary '$elideBin' does not exist or is not executable." >&2
  exit 1
fi

# load from `ELIDE_VERSION` or default to `./.release`
releaseVersion=$($elideBin --version)
version="${ELIDE_VERSION:-$releaseVersion}"
