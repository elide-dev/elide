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
CUSTOM_ZLIB ?= no
NATIVE ?= no
RELEASE ?= no

USE_NINJA ?= yes

ELIDE_ROOT ?= $(realpath $(shell pwd)/..)

ifeq ($(RELEASE),yes)
TARGET_ROOT ?= $(ELIDE_ROOT)/target/release
else
TARGET_ROOT ?= $(ELIDE_ROOT)/target/debug
endif

TYPESCRIPT_VERSION = 5.5.0
LABS_JDK ?= labsjdk-ce-latest-24+3-jvmci-b01

CARGO ?= $(shell which cargo)
GO ?= $(shell which go)
NPM ?= $(shell which npm)
NPX ?= $(shell which npx)
BUN ?= $(shell which bun)
MX ?= $(shell which mx)
NINJA ?= $(shell which ninja)
PWD ?= $(shell pwd)
CMAKE ?= $(shell which cmake)
export RANLIB ?= $(shell which true)
GCC ?= $(shell which gcc)
LLD ?= $(shell which lld)
CLANG ?= $(shell which clang)

OS ?= $(shell uname -s)
UNAME_P := $(shell uname -p)
CMAKE_BUILD = $(MAKE) -j`nproc`
CMAKE_INSTALL = $(MAKE) install

ifneq ($(NINJA),)
ifeq ($(USE_NINJA),yes)
EXTRA_CMAKE_FLAGS += -GNinja
CMAKE_BUILD = $(NINJA) -j`nproc`
CMAKE_INSTALL = $(NINJA) install
endif
endif

GRADLE ?= ./gradlew

CFLAGS_BASE_ALL ?=-g -O2 -fPIC -fPIE -fno-omit-frame-pointer -DELIDE -DHAVE_OPENSSL -U_FORTIFY_SOURCE -D_FORTIFY_SOURCE=3 -D_GLIBCXX_ASSERTIONS $(CFLAGS)
CFLAGS_BASE_STRICT ?= -Werror

CFLAGS_BASE_GCC ?=$(CFLAGS_BASE_ALL)
CFLAGS_BASE_GCC += -Wall -Wextra -Wpedantic -Wformat=2 -Wformat-overflow=2 -Wformat-truncation=2 -Wformat-security -Wnull-dereference -Wstack-protector -Wtrampolines -Walloca -Wvla -Warray-bounds=2 -Wimplicit-fallthrough=3 -Wtraditional-conversion -Wshift-overflow=2 -Wcast-qual -Wstringop-overflow=4 -Wconversion -Warith-conversion -Wlogical-op -Wduplicated-cond -Wduplicated-branches -Wformat-signedness -Wshadow -Wstrict-overflow=4 -Wundef -Wstrict-prototypes -Wswitch-default -Wswitch-enum -Wstack-usage=1000000 -Wcast-align=strict
CFLAGS_BASE_GCC += -fstack-protector-strong -fstack-clash-protection -fsanitize=bounds -fsanitize-undefined-trap-on-error -fhardened -fstrict-flex-arrays=3 -fno-delete-null-pointer-checks -fno-strict-overflow -fno-strict-aliasing -ftrivial-auto-var-init=zero -fexceptions
CFLAGS_BASE_GCC += -Wl,-z,relro -Wl,-z,now -Wl,-z,noexecstack -Wl,-z,separate-code

CFLAGS_BASE_CLANG ?=$(CFLAGS_BASE_ALL)
#CFLAGS_BASE_CLANG += -Walloca -Wformat=2 -Wformat-security -Wnull-dereference -Wstack-protector -Wvla -Warray-bounds -Warray-bounds-pointer-arithmetic -Wassign-enum -Wformat-type-confusion -Widiomatic-parentheses -Wimplicit-fallthrough -Wloop-analysis -Wpointer-arith -Wshift-sign-overflow -Wtautological-constant-in-range-compare -Wthread-safety -Wthread-safety-beta -Werror=implicit -Werror=incompatible-pointer-types -Werror=int-conversion
#CFLAGS_BASE_CLANG += -fstack-protector-strong -fsanitize=bounds -fsanitize-undefined-trap-on-error -fstrict-flex-arrays=2 -fno-delete-null-pointer-checks -fno-strict-overflow -fno-strict-aliasing -ftrivial-auto-var-init=zero -fexceptions

