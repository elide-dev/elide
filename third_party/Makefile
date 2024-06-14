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
PWD ?= $(shell pwd)

GRADLE ?= ./gradlew
CMAKE ?= cmake

CFLAGS_BASE ?=

GRADLE_ARGS ?= -x test -x check
LIBROOT ?= lib/

NATIVE_TOOLS ?= sqlite boringssl apr
JS_TOOLS ?= oxc
PY_TOOLS ?= ruff uv
LANGS ?= pkl

REQUIRED ?= sqlite boringssl apr

ALL_LIBS ?= $(REQUIRED)

ifeq ($(RELOCK),yes)
GRADLE_ARGS += --write-verification-metadata sha256,sha512 --export-keys --write-locks
endif

ifeq ($(VERBOSE),yes)
RULE ?=
else
RULE ?= @
endif

all: $(ALL_LIBS)

pkl: apple/pkl/build

apple/pkl/build:
	@echo ""
	@echo "Building apple/pkl..."
	$(RULE)cd apple/pkl && $(GRADLE) build $(GRADLE_ARGS)

apr: apache/apr/.libs

CFLAGS_APR ?="-fPIC -O3"
CFLAGS_APR +=$(CFLAGS_BASE)

apache/apr/.libs: google/boringssl/build
	@echo ""
	@echo "Building apache/apr..."
	$(RULE)cd apache/apr && ./buildconf \
		&& CFLAGS=$(CFLAGS_APR) ./configure \
			--prefix=`pwd`/target \
			--enable-shared \
			--enable-static \
			--enable-threads \
			--enable-posix-shm \
			--enable-sysv-shm \
			--with-crypto \
			--with-openssl=$(PWD)/google/boringssl \
		&& CFLAGS=$(CFLAGS_APR) make -j`nproc` \
		&& CFLAGS=$(CFLAGS_APR) make install

clean-apr:
	$(RULE)cd apache/apr && git clean -xdf

boringssl: google/boringssl/build

CFLAGS_BORINGSSL ?="-fPIC -O3"
CFLAGS_BORINGSSL +=$(CFLAGS_BASE)

google/boringssl/build:
	@echo ""
	@echo "Building google/boringssl..."
	$(RULE)cd google/boringssl && mkdir build && cd build \
		&& CFLAGS=$(CFLAGS_BORINGSSL) CXXFLAGS=$(CFLAGS_BORINGSSL) $(CMAKE) .. \
		&& CFLAGS=$(CFLAGS_BORINGSSL) CXXFLAGS=$(CFLAGS_BORINGSSL) make -j`nproc`

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

oxc: oxc-project/oxc/target

oxc-project/oxc/target:
	@echo ""
	@echo "Building oxc-project/oxc..."
	$(RULE)cd oxc-project/oxc && $(CARGO) build

SQLITE3_CONFIGURE ?=--enable-all --enable-memsys5 --enable-update-limit --enable-tempstore=yes --enable-debug

sqlite: sqlite/sqlite3.c

sqlite-clean:
	$(RULE)cd sqlite && $(MAKE) clean && rm -fv Makefile sqlite3.c

sqlite/Makefile:
	@echo ""
	@echo "Configuring sqlite3 amalgamation..."
	$(RULE)cd sqlite && ./configure $(SQLITE3_CONFIGURE)

sqlite/sqlite3.c: sqlite/Makefile
	@echo ""
	@echo "Building sqlite3 amalgamation..."
	$(RULE)cd sqlite && $(MAKE) -j`nproc` sqlite3.c

libroot: $(LIBROOT)

$(LIBROOT):
	@echo "Making libroot..."
	$(RULE)mkdir -p $(LIBROOT)
	@echo "Copying 3rd-party libraries..."
	@echo "Done."

clean:
	$(info Cleaning third-party outputs...)
	$(RULE)-rm -rf \
		lib \
		apple/pkl/build \
		astral/ruff/target \
		astral/uv/target \
		oxc-project/oxc/target \
		apache/apr/target \
		google/boringssl/build
	$(RULE)-cd astral/ruff && $(CARGO) clean
	$(RULE)-cd astral/uv && $(CARGO) clean
	$(RULE)-cd oxc-project/oxc && $(CARGO) clean
	$(RULE)-cd sqlite && $(MAKE) clean && rm -fv Makefile sqlite3.c
	$(RULE)-cd google/boringssl && rm -fr build
	$(RULE)-cd apache/apr && git clean -xdf

.PHONY: all pkl ruff uv oxc google libroot
