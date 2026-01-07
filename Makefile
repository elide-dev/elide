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
# Makefile: Elide
#

# Build Configuration
# ---------------------------------------------------------------------------------------------------------------------
VERSION ?= $(shell cat .version)

JVM ?= 22
RELEASE ?= no
SAMPLES ?= no
SIGNING_KEY ?= F812016B
REMOTE ?= no
PUSH ?= no
CHECK ?= yes
NATIVE ?= no
MUSL ?= no
COVERAGE ?= yes
BUILD_NATIVE_IMAGE ?= no
BUILD_STDLIB ?= no
RELEASE ?= no
MACOS_MIN_VERSION ?= 12.3
ENABLE_CCACHE ?= no
ENABLE_SCCACHE ?= yes
CUSTOM_JVM ?= no
export STRICT ?= yes
export WASM ?= no
export RELOCK ?= no
DEFAULT_REPOSITORY ?= s3://elide-maven
REPOSITORY ?= $(DEFAULT_REPOSITORY)

ifeq ($(CC),clang)
export COMPILER ?= clang
export RUST_CONFIG ?= clang
else
export COMPILER ?= gcc
export RUST_CONFIG ?= default
endif

# Flags that are exported to the third_party build.
export CUSTOM_ZLIB ?= no

# Required tools for build:
# ---------------------------------------------------------------------------------------------------------------------
# - Bash
# - Git
# - Rust
# - Java & GraalVM
# - Node.js & Yarn
# - Bazel
# - Buf
# - jq

# Optional tools for build:
# ---------------------------------------------------------------------------------------------------------------------
# - ccache
# - sccache

# Flags that control this makefile, along with their defaults:
# ---------------------------------------------------------------------------------------------------------------------
# DEBUG ?= no
# STRICT ?= yes
# RELEASE ?= no
# JVMDEBUG ?= no
# NATIVE ?= no
# BUILD_NATIVE_IMAGE ?= no
# CI ?= no
# DRY ?= no
# SCAN ?= no
# IGNORE_ERRORS ?= no
# RELOCK ?= no
# SIGNING ?= no
# SIGSTORE ?= no
# WASM ?= no
# SIGNING_KEY ?= "F812016B"
# REMOTE ?= no
# PUSH ?= no
# CHECK ?= yes
# CUSTOM_GVM ?= no
# CUSTOM_JVM ?= no
# CUSTOM_ZLIB ?= no
# USE_GVM_AS_JVM ?= no
# GVM_PROFILE ?= (default for os)
# BUILD_STDLIB ?= yes


# Required packages for build:
# ---------------------------------------------------------------------------------------------------------------------

LINUX_PKGS ?= libtool-bin tclsh build-essential git curl automake autoconf pkg-config
MACOS_PKGS ?= automake autoconf libtool pkg-config tcl-tk git curl

# ---------------------------------------------------------------------------------------------------------------------
# Key Environment Variables

export PATH
export JAVA_HOME
export GRAALVM_HOME

# ---------------------------------------------------------------------------------------------------------------------
# Tools

GRADLE ?= ./gradlew
BASH ?= $(shell which bash)
RUSTUP ?= $(shell which rustup)
CCACHE ?= $(shell which ccache)
SCCACHE ?= $(shell which sccache)
CARGO ?= $(shell which cargo)
YARN ?= $(shell which yarn)
RM ?= $(shell which rm)
FIND ?= $(shell which find)
MKDIR ?= $(shell which mkdir)
CP ?= $(shell which cp)
RSYNC ?= $(shell which rsync)
UNZIP ?= $(shell which unzip)
TAR ?= $(shell which tar)
ZIP ?= $(shell which zip)
XZ ?= $(shell which xz)
ZSTD ?= $(shell which zstd)
BZIP2 ?= $(shell which bzip2)
GZIP ?= $(shell which gzip)
GPG2 ?= $(shell which gpg)
PNPM ?= $(shell which pnpm)
BAZEL ?= $(shell which bazel)
NODE ?= $(shell which node)
BUN ?= $(shell which bun)
JAVA ?= $(shell which java)
NATIVE_IMAGE ?= $(shell which native-image)
BUF ?= $(shell which buf)
JQ ?= $(shell which jq)
BAZEL ?= $(shell which bazel)
RUSTC ?= $(shell which rustc)
CARGO ?= $(shell which cargo)
DOCKER ?= $(shell which docker)
LLVM_COV ?= $(shell which llvm-cov)
LLVM_PROFDATA ?= $(shell which llvm-profdata)
LLVM_CONFIG ?= $(shell which llvm-config)
CLANG ?= $(shell which clang)
LLD ?= $(shell which lld)
BOLT ?= $(shell which llvm-bolt)
PYTHON ?= $(shell which python)
RUFF ?= $(shell which ruff)
RUBY ?= $(shell which ruby)

# ---------------------------------------------------------------------------------------------------------------------
# Build State/Environment

export ELIDE_ROOT ?= $(shell pwd)
PWD ?= $(ELIDE_ROOT)
TARGET ?= $(PWD)/build
DIST ?= $(PWD)/target
DOCS ?= $(PWD)/docs
REPORTS ?=
SYSTEM ?= $(shell uname -s)
OS ?= $(shell uname -s)
UNAME_P := $(shell uname -p)
ARCH := $(shell uname -m)

export PROJECT_ROOT ?= $(shell pwd)
export ELIDE_ROOT ?= yes

ifeq ($(ENABLE_SCCACHE),yes)
ifeq ($(SCCACHE),)
ENABLE_SCCACHE = no
else
CACHE_MODE = sccache
endif
endif

ifeq ($(CACHE_MODE),)
ifeq ($(ENABLE_CCACHE),yes)
ifeq ($(CCACHE),)
ENABLE_CCACHE = no
else
CACHE_MODE = ccache
endif
endif
endif

# Exports for `third_party` Makefile.
export RELEASE
export NATIVE
export MACOS_MIN_VERSION

ifeq ($(RELEASE),yes)
export TARGET_ROOT ?= $(ELIDE_ROOT)/target/release
else
export TARGET_ROOT ?= $(ELIDE_ROOT)/target/debug
endif

PATH := $(shell echo $$PATH)

# Handle custom Java home path.
JAVA_HOME ?= $(shell echo $$JAVA_HOME)
GRAALVM_HOME ?= $(shell echo $$GRAALVM_HOME)

