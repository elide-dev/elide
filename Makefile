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

VERSION ?= $(shell cat .version)
export STRICT ?= yes
export WASM ?= no
export RELOCK ?= no
DEFAULT_REPOSITORY ?= s3://elide-maven
REPOSITORY ?= $(DEFAULT_REPOSITORY)

SAMPLES ?= no
SIGNING_KEY ?= F812016B
REMOTE ?= no
PUSH ?= no
CHECK ?= yes

# Flags that are exported to the third_party build.
export CUSTOM_ZLIB ?= no

# Flags that control this makefile, along with their defaults:
#
# DEBUG ?= no
# STRICT ?= yes
# RELEASE ?= no
# JVMDEBUG ?= no
# NATIVE ?= no
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

export PATH
export JAVA_HOME
export GRAALVM_HOME

GRADLE ?= ./gradlew
RUSTUP ?= $(shell which rustup)
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
PWD ?= $(shell pwd)
TARGET ?= $(PWD)/build
DOCS ?= $(PWD)/docs
REPORTS ?=
BUF ?= $(shell which buf)
JVM ?= 21
SYSTEM ?= $(shell uname -s)
JQ ?= $(shell which jq)
BAZEL ?= $(shell which bazel)
export PROJECT_ROOT ?= $(shell pwd)
export ELIDE_ROOT ?= yes

PATH := $(shell echo $$PATH)

# Handle custom Java home path.
CUSTOM_JVM ?= no
JAVA_HOME ?= $(shell echo $$JAVA_HOME)
GRAALVM_HOME ?= $(shell echo $$GRAALVM_HOME)

ifneq ("$(wildcard ./.java-home)","")
		$(info Using custom JVM...)
    CUSTOM_JVM = $(shell cat .java-home)
    JAVA_HOME = $(CUSTOM_JVM)
    PATH := $(JAVA_HOME)/bin:$(PATH)
    JAVA = $(JAVA_HOME)/bin/java
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

JS_FACADE_BIN ?= runtime/bazel-bin/elide/runtime/js/runtime.bin.js
JS_FACADE_OUT ?= packages/graalvm/src/main/resources/META-INF/elide/embedded/runtime/js/facade.js
JS_POLYFILLS_BIN ?= runtime/bazel-bin/elide/runtime/js/polyfills/polyfills.min.js
JS_POLYFILLS_OUT ?= packages/graalvm/src/main/resources/META-INF/elide/embedded/runtime/js/polyfills.js
JS_MODULE_BIN ?= runtime/bazel-bin/elide/runtime/js/js.modules.tar
JS_MODULE_OUT ?= packages/graalvm/src/main/resources/META-INF/elide/embedded/runtime/js/js.modules.tar

PY_FACADE_BIN ?= packages/graalvm-py/src/main/resources/META-INF/elide/embedded/runtime/python/preamble.py
PY_MODULE_BIN ?= runtime/bazel-bin/elide/runtime/python/py.modules.tar
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

ifneq ($(NATIVE),)
ifneq ($(NATIVE),no)
BUILD_ARGS += $(patsubst %,-d %,$(NATIVE_TASKS))
OMIT_NATIVE =
else
ifeq ($(RELEASE),yes)
CLI_TASKS += :packages:cli:nativeOptimizedCompile -Pelide.buildMode=release -Pelide.release=true -PenableSigning=true -PbuildDocs=true
else
CLI_TASKS += :packages:cli:nativeCompile
endif
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
DEPS ?= node_modules/ third-party umbrella $(RUNTIME_GEN)

GRADLE_PREFIX ?= JAVA_HOME="$(JAVA_HOME)" GRAALVM_HOME="$(GRAALVM_HOME)" PATH="$(PATH)"

# ---- Targets ---- #

all: build

setup: $(DEPS)  ## Setup development pre-requisites.

build: $(DEPS)  ## Build the main library, and code-samples if SAMPLES=yes.
	$(info Building Elide $(VERSION)...)
	$(CMD)$(GRADLE_PREFIX) $(GRADLE) build $(CLI_TASKS) $(GRADLE_OMIT) $(_ARGS)

native:  ## Build Elide's native image target; use BUILD_MODE=release for a release binary.
	$(info Building Elide native $(VERSION) ($(BUILD_MODE))...)
ifeq ($(BUILD_MODE),release)
	$(CMD)$(GRADLE_PREFIX) $(GRADLE) :packages:runtime:nativeOptimizedCompile $(_ARGS)
else
	$(CMD)$(GRADLE_PREFIX) $(GRADLE) :packages:runtime:nativeCompile $(_ARGS)
endif

