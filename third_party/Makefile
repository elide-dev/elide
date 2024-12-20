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
ENABLE_CCACHE ?= yes
ENABLE_SCCACHE ?= yes
ELIDE_ROOT ?= $(realpath $(shell pwd)/..)
MACOS_MIN_VERSION ?= 12.3

NATIVE_TOOLS ?= sqlite boringssl apr
LANGS ?= pkl

REQUIRED ?= zlib sqlite boringssl apr

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
CMAKE_BUILD = $(MAKE)
CMAKE_INSTALL = $(MAKE) install

JOBS ?= $(shell nproc)

ifneq ($(NINJA),)
ifeq ($(USE_NINJA),yes)
EXTRA_CMAKE_FLAGS += -GNinja
CMAKE_BUILD = $(NINJA) -j$(JOBS)
CMAKE_INSTALL = $(NINJA) install
endif
endif

ifeq ($(OS),Darwin)
EXTRA_CMAKE_FLAGS += -DCMAKE_OSX_SYSROOT=macosx -DCMAKE_OSX_DEPLOYMENT_TARGET=$(MACOS_MIN_VERSION)
endif

CCACHE ?= $(shell which ccache)
SCCACHE ?= $(shell which sccache)

ifeq ($(ENABLE_SCCACHE),yes)
ifneq ($(SCCACHE),)
CC_INJECTED ?= $(SCCACHE) $(CC)
EXTRA_CMAKE_FLAGS += -DCMAKE_C_COMPILER_LAUNCHER=$(SCCACHE) -DCMAKE_CXX_COMPILER_LAUNCHER=$(SCCACHE)
endif
else ifeq ($(ENABLE_CCACHE),yes)
ifneq ($(CCACHE),)
CC_INJECTED ?= $(CCACHE) $(CC)
EXTRA_CMAKE_FLAGS += -DCMAKE_C_COMPILER_LAUNCHER=$(CCACHE) -DCMAKE_CXX_COMPILER_LAUNCHER=$(CCACHE)
endif
else
CC_INJECTED ?= $(CC)
endif

GRADLE ?= ./gradlew

CFLAGS_BASE_ALL ?=-g -O2 -fPIC -fPIE -fno-omit-frame-pointer -DELIDE -DHAVE_OPENSSL -U_FORTIFY_SOURCE -D_FORTIFY_SOURCE=3 -D_GLIBCXX_ASSERTIONS $(CFLAGS)
CFLAGS_BASE_STRICT ?= -Werror -fsanitize=bounds -fsanitize-undefined-trap-on-error

CFLAGS_BASE_GCC ?=$(CFLAGS_BASE_ALL)
CFLAGS_BASE_GCC += -Wall -Wextra -Wpedantic -Wformat=2 -Wformat-overflow=2 -Wformat-truncation=2 -Wformat-security -Wnull-dereference -Wstack-protector -Wtrampolines -Walloca -Wvla -Warray-bounds=2 -Wimplicit-fallthrough=3 -Wtraditional-conversion -Wshift-overflow=2 -Wcast-qual -Wstringop-overflow=4 -Wconversion -Warith-conversion -Wlogical-op -Wduplicated-cond -Wduplicated-branches -Wformat-signedness -Wshadow -Wstrict-overflow=4 -Wundef -Wstrict-prototypes -Wswitch-default -Wswitch-enum -Wstack-usage=1000000 -Wcast-align=strict
CFLAGS_BASE_GCC += -fstack-protector-strong -fstack-clash-protection -fhardened -fstrict-flex-arrays=3 -fno-delete-null-pointer-checks -fno-strict-overflow -fno-strict-aliasing -fexceptions
CFLAGS_BASE_GCC += -Wl,-z,relro -Wl,-z,now -Wl,-z,noexecstack -Wl,-z,separate-code

CFLAGS_BASE_CLANG ?=$(CFLAGS_BASE_ALL)
CFLAGS_BASE_CLANG += -fstack-protector-strong -fno-delete-null-pointer-checks -fno-strict-overflow -fno-strict-aliasing -fexceptions

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

ifeq ($(OS),Darwin)
CFLAGS_BASE += -mmacosx-version-min=$(MACOS_MIN_VERSION)
endif

ifeq ($(NATIVE),yes)
ifeq ($(OS),Darwin)
ifeq ($(UNAME_P),arm)
CFLAGS_BASE += -march=armv8-a+crypto+crc+simd -mtune=native -D__ARM_NEON -D__ARM_FEATURE_AES -D__ARM_FEATURE_SHA2
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