ifneq ("$(wildcard ./.java-home)","")
$(info Using custom JVM...)
CUSTOM_JVM = $(shell cat .java-home)
JAVA_HOME = $(CUSTOM_JVM)
GRAALVM_HOME = $(CUSTOM_JVM)
PATH := $(JAVA_HOME)/bin:$(PATH)
JAVA = $(JAVA_HOME)/bin/java
export JAVA_HOME
export GRAALVM_HOME
endif

# Handle custom GraalVM home path.
CUSTOM_GVM ?= no
USE_GVM_AS_JVM ?= no

ifneq ("$(wildcard ./.graalvm-home)","")
    $(info Using custom GraalVM...)
    CUSTOM_GVM = $(shell cat .graalvm-home)
    GRAALVM_HOME = $(CUSTOM_GVM)
    NATIVE_IMAGE = $(GRAALVM_HOME)/bin/native-image
ifeq ($(USE_GVM_AS_JVM),yes)
		JAVA_HOME = $(GRAALVM_HOME)
		PATH := $(GRAALVM_HOME)/bin:$(PATH)
		JAVA = $(GRAALVM_HOME)/bin/java
endif
endif

ifeq ($(SYSTEM),Darwin)
export GVM_PROFILE ?= gvm-ce-macos-aarch64
endif
ifeq ($(SYSTEM),Linux)
export GVM_PROFILE ?= gvm-ce-linux-amd64
endif

ifneq ($(RUST_CONFIG),default)
ifeq ($(ARCH),x86_64)
# Render flags for assigning a Cargo config.
RUST_CONFIG_FLAGS ?= --config=$(ELIDE_ROOT)/.cargo/config.$(RUST_CONFIG)-x86_64.toml
else
RUST_CONFIG_FLAGS ?= --config=$(ELIDE_ROOT)/.cargo/config.$(RUST_CONFIG)-$(ARCH).toml
endif
else
RUST_CONFIG_FLAGS ?=
endif

JS_FACADE_BIN ?= tools/runtime/bazel-bin/elide/runtime/js/runtime.bin.js
JS_FACADE_OUT ?= packages/graalvm-js/src/main/resources/META-INF/elide/embedded/runtime/js/facade.js
JS_POLYFILLS_BIN ?= tools/runtime/bazel-bin/elide/runtime/js/polyfills/polyfills.min.js
JS_POLYFILLS_OUT ?= packages/graalvm-js/src/main/resources/META-INF/elide/embedded/runtime/js/polyfills.js
JS_MODULE_BIN ?= tools/runtime/bazel-bin/elide/runtime/js/js.modules.tar
JS_MODULE_OUT ?= packages/graalvm-js/src/main/resources/META-INF/elide/embedded/runtime/js/js.vfs.tar

PY_FACADE_BIN ?= packages/graalvm-py/src/main/resources/META-INF/elide/embedded/runtime/python/preamble.py
PY_MODULE_BIN ?= tools/runtime/bazel-bin/elide/runtime/python/py.modules.tar
PY_MODULE_OUT ?= packages/graalvm-py/src/main/resources/META-INF/elide/embedded/runtime/python/py.modules.tar

POSIX_FLAGS ?=
GRADLE_OPTS ?=
GRADLE_ARGS ?= -Pversions.java.language=$(JVM)
BUILD_ARGS ?=
NATIVE_TASKS ?= nativeCompile
DEP_HASH_ALGO ?= sha256,sha512,pgp
ARGS ?=

ifneq ($(REPOSITORY),$(DEFAULT_REPOSITORY))
PUBLISH_PROPS ?= -Pelide.publish.repo.maven.auth=true -Pelide.publish.repo.maven=$(REPOSITORY)
else
PUBLISH_PROPS ?=
endif

LOCAL_CLI_INSTALL_DIR ?= ~/bin

HASHSUM_SIZE ?= 256
HASHSUM_CLASS ?= sha
HASHSUM_ALGORITHM ?= $(HASHSUM_CLASS)$(HASHSUM_SIZE)
HASHSUM ?= shasum -a $(HASHSUM_SIZE)

ifeq ($(SAMPLES),yes)
BUILD_ARGS += -PbuildSamples=true
else
BUILD_ARGS += -PbuildSamples=false
endif

ifeq ($(NATIVE),yes)
BUILD_ARGS += -Pelide.march=native
endif

ifeq ($(WASM),yes)
BUILD_ARGS += -PbuildWasm=true -Pelide.build.kotlin.wasm.disable=false
else
BUILD_ARGS += -PbuildWasm=false -Pelide.build.kotlin.wasm.disable=true
endif

ifeq ($(RELOCK),yes)
BUILD_ARGS += --write-verification-metadata $(DEP_HASH_ALGO) --write-locks
endif

ifeq ($(CI),yes)
BUILD_ARGS += -Pelide.ci=true
endif

ifeq ($(STRICT),yes)
BUILD_ARGS += --warning-mode=none -Pelide.strict=true
endif

ifeq ($(SCAN),yes)
BUILD_ARGS += --scan
endif

CLI_TASKS ?=

ifeq ($(CHECK),yes)
CLI_TASKS += check
endif

ifeq ($(RELEASE),yes)
BUILD_MODE ?= release
BAZEL_MODE ?= opt
SIGNING ?= yes
SIGSTORE ?= yes
NATIVE_TARGET_NAME ?= nativeOptimizedCompile
CLI_DISTPATH ?= ./packages/cli/build/dist/release
BUILD_ARGS += -Pelide.buildMode=prod -Pelide.stamp=true -Pelide.release=true -Pelide.strict=true
CLI_RELEASE_TARGETS ?= cli-local cli-release-artifacts
else
ifeq ($(BUILD_MODE),debug)
BUILD_MODE ?= debug
BAZEL_MODE ?= debug
else
BUILD_MODE ?= dev
BAZEL_MODE ?= fastbuild
endif
CLI_DISTPATH ?= ./packages/cli/build/dist/debug
NATIVE_TARGET_NAME ?= nativeCompile
CLI_RELEASE_TARGETS ?= cli-local
endif

ifeq ($(SIGNING),yes)
SIGNING_ON = true
else
SIGNING_ON = false
endif