test:  ## Run the library testsuite, and code-sample tests if SAMPLES=yes.
	$(info Running testsuite...)
	$(CMD)$(GRADLE_PREFIX) $(GRADLE) test check $(_ARGS)
	$(CMD)$(GRADLE_PREFIX) $(GRADLE) :packages:cli:optimizedRun --args="selftest"

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

umbrella: $(UMBRELLA_TARGET_PATH)  ## Build the native umbrella tooling library.

$(UMBRELLA_TARGET_PATH):
	$(info Building tools/umbrella...)
ifeq ($(BUILD_MODE),release)
	$(CMD)$(CARGO) build --release --all-targets
else
	$(CARGO) build --all-targets
endif

third-party: third_party/sqlite third_party/lib  ## Build all third-party embedded projects.

third_party/sqlite:
	@echo "Setting up submodules..."
	$(CMD)$(GIT) submodule update --init --recursive

third_party/lib:
	@echo "Building third-party projects..."
	$(CMD)$(MAKE) RELOCK=$(RELOCK) -C third_party -j`nproc` && mkdir -p third_party/lib
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

clean: clean-docs  ## Clean build outputs and caches.
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

IMAGES ?= image-base image-base-alpine image-gvm21 image-gvm22 image-jdk17 image-jdk21 image-jdk22 image-runtime-jvm21 image-runtime-jvm22 image-native image-native-alpine

images: $(IMAGES)  ## Build all Docker images.
	@echo "All Docker images built."

image-base:  ## Build base Ubuntu image.
	@echo "Building image 'base'..."
	$(CMD)$(MAKE) -C tools/images/base PUSH=$(PUSH) REMOTE=$(REMOTE)

image-base-alpine:  ## Build base Alpine image.
	@echo "Building image 'base/alpine'..."
	$(CMD)$(MAKE) -C tools/images/base-alpine PUSH=$(PUSH) REMOTE=$(REMOTE)

image-gvm17:  ## Build GVM17 builder image.
	@echo "Building image 'gvm17'..."
	$(CMD)$(MAKE) -C tools/images/gvm17 PUSH=$(PUSH) REMOTE=$(REMOTE)

image-gvm20:  ## Build GVM20 builder image.
	@echo "Building image 'gvm20'..."
	$(CMD)$(MAKE) -C tools/images/gvm20 PUSH=$(PUSH) REMOTE=$(REMOTE)

image-gvm21:  ## Build GVM21 builder image.
	@echo "Building image 'gvm21'..."
	$(CMD)$(MAKE) -C tools/images/gvm21 PUSH=$(PUSH) REMOTE=$(REMOTE)

image-gvm22:  ## Build GVM22 builder image.
	@echo "Building image 'gvm22'..."
	$(CMD)$(MAKE) -C tools/images/gvm22 PUSH=$(PUSH) REMOTE=$(REMOTE)

image-jdk17:  ## Build JDK17 builder image.
	@echo "Building image 'jdk17'..."
	$(CMD)$(MAKE) -C tools/images/jdk17 PUSH=$(PUSH) REMOTE=$(REMOTE)

image-jdk20:  ## Build JDK20 builder image.
	@echo "Building image 'jdk20'..."
	$(CMD)$(MAKE) -C tools/images/jdk20 PUSH=$(PUSH) REMOTE=$(REMOTE)

image-jdk21:  ## Build JDK21 builder image.
	@echo "Building image 'jdk21'..."
	$(CMD)$(MAKE) -C tools/images/jdk21 PUSH=$(PUSH) REMOTE=$(REMOTE)

image-jdk22:  ## Build JDK20 builder image.
	@echo "Building image 'jdk22'..."
	$(CMD)$(MAKE) -C tools/images/jdk22 PUSH=$(PUSH) REMOTE=$(REMOTE)

image-runtime-jvm17:  ## Build runtime GVM17 builder image.
	@echo "Building image 'gvm17'..."
	$(CMD)$(MAKE) -C tools/images/runtime-jvm17 PUSH=$(PUSH) REMOTE=$(REMOTE)

image-runtime-jvm20:  ## Build runtime GVM20 builder image.
	@echo "Building image 'gvm20'..."
	$(CMD)$(MAKE) -C tools/images/runtime-jvm20 PUSH=$(PUSH) REMOTE=$(REMOTE)

image-runtime-jvm21:  ## Build runtime GVM21 builder image.
	@echo "Building image 'gvm21'..."
	$(CMD)$(MAKE) -C tools/images/runtime-jvm21 PUSH=$(PUSH) REMOTE=$(REMOTE)

image-runtime-jvm22:  ## Build runtime GVM22 builder image.
	@echo "Building image 'gvm22'..."
	$(CMD)$(MAKE) -C tools/images/runtime-jvm22 PUSH=$(PUSH) REMOTE=$(REMOTE)

