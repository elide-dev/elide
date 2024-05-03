#!/bin/bash

echo "Building 'debug' release artifacts..."
bash ./tools/scripts/build-release-debug.sh

echo ""
echo "Building 'opt' release artifacts..."
bash ./tools/scripts/build-release-opt.sh