ifeq ($(SIGSTORE),yes)
SIGSTORE_ON = true
else
SIGSTORE_ON = false
endif

OMIT_NATIVE ?= -x nativeCompile -x nativeTest -x nativeOptimizedCompile

ifeq ($(BUILD_NATIVE_IMAGE),yes)
ifeq ($(RELEASE),yes)
CLI_TASKS += :packages:cli:nativeOptimizedCompile -Pelide.buildMode=release -Pelide.release=true -PenableSigning=true -PbuildDocs=true
else
CLI_TASKS += :packages:cli:nativeCompile
endif
endif

ifneq ($(VERBOSE),)
RULE ?=
POSIX_FLAGS +=v
GRADLE_LOGS ?= info
GRADLE_ARGS += --$(GRADLE_LOGS)
else
RULE ?= @
endif

ifeq ($(IGNORE_ERRORS),yes)
RULE += -
endif

ifeq ($(DRY),yes)
CMD ?= $(RULE)echo
CMD += " "
else
CMD ?= $(RULE)
endif

ifeq ($(CUSTOM_GVM),yes)
# setup a custom JVM prefix for graalvm (coming soon)
endif

RUNTIME_GEN = $(JS_MODULE_OUT) $(PY_FACADE_OUT) $(PY_MODULE_OUT)

GRADLE_OMIT ?= $(OMIT_NATIVE)
_ARGS ?= $(GRADLE_ARGS) $(BUILD_ARGS) $(ARGS)
DEPS ?= node_modules/ third-party

GRADLE_PREFIX ?= JAVA_HOME="$(JAVA_HOME)" GRAALVM_HOME="$(GRAALVM_HOME)" PATH="$(PATH)" CC="" CXX="" CFLAGS="" LDFLAGS=""

ifeq ($(RELEASE),yes)
EXTRA_RUSTC_FLAGS ?=
else
ifeq ($(COVERAGE),yes)
EXTRA_RUSTC_FLAGS ?=-Cinstrument-coverage
endif
endif

ifneq ($(EXTRA_RUSTC_FLAGS),)
RUSTC_FLAGS = "$(RUSTC_FLAGS) $(EXTRA_RUSTC_FLAGS)"
endif

# ---------------------------------------------------------------------------------------------------------------------
# Targets

all: build

dependency-packages:  ## Print the suite of dependencies to install for this OS.
ifeq ($(OS),Darwin)
	@printf "$(MACOS_PKGS)"
else
	@printf "$(LINUX_PKGS)"
endif

setup: $(DEPS) setup-env  ## Setup development pre-requisites.

setup-env: ./.env

./.env:
	cp -fv ./config/env ./.env

build: $(DEPS)  ## Build the main library, and code-samples if SAMPLES=yes.
	$(info Building Elide $(VERSION)...)
ifeq ($(BUILD_MODE),release)
	$(CMD)$(CARGO) build --release
else
	$(CARGO) build
endif
	$(CMD)$(GRADLE_PREFIX) $(GRADLE) build $(CLI_TASKS) $(GRADLE_OMIT) $(_ARGS)

native: $(DEPS)  ## Build Elide's native image target; use BUILD_MODE=release for a release binary.
	$(info Building Elide native $(VERSION) ($(BUILD_MODE))...)
ifeq ($(BUILD_MODE),release)
	$(CMD)$(GRADLE_PREFIX) $(GRADLE) :packages:cli:nativeOptimizedCompile $(_ARGS)
else
	$(CMD)$(GRADLE_PREFIX) $(GRADLE) :packages:cli:nativeCompile $(_ARGS)
endif

natives-test: $(DEPS)  ## Run Cargo and native tests, optionally buildin coverage if COVERAGE=yes.
ifeq ($(COVERAGE),yes)
	$(info Running native tests (+coverage)...)
	$(CMD)$(BASH) ./tools/scripts/cargo-test-coverage.sh \
		&& $(BASH) ./tools/scripts/cargo-coverage-report.sh
else
	$(info Running native tests...)
	$(CMD)$(CARGO) test
endif

natives-coverage:  ## Show the current native coverage report; only run if `natives-test` is run first.
	$(info Opening coverage report...)
	$(CMD)$(BASH) ./tools/scripts/cargo-coverage-show.sh

test: $(DEPS)  ## Run the library testsuite, and code-sample tests if SAMPLES=yes.
	$(info Running testsuite...)
	$(CMD)$(MAKE) natives-test
	@#we need the debug libs for the tests, which do not produce them; so we re-build them here, but with flags aligned to
	@#what coverage would add, to avoid a significant invalidation and re-build.
	$(CMD)RUSTFLAGS="-C instrument-coverage" $(CARGO) build -p sqlite
	$(CMD)$(GRADLE_PREFIX) $(GRADLE) test $(_ARGS) -x detekt -x spotlessCheck -x apiCheck
	@#since we are running using instrumented code, profiles may be written; delete them after running tests
	$(CMD)-$(FIND) ./packages -name "*.profraw" -delete

check: $(DEPS)  ## Build all targets, run all tests, run all checks.
	$(info Running testsuite...)
	$(CMD)$(MAKE) natives-test
	$(CMD)RUSTFLAGS="-C instrument-coverage" $(CARGO) build -p sqlite
	$(CMD)$(GRADLE_PREFIX) $(GRADLE) build test check $(_ARGS)
	$(CMD)$(CARGO) clippy
	$(CMD)$(PNPM) run prettier --check .
	$(CMD)$(PNPM) biome format .
	$(CMD)$(PNPM) biome check .
	$(CMD)$(CARGO) fmt -- --check
	$(CMD)$(RUFF) check packages

format:  ## Alias for `make fmt`.
	$(CMD)$(MAKE) fmt

fmt:  ## Run all formatter tools.
	$(info Running formatters...)
	$(CMD)$(CARGO) fmt
	$(CMD)$(PNPM) run prettier --write .
	$(CMD)$(PNPM) biome format --write .
	$(CMD)$(RUFF) format packages

gvm: .graalvm-home  ## Build a custom copy of GraalVM for use locally.
	@echo "GraalVM is ready (profile: $(GVM_PROFILE))."

.graalvm-home:
	$(info Building GraalVM...)
	$(CMD)$(MAKE) -C third_party $(GVM_PROFILE)

