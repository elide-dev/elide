# Copyright (c) 2023 Elide Ventures, LLC.
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
STRICT ?= yes
RELOCK ?= no
SITE ?= yes
DEFAULT_REPOSITORY ?= gcs://elide-snapshots/repository/v3
REPOSITORY ?= $(DEFAULT_REPOSITORY)

SAMPLES ?= no
SIGNING_KEY ?= F812016B
REMOTE ?= no
PUSH ?= no

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

GRADLE ?= ./gradlew
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
PWD ?= $(shell pwd)
TARGET ?= $(PWD)/build
DOCS ?= $(PWD)/docs
SITE_BUILD ?= $(PWD)/build/site
REPORTS ?= $(SITE_BUILD)/reports
JVM ?= 20
SYSTEM ?= $(shell uname -s)
JQ ?= $(shell which jq)

POSIX_FLAGS ?=
GRADLE_OPTS ?=
GRADLE_ARGS ?= -Pversions.java.language=$(JVM)
BUILD_ARGS ?=
NATIVE_TASKS ?= nativeCompile
DEP_HASH_ALGO ?= sha256,pgp
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

ifeq ($(SITE),yes)
BUILD_ARGS += -PbuildDocsSite=true
else
BUILD_ARGS += -PbuildDocsSite=false
endif

ifeq ($(WASM),yes)
BUILD_ARGS += -PbuildWasm=true
else
BUILD_ARGS += -PbuildWasm=false
endif

ifeq ($(RELOCK),yes)
BUILD_ARGS += kotlinUpgradeYarnLock --write-verification-metadata $(DEP_HASH_ALGO) --export-keys --write-locks
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

CLI_TASKS ?= :packages:cli:installDist

ifeq ($(RELEASE),yes)
BUILD_MODE ?= release
SIGNING ?= yes
SIGSTORE ?= yes
NATIVE_TARGET_NAME ?= nativeOptimizedCompile
CLI_DISTPATH ?= ./packages/cli/build/dist/release
BUILD_ARGS += -Pelide.buildMode=prod -Pelide.stamp=true -Pelide.release=true -Pelide.strict=true
CLI_RELEASE_TARGETS ?= cli-local cli-release-artifacts
else
BUILD_MODE ?= dev
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

GRADLE_OMIT ?= $(OMIT_NATIVE)
_ARGS ?= $(GRADLE_ARGS) $(BUILD_ARGS) $(ARGS)


# ---- Targets ---- #

all: build test docs

build:  ## Build the main library, and code-samples if SAMPLES=yes.
	$(info Building Elide $(VERSION)...)
	$(CMD) $(GRADLE) build $(CLI_TASKS) -x test -x check $(GRADLE_OMIT) $(_ARGS)

test:  ## Run the library testsuite, and code-sample tests if SAMPLES=yes.
	$(info Running testsuite...)
	$(CMD)$(GRADLE) test $(_ARGS)
	$(CMD)$(GRADLE) :packages:cli:optimizedRun --args="selftest"

publish-substrate:
	$(info Publishing Elide Substrate "$(VERSION)"...)
	$(CMD)$(GRADLE) \
		publishSubstrate \
		--no-daemon \
		--warning-mode=none \
		--no-configuration-cache \
		-Pversion=$(VERSION) \
		-PbuildSamples=false \
		-PbuildDocs=true \
		-PbuildDocsSite=false \
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
	$(CMD)$(GRADLE) \
		publishElide \
		--no-daemon \
		--warning-mode=none \
		--no-configuration-cache \
		-Pversion=$(VERSION) \
		-PbuildSamples=false \
		-PbuildDocs=true \
		-PbuildDocsSite=false \
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
	$(CMD)$(GRADLE) \
		publishBom \
		--no-daemon \
		--warning-mode=none \
		--no-configuration-cache \
		-Pversion=$(VERSION) \
		-PbuildSamples=false \
		-PbuildDocs=true \
		-PbuildDocsSite=false \
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
	$(CMD)$(GRADLE) \
		:packages:cli:$(NATIVE_TARGET_NAME) \
		-Pversion=$(VERSION) \
		-PbuildSamples=false \
		-PbuildDocs=false \
		-PbuildDocsSite=false \
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

clean: clean-docs clean-site  ## Clean build outputs and caches.
	@echo "Cleaning targets..."
	$(CMD)$(RM) -fr$(strip $(POSIX_FLAGS)) $(TARGET)
	$(CMD)$(GRADLE) clean cleanTest $(_ARGS)
	$(CMD)$(FIND) . -name .DS_Store -delete

clean-docs:  ## Clean documentation targets.
	@echo "Cleaning docs..."
	$(CMD)$(RM) -fr$$(strip $(POSIX_FLAGS)) $(SITE_BUILD)/docs

