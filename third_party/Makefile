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
MUSL ?= no
USE_NINJA ?= yes
ENABLE_CCACHE ?= no
ENABLE_SCCACHE ?= yes
ELIDE_ROOT ?= $(realpath $(shell pwd)/..)
MACOS_MIN_VERSION ?= 12.3

NATIVE_TOOLS ?= boringssl sqlite apr
LANGS ?= pkl

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

REQUIRED ?= sqlite boringssl apr

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

CFLAGS_BASE_ALL ?=$(shell cat ../tools/cflags/base | xargs) $(CFLAGS)
CFLAGS_BASE_STRICT ?= -Werror -fsanitize=bounds -fsanitize-undefined-trap-on-error
CFLAGS_BASE_GCC ?=$(CFLAGS_BASE_ALL) $(shell cat ../tools/cflags/gcc | xargs)
CFLAGS_BASE_CLANG ?=$(CFLAGS_BASE_ALL) $(shell cat ../tools/cflags/clang | xargs)

ifeq ($(COMPILER),gcc)
  CFLAGS_BASE += $(CFLAGS_BASE_GCC)
else ifeq ($(COMPILER),clang)
  CFLAGS_BASE += $(CFLAGS_BASE_CLANG)
  #USE_LD ?= lld
else
	# default to baseline flags
	CFLAGS_BASE += $(CFLAGS_BASE_ALL)
endif

ifeq ($(UNAME_P),x86_64)
	CFLAGS_BASE += -fcf-protection=full
else ifeq ($(UNAME_P),arm)
	CFLAGS_BASE += -mbranch-protection=standard
endif

ifneq ($(USE_LD),)
LD = $(USE_LD)
CFLAGS_BASE += -fuse-ld=$(USE_LD)
endif

ASMFLAGS_BASE ?=-Wa,--noexecstack $(ASMFLAGS)
LDFLAGS_BASE ?= $(LDFLAGS)

ifneq ($(OS),Darwin)
LDFLAGS_BASE +=-Wl,-z,relro -Wl,-z,now -Wl,-z,noexecstack -Wl,-z,separate-code
endif

ifeq ($(RELEASE),yes)
CMAKE_BUILD_TYPE ?= Release
CFLAGS_BASE += -O3 -flto=auto

else
CFLAGS_BASE += -O1 -g -flto=auto
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
CFLAGS_BASE += -mtune=native
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

ifeq ($(MUSL),yes)
TARGET_FLAGS += --host=x86_64-linux-musl
export CC = x86_64-linux-musl-gcc
CC_INJECTED = $(CC)
endif

all: $(ALL_LIBS)

effective-cflags:
	@echo "$(CFLAGS_BASE)"

pkl: apple/pkl/build

apple/pkl/build:
	@echo ""
	@echo "Building apple/pkl..."
	$(RULE)cd apple/pkl && $(GRADLE) build $(GRADLE_ARGS)

apr: apache/apr/.libs

CFLAGS_APR ?=
CFLAGS_APR +=$(CFLAGS_BASE) -w
LDFLAGS_APR ?=-L$(ELIDE_ROOT)/third_party/google/boringssl/build -lssl -lcrypto
LDFLAGS_APR +=$(LDFLAGS_BASE)

# APR does not link with LLD.
APR_PREFIX = CC="$(CC_INJECTED)" CXX="$(CXX)" LD="ld" CFLAGS="$(CFLAGS_APR)" LDFLAGS="$(LDFLAGS_APR)"

ifeq ($(OS),Darwin)
apache/apr/.libs: boringssl
else
apache/apr/.libs:
endif
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
			$(TARGET_FLAGS) \
		&& $(APR_PREFIX) make

clean-apr:
	$(RULE)cd apache/apr && git clean -xdf

boringssl: google/boringssl/build

CMAKE_BORINGSSL ?= -DCMAKE_BUILD_TYPE=Release -DCMAKE_POSITION_INDEPENDENT_CODE=TRUE -DCMAKE_LINKER=$(LD) $(EXTRA_CMAKE_FLAGS)

ifeq ($(RELEASE),yes)
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
		&& echo "BoringSSL is ready."
ifeq ($(MUSL),no)
	$(RULE)cd google/boringssl/build && ./ssl_test && echo "BoringSSL test complete."
endif

