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

# This script is meant to be run from the root of the Elide and acts as an entry point for the release pipeline.
# It unifies the entier pipeline into a single script.
# Several flags are supported:
# --dry: Skip unsafe commands, just print what would be done. Default mode.
# --release: Builds in local release mode.
# --publish: Publish the built artifacts to the remote repositories and package managers.
# --help: Show a help message.
source tools/scripts/release/platform-set.sh

dry=true
publish=false
release=false
help=false
version=""
RELEASE_PARAMS=""

if [[ "$@" == *"--help"* ]]; then
  help=true
fi

# render help message and exit if requested.
if [[ "$help" == true ]]; then
  echo "Usage: $0 [--dry] [--publish] [--release] [--version <version>] [--help]"
  echo ""
  echo "Performs a full release build of Elide, including native binaries, packaging,."
  echo ""
  echo "Options:"
  echo "  --dry             Dry run, skips unsafe commands (still builds, etc). Default mode."
  echo "  --release         Build in local release mode."
  echo "  --publish         Publish the built artifacts to the remote repository."
  echo "  --version         Specify a version to release (default: read from .release)."
  echo "  --help            Show this help message."
  exit 0
fi

#TODO: add logic to make sure --publish and --release are exclusive.
if [["$@" == *"--dry"*]]; then
  RELEASE_PARAMS="$RELEASE_PARAMS --dry"
fi

if [[ "$@" == *"--publish"* ]]; then
  RELEASE_PARAMS="$RELEASE_PARAMS --publish"
fi

if [[ "$@" == *"--release"* ]]; then
  RELEASE_PARAMS="$RELEASE_PARAMS --release"
fi

echo "┌─────────────────────────────────────────────────┐"
echo "╵ Elide Release                                   ╵"
echo "└─────────────────────────────────────────────────┘"
echo " Creating a release for $platform-$arch."
echo "┌─────────────────────────────────────────────────┐"
echo "╵ Cooking a native build                          ╵"
echo "└─────────────────────────────────────────────────┘"
bash ./native-build.sh $RELEASE_PARAMS
echo "┌─────────────────────────────────────────────────┐"
echo "╵ Packaging and signing the release.              ╵"
echo "└─────────────────────────────────────────────────┘"
bash ./package-release.sh $RELEASE_PARAMS
echo "┌─────────────────────────────────────────────────┐"
echo "╵ Staging the release.                            ╵"
echo "└─────────────────────────────────────────────────┘"
bash ./package-release.sh $RELEASE_PARAMS

source tools/scripts/release/version-set.sh
echo "┌─────────────────────────────────────────────────┐"
echo "╵ Elide release receipt                           ╵"
echo "└─────────────────────────────────────────────────┘"
echo "- Version: $version"
echo "- Platform: $platform"
echo "- Architecture: $arch"
echo "- Tag: ${platform}-${arch}"
