
#
# Copyright (c) 2023 Elide Ventures, LLC.
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

binpath=${1:-$(pwd)/packages/cli/build/native/nativeOptimizedCompile/elide};

echo "Establishing temp folder...";
workspace=$(mktemp -d);

cd "$workspace";
echo "Copying $binpath...";
cp -f "$binpath" ./elide.orig;
cp ./elide.orig ./elide;

## Compress with no upx
echo "Compressing original binary...";
xz -9kv elide;
mv elide.xz elide.orig.xz;

## Begin compression with upx (level 2)
upx -v -1 elide && xz -9kv elide && mv elide elide.2 && mv elide.xz elide.2.xz;

## Show sizes
du -h elide*;

echo "Done.";
exit 0;
