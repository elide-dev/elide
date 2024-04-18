# Copyright (c) 2024 Elide Technologies, Inc.
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
# Makefile: Elide Third-party Tools
#

VERBOSE ?= no
RELOCK ?= no

TYPESCRIPT_VERSION = 5.5.0
CARGO ?= $(shell which cargo)
GO ?= $(shell which go)
NPM ?= $(shell which npm)
NPX ?= $(shell which npx)
BUN ?= $(shell which bun)
GRADLE ?= ./gradlew

GRADLE_ARGS ?= -x test -x check
LIBROOT ?= lib/

JS_TOOLS ?= oxc esbuild
PY_TOOLS ?= ruff uv
LANGS ?= pkl

ALL_LIBS ?= $(JS_TOOLS) $(PY_TOOLS) $(LANGS)

ifeq ($(RELOCK),yes)
GRADLE_ARGS += --write-verification-metadata sha256,sha512 --export-keys --write-locks
endif

ifeq ($(VERBOSE),yes)
RULE ?=
else
RULE ?= @
endif

all: pkl ruff uv esbuild oxc

pkl: apple/pkl/build

apple/pkl/build:
	@echo ""
	@echo "Building apple/pkl..."
	$(RULE)cd apple/pkl && $(GRADLE) build $(GRADLE_ARGS)

ruff: astral/ruff/target

astral/ruff/target:
	@echo ""
	@echo "Building astral/ruff..."
	$(RULE)cd astral/ruff && $(CARGO) build

uv: astral/uv/target

astral/uv/target:
	@echo ""
	@echo "Building astral/uv..."
	$(RULE)cd astral/uv && $(CARGO) build

esbuild: evanw/esbuild/esbuild

evanw/esbuild/esbuild:
	@echo ""
	@echo "Building evanw/esbuild..."
	$(RULE)cd evanw/esbuild && PWD="" $(MAKE) \
		platform-wasm \
		platform-neutral \
		platform-darwin-x64 \
		platform-darwin-arm64 \
		platform-linux-x64 \
		platform-linux-arm64 \
		platform-win32-x64

oxc: oxc-project/oxc/target

oxc-project/oxc/target:
	@echo ""
	@echo "Building oxc-project/oxc..."
	$(RULE)cd oxc-project/oxc && $(CARGO) build

libroot: $(LIBROOT)

$(LIBROOT):
	@echo "Making libroot..."
	$(RULE)mkdir -p $(LIBROOT)
	@echo "Copying 3rd-party libraries..."
	@echo "Done."

clean:
	$(info Cleaning third-party outputs...)
	$(RULE)-rm -rf \
		apple/pkl/build \
		astral/ruff/target \
		astral/uv/target \
		oxc-project/oxc/target
	$(RULE)-cd evanw/esbuild && $(MAKE) clean
	$(RULE)-cd apple/pkl && $(GRADLE) clean
	$(RULE)-cd astral/ruff && $(CARGO) clean
	$(RULE)-cd astral/uv && $(CARGO) clean
	$(RULE)-cd oxc-project/oxc && $(CARGO) clean

.PHONY: all pkl ruff uv oxc esbuild libroot