ifeq ($(CC),gcc)
  CFLAGS_BASE += $(CFLAGS_BASE_GCC)
else ifeq ($(CC),cc)
  CFLAGS_BASE += $(CFLAGS_BASE_CLANG)
endif

ifeq ($(UNAME_P),x86_64)
	CFLAGS_BASE += -fcf-protection=full
else ifeq ($(UNAME_P),arm)
	CFLAGS_BASE += -mbranch-protection=standard
endif

ASMFLAGS_BASE ?=-Wa,--noexecstack $(ASMFLAGS)
LDFLAGS_BASE ?= $(LDFLAGS)

ifneq ($(OS),Darwin)
LDFLAGS_BASE +=-Wl,-z,relro -Wl,-z,now -Wl,-z,noexecstack -Wl,-z,separate-code
endif

ifeq ($(RELEASE),yes)
CMAKE_BUILD_TYPE ?= Release
ifeq ($(OS),Darwin)
CFLAGS_BASE += -O3
CFLAGS_LTO ?= -flto=thin
LDFLAGS_BASE += -flto=thin
else
CFLAGS_BASE += -O3
endif
else
CMAKE_BUILD_TYPE ?= Debug
endif

ifeq ($(NATIVE),yes)
ifeq ($(OS),Darwin)
ifeq ($(UNAME_P),arm)
CFLAGS_BASE += -march=armv8-a+crypto+crc -mtune=native -D__ARM_NEON -D__ARM_FEATURE_AES -D__ARM_FEATURE_SHA2
else
CFLAGS_BASE += -march=native -mtune=native
endif
else
CFLAGS_BASE += -march=native -mtune=native
endif
else
CFLAGS_BASE += -mtune=generic
ifeq ($(UNAME_P),x86_64)
CFLAGS_BASE += -march=x86-64-v3
endif
endif

ifeq ($(BUILD_MODE),debug)
CFLAGS_BASE +=-g
endif

GRADLE_ARGS ?= -x test -x check
LIBROOT ?= lib/

NATIVE_TOOLS ?= sqlite boringssl apr
LANGS ?= pkl

REQUIRED ?= zlib sqlite boringssl apr

ifeq ($(CUSTOM_ZLIB),yes)
ZLIB_TARGET ?= cloudflare/zlib/target
ZLIB_SRC ?= cloudflare/zlib
else
ZLIB_TARGET ?= madler/zlib/target
ZLIB_SRC ?= madler/zlib
endif

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

CFLAGS_APR ?=
CFLAGS_APR +=$(CFLAGS_BASE) $(CFLAGS_LTO) -I$(TARGET_ROOT)/include -Wno-macro-redefined -Wno-tautological-type-limit-compare
LDFLAGS_APR ?=-L$(TARGET_ROOT)/lib -lssl -lcrypto
LDFLAGS_APR +=$(LDFLAGS_BASE)

# APR does not link with LLD.
APR_PREFIX = CC="$(CC)" CXX="$(CXX)" LD="ld" CFLAGS="$(CFLAGS_APR)" LDFLAGS="$(LDFLAGS_APR)"

apache/apr/.libs: google/boringssl/build
	@echo ""
	@echo "Building apache/apr..."
	$(RULE)cd apache/apr && ./buildconf \
		&& $(APR_PREFIX) ./configure \
			--prefix=$(TARGET_ROOT) \
			--enable-shared \
			--enable-static \
			--enable-threads \
			--enable-posix-shm \
			--enable-sysv-shm \
		&& $(APR_PREFIX) make -j`nproc` \
		&& $(APR_PREFIX) make install

clean-apr:
	$(RULE)cd apache/apr && git clean -xdf

boringssl: google/boringssl/build

