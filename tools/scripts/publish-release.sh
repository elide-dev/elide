#!/usr/bin/env bash

set -euo pipefail
set +x

# This script is meant to be run from the root of the Elide repository. Several flags are supported:
# --release: Builds in release mode.
# --publish: Publish the built artifacts to the remote repository.
# --dry: Skip unsafe commands, just print what would be done.
# --version: Specify a version to release; if not provided, one is read from `.release`.
# --stamp: Whether to stamp an actual (or dev) version.
# --libc: LibC library to use (default: auto, omitted).
# --pgo: Activates Profile-Guided Optimization (PGO) build mode.
# --provenance: Enable provenance during this release.
# --help: Show a help message.

source ./tools/scripts/release/commons.sh

stamp=false
dry=false
publish=false
release=false
provenance=false
pgo=false
version=""
help=false
clean=true
thirdParty=true
nativeTask=nativeCompile
platform="$(uname -s | tr '[:upper:]' '[:lower:]')"
arch="$(uname -m | sed 's/x86_64/amd64/')"
publishTask=publishAllPublicationsToStageRepository
libc="auto"

if [[ "$@" == *"--stamp"* ]]; then
  stamp=true
fi
if [[ "$@" == *"--publish"* ]]; then
  publish=true
  if [[ "$@" == *"--release"* ]]; then
    publishTask=publishAllElidePublications
    if [[ "$@" == *"--provenance"* ]]; then
      provenance=true
    fi
  fi
fi
if [[ "$@" == *"--dry"* ]]; then
  dry=true
  publishTask=publishAllPublicationsToStageRepository
  provenance=false
fi
if [[ "$@" == *"--release"* ]]; then
  release=true
  pgo=true
  stamp=true
  nativeTask=nativeOptimizedCompile
fi
if [[ "$@" == *"--no-clean"* ]]; then
  clean=false
fi
if [[ "$@" == *"--no-third-party"* ]]; then
  thirdParty=false
fi
if [[ "$@" == *"--version"* ]]; then
  version=$(echo "$@" | grep -oP '(?<=--version\s)[^\s]+')
fi
if [[ "$@" == *"--libc"* ]]; then
  libc=$(echo "$@" | grep -oP '(?<=--libc\s)[^\s]+')
fi
if [[ "$@" == *"--pgo"* ]]; then
  pgo=true
fi
if [[ -z "$version" ]]; then
  version=$(cat .release)
fi
if [[ "$@" == *"--help"* ]]; then
  help=true
fi

NPM_ARGS=""

buildMode=dev
# if release is active, set buildMode to release
if [[ "$release" == true ]]; then
  buildMode=release
fi

# if a libc value is specified, build a property for it
if [[ "$libc" != "auto" ]]; then
  GRADLE_PROPS="-Pelide.libc=$libc"
else
  GRADLE_PROPS=""
fi

# in provenance mode, we need to add `--provenance` to the NPM args
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

# if stamp is active, pass version props
if [[ "$stamp" == true ]]; then
  GRADLE_PROPS="$GRADLE_PROPS -Pversion=$version -Pelide.version=$version"
fi

ELIDE_TOOLS_REPO="elide:elide-tools"
ELIDE_MAVEN_REPO="elide:elide-maven"
GRADLE_PROPS="-Pelide.release=$release -Pelide.buildMode=$buildMode -Pelide.stamp=$stamp -Pelide.pgo=$pgo $GRADLE_PROPS"
GRADLE_ARGS="--no-configuration-cache"
PUBLISH_TARGETS=":packages:core:$publishTask :packages:base:$publishTask :packages:test:$publishTask :packages:tooling:$publishTask :packages:builder:$publishTask :packages:graalvm:$publishTask :packages:server:$publishTask :packages:http:$publishTask :packages:graalvm-java:$publishTask :packages:graalvm-js:$publishTask :packages:graalvm-jvm:$publishTask :packages:graalvm-kt:$publishTask :packages:graalvm-llvm:$publishTask :packages:graalvm-py:$publishTask :packages:graalvm-rb:$publishTask :packages:graalvm-ts:$publishTask :packages:graalvm-wasm:$publishTask :packages:engine:$publishTask"

