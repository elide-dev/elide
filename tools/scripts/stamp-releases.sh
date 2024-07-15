#!/bin/bash

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

version=$(cat ./.release)
platform=$(uname -o | tr '[:upper:]' '[:lower:]')
arch=$(uname -m)
variant="opt"

archive_prefix="elide"

if [ "$arch" = "arm64" ]; then
  arch="aarch64"
fi
platform="$platform-$arch"

# echo "platform: $platform"
# echo "variant: $variant"
# echo "version: $version"
# echo "arch: $arch"

if [ "$platform" = "" ]; then exit 2; fi
if [ "$variant" = "" ]; then exit 3; fi
if [ "$version" = "" ]; then exit 4; fi
if [ "$arch" = "" ]; then exit 5; fi

echo "- Building release root (variant: $variant / platform: $platform)..."
cd packages/cli/build/native/nativeCompile \
  && mkdir "$archive_prefix-$version-$platform/" \
  && cp -fr elide resources "$archive_prefix-$version-$platform/"

echo "- Building tar package (variant: $variant / platform: $platform)..."
tar -cf "$archive_prefix-$version-$platform.tar" "$archive_prefix-$version-$platform/"

echo "- Building zip package (variant: $variant / platform: $platform)..."
zip -v -9 -r "$archive_prefix-$version-$platform.zip" "$archive_prefix-$version-$platform/"

echo "- Building tgz package (variant: $variant / platform: $platform)..."
gzip -v --best -k "elide-debug-$version-$platform.tar"
mv "$archive_prefix-$version-$platform.tar.gz" "$archive_prefix-$version-$platform.tgz"

echo "- Building txz package (variant: $variant / platform: $platform)..."
xz -v --best -k "$archive_prefix-$version-$platform.tar"
mv "$archive_prefix-$version-$platform.tar.xz" "$archive_prefix-$version-$platform.txz"
