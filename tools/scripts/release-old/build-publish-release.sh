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
clean=false
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
PACKAGE_ARGS=""
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

if [[ "$@" == *"--version"* ]]; then
  version=$(echo "$@" | grep -oP '(?<=--version\s)[^\s]+')
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
  PACKAGE_ARGS="$PACKAGE_ARGS --provenance"
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

ELIDE_TOOLS_REPO="elide:elide-tools"
ELIDE_MAVEN_REPO="elide:elide-maven"
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

source ./tools/scripts/release/commons.sh

## Build release packages
export ELIDE_VERSION="$version";
export ELIDE_PLATFORM="$platform";
export ELIDE_ARCH="$arch";

source ./tools/scripts/release/publish-release.sh --version=$version $PACKAGE_ARGS

if [[ "$dry" != true ]]; then
  set -x;
  cd ./packages/cli/build/native/nativeOptimizedCompile/;
  mkdir -p "release/${platform}-${arch}/$version;"
  cp -fv elide-*.{tgz,txz,zip}* "release/${platform}-${arch}/$version/"
  cp -fv elide.sbom.json "release/${platform}-${arch}/$version/"
  cp -fv elide-build-report.html "release/${platform}-${arch}/$version/elide.build-report.html"
  cd -;
  mkdir -p staging/release
  mv "packages/cli/build/native/nativeOptimizedCompile/release" "staging/"
  tree -L 3 staging/
  echo "Release built."
  set +x;
fi

## Build build/m2 root into a zip
if [[ "$publish" == true ]]; then
  echo "Building m2 zip..."
  cd build/m2;
  set -x;
  rm -fv ../elide-m2.zip && zip -r ../elide-m2.zip . || {
    echo "Failed to create m2 zip. Exiting."
    exit 1
  };
  set +x;
  cd -;
fi

## I THINK??
## if --publish is active, run rclone
if [[ "$publish" == true ]]; then
  echo "Publishing Maven artifacts to Elide repository..."
  if [[ "$dry" == true ]]; then
    echo "Dry run: would publish to remote repository."
    echo cd build/m2 && echo rclone copy --progress . $ELIDE_MAVEN_REPO/
    echo "Dry run: would publish npm types."
    cd packages/types && npm publish --access public --dry $NPM_ARGS;
  else
    set -x;
    cd build/m2 && rclone copy --progress . $ELIDE_MAVEN_REPO/
    set +x;
    echo "Elide Maven updated."
    set -x;
    cd packages/types && npm publish --access public $NPM_ARGS;
    set +x;
    echo "Elide NPM libs updated."
  fi
else
  echo "Skipping publishing step."
fi

echo "Release build completed successfully."

echo "┌─────────────────────────────────────────────────┐"
echo "╵ Elide release receipt                           ╵"
echo "└─────────────────────────────────────────────────┘"
echo "- Version: $version"
echo "- Platform: $platform"
echo "- Architecture: $arch"
echo "- Tag: ${platform}-${arch}"
echo "- Mode: $buildMode"
echo "- Release: $release"
echo "- Stamp: $stamp"
echo "- Dry run: $dry"
echo "- Publish: $publish"
echo "- Provenance: $provenance"
echo "- LibC: $libc"
echo "- PGO: $pgo"
echo "- Clean: $clean"
echo "- Third-party: $thirdParty"

echo ""
echo "Done."
exit 0
