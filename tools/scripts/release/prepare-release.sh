#!/usr/bin/env bash
#
# Copyright (c) 2025 Elide Technologies, Inc.
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
set +x



currentVersion=$(cat ./.release)
releaseOverride="${{ inputs.version }}"
version="${releaseOverride:-$currentVersion}"
cd ./packages/cli/build/native/nativeOptimizedCompile/;
mkdir -p "release/${{ inputs.os }}-${{ inputs.arch }}/$version"
cp -fv elide-*.{tgz,txz,zip}* "release/${{ inputs.os }}-${{ inputs.arch }}/$version/"
cp -fv elide.sbom.json "release/${{ inputs.os }}-${{ inputs.arch }}/$version/"
cp -fv elide-build-report.html "release/${{ inputs.os }}-${{ inputs.arch }}/$version/elide.build-report.html"
cd -;
mkdir -p staging/release
mv "packages/cli/build/native/nativeOptimizedCompile/release" "staging/"
echo "version=$version"
tree -L 3 staging/
echo "Release built."
set -x
exit 0
