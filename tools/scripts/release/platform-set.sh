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
# It sets the pipeline's platform.
hostPlatform=$(uname -s | tr '[:upper:]' '[:lower:]')
platform="${ELIDE_PLATFORM-$hostPlatform}"
hostArch=$(uname -m)
arch="${ELIDE_ARCH-$hostArch}"
variant="opt"
archive_prefix="elide"
install_prefix="/opt/elide"
revision="0"

if [ "$arch" = "arm64" ]; then
  arch="aarch64"
fi
if [ "$arch" = "x86_64" ]; then
  arch="amd64"
fi