publish-substrate:
	$(info Publishing Elide Substrate "$(VERSION)"...)
	$(CMD)$(GRADLE_PREFIX) $(GRADLE) \
		publishSubstrate \
		--no-daemon \
		--warning-mode=none \
		--no-configuration-cache \
		-Pversion=$(VERSION) \
		-PbuildSamples=false \
		-PbuildDocs=true \
		-PenableSigning=$(SIGNING_ON) \
		-PenableSigstore=$(SIGSTORE_ON) \
		-Pelide.stamp=true \
		-Pelide.release=true \
		-Pelide.buildMode=release \
		$(PUBLISH_PROPS) \
		-x test \
		-x jvmTest \
		-x jsTest;

publish-framework:
	$(info Publishing Elide Framework "$(VERSION)"...)
	$(CMD)$(GRADLE_PREFIX) $(GRADLE) \
		publishElide \
		--no-daemon \
		--warning-mode=none \
		--no-configuration-cache \
		-Pversion=$(VERSION) \
		-PbuildSamples=false \
		-PbuildDocs=true \
		-PenableSigning=$(SIGNING_ON) \
		-PenableSigstore=$(SIGSTORE_ON) \
		-Pelide.stamp=true \
		-Pelide.release=true \
		-Pelide.buildMode=release \
		$(PUBLISH_PROPS) \
		-x test \
		-x jvmTest \
		-x jsTest;

publish-bom:
	$(info Publishing Elide BOM "$(VERSION)"...)
	$(CMD)$(GRADLE_PREFIX) $(GRADLE) \
		publishBom \
		--no-daemon \
		--warning-mode=none \
		--no-configuration-cache \
		-Pversion=$(VERSION) \
		-PbuildSamples=false \
		-PbuildDocs=true \
		-PenableSigning=$(SIGNING_ON) \
		-PenableSigstore=$(SIGSTORE_ON) \
		-Pelide.stamp=true \
		-Pelide.release=true \
		-Pelide.buildMode=release \
		$(PUBLISH_PROPS) \
		-x test \
		-x jvmTest \
		-x jsTest;

publish: publish-substrate publish-framework publish-bom  ## Publish a new version of all Elide packages.
	@echo "Elide version '$(VERSION)' published.";

ifneq ($(RELEASE),yes)
release:
	@echo "To perform a release, unlock the release gate with RELEASE=yes. Make sure \`.version\` is up-to-date."
else
release:  ## Perform a full release, including publishing to Maven Central and the Elide repository.
	@echo "Releasing version '$(VERSION)'..."
	$(CMD)$(CP) -f .version .release
	$(CMD)$(MAKE) publish RELEASE=yes
	@echo "Release complete. Please commit changes to the '.release' file."
endif

clean-cli:  ## Clean built CLI targets.
	$(CMD)echo "Cleaning CLI targets..."
	$(CMD)$(RM) -fr$(strip $(POSIX_FLAGS)) ./packages/cli/build/dist

cli:  ## Build the Elide command-line tool (native target).
	$(info Building Elide CLI tool...)
	$(CMD)mkdir -p $(CLI_DISTPATH)
	$(CMD)$(GRADLE_PREFIX) $(GRADLE) \
		:packages:cli:$(NATIVE_TARGET_NAME) \
		-Pversion=$(VERSION) \
		-PbuildSamples=false \
		-PbuildDocs=false \
		-Pelide.buildMode=$(BUILD_MODE) \
		-x test \
		$(_ARGS);
	$(CMD)$(MAKE) cli-distroot cli-sizereport

cli-distroot:
	@echo "Built Elide CLI binary. Creating distroot..."
	@$(MKDIR) -p $(CLI_DISTPATH) \
		&& $(CP) ./packages/cli/build/native/$(NATIVE_TARGET_NAME)/elide $(CLI_DISTPATH)/elide;
	$(CMD)cd $(CLI_DISTPATH) && du -h elide;
	@echo "Compressing...";
	$(CMD)cd $(CLI_DISTPATH) \
		&& $(GZIP) --best --keep --verbose elide \
		&& $(BZIP2) --best --keep --verbose elide \
		&& $(XZ) --best --keep --verbose elide \
		&& $(ZSTD) --ultra -22 -k -v elide \
		&& $(ZIP) -9 --verbose elide.zip elide;

cli-sizereport:
	@echo "\nSize report:" \
		&& cd $(CLI_DISTPATH) && du -h elide* \
		&& echo "\nChecksums:" \
		&& $(HASHSUM) elide* \
		&& echo "";

cli-local: cli  ## Build the Elide command line tool and install it locally (into ~/bin, or LOCAL_CLI_INSTALL_DIR).
ifeq ($(RELEASE),yes)
	$(CMD)$(MAKE) clean-cli
endif
	$(CMD)$(MAKE) cli-install-local

ifeq ($(BUILD_MODE),release)
UMBRELLA_TARGET=release
else
UMBRELLA_TARGET=debug
endif

UMBRELLA_TARGET_PATH = target/$(UMBRELLA_TARGET)

cflags-base:
	@cat $(ELIDE_ROOT)/tools/cflags/base | xargs

cflags-gcc:
	@cat $(ELIDE_ROOT)/tools/cflags/base $(ELIDE_ROOT)/tools/cflags/gcc | xargs 2> /dev/null

cflags-clang:
	@cat $(ELIDE_ROOT)/tools/cflags/gcc $(ELIDE_ROOT)/tools/cflags/clang | xargs 2> /dev/null

cflags:  ## Generate cflags for assigned target (pass COMPILER=clang for clang).
	@cat $(ELIDE_ROOT)/tools/cflags/base $(ELIDE_ROOT)/tools/cflags/$(COMPILER) | xargs 2> /dev/null

env:  ## Show a summary of the build environment.
	@echo "Elide Build Environment:"
	@echo "- OS: $(OS)"
	@echo "- Arch: $(ARCH)"
	@echo "- Compiler: $(COMPILER)"
	@echo "- Release: $(RELEASE)"
	@echo "- Native: $(NATIVE)"
	@echo "---------------------------------------------"
	@echo "rustc:" && $(RUSTC) -vV && echo ""
	@echo "javac:" && $(JAVA) -version && echo ""
	@echo "clang:" && $(CLANG) -v && echo ""
	@echo "lld:  " && $(LLD) -v && echo ""
	@$(MAKE) cenv