CMAKE_BORINGSSL ?= -DCMAKE_BUILD_TYPE=Release -DCMAKE_POSITION_INDEPENDENT_CODE=TRUE -DCMAKE_LINKER=$(LD) -DCMAKE_INTERPROCEDURAL_OPTIMIZATION=ON $(EXTRA_CMAKE_FLAGS)
CFLAGS_BORINGSSL ?=
CFLAGS_BORINGSSL +=$(CFLAGS_BASE)
LDFLAGS_BORINGSSL ?=
LDFLAGS_BORINGSSL +=$(LDFLAGS_BASE)
ASMFLAGS_BORINGSSL ?=
ASMFLAGS_BORINGSSL +=$(ASMFLAGS_BASE)
BORINGSSL_PREFIX = CC="$(CC)" CXX="$(CXX)" LD="$(LD)" CFLAGS="$(CFLAGS_BORINGSSL)" LDFLAGS="$(LDFLAGS_BORINGSSL)" CXXFLAGS="$(CFLAGS_BORINGSSL)" ASMFLAGS="$(ASMFLAGS_BORINGSSL)" CMAKE_INSTALL_PREFIX=$(TARGET_ROOT)

google/boringssl/build:
	@echo ""
	@echo "Building google/boringssl..."
	$(RULE)cd google/boringssl && mkdir build && cd build \
		&& $(BORINGSSL_PREFIX) $(CMAKE) $(CMAKE_BORINGSSL) -DCMAKE_INSTALL_PREFIX=$(TARGET_ROOT) .. \
		&& $(BORINGSSL_PREFIX) $(CMAKE_BUILD) \
		&& $(BORINGSSL_PREFIX) $(CMAKE_INSTALL) \
		&& ./ssl_test

CFLAGS_ZLIB ?=
CFLAGS_ZLIB +=$(CFLAGS_BASE) $(CFLAGS_BASE_STRICT) -std=c18
LDFLAGS_ZLIB ?=
LDFLAGS_ZLIB +=$(LDFLAGS_BASE)
ZLIB_PREFIX = CC="$(CC)" CXX="$(CXX)" LD="$(LD)" CFLAGS="$(CFLAGS_ZLIB)" LDFLAGS="$(LDFLAGS_ZLIB)"
ZLIB_CONFIGURE ?= --const --prefix=$(TARGET_ROOT) --static --64

zlib: $(ZLIB_TARGET)

zlib-clean:
	$(RULE)cd $(ZLIB_SRC) && $(MAKE) clean && git clean -xdf

$(ZLIB_TARGET):
	@echo ""
	@echo "Configuring zlib..."
	$(RULE)cd $(ZLIB_SRC) \
		&& $(ZLIB_PREFIX) ./configure $(ZLIB_CONFIGURE) \
		&& $(ZLIB_PREFIX) make -j`nproc` \
		&& $(ZLIB_PREFIX) make install

SQLITE3_CONFIGURE ?=--enable-all --enable-memsys5 --enable-update-limit --enable-tempstore=yes --enable-debug
CFLAGS_SQLITE3 ?=
CFLAGS_SQLITE3 +=$(CFLAGS_BASE) $(CFLAGS_LTO) $(CFLAGS_BASE_STRICT) -std=c18 -Wno-array-bounds -Wno-format-nonliteral -Wno-array-bounds-pointer-arithmetic -Wno-assign-enum -Wno-implicit-fallthrough -Wno-tautological-constant-in-range-compare
LDFLAGS_SQLITE3 ?=
LDFLAGS_SQLITE3 +=$(LDFLAGS_BASE)

# SQLite does not link under LLD.
SQLITE3_PREFIX = CC="$(CC)" CXX="$(CXX)" LD="ld" CFLAGS="$(CFLAGS_SQLITE3)" LDFLAGS="$(LDFLAGS_SQLITE3)"

sqlite: sqlite/sqlite3.c

sqlite-clean:
	$(RULE)cd sqlite && $(MAKE) clean && rm -fv Makefile sqlite3.c

