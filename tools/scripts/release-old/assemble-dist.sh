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

target_root="packages/cli/build/native/nativeOptimizedCompile"

mkdir build/dist;

#
# Assemble: Publishable dist
#

cd "$target_root";

echo "Assembling distribution structure for platform \"$platform\" at \"$version\"...";
mkdir -p "release/$platform/$version";

# first, copy all the packed files into the release dist, in fully-qualified form
cp -fv elide-*.{tgz,txz,zip}* "release/$platform/$version/";
cp -fv elide.sbom.json "release/$platform/$version/elide-$platform-$version.sbom.json";
cp -fv elide-build-report.html "release/$platform/$version/elide-$platform-$version.build-report.html";

cd -;
mkdir -p build/dist/github build/dist/pub/release build/dist/pkg;

# copy github release structure before pub
echo "- Building GitHub release structure...";
cp -frv "$target_root/release/$platform/$version/*" "build/dist/github/"
echo "- GitHub release is ready."

# mount the pub release extras
echo "- Building pub release structure...";
cd "$target_root/release/$platform/$version";
cp -fv elide-*.txz elide.txz;
cp -fv elide-*.tgz elide.tgz;
cp -fv elide-*.zip elide.zip;
cp -fv "elide-$platform-$version.sbom.json" elide.sbom.json;
cp -fv "elide-$platform-$version.build-report.html" elide.build-report.html;
cd -;
cp -frv "$target_root/release/*" "build/dist/pub/release/";
echo "- Pub release is ready."

echo "Dist assembly complete."