CFLAGS_ZLIB ?=
CFLAGS_ZLIB +=$(CFLAGS_BASE) $(CFLAGS_BASE_STRICT) -std=c11 -w
LDFLAGS_ZLIB ?=
LDFLAGS_ZLIB +=$(LDFLAGS_BASE)
ZLIB_PREFIX = CC="$(CC_INJECTED)" CXX="$(CXX)" LD="$(LD)" CFLAGS="$(CFLAGS_ZLIB)" LDFLAGS="$(LDFLAGS_ZLIB)"
ZLIB_CONFIGURE ?= --const --prefix=$(TARGET_ROOT) --static --64

zlib: $(ZLIB_TARGET)

zlib-clean:
	$(RULE)cd $(ZLIB_SRC) && $(MAKE) clean && git clean -xdf

$(ZLIB_TARGET): $(TARGET_ROOT)/lib/libz.a
$(TARGET_ROOT)/lib/libz.a:
	@echo ""
	@echo "Configuring zlib..."
	$(RULE)cd $(ZLIB_SRC) \
		&& $(ZLIB_PREFIX) ./configure $(TARGET_FLAGS) $(ZLIB_CONFIGURE) \
		&& $(ZLIB_PREFIX) $(MAKE) \
		&& $(ZLIB_PREFIX) $(MAKE) install

SQLITE3_CONFIGURE ?=--enable-all
SQLITE3_CONFIGURE += --disable-debug
SQLITE3_CONFIGURE += --enable-static
SQLITE3_CONFIGURE += --enable-shared
SQLITE3_CONFIGURE += --disable-math
SQLITE3_CONFIGURE += --disable-tcl

SQLITE3_FLAGS =SQLITE_CORE=1
SQLITE3_FLAGS += SQLITE_DEFAULT_FILE_PERMISSIONS=0666
SQLITE3_FLAGS += SQLITE_DEFAULT_MEMSTATUS=0
SQLITE3_FLAGS += SQLITE_DISABLE_PAGECACHE_OVERFLOW_STATS=1
SQLITE3_FLAGS += SQLITE_ENABLE_API_ARMOR=1
SQLITE3_FLAGS += SQLITE_ENABLE_COLUMN_METADATA=1
SQLITE3_FLAGS += SQLITE_ENABLE_DBSTAT_VTAB=1
SQLITE3_FLAGS += SQLITE_ENABLE_FTS3=1
SQLITE3_FLAGS += SQLITE_ENABLE_FTS3_PARENTHESIS=1
SQLITE3_FLAGS += SQLITE_ENABLE_FTS5=1
SQLITE3_FLAGS += SQLITE_ENABLE_LOAD_EXTENSION=1
SQLITE3_FLAGS += SQLITE_ENABLE_MATH_FUNCTIONS=0
SQLITE3_FLAGS += SQLITE_ENABLE_RTREE=1
SQLITE3_FLAGS += SQLITE_ENABLE_STAT4=1
SQLITE3_FLAGS += SQLITE_HAVE_ISNAN=1
SQLITE3_FLAGS += SQLITE_MAX_ATTACHED=25
SQLITE3_FLAGS += SQLITE_MAX_COLUMN=32767
SQLITE3_FLAGS += SQLITE_MAX_FUNCTION_ARG=127
SQLITE3_FLAGS += SQLITE_MAX_LENGTH=2147483647
SQLITE3_FLAGS += SQLITE_MAX_MMAP_SIZE=1099511627776
SQLITE3_FLAGS += SQLITE_MAX_PAGE_COUNT=4294967294
SQLITE3_FLAGS += SQLITE_MAX_SQL_LENGTH=1073741824
SQLITE3_FLAGS += SQLITE_MAX_VARIABLE_NUMBER=250000
SQLITE3_FLAGS += SQLITE_THREADSAFE=0

SQLITE3_SETTINGS ?=$(foreach flag,$(SQLITE3_FLAGS),-D$(flag))

# Force -O3 for sqlite.
CFLAGS_SQLITE3 ?=$(CFLAGS_BASE) -fPIC -w -O3

sqlite-settings:
	@echo "Flags: $(SQLITE3_FLAGS)"
	@echo "Settings: $(SQLITE3_SETTINGS)"

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
	$(RULE)cd sqlite && $(SQLITE3_PREFIX) ./configure --prefix=`pwd`/install $(TARGET_FLAGS) $(SQLITE3_CONFIGURE) $(SQLITE3_FLAGS)

sqlite/sqlite3.c: sqlite/Makefile
	@echo ""
	@echo "Building sqlite3..."
	$(RULE)cd sqlite && $(SQLITE3_PREFIX) $(MAKE) sqlite3.c install

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
