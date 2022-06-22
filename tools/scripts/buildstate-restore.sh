#!/bin/bash
set -euo pipefail

set +x
echo "Restoring local build state...";

if test -f "./.buildstate.tar.gz"; then
  echo "Restoring local build state...";
  set -x
  ## restore from tarball
  tar -xzvf ./.buildstate.tar.gz;
  set +x
else
  echo "No buildstate found to restore.";
  exit 1;
fi

set +x
echo "Build state restored from '.buildstate.tar.gz'";
set -x
