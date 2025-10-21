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

# This script is meant to be run from the root of the Elide repository as a part of release pipeline.
# It builds a native image.
# Several flags are supported:
# --dry: Skip unsafe commands, just print what would be done. Default mode.
# --release: Builds in local release mode.
# --publish: Publish the built artifacts to the remote repositories and package managers.
# --help: Show a help message.
dry=true
publish=false
release=false

stamp=false
clean=false
pgo=false
help=false
nativeTask=nativeCompile
publishTask=publishAllPublicationsToStageRepository

if [["$@" == *"--dry"*]]; then
  dry=true
fi

if [[ "$@" == *"--publish"* ]]; then
  publish=true
  release=true
  dry=false
fi

 if [[ "$@" == *"--release"* ]]; then
   release=true
   clean=true
fi

if [[ "$release" == true ]]; then
    stamp=true
    publishTask=publishAllElidePublications
    pgo=true
    nativeTask=nativeOptimizedCompile
    buildMode=release
fi

if [[ "$dry" == true ]]; then
  publishTask=publishAllPublicationsToStageRepository
fi

# in release mode, tell gradle to build the docs and enable signing
if [[ "$release" == true ]]; then
  GRADLE_PROPS="$GRADLE_PROPS -Pelide.buildDocs=true -PenableSigning=true"
  PACKAGE_ARGS="$PACKAGE_ARGS --release --stamp"
  if [[ "$dry" != true ]]; then
    GRADLE_PROPS="$GRADLE_PROPS -PenableSigstore=true"
  fi
else
  GRADLE_PROPS="$GRADLE_PROPS -Pelide.buildDocs=false -PenableSigning=false -PenableSigstore=false"
fi

GRADLE_PROPS="-Pelide.release=$release -Pelide.buildMode=$buildMode -Pelide.stamp=$stamp -Pelide.pgo=$pgo $GRADLE_PROPS"
GRADLE_ARGS="--no-configuration-cache"
PUBLISH_TARGETS=":packages:core:$publishTask :packages:base:$publishTask :packages:test:$publishTask :packages:tooling:$publishTask :packages:builder:$publishTask :packages:graalvm:$publishTask :packages:server:$publishTask :packages:http:$publishTask :packages:graalvm-java:$publishTask :packages:graalvm-js:$publishTask :packages:graalvm-jvm:$publishTask :packages:graalvm-kt:$publishTask :packages:graalvm-llvm:$publishTask :packages:graalvm-py:$publishTask :packages:graalvm-rb:$publishTask :packages:graalvm-ts:$publishTask :packages:graalvm-wasm:$publishTask :packages:engine:$publishTask"

## Perform a clean if needed
if [[ "$clean" == true ]]; then
  echo "Cleaning previous build artifacts..."
  set -x;
  ./gradlew \
    $GRADLE_ARGS \
    $GRADLE_PROPS \
    clean;
  set +x;
fi

## Assemble libraries
set -x;
./gradlew \
  $GRADLE_ARGS \
  $GRADLE_PROPS \
  assemble;
set +x;

## Build native image
set -x;
  ./gradlew \
    $GRADLE_ARGS \
    $GRADLE_PROPS \
    :packages:cli:$nativeTask;
set +x;

exit 0