# render help message and exit if requested.
if [[ "$help" == true ]]; then
  echo "Usage: $0 [--release] [--publish] [--dry] [--version <version>] [--stamp] [--libc <libc>] [--pgo] [--help]"
  echo ""
  echo "Performs a full release of Elide, including native binaries, Maven artifacts, and others."
  echo ""
  echo "Options:"
  echo "  --release         Build in release mode."
  echo "  --publish         Publish the built artifacts to the remote repository."
  echo "  --dry             Dry run, skips unsafe commands (still builds, etc)."
  echo "  --version         Specify a version to release (default: read from .release)."
  echo "  --stamp           Stamp the build with a version (or dev) stamp."
  echo "  --libc            Specify the LibC library to use (default: auto)."
  echo "  --pgo             Activate Profile-Guided Optimization (PGO) build mode."
  echo "  --provenance      Enable provenance during this release."
  echo "  --no-clean        Do not clean before building."
  echo "  --no-third-party  Do not build third-party libraries."
  echo "  --help            Show this help message."
  exit 0
fi

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
if [[ "$dry" == true ]]; then
  echo "Building native image ($nativeTask)..."
  set -x;
  ./gradlew \
    $GRADLE_ARGS \
    $GRADLE_PROPS \
    :packages:cli:$nativeTask;
  set +x;
fi

## Build release packages
echo "Building release packages...";
export ELIDE_VERSION="$version";
export ELIDE_PLATFORM="$platform";
export ELIDE_ARCH="$arch";

if [[ "$dry" == true ]]; then
  set -x;
  ELIDE_VERSION="$version" ELIDE_PLATFORM="$platform" ELIDE_ARCH="$arch" ./tools/scripts/build-release-opt.sh --dry;
  set +x;
else
  set -x;
  ELIDE_VERSION="$version" ELIDE_PLATFORM="$platform" ELIDE_ARCH="$arch" ./tools/scripts/build-release-opt.sh;
  cd ./packages/cli/build/native/$nativeTask;
  mkdir -p releases/${platform}-${arch}/$version;
  cp -fv elide.sbom.json releases/${platform}-${arch}/$version/elide.sbom.json;
  cp -fv elide.sbom.json releases/${platform}-${arch}/$version/elide-${version}-${platform}-${arch}.sbom.json;
  cp -fv elide-build-report.html releases/${platform}-${arch}/$version/elide.build-report.html;
  cp -fv elide-build-report.html releases/${platform}-${arch}/$version/elide-${version}-${platform}-${arch}.build-report.html;
  cd -;
  cd ./packages/cli/build/native/$nativeTask/releases/${platform}-${arch}/$version;
  if [[ "$publish" == true ]]; then
    echo "Mounting latest version aliases...";
    cp -fv elide-*.txz elide.txz;
    cp -fv elide-*.zip elide.zip;
    cp -fv elide-*.tgz elide.tgz;
    rclone copy --progress . $ELIDE_TOOLS_REPO/cli/v1/snapshot/${platform}-${arch}/$version/
    rm -fv elide.txz elide.zip elide.tgz elide.sbom.json elide.build-report.html;
  fi
  set +x;
fi

## If publishing and release modes are active, run a full Maven publish run
if [[ "$publish" == true ]]; then
  echo "Publishing artifacts to Maven repository (${publishTask})..."
  set -x;
  ./gradlew \
    $GRADLE_ARGS \
    $GRADLE_PROPS \
    $PUBLISH_TARGETS;
  set +x;
fi

## Build third-party libs
if [[ "$thirdParty" == true ]]; then
  echo "Building third-party libraries..."
  set -x;
  mkdir -p build/m2 && ./third_party/oracle/publish.sh;
  set +x;
else
  echo "Skipping third-party libraries build."
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