image-native:  ## Build native Ubuntu base image.
	@echo "Building image 'native'..."
	$(CMD)$(MAKE) -C tools/images/native PUSH=$(PUSH) REMOTE=$(REMOTE)

image-native-alpine:  ## Build native Alpine base image.
	@echo "Building image 'native-alpine'..."
	$(CMD)$(MAKE) -C tools/images/native-alpine PUSH=$(PUSH) REMOTE=$(REMOTE)

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
	@echo "" && echo "---- Toolchain --------------------------------------------"
	@echo "- Java: $(JAVA)"
	@echo "- Native Image: $(NATIVE_IMAGE)"
	@echo "- Rust: $(RUSTUP)"
	@echo "- Cargo: $(CARGO)"
	@echo "- Yarn: $(YARN)"
	@echo "- Bazel: $(BAZEL)"
	@echo "- Buf: $(BUF)"
	@echo "- Node: $(NODE)"
	@echo "- PNPM: $(PNPM)"
	@echo "" && echo "---- Toolchain Versions -----------------------------------"
	@echo "Java:"
	$(CMD)$(JAVA) --version
	@echo ""
	@echo "Native Image:"
	$(CMD)$(NATIVE_IMAGE) --version
	@echo ""
	@echo "Rustup:"
	$(CMD)$(RUSTUP) --version
	@echo ""
	@echo "Cargo:"
	$(CMD)$(CARGO) --version
	@echo ""
	@echo "Yarn:"
	$(CMD)$(YARN) --version
	@echo ""
	@echo "Bazel:"
	$(CMD)$(BAZEL) --version
	@echo ""
	@echo "Buf:"
	$(CMD)$(BUF) --version
	@echo ""
	@echo "Node:"
	$(CMD)$(NODE) --version
	@echo ""
	@echo "PNPM:"
	$(CMD)$(PNPM) --version
	@echo "" && echo "---- OS Info ----------------------------------------------"
	@echo "- System: $(SYSTEM)"
	@echo "- Kernel: $(shell uname -r)"
	@echo "- Hostname: $(shell hostname)"
	@echo "" && echo "---- Environment ------------------------------------------"
	@echo "- PATH: $(shell echo $$PATH)"
	@echo "- CC: $(shell echo $$CC)"
	@echo "- CFLAGS: $(shell echo $$CFLAGS)"
	@echo "- LDFLAGS: $(shell echo $$LDFLAGS)"
	@echo "- CPPFLAGS: $(shell echo $$CPPFLAGS)"
	@echo "- JAVA_HOME: $(shell echo $$JAVA_HOME)"
	@echo "- GRAALVM_HOME: $(shell echo $$GRAALVM_HOME)"

# ---- Runtime submodule ---- #
# Note: make sure the Git submodule is up to date by running `git submodule update [--init] runtime`

runtime: runtime/WORKSPACE $(RUNTIME_GEN) ## Build and update the JS runtime if needed.

runtime/WORKSPACE:
	@echo "Setting up submodules..."
	$(CMD)$(GIT) submodule update --init --recursive

runtime-build: runtime/bazel-bin ## Build the JS runtime facade and the builtin modules bundle

$(RUNTIME_GEN): runtime/bazel-bin

runtime/bazel-bin:
	@echo "" && echo "Building runtime facades..."
	$(CMD)cd runtime && $(BAZEL) build -c $(BAZEL_MODE) //...
	$(CMD)$(MAKE) runtime-update-copy

runtime-update: runtime-build $(RUNTIME_GEN) ## Rebuild and copy the JS runtime facade

runtime-update-copy:
	@echo "" && echo "Updating runtime artifacts..."
	@echo "- Updating 'facade.js'"
	$(CMD)cp -f$(POSIX_FLAGS) $(JS_FACADE_BIN) $(JS_FACADE_OUT)
	@echo "- Updating 'polyfills.js'"
	$(CMD)cp -f$(POSIX_FLAGS) $(JS_POLYFILLS_BIN) $(JS_POLYFILLS_OUT)
	@echo "- Updating 'js.modules.tar'"
	$(CMD)cp -f$(POSIX_FLAGS) $(JS_MODULE_BIN) $(JS_MODULE_OUT)
	@echo "- Updating 'py.modules.tar.'"
	$(CMD)cp -f$(POSIX_FLAGS) $(PY_MODULE_BIN) $(PY_MODULE_OUT)

.PHONY: all docs build test clean distclean forceclean docs images image-base image-base-alpine image-jdk17 image-jdk20 image-jdk21 image-jdk22 image-gvm17 image-gvm20 image-runtime-jvm17 image-runtime-jvm20 image-runtime-jvm21 image-runtime-jvm21 image-native image-native-alpine runtime runtime-build runtime-update third-party
