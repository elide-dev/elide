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

echo "----------------------------------------------------"

echo "Elide Deb Builder"
echo "- Version: $version"
echo "- Platform: $platform"
echo "- Architecture: $arch"

echo "----------------------------------------------------"

echo "Output of 'fpm --version':"
fpm --version

debname="$archive_prefix-$version-$revision-$platform.deb"

echo "Building \"$debname\"..."

# if `DRY` is equal to `true`, skip the build
if [[ "$@" == *"--dry"* ]]; then
  echo "Dry run mode enabled, skipping build."
  exit 0
fi

set -x

# mount version file, which needs to make it into the installation
echo "$version" > "$nativePrefix/.version"

fpm \
  -C "packages/cli/build/native/$nativeBinTarget" \
  -t deb \
  -s dir \
  -p "$debname" \
  --log info \
  --prefix "$install_prefix" \
  --version "$version" \
  --iteration "$revision" \
  --architecture "$arch" \
  --name "elide" \
  --license "MIT" \
  --vendor "Elide Technologies" \
  --url "https://elide.dev" \
  --description "Elide is an all-in-one, full-stack, AI-native, polyglot runtime" \
  --maintainer "Elide Team <engineering@elide.dev>" \
  --provides "elide" \
  --depends libc6 \
  --depends libstdc++6 \
  --depends ca-certificates \
  --deb-recommends libgomp1 \
  --deb-compression xz \
  --deb-compression-level 9 \
  --deb-dist unstable \
  --before-install="packages/cli/packaging/scripts/before-install.sh" \
  --after-install="packages/cli/packaging/scripts/after-install.sh" \
  "elide=$version/elide" \
  "../../../packaging/content/README.txt=$version/README.txt" \
  "../../../packaging/content/doc=$version/" \
  "resources=$version/" \
  ".version=.version"

mv "$debname" build/

set +x

echo "Deb ready."