clean-site:  ## Clean site targets.
	@echo "Cleaning site..."
	$(CMD)$(RM) -fr$$(strip $(POSIX_FLAGS)) $(SITE_BUILD)

docs: $(DOCS) $(SITE_BUILD)/docs/kotlin $(SITE_BUILD)/docs/javadoc  ## Generate docs for all library modules.

$(TARGET)/docs:
	@echo "Generating docs..."
	$(CMD)$(RM) -fr$(strip $(POSIX_FLAGS)) $(SITE_BUILD)/docs/kotlin $(SITE_BUILD)/docs/javadoc
	$(CMD)$(GRADLE) docs $(_ARGS)
	@echo "Docs build complete."

$(SITE_BUILD)/docs/kotlin $(SITE_BUILD)/docs/javadoc: $(TARGET)/docs
	@echo "Assembling docs site..."
	$(CMD)$(MKDIR) -p $(SITE_BUILD) $(SITE_BUILD)/docs/kotlin $(SITE_BUILD)/docs/javadoc
	$(CMD)cd $(DOCS) \
		&& $(RSYNC) -a$(strip $(POSIX_FLAGS)) --exclude="Makefile" --exclude="buf.work.yml" --exclude="README.md" "./" $(SITE_BUILD)/;
	$(CMD)cd $(TARGET)/docs/kotlin/html \
		&& $(RSYNC) -a$(strip $(POSIX_FLAGS)) ./* $(SITE_BUILD)/docs/kotlin/
	$(CMD)cd packages/server/build/dokka \
		&& $(MKDIR) $(SITE_BUILD)/docs/javadoc/server \
		&& $(CP) -fr$(strip $(POSIX_FLAGS)) ./javadoc/* $(SITE_BUILD)/docs/javadoc/server/
	@echo "Docs assemble complete."

api-check:  ## Check API/ABI compatibility with current changes.
	$(info Checking ABI compatibility...)
	$(CMD)$(GRADLE) apiCheck -PbuildDocsSite=false -PbuildSamples=false -PbuildDocs=false

reports: $(REPORTS)  ## Generate reports for tests, coverage, etc.
	@$(RM) -f $(SITE_BUILD)/reports/project/properties.txt

$(REPORTS):
	@echo "Generating reports..."
	$(CMD)$(GRADLE) \
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

site-assets: $(SITE_BUILD)/creative

$(SITE_BUILD)/creative:
	@echo "Copying site assets..."
	$(CMD)$(MKDIR) -p $(SITE_BUILD)/creative/logo $(SITE_BUILD)/docs/kotlin/creative/logo/
	$(CMD)cd creative/logo \
		&& $(CP) -fr$(strip $(POSIX_FLAGS)) ./* $(SITE_BUILD)/creative/logo/ \
		&& $(CP) -fr$(strip $(POSIX_FLAGS)) ./* $(SITE_BUILD)/docs/kotlin/creative/logo/

SITE_PKG_ZIP ?= $(PWD)/

site: docs reports site-assets site/docs/app/build site/docs/app/build/ssg-site.zip  ## Generate the static Elide website.
	@echo "Assembling Elide site..."
	$(CMD)-$(UNZIP) -o -d $(SITE_BUILD) $(PWD)/site/docs/app/build/ssg-site.zip

site/docs/app/build:
	@echo "Building Elide site..."
	$(CMD)$(GRADLE) \
		-PbuildDocsSite=true \
		-PbuildDocs=true \
		-PbuildSamples=false \
		-Pversions.java.language=$(JVM) \
		-x test \
		-x check \
		:site:docs:app:build

ifeq ($(CI),yes)
site/docs/app/build/ssg-site.zip:
	@echo "Failed to locate Zip site output. Skipping."
else
site/docs/app/build/ssg-site.zip: site/docs/app/build
	@echo "Starting Elide docs site for SSG build..."
	$(CMD)$(RM) -fv server_pid.txt
	-nohup $(GRADLE) \
		--no-daemon \
		:site:docs:app:run \
		-Pelide.release=true \
		-PbuildSamples=false \
		-Pversions.java.language=$(JVM) \
		> server_log.txt 2>&1 & \
		echo $$! > server_pid.txt \
		&& echo "Elide site server started at PID $(shell cat server_pid.txt)" \
		&& echo "Waiting for server to be ready..." \
		&& sleep 5 \
		&& $(GRADLE) \
			:packages:ssg:run \
			--warning-mode=none \
			--dependency-verification=lenient \
			-Pelide.ci=true \
			-Pelide.release=true \
			-PbuildSamples=false \
			-PbuildDocs=false \
			-PbuildDocsSite=false \
			-Pversions.java.language=$(JVM) \
			--args="--http --ignore-cert-errors --verbose --no-crawl $(PWD)/site/docs/app/build/generated/ksp/main/resources/elide/runtime/generated/app.manifest.pb https://localhost:8443 $(PWD)/site/docs/app/build/ssg-site.zip" \
		&& echo "Finishing up..." \
		&& flush || echo "No flush needed." \
		&& sleep 3 \
		&& echo "Site SSG build complete.";
	@echo "Killing server at PID $(shell cat server_pid.txt)..." \
		&& sudo kill -9 `cat server_pid.txt` || echo "No process to kill." \
		&& $(RM) -f server_pid.txt
endif

update-dep-hashes:
	@echo "- Updating dependency hashes..."
	$(CMD)$(GRADLE) \
		-Pversions.java.language=$(JVM) \
		--write-verification-metadata $(DEP_HASH_ALGO)
	@echo "Dependency hashes updated."

update-dep-locks:
	@echo "- Updating dependency locks (yarn)..."
	$(CMD)$(YARN)
	@echo ""
	@echo "- Updating dependency locks (gradle)..."
	$(CMD)$(GRADLE) resolveAndLockAll --write-locks
	@echo "Dependency locks updated."

update-deps:  ## Perform interactive dependency upgrades across Yarn and Gradle.
	@echo "Upgrading dependencies..."
	$(CMD)$(MAKE) update-jsdeps
	@echo ""
	$(CMD)$(MAKE) update-jdeps

update-jsdeps:  ## Interactively update Yarn dependencies.
	@echo "Running interactive update for Gradle..."
	$(CMD)$(GRADLE) upgrade-gradle

update-jdeps:  ## Interactively update Gradle dependencies.
	@echo "Running interactive update for Gradle..."
	$(CMD)$(GRADLE) upgrade-gradle

relock-deps:  ## Update dependency locks and hashes across Yarn and Gradle.
	@echo "Relocking dependencies..."
	$(CMD)$(MAKE) update-dep-hashes update-dep-locks

serve-docs:  ## Serve documentation locally.
	@echo "Serving docs at http://localhost:8000..."
	$(CMD)cd $(SITE_BUILD)/docs/kotlin \
		&& open http://localhost:8000 \
		&& python -m SimpleHTTPServer

serve-site:  ## Serve Elide site locally.
	@echo "Serving site at http://localhost:8000..."
	$(CMD)cd $(SITE_BUILD) \
		&& open http://localhost:8000 \
		&& python3 -m http.server

IMAGES ?= image-base image-base-alpine image-gvm17 image-gvm20 image-jdk17 image-jdk20 image-runtime-jvm17 image-runtime-jvm20 image-native image-native-alpine

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

image-jdk17:  ## Build JDK17 builder image.
	@echo "Building image 'jdk17'..."
	$(CMD)$(MAKE) -C tools/images/jdk17 PUSH=$(PUSH) REMOTE=$(REMOTE)

image-jdk20:  ## Build JDK20 builder image.
	@echo "Building image 'jdk20'..."
	$(CMD)$(MAKE) -C tools/images/jdk20 PUSH=$(PUSH) REMOTE=$(REMOTE)

image-runtime-jvm17:  ## Build runtime GVM17 builder image.
	@echo "Building image 'gvm17'..."
	$(CMD)$(MAKE) -C tools/images/runtime-jvm17 PUSH=$(PUSH) REMOTE=$(REMOTE)

image-runtime-jvm20:  ## Build runtime GVM20 builder image.
	@echo "Building image 'gvm20'..."
	$(CMD)$(MAKE) -C tools/images/runtime-jvm20 PUSH=$(PUSH) REMOTE=$(REMOTE)

image-native:  ## Build native Ubuntu base image.
	@echo "Building image 'native'..."
	$(CMD)$(MAKE) -C tools/images/native PUSH=$(PUSH) REMOTE=$(REMOTE)

image-native-alpine:  ## Build native Alpine base image.
	@echo "Building image 'native-alpine'..."
	$(CMD)$(MAKE) -C tools/images/native-alpine PUSH=$(PUSH) REMOTE=$(REMOTE)

distclean: clean  ## DANGER: Clean and remove any persistent caches. Drops changes.
	@echo "Cleaning caches..."
	$(CMD)$(RM) -fr$(strip $(POSIX_FLAGS)) kotlin-js-store .buildstate.tar.gz

forceclean: forceclean  ## DANGER: Clean, distclean, and clear untracked files.
	@echo "Resetting codebase..."
	$(CMD)$(GIT) reset --hard
	@echo "Cleaning untracked files..."
	$(CMD)$(GIT) clean -xdf

help:  ## Show this help text ('make help').
	$(info Elide:)
	@grep -E '^[a-z1-9A-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'

.PHONY: all build test clean distclean forceclean docs images image-base image-base-alpine image-jdk17 image-jdk20 image-gvm17 image-gvm20 image-runtime-jvm17 image-runtime-jvm20 image-native image-native-alpine