ifeq ($(CUSTOM_ZLIB),yes)
ZLIB_TARGET ?= cloudflare/zlib/libz.a
ZLIB_SRC ?= cloudflare/zlib
else
ZLIB_TARGET ?= madler/zlib/libz.a
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
CFLAGS_APR +=$(CFLAGS_BASE) $(CFLAGS_LTO) -I$(ELIDE_ROOT)/third_party/google/boringssl/include -Wno-macro-redefined -Wno-tautological-type-limit-compare
LDFLAGS_APR ?=-L$(ELIDE_ROOT)/third_party/google/boringssl/build -lssl -lcrypto
LDFLAGS_APR +=$(LDFLAGS_BASE)

# APR does not link with LLD.
APR_PREFIX = CC="$(CC_INJECTED)" CXX="$(CXX)" LD="ld" CFLAGS="$(CFLAGS_APR)" LDFLAGS="$(LDFLAGS_APR)"

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
		&& $(APR_PREFIX) make

clean-apr:
	$(RULE)cd apache/apr && git clean -xdf

boringssl: google/boringssl/build

CMAKE_BORINGSSL ?= -DCMAKE_BUILD_TYPE=Release -DCMAKE_POSITION_INDEPENDENT_CODE=TRUE -DCMAKE_LINKER=$(LD) $(EXTRA_CMAKE_FLAGS)
ifeq ($(OS),Darwin)
CMAKE_BORINGSSL += -DCMAKE_INTERPROCEDURAL_OPTIMIZATION=ON
endif

CFLAGS_BORINGSSL ?=
#CFLAGS_BORINGSSL +=$(CFLAGS_BASE)
LDFLAGS_BORINGSSL ?=
#LDFLAGS_BORINGSSL +=$(LDFLAGS_BASE)
ASMFLAGS_BORINGSSL ?=
#ASMFLAGS_BORINGSSL +=$(ASMFLAGS_BASE)
BORINGSSL_PREFIX = CC="$(CC)" CXX="$(CXX)" LD="$(LD)" CFLAGS="$(CFLAGS_BORINGSSL)" LDFLAGS="$(LDFLAGS_BORINGSSL)" CXXFLAGS="$(CFLAGS_BORINGSSL)" ASMFLAGS="$(ASMFLAGS_BORINGSSL)" CMAKE_INSTALL_PREFIX=$(TARGET_ROOT)

google/boringssl/build:
	@echo ""
	@echo "Building google/boringssl..."
	$(RULE)cd google/boringssl && mkdir build && cd build \
		&& $(BORINGSSL_PREFIX) $(CMAKE) $(CMAKE_BORINGSSL) -DCMAKE_INSTALL_PREFIX=$(TARGET_ROOT) .. \
		&& $(BORINGSSL_PREFIX) $(CMAKE_BUILD) \
		&& ./ssl_test \
		&& cd .. \
		&& echo "BoringSSL is ready."

CFLAGS_ZLIB ?=
CFLAGS_ZLIB +=$(CFLAGS_BASE) $(CFLAGS_BASE_STRICT) -std=c11
LDFLAGS_ZLIB ?=
LDFLAGS_ZLIB +=$(LDFLAGS_BASE)
ZLIB_PREFIX = CC="$(CC_INJECTED)" CXX="$(CXX)" LD="$(LD)" CFLAGS="$(CFLAGS_ZLIB)" LDFLAGS="$(LDFLAGS_ZLIB)"
ZLIB_CONFIGURE ?= --const --prefix=$(TARGET_ROOT) --static --64

zlib: $(ZLIB_TARGET)

zlib-clean:
	$(RULE)cd $(ZLIB_SRC) && $(MAKE) clean && git clean -xdf

$(ZLIB_TARGET):
	@echo ""
	@echo "Configuring zlib..."
	$(RULE)cd $(ZLIB_SRC) \
		&& $(ZLIB_PREFIX) ./configure $(ZLIB_CONFIGURE) \
		&& $(ZLIB_PREFIX) make

SQLITE3_CONFIGURE ?=--enable-all --enable-memsys5 --enable-update-limit --enable-tempstore=yes --enable-debug --enable-static --enable-shared --with-pic
CFLAGS_SQLITE3 ?=$(CFLAGS_BASE)