sqlite/Makefile:
	@echo ""
	@echo "Configuring sqlite3 amalgamation..."
	$(RULE)cd sqlite && $(SQLITE3_PREFIX) ./configure --prefix=$(TARGET_ROOT) $(SQLITE3_CONFIGURE)

sqlite/sqlite3.c: sqlite/Makefile
	@echo ""
	@echo "Building sqlite3 amalgamation..."
	$(RULE)cd sqlite \
		&& $(SQLITE3_PREFIX) $(MAKE) -j`nproc` \
		&& $(SQLITE3_PREFIX) $(MAKE) -j`nproc` sqlite3.c \
		&& $(SQLITE3_PREFIX) $(MAKE) install

GVM_BUILD_PREFIX ?= JAVA_HOME="$$HOME/.mx/jdks/$(LABS_JDK)"

gvm-fetch-labsjdk:
	@echo ""
	@echo "Fetching labs JDK..."
	$(RULE)cd oracle/graalvm/vm && yes Y | $(MX) fetch-jdk --strip-contents-home labsjdk-ce-latest

gvm-ce-macos-aarch64:
	@echo ""
	@echo "Building GraalVM (macOS arm64)..."
	$(RULE)cd oracle/graalvm/vm \
		&& $(GVM_BUILD_PREFIX) $(MX) --env ce-darwin-aarch64 build \
		&& $(GVM_BUILD_PREFIX) $(MX) --env ce-darwin-aarch64 graalvm-home > ../../../../.graalvm-home

gvm-ce-linux-amd64:
	@echo ""
	@echo "Building GraalVM (Linux amd64)..."
	$(RULE)cd oracle/graalvm/vm \
		&& $(GVM_BUILD_PREFIX) $(MX) --env ce-linux-amd64 build \
		&& $(GVM_BUILD_PREFIX) $(MX) --env ce-linux-amd64 graalvm-home > ../../../../.graalvm-home

gvm-ce-linux-amd64-complete:
	@echo ""
	@echo "Building GraalVM (Linux amd64, with artifacts)..."
	$(RULE)cd oracle/graalvm/vm \
		&& $(GVM_BUILD_PREFIX) $(MX) --env ce-linux-amd64-complete build \
		&& $(GVM_BUILD_PREFIX) $(MX) --env ce-linux-amd64-complete graalvm-home > ../../../../.graalvm-home

gvm-ce-macos-aarch64-complete:
	@echo ""
	@echo "Building GraalVM (macOS arm64, with artifacts)..."
	$(RULE)cd oracle/graalvm/vm \
		&& $(GVM_BUILD_PREFIX) $(MX) --env ce-darwin-aarch64-complete build \
		&& $(GVM_BUILD_PREFIX) $(MX) --env ce-darwin-aarch64-complete graalvm-home > ../../../../.graalvm-home

libroot: $(LIBROOT)

$(LIBROOT):
	@echo "Making libroot..."
	$(RULE)mkdir -p $(LIBROOT)
	@echo "Copying 3rd-party libraries..."
	$(RULE)cp -fr$(POSIX_FLAGS) \
		google/boringssl/build/ssl/libssl.a \
		google/boringssl/build/crypto/libcrypto.a \
		$(LIBROOT);
	$(RULE)cp -fr$(POSIX_FLAGS) \
		apache/apr/.libs/libapr-2.0.dylib \
		apache/apr/.libs/libapr-2.a \
		$(LIBROOT);
	@echo "Done."

clean:
	$(info Cleaning third-party outputs...)
	$(RULE)-rm -rf \
		lib \
		apple/pkl/build \
		apache/apr/target \
		google/boringssl/build \
		$(LIBROOT)
	$(RULE)-cd sqlite && $(MAKE) clean && rm -fv Makefile sqlite3.c
	$(RULE)-cd google/boringssl && rm -fr build
	$(RULE)-cd apache/apr && git clean -xdf
	$(RULE)-cd cloudflare/zlib && make clean && git clean -xdf

gvm-clean:
	$(RULE)-cd oracle/graalvm/vm && $(GVM_BUILD_PREFIX) $(MX) clean

distclean: clean gvm-clean

.PHONY: all pkl google libroot
