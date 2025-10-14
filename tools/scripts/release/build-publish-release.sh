#!/usr/bin/env bash

set -euo pipefail
set +x

# This script is meant to be run from the root of the Elide repository as a part of release pipeline.
# Several flags are supported:
# --dry: Skip unsafe commands, just print what would be done. Default mode.
# --release: Builds in local release mode.
# --publish: Publish the built artifacts to the remote repositories and package managers.
# --help: Show a help message.

source ./tools/scripts/release/commons.sh

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
  dry=false
fi

 if [[ "$@" == *"--release"* ]]; then
   release=true
   dry=false
fi

if[["$release"==true]]; then
    stamp=true
    publishTask=publishAllElidePublications
    provenance=true
    pgo=true
    nativeTask=nativeOptimizedCompile
fi

if [["$dry"==true]]; then
  publishTask=publishAllPublicationsToStageRepository
  provenance=false
fi
