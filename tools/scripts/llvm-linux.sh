#!/bin/bash

LLVM_VERSION="18"

echo "" && echo "Installing initial dependencies...";
apt-get update && apt install -y \
	ca-certificates curl wget build-essential lsb-release software-properties-common gnupg;

echo "" && echo "Installing LLVM $LLVM_VERSION...";
wget https://apt.llvm.org/llvm.sh \
	&& chmod +x llvm.sh \
	&& ./llvm.sh $LLVM_VERSION all;

echo "" && echo "Finalizing software...";
apt-get update && apt-get upgrade -y;

/usr/lib/llvm-18/bin/clang --version;
echo "LLVM $LLVM_VERSION is ready.";

