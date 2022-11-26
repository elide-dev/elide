
#
# Makefile: Elide
#

VERSION ?= $(shell cat .version)
STRICT ?= yes
RELOCK ?= no

SAMPLES ?= no

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

GRADLE ?= ./gradlew
YARN ?= $(shell which yarn)
RM ?= $(shell which rm)
FIND ?= $(shell which find)
MKDIR ?= $(shell which mkdir)
CP ?= $(shell which cp)
RSYNC ?= $(shell which rsync)
UNZIP ?= $(shell which unzip)
PWD ?= $(shell pwd)
TARGET ?= $(PWD)/build
DOCS ?= $(PWD)/docs
SITE_BUILD ?= $(PWD)/build/site
REPORTS ?= $(SITE_BUILD)/reports

POSIX_FLAGS ?=
GRADLE_ARGS ?=
BUILD_ARGS ?=
NATIVE_TASKS ?= nativeCompile
DEP_HASH_ALGO ?= pgp,sha256
ARGS ?=

ifeq ($(SAMPLES),yes)
BUILD_ARGS += -PbuildSamples=true
else
BUILD_ARGS += -PbuildSamples=false
endif

ifeq ($(RELOCK),yes)
BUILD_ARGS += --write-verification-metadata sha256,pgp --export-keys --write-locks
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

ifeq ($(RELEASE),yes)
BUILD_ARGS += -Pelide.buildMode=prod -Pelide.stamp=true
endif


ifneq ($(NATIVE),)
ifneq ($(NATIVE),no)
BUILD_ARGS += $(patsubst %,-d %,$(NATIVE_TASKS))
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

_ARGS ?= $(GRADLE_ARGS) $(BUILD_ARGS) $(ARGS)


# ---- Targets ---- #


all: build test docs


build:  ## Build the main library, and code-samples if SAMPLES=yes.
	$(info Building Elide v3...)
	$(CMD) $(GRADLE) build -x test $(_ARGS)

test:  ## Run the library testsuite, and code-sample tests if SAMPLES=yes.
	$(info Running testsuite...)
	$(CMD)$(GRADLE) test $(_ARGS)

publish:  ## Publish a new version of all Elide packages.
	$(info Publishing packages for version "$(VERSION)"...)
	$(CMD)$(GRADLE) \
		publish \
		--no-daemon \
		--warning-mode=none \
		-Pversion=$(VERSION) \
		-PbuildSamples=false \
		-PbuildDocs=true \
		-PbuildDocsSite=false \
		-PenableSigning=true \
		-Pelide.release=true \
		-Pelide.buildMode=release \
		-x test \
		-x jvmTest \
		-x jsTest;

clean: clean-docs clean-site  ## Clean build outputs and caches.
	@echo "Cleaning targets..."
	$(CMD)$(RM) -fr$(strip $(POSIX_FLAGS)) $(TARGET)
	$(CMD)$(GRADLE) clean $(_ARGS)
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
	$(CMD)cd packages/rpc-jvm/build/dokka \
		&& $(MKDIR) $(SITE_BUILD)/docs/javadoc/rpc-jvm \
		&& $(CP) -fr$(strip $(POSIX_FLAGS)) ./javadoc/* $(SITE_BUILD)/docs/javadoc/rpc-jvm/
	@echo "Docs assemble complete."

reports: $(REPORTS)  ## Generate reports for tests, coverage, etc.
	@$(RM) -f $(SITE_BUILD)/reports/project/properties.txt

$(REPORTS):
	@echo "Generating reports..."
	$(CMD)$(GRADLE) \
		:reports \
		:tools:reports:reports \
		-x nativeCompile \
		-x test
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
	$(CMD)$(UNZIP) -o -d $(SITE_BUILD) $(PWD)/site/docs/app/build/ssg-site.zip

site/docs/app/build:
	@echo "Building Elide site..."
	$(CMD)$(GRADLE) \
		-PbuildDocsSite=true \
		-PbuildDocs=true \
		-PbuildSamples=false \
		-x test \
		-x check \
		:site:docs:app:build

site/docs/app/build/ssg-site.zip: site/docs/app/build
	@echo "Starting Elide docs site for SSG build..."
	$(CMD)$(RM) -fv server_pid.txt
	-nohup $(GRADLE) \
		--no-daemon \
		:site:docs:app:run \
		-Pelide.release=true \
		-PbuildSamples=false \
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
			--args="--http --ignore-cert-errors --verbose --no-crawl $(PWD)/site/docs/app/build/generated/ksp/main/resources/elide/runtime/generated/app.manifest.pb https://localhost:8443 $(PWD)/site/docs/app/build/ssg-site.zip" \
		&& echo "Finishing up..." \
		&& flush || echo "No flush needed." \
		&& sleep 3 \
		&& echo "Site SSG build complete.";
	@echo "Killing server at PID $(shell cat server_pid.txt)..." \
		&& sudo kill -9 `cat server_pid.txt` || echo "No process to kill." \
		&& $(RM) -f server_pid.txt

update-dep-hashes:
	@echo "- Updating dependency hashes..."
	$(CMD)$(GRADLE) \
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
		&& python -m SimpleHTTPServer

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

.PHONY: all build test clean distclean forceclean docs