cenv:  ## Show the C compiler environment.
	@echo "CC=$(CC)"
	@echo "CXX=$(CXX)"
	@echo "LD=$(LD)"
	@echo "COMPILER=$(COMPILER)"
	@echo "CFLAGS=\"$(shell cat $(ELIDE_ROOT)/tools/cflags/base $(ELIDE_ROOT)/tools/cflags/$(COMPILER) | xargs 2> /dev/null)\""

umbrella: third-party $(UMBRELLA_TARGET_PATH)  ## Build the native umbrella tooling library.

$(UMBRELLA_TARGET_PATH): third_party/lib
	$(info Building tools/umbrella...)
ifeq ($(BUILD_MODE),release)
	$(CMD)$(CARGO) build --release
else
	$(CARGO) build
endif

ifeq ($(OS),Darwin)
ifeq ($(ARCH),arm64)
RUSTC_TARGET=aarch64-apple-darwin
else
RUSTC_TARGET=x86_64-apple-darwin
endif
else
ifeq ($(OS),Linux)
ifeq ($(ARCH),arm64)
RUSTC_TARGET=armv7-unknown-linux-gnueabihf
else
RUSTC_TARGET=x86_64-unknown-linux-gnu
endif
else
RUSTC_TARGET=$(OS)-$(ARCH)
endif
endif

ifeq ($(RELEASE),yes)
CARGO_FLAGS += --release
endif

clean-natives:  ## Clean local native targets.
	@echo "Cleaning native targets..."
	$(CMD)$(CARGO) clean
	$(CMD)rm -fr target
	$(CMD)$(MAKE) -C third_party clean

natives: $(DEPS)  ## Rebuild natives (C/C++ and Rust).
ifeq ($(BUILD_STDLIB),yes)
	@echo "" && echo "Building Rust stdlib (mode: $(BUILD_MODE), native: $(NATIVE))"
	$(CMD)$(CARGO) run +nightly -Zbuild-std --target $(RUSTC_TARGET)
endif
	@echo "" && echo "Building Elide crates (mode: $(BUILD_MODE), native: $(NATIVE), target: $(RUSTC_TARGET), config: $(RUST_CONFIG))"
	$(CMD)$(CARGO) $(RUST_CONFIG_FLAGS) build $(CARGO_FLAGS) --target $(RUSTC_TARGET) 

third-party: third_party/sqlite third_party/lib  ## Build all third-party embedded projects.

third_party/sqlite:
	@echo "Setting up submodules..."
	$(CMD)mkdir -p $(TARGET_ROOT)
	$(CMD)$(GIT) submodule update --init --recursive

third_party/lib:
	@echo "Building third-party projects..."
	$(CMD)mkdir -p $(TARGET_ROOT)
	$(CMD)$(MAKE) RELOCK=$(RELOCK) -C third_party && mkdir -p third_party/lib
	@echo ""

cli-release-artifacts:
	$(CMD)echo "Building release artifacts..." \
		&& cd $(CLI_DISTPATH) \
		&& $(HASHSUM) elide elide* > manifest.txt \
		&& $(HASHSUM) elide | cut -d " " -f 1 > elide.$(HASHSUM_ALGORITHM) \
		&& $(HASHSUM) elide.bz2 | cut -d " " -f 1 > elide.bz2.$(HASHSUM_ALGORITHM) \
		&& $(HASHSUM) elide.gz | cut -d " " -f 1 > elide.gz.$(HASHSUM_ALGORITHM) \
		&& $(HASHSUM) elide.xz | cut -d " " -f 1 > elide.xz.$(HASHSUM_ALGORITHM) \
		&& $(HASHSUM) elide.zst | cut -d " " -f 1 > elide.xz.$(HASHSUM_ALGORITHM) \
		&& $(HASHSUM) elide.zip | cut -d " " -f 1 > elide.zip.$(HASHSUM_ALGORITHM) \
		&& $(HASHSUM) manifest.txt | cut -d " " -f 1 > manifest.txt.$(HASHSUM_ALGORITHM) \
		&& $(TAR) -cvf elide.tar elide elide.sha256 $(SIGNATURE_FILE) \
		&& $(GZIP) --best --verbose elide.tar;
ifeq ($(SIGNING),yes)
	$(CMD)echo "Signing release artifacts..." \
		&& cd $(CLI_DISTPATH) \
		&& $(GPG2) --default-key "$(SIGNING_KEY)" --armor --detach-sign --output elide.asc elide;
else
	@echo "Skipping signatures: signing is disabled.";
endif
	$(CMD)echo "Building release descriptor..." \
		&& $(HASHSUM) $(CLI_DISTPATH)/manifest.txt | cut -d " " -f 1 > $(CLI_DISTPATH)/manifest.txt.$(HASHSUM_ALGORITHM) \
		&& MANIFEST_FINGERPRINT=$$(cat $(CLI_DISTPATH)/manifest.txt.$(HASHSUM_ALGORITHM)) \
		&& RELEASE_SYSTEM=$$(echo "$(SYSTEM)" | tr A-Z a-z) \
		&& RELEASE_ARCH=$$(uname -m | tr A-Z a-z) \
		&& cat ./packages/cli/packaging/release.json | \
			$(JQ) ".version = \"$(VERSION)\"" | \
			$(JQ) ".platform = \"$${RELEASE_SYSTEM}\"" | \
			$(JQ) ".fingerprint = \"$${MANIFEST_FINGERPRINT}\"" > $(CLI_DISTPATH)/release.json \
		&& $(HASHSUM) $(CLI_DISTPATH)/release.json | cut -d " " -f 1 > $(CLI_DISTPATH)/release.json.$(HASHSUM_ALGORITHM) \
		&& echo "Release manifest built for version '$(VERSION)'.";
ifeq ($(SIGNING),yes)
	$(CMD)echo "Signing release descriptor..." \
		&& cd $(CLI_DISTPATH) \
		&& cat release.json.sha256 manifest.txt.sha256 | $(GPG2) --default-key "$(SIGNING_KEY)" --armor --detach-sign --output release.asc;
endif
	$(CMD)echo "Building final release tarball..." \
		&& cd $(CLI_DISTPATH) \
		&& $(TAR) -cvf release.tar elide elide.* manifest.txt release.json;

