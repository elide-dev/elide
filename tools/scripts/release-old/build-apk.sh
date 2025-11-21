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

echo "Elide Apk Builder"
echo "- Version: $version"
echo "- Platform: $platform"
echo "- Architecture: $arch"

echo "----------------------------------------------------"

echo "Output of 'fpm --version':"
fpm --version

apkname="$archive_prefix-$version-$revision-$platform.apk"

echo "Building \"$apkname\"..."

# if `DRY` is equal to `true`, skip the build
if [[ "$@" == *"--dry"* ]]; then
  echo "Dry run mode enabled, skipping build."
  exit 0
fi

set -x

# mount version file, which needs to make it into the installation
echo "$version" > "$nativePrefix/.version"

# (We intentionally exclude the package name argument so that fpm uses a compliant apk name.)
fpm \
  -C "packages/cli/build/native/$nativeBinTarget" \
  -t apk \
  -s dir \
  --log info \
  --prefix "$install_prefix" \
  --version "$version" \
  --iteration "$revision" \
  --architecture "$arch" \
  --name "elide" \
  --provides "elide" \
  --license "MIT" \
  --vendor "Elide Technologies" \
  --url "https://elide.dev" \
  --depends libgomp \
  --depends ca-certificates \
  --before-install="packages/cli/packaging/scripts/before-install.sh" \
  --after-install="packages/cli/packaging/scripts/after-install.sh" \
  --description "Elide is an all-in-one, full-stack, AI-native, polyglot runtime" \
  --maintainer "Elide Team <engineering@elide.dev>" \
  "../../../packaging/content/README.txt=$version/README.txt" \
  "../../../packaging/content/doc=$version/" \
  ".version=.version" \
  "resources.tgz=$version/resources.tgz" \
  "elide=$version/elide"

mv *.apk build/

set +x

echo "Apk ready."