SQLITE3_SETTINGS += -DSQLITE_CORE=1 -DSQLITE_DEFAULT_FILE_PERMISSIONS=0666 -DSQLITE_DEFAULT_MEMSTATUS=0 -DSQLITE_DISABLE_PAGECACHE_OVERFLOW_STATS=1 -DSQLITE_ENABLE_API_ARMOR=1 -DSQLITE_ENABLE_COLUMN_METADATA=1 -DSQLITE_ENABLE_DBSTAT_VTAB=1 -DSQLITE_ENABLE_FTS3=1 -DSQLITE_ENABLE_FTS3_PARENTHESIS=1 -DSQLITE_ENABLE_FTS5=1 -DSQLITE_ENABLE_LOAD_EXTENSION=1 -DSQLITE_ENABLE_MATH_FUNCTIONS=1 -DSQLITE_ENABLE_RTREE=1 -DSQLITE_ENABLE_STAT4=1 -DSQLITE_HAVE_ISNAN=1 -DSQLITE_MAX_ATTACHED=125 -DSQLITE_MAX_COLUMN=32767 -DSQLITE_MAX_FUNCTION_ARG=127 -DSQLITE_MAX_LENGTH=2147483647 -DSQLITE_MAX_MMAP_SIZE=1099511627776 -DSQLITE_MAX_PAGE_COUNT=4294967294 -DSQLITE_MAX_SQL_LENGTH=1073741824 -DSQLITE_MAX_VARIABLE_NUMBER=250000 -DSQLITE_THREADSAFE=1

ifeq ($(OS),Darwin)
CFLAGS_SQLITE3 +=$(CFLAGS_BASE) -Wno-array-bounds -Wno-format-nonliteral -Wno-array-bounds-pointer-arithmetic -Wno-assign-enum -Wno-implicit-fallthrough -Wno-tautological-constant-in-range-compare $(SQLITE3_SETTINGS)
else
CFLAGS_SQLITE3 +=
endif

LDFLAGS_SQLITE3 ?=
LDFLAGS_SQLITE3 +=$(LDFLAGS_BASE)

# SQLite does not link under LLD.
SQLITE3_PREFIX = CC="$(CC_INJECTED)" CXX="$(CXX)" LD="ld" CFLAGS="$(CFLAGS_SQLITE3)" LDFLAGS="$(LDFLAGS_SQLITE3)"

sqlite: sqlite/sqlite3.c

sqlite-clean:
	$(RULE)cd sqlite && $(MAKE) clean && rm -fv Makefile sqlite3.c

sqlite/Makefile:
	@echo ""
	@echo "Configuring sqlite3..."
	$(RULE)cd sqlite && $(SQLITE3_PREFIX) ./configure --prefix=`pwd`/install $(SQLITE3_CONFIGURE)

sqlite/sqlite3.c: sqlite/Makefile
	@echo ""
	@echo "Building sqlite3..."
	$(RULE)cd sqlite \
		&& $(SQLITE3_PREFIX) $(MAKE) OPTS="$(SQLITE3_SETTINGS)" sqlite3.c \
		&& $(SQLITE3_PREFIX) $(MAKE) OPTS="$(SQLITE3_SETTINGS)" \
		&& $(SQLITE3_PREFIX) $(MAKE) OPTS="$(SQLITE3_SETTINGS)" install

GVM_BUILD_PREFIX ?= JAVA_HOME="$$HOME/.mx/jdks/$(LABS_JDK)/Contents/Home"

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

gvm-ce-llvm:
	@echo ""
	@echo "Building GraalVM (LLVM)..."
	$(RULE)cd oracle/graalvm/vm \
		&& $(GVM_BUILD_PREFIX) $(MX) --env ce-llvm build \
		&& $(GVM_BUILD_PREFIX) $(MX) --env ce-llvm graalvm-home > ../../../../.graalvm-home

gvm-ce-macos-aarch64-llvm:
	@echo ""
	@echo "Building GraalVM (macOS arm64, LLVM)..."
	$(RULE)cd oracle/graalvm/vm \
		&& cp -fv $(ELIDE_ROOT)/third_party/graalvm-ce-darwin-aarch64-llvm \
			$(ELIDE_ROOT)/third_party/oracle/graalvm/vm/mx.vm/graalvm-ce-darwin-aarch64-llvm \
		&& $(GVM_BUILD_PREFIX) $(MX) --env graalvm-ce-darwin-aarch64-llvm build \
		&& $(GVM_BUILD_PREFIX) $(MX) --env graalvm-ce-darwin-aarch64-llvm graalvm-home > ../../../../.graalvm-home

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
	$(RULE)-cd sqlite && $(MAKE) clean && rm -fv Makefile sqlite3.c && git clean -xdf
	$(RULE)-cd google/boringssl && rm -fr build
	$(RULE)-cd apache/apr && git clean -xdf
	$(RULE)-cd cloudflare/zlib && make clean && git clean -xdf
	$(RULE)-cd madler/zlib && make clean && git clean -xdf

gvm-clean:
	$(RULE)-cd oracle/graalvm/vm && $(GVM_BUILD_PREFIX) $(MX) clean

distclean: clean gvm-clean

help:
	@echo "Please use the main project Makefile."

.PHONY: all pkl google libroot