cli-release: cli  ## Build an Elide command-line release.
	$(CMD)make $(CLI_RELEASE_TARGETS) RELEASE=yes

cli-install-local:
	@echo "Installing CLI locally (location: \"$(LOCAL_CLI_INSTALL_DIR))\"..."
	$(CMD)$(CP) -f$(strip $(POSIX_FLAGS)) \
		./packages/cli/build/native/$(NATIVE_TARGET_NAME)/elide \
		$(LOCAL_CLI_INSTALL_DIR)/elide
	@echo ""; echo "Done. Testing CLI tool..."
	$(CMD)elide --version

clean: clean-docs clean-natives  ## Clean build outputs and caches.
	@echo "Cleaning targets..."
	$(CMD)$(RM) -fr$(strip $(POSIX_FLAGS)) $(TARGET)
	$(CMD)$(FIND) . -name .DS_Store -delete
	$(CMD)$(GRADLE_PREFIX) $(GRADLE) clean cleanTest $(_ARGS)

clean-docs:  ## Clean documentation targets.
	@echo "Cleaning docs..."
	$(CMD)$(RM) -fr$(strip $(POSIX_FLAGS))

docs: $(DOCS) $(TARGET)/docs  ## Generate docs for all library modules.

model:  ## Build proto model targets.
	@echo "Building proto model..."
	$(CMD)$(BUF) lint
	@echo "- Building binary model..."
	$(CMD)$(BUF) build -o proto/buf.pb.bin
	@echo "- Building JSON model..."
	$(CMD)$(BUF) build -o proto/buf.pb.json.gz
	@echo "Model build complete."

model-update:  ## Update the proto model and re-build it.
	@echo "Updating proto model..."
	$(CMD)cd proto && $(BUF) mod update
	$(CMD)$(MAKE) model

$(TARGET)/docs:
	@echo "Generating docs..."
	$(CMD)$(RM) -fr$(strip $(POSIX_FLAGS))
	$(CMD)$(GRADLE_PREFIX) $(GRADLE) docs dokkaHtmlMultiModule $(_ARGS) -PbuildDocs=true --no-configuration-cache -x htmlDependencyReport
	@echo "Docs build complete."

api-check:  ## Check API/ABI compatibility with current changes.
	$(info Checking ABI compatibility...)
	$(CMD)$(GRADLE_PREFIX) $(GRADLE) apiCheck -PbuildSamples=false -PbuildDocs=false

reports: $(REPORTS)  ## Generate reports for tests, coverage, etc.

