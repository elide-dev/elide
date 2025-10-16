#!/usr/bin/env bash

set -euo pipefail
set +x

# This script is meant to be run from the root of the Elide repository as a part of release pipeline.
# Several flags are supported:
# --dry: Skip unsafe commands, just print what would be done. Default mode.
# --release: Builds in local release mode.
# --publish: Publish the built artifacts to the remote repositories and package managers.
# --help: Show a help message.

#
dry=true
publish=false
release=false

stamp=false
pgo=false
provenance=false
help=false
clean=true
thirdParty=true
nativeTask=nativeCompile
platform="$(uname -s | tr '[:upper:]' '[:lower:]')"
arch="$(uname -m | sed 's/x86_64/amd64/')"
libc="auto"

publishTask=publishAllPublicationsToStageRepository
version=""
NPM_ARGS=""
GRADLE_PROPS=""
buildMode=dev

if [[ "$@" == *"--help"* ]]; then
  help=true
  dev=false
fi

# render help message and exit if requested.
if [[ "$help" == true ]]; then
  echo "Usage: $0 [--dry] [--publish] [--release] [--version <version>] [--help]"
  echo ""
  echo "Performs a full release of Elide, including native binaries, Maven artifacts, and others."
  echo ""
  echo "Options:"
  echo "  --dry             Dry run, skips unsafe commands (still builds, etc). Default mode."
  echo "  --release         Build in local release mode."
  echo "  --publish         Publish the built artifacts to the remote repository."
  echo "  --version         Specify a version to release (default: read from .release)."
  echo "  --help            Show this help message."
  exit 0
fi

if [[ "$@" == *"--publish"* ]]; then
  publish=true
  release=true
fi

 if [[ "$@" == *"--release"* ]]; then
   release=true
fi

if [[ "$release" == true ]]; then
    stamp=true
    publishTask=publishAllElidePublications
    provenance=true
    pgo=true
    nativeTask=nativeOptimizedCompile
    buildMode=release
fi

if [[ "$dry" == true ]]; then
  publishTask=publishAllPublicationsToStageRepository
  provenance=false
fi

# Build Options, NPM and Gradle
if [[ "$provenance" == true ]]; then
  NPM_ARGS="$NPM_ARGS --provenance"
fi

# in release mode, tell gradle to build the docs and enable signing
if [[ "$release" == true ]]; then
  GRADLE_PROPS="$GRADLE_PROPS -Pelide.buildDocs=true -PenableSigning=true"
  if [[ "$dry" != true ]]; then
    GRADLE_PROPS="$GRADLE_PROPS -PenableSigstore=true"
  fi
else
  GRADLE_PROPS="$GRADLE_PROPS -Pelide.buildDocs=false -PenableSigning=false -PenableSigstore=false"
fi

ELIDE_TOOLS_REPO="elide:elide-tools"
ELIDE_MAVEN_REPO="elide:elide-maven"
GRADLE_PROPS="-Pelide.release=$release -Pelide.buildMode=$buildMode -Pelide.stamp=$stamp -Pelide.pgo=$pgo $GRADLE_PROPS"
GRADLE_ARGS="--no-configuration-cache"
PUBLISH_TARGETS=":packages:core:$publishTask :packages:base:$publishTask :packages:test:$publishTask :packages:tooling:$publishTask :packages:builder:$publishTask :packages:graalvm:$publishTask :packages:server:$publishTask :packages:http:$publishTask :packages:graalvm-java:$publishTask :packages:graalvm-js:$publishTask :packages:graalvm-jvm:$publishTask :packages:graalvm-kt:$publishTask :packages:graalvm-llvm:$publishTask :packages:graalvm-py:$publishTask :packages:graalvm-rb:$publishTask :packages:graalvm-ts:$publishTask :packages:graalvm-wasm:$publishTask :packages:engine:$publishTask"

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

source ./tools/scripts/release/commons.sh

## Build release packages
export ELIDE_VERSION="$version";
export ELIDE_PLATFORM="$platform";
export ELIDE_ARCH="$arch";



echo ""
echo "Done."
exit 0