$(REPORTS):
	@echo "Generating reports..."
	$(CMD)$(GRADLE_PREFIX) $(GRADLE) \
		:reports \
		:tools:reports:reports \
		-x nativeCompile \
		-x test \
		-Pversions.java.language=$(JVM)
	$(CMD)$(MKDIR) -p $(REPORTS)
	@echo "Copying merged reports to '$(REPORTS)'..."
	$(CMD)-cd $(TARGET)/reports && $(CP) -fr$(strip $(POSIX_FLAGS)) ./* $(REPORTS)/
	@echo "Copying test reports to '$(REPORTS)'..."
	$(CMD)$(MKDIR) -p tools/reports/build/reports
	$(CMD)cd tools/reports/build/reports && $(CP) -fr$(strip $(POSIX_FLAGS)) ./* $(REPORTS)/
	$(CMD)$(RM) -f docs/reports/project/properties.txt docs/reports/project/tasks.txt
	@echo "Reports synced."

update-dep-hashes:
	@echo "- Updating dependency hashes..."
	$(CMD)$(GRADLE_PREFIX) $(GRADLE) \
		-Pversions.java.language=$(JVM) \
		--write-verification-metadata $(DEP_HASH_ALGO)
	@echo "Dependency hashes updated."

update-dep-locks:
	@echo "- Updating dependency locks (yarn)..."
	$(CMD)$(YARN)
	@echo ""
	@echo "- Updating dependency locks (gradle)..."
	$(CMD)$(GRADLE_PREFIX) $(GRADLE) resolveAndLockAll --write-locks
	@echo "Dependency locks updated."

update-deps:  ## Perform interactive dependency upgrades across Yarn and Gradle.
	@echo "Upgrading dependencies..."
	$(CMD)$(MAKE) update-jsdeps
	@echo ""
	$(CMD)$(MAKE) update-jdeps

update-jsdeps:  ## Interactively update Yarn dependencies.
	@echo "Running interactive update for Gradle..."
	$(CMD)$(GRADLE_PREFIX) $(GRADLE) upgrade-gradle

update-jdeps:  ## Interactively update Gradle dependencies.
	@echo "Running interactive update for Gradle..."
	$(CMD)$(GRADLE_PREFIX) $(GRADLE) upgrade-gradle

relock-deps:  ## Update dependency locks and hashes across Yarn and Gradle.
	@echo "Relocking dependencies..."
	$(CMD)$(MAKE) update-dep-hashes update-dep-locks

serve-docs:  ## Serve documentation locally.
	@echo "Serving docs at http://localhost:8000..."
	$(CMD)cd docs \
		&& open http://localhost:8000 \
		&& python3 -m http.server

IMAGES ?= image-base image-base-alpine image-native image-native-alpine image-bash image-elide

images: $(IMAGES)  ## Build all Docker images.
	@echo "All Docker images built."

image-base:  ## Build base Ubuntu image.
	@echo "Building image 'base'..."
	$(CMD)$(MAKE) -C tools/images/base PUSH=$(PUSH) REMOTE=$(REMOTE)

image-bash:  ## Build bash Ubuntu image.
	@echo "Building image 'bash'..."
	$(CMD)$(MAKE) -C tools/images/bash PUSH=$(PUSH) REMOTE=$(REMOTE)

image-elide:  ## Build the Elide-only Ubuntu image.
	@echo "Building image 'elide'..."
	$(CMD)$(MAKE) -C tools/images/elide PUSH=$(PUSH) REMOTE=$(REMOTE)

image-base-alpine:  ## Build base Alpine image.
	@echo "Building image 'base/alpine'..."
	$(CMD)$(MAKE) -C tools/images/base-alpine PUSH=$(PUSH) REMOTE=$(REMOTE)

image-native:  ## Build native Ubuntu base image.
	@echo "Building image 'native'..."
	$(CMD)$(MAKE) -C tools/images/native PUSH=$(PUSH) REMOTE=$(REMOTE)

image-native-alpine:  ## Build native Alpine base image.
	@echo "Building image 'native-alpine'..."
	$(CMD)$(MAKE) -C tools/images/native-alpine PUSH=$(PUSH) REMOTE=$(REMOTE)

image-dist:  ## Build distribution builder image.
	@echo "Building image 'dist'..."
	$(CMD)$(MAKE) -C tools/images/dist PUSH=$(PUSH) REMOTE=$(REMOTE)

build-deb:  ## Build a Debian package for Elide.
	@echo "Building Debian package for Elide..."
	$(CMD)bash ./tools/scripts/release/build-deb.sh

sign-deb:  ## Sign a Debian package for Elide.
	@echo "Signing Debian package for Elide..."
	$(CMD)bash ./tools/scripts/release/sign-deb.sh

build-test-deb:  ## Test the Debian package for Elide.
	@echo "Testing Debian package for Elide..."
	$(CMD)$(DOCKER) buildx build -f ./packages/cli/packaging/deb/Dockerfile . -t elide-deb

test-deb: build-test-deb  ## Run the Debian test image.
	@echo "Running Debian test image for Elide..."
	$(CMD)$(DOCKER) run --rm -it -v $(PWD):/elide elide-deb elide --version

run-test-deb: build-test-deb  ## Run the Debian test image with Bash.
	@echo "Running Debian test image for Elide..."
	$(CMD)$(DOCKER) run --rm -it -v $(PWD):/elide elide-deb bash

build-rpm:  ## Build a RPM package for Elide.
	@echo "Building RPM package for Elide..."
	$(CMD)bash ./tools/scripts/release/build-rpm.sh

sign-rpm:  ## Sign a RPM package for Elide.
	@echo "Signing RPM package for Elide..."
	$(CMD)bash ./tools/scripts/release/sign-rpm.sh

build-test-rpm:  ## Test the RPM package for Elide.
	@echo "Testing RPM package for Elide..."
	$(CMD)$(DOCKER) buildx build -f ./packages/cli/packaging/rpm/Dockerfile . -t elide-rpm

test-rpm: build-test-rpm  ## Run the RPM test image.
	@echo "Running RPM test image for Elide..."
	$(CMD)$(DOCKER) run --rm -it -v $(PWD):/elide elide-rpm elide --version

run-test-rpm: build-test-rpm  ## Run the RPM test image with Bash.
	@echo "Running RPM test image for Elide..."
	$(CMD)$(DOCKER) run --rm -it -v $(PWD):/elide elide-rpm bash

RELEASE_RESOURCES_TGZ = packages/cli/build/native/nativeOptimizedCompile/resources.tgz

# special case: apk requires `resources.tgz`
$(RELEASE_RESOURCES_TGZ):
	@echo "Building resources.tgz for Alpine package..."
	$(CMD)cd packages/cli/build/native/nativeOptimizedCompile \
		&& $(TAR) -cf resources.tar resources \
		&& $(GZIP) --best --verbose resources.tar \
		&& mv resources.tar.gz resources.tgz

build-apk: $(RELEASE_RESOURCES_TGZ)  ## Build an Alpine package for Elide.
	@echo "Building Alpine package for Elide..."
	$(CMD)bash ./tools/scripts/release/build-apk.sh

sign-apk:  ## Sign a Alpine package for Elide.
	@echo "Signing Alpine package for Elide..."
	$(CMD)bash ./tools/scripts/release/sign-apk.sh

build-test-apk:  ## Test the Alpine package for Elide.
	@echo "Testing Alpine package for Elide..."
	$(CMD)$(DOCKER) buildx build -f ./packages/cli/packaging/apk/Dockerfile . -t elide-apk

test-apk: build-test-apk  ## Run the Alpine test image.
	@echo "Running Alpine test image for Elide..."
	$(CMD)$(DOCKER) run --rm -it -v $(PWD):/elide elide-apk elide --version

run-test-apk: build-test-apk  ## Run the Alpine test image with Bash.
	@echo "Running Alpine test image for Elide..."
	$(CMD)$(DOCKER) run --rm -it -v $(PWD):/elide elide-apk bash

build-dists:  ## Build all binary distributions for a newly-minted native image.
	@echo "Building all distributions..."
	$(CMD)$(MAKE) build-deb
	$(CMD)$(MAKE) build-rpm
	$(CMD)$(MAKE) build-apk
	@echo "All distributions built (apk, deb, rpm)."

node_modules/:
	$(info Installing NPM dependencies...)
	$(CMD)$(PNPM) install --strict-peer-dependencies --frozen-lockfile

distclean: clean  ## DANGER: Clean and remove any persistent caches. Drops changes.
	@echo "" && echo "Cleaning dependencies..."
	$(CMD)$(MAKE) -C third_party clean
	$(CMD)$(CARGO) clean
	$(CMD)cd runtime && $(BAZEL) clean
	$(CMD)$(RM) -fr node_modules .graalvm-home .java-home
	@echo "" && echo "Cleaning caches..."
	$(CMD)$(RM) -fr$(strip $(POSIX_FLAGS)) kotlin-js-store .buildstate.tar.gz .dev node_modules target
	@echo "" && echo "Cleaning runtime facade..."
	$(CMD)-cd runtime && $(BAZEL) clean --expunge
	@echo "" && echo "Dist-cleaning third-party code..."
	$(CMD)$(MAKE) -C third_party distclean
	@echo "Dist-clean complete."

forceclean: distclean  ## DANGER: Clean, distclean, and clear untracked files.
	@echo "Resetting codebase..."
	$(CMD)$(GIT) reset --hard
	@echo "Cleaning untracked files..."
	$(CMD)$(GIT) clean -xdf

help:  ## Show this help text ('make help').
	$(info Elide:)
	@grep -E '^[a-z1-9A-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'

info:  ## Show info about the current codebase and toolchain.
	@echo "Elide version: $(VERSION)"
	@echo "Build mode: $(BUILD_MODE)"
	@echo "JVM target: $(JVM)"
	@echo "Java Home: $(JAVA_HOME)"
	@echo "GraalVM Home: $(GRAALVM_HOME)"
	@echo "" && echo "---- OS Info ----------------------------------------------"
	@echo "- System: $(SYSTEM)"
	@echo "- Kernel: $(shell uname -r)"
	@echo "- Hostname: $(shell hostname)"
	@echo "" && echo "---- Toolchain --------------------------------------------"
	@echo "- Java: $(JAVA)"
	@echo "- Native Image: $(NATIVE_IMAGE)"
	@echo "- Python: $(PYTHON)"
	@echo "- Ruby: $(RUBY)"
	@echo "- Bun: $(BUN)"
	@echo "- Node: $(NODE)"
	@echo "- Rust: $(RUSTC)"
	@echo "- Cargo: $(CARGO)"
	@echo "- Clang: $(CLANG)"
	@echo "- LLD: $(LLD)"
	@echo "- Bolt: $(BOLT)"
	@echo "- Yarn: $(YARN)"
	@echo "- Bazel: $(BAZEL)"
	@echo "- Buf: $(BUF)"
	@echo "- PNPM: $(PNPM)"
	@echo "" && echo "---- Toolchain Versions -----------------------------------"
	@echo "Java:"
	$(CMD)$(JAVA) -version
	@echo ""
	@echo "Native Image:"
	$(CMD)$(NATIVE_IMAGE) --version
	@echo ""
	@echo "Python:"
	$(CMD)-$(PYTHON) --version
	@echo ""
	@echo "Ruby:"
	$(CMD)-$(RUBY) --version
	@echo ""
	@echo "Rust:"
	$(CMD)-$(RUSTC) -vV
	@echo ""
	@echo "Cargo:"
	$(CMD)-$(CARGO) --version
	@echo ""
	@echo "Clang:"
	$(CMD)-$(CLANG) --version
	@echo ""
	@echo "LLD:"
	$(CMD)-$(LLD) --version
	@echo ""
	@echo "Bolt:"
	$(CMD)-$(BOLT) --version
	@echo "Yarn:"
	$(CMD)-$(YARN) --version
	@echo ""
	@echo "Bazel:"
	$(CMD)-$(BAZEL) --version
ifneq ($(BUN),)
	@echo ""
	@echo "Bun:"
	$(CMD)$(BUN) --version
endif
	@echo ""
	@echo "Buf:"
	$(CMD)$(BUF) --version
	@echo ""
	@echo "Node:"
	$(CMD)$(NODE) --version
	@echo ""
	@echo "PNPM:"
	$(CMD)$(PNPM) --version
	@echo "" && echo "---- Utilities --------------------------------------------"
	@echo "- BASH: $(BASH)"
	@echo "- BUF: $(BUF)"
	@echo "- GPG2: $(GPG2)"
	@echo "- JQ: $(JQ)"
	@echo "- LLVM_COV: $(LLVM_COV)"
	@echo "- LLVM_PROFDATA: $(LLVM_PROFDATA)"
  @echo "- LLVM_CONFIG: $(LLVM_CONFIG)"
	@echo "- TAR: $(TAR)"
	@echo "- UNZIP: $(UNZIP)"
	@echo "- XZ: $(XZ)"
	@echo "- ZIP: $(ZIP)"
	@echo "- ZSTD: $(ZSTD)"
	@echo "" && echo "---- Environment ------------------------------------------"
	@echo "- CC: $(shell echo $$CC)"
	@echo "- CXX: $(shell echo $$CXX)"
	@echo "- LD: $(shell echo $$LD)"
	@echo "- CFLAGS: $(shell echo $$CFLAGS)"
	@echo "- LDFLAGS: $(shell echo $$LDFLAGS)"
	@echo "- CPPFLAGS: $(shell echo $$CPPFLAGS)"
	@echo "- JAVA_HOME: $(shell echo $$JAVA_HOME)"
	@echo "- GRAALVM_HOME: $(shell echo $$GRAALVM_HOME)"
	@echo "- RUSTFLAGS: $(shell echo $$RUSTFLAGS)"
	@echo "- PATH: $(shell echo $$PATH)"

# ---- Runtime submodule ---- #
# Note: make sure the Git submodule is up to date by running `git submodule update [--init] runtime`

runtime: tools/runtime/WORKSPACE $(RUNTIME_GEN) ## Build and update the JS runtime if needed.

tools/runtime/WORKSPACE:
	@echo "Setting up submodules..."
	$(CMD)$(GIT) submodule update --init --recursive

runtime-build: tools/runtime/bazel-bin ## Build the JS runtime facade and the builtin modules bundle

$(RUNTIME_GEN): tools/runtime/bazel-bin

tools/runtime/bazel-bin:
	@echo "" && echo "Building runtime facades..."
	$(CMD)cd tools/runtime && $(BAZEL) build -c $(BAZEL_MODE) //...
	$(CMD)$(MAKE) runtime-update-copy

runtime-update: runtime-build $(RUNTIME_GEN) ## Rebuild and copy the JS runtime facade

runtime-clean:  ## Clean generated runtime artifacts.
	@echo "" && echo "Cleaning runtime facades..."
	$(CMD)rm -fv$(POSIX_FLAGS) $(JS_FACADE_BIN) $(JS_FACADE_OUT) $(JS_POLYFILLS_BIN) $(JS_POLYFILLS_OUT) $(JS_MODULE_BIN) $(JS_MODULE_OUT) $(PY_MODULE_BIN) $(PY_MODULE_OUT)

runtime-update-copy:
	@echo "" && echo "Updating runtime artifacts..."
	@echo "- Updating 'polyfills.js'"
	$(CMD)cp -f$(POSIX_FLAGS) $(JS_POLYFILLS_BIN) $(JS_POLYFILLS_OUT)
	@echo "- Updating 'js.vfs.tar'"
	$(CMD)cp -f$(POSIX_FLAGS) $(JS_MODULE_BIN) $(JS_MODULE_OUT)
	@echo "- Updating 'py.modules.tar.'"
	@$(CMD)cp -f$(POSIX_FLAGS) $(PY_MODULE_BIN) $(PY_MODULE_OUT)

.PHONY: all docs build test clean distclean forceclean docs images image-base image-base-alpine image-jdk17 image-jdk20 image-jdk21 image-jdk22 image-gvm17 image-gvm20 image-runtime-jvm17 image-runtime-jvm20 image-runtime-jvm21 image-runtime-jvm21 image-native image-native-alpine runtime runtime-build runtime-update third-party
