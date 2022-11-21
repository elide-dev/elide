
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
PWD ?= $(shell pwd)
TARGET ?= $(PWD)/build
DOCS ?= $(PWD)/docs
REPORTS ?= $(DOCS)/reports

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
POSIX_FLAGS += v
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
		:conventions:publish \
		:substrate:publish \
		:tools:processor:publish \
		publish \
		-Pversion=$(VERSION) \
		-PbuildSamples=false \
		-PbuildDocs=true \
		-PbuildDocsSite=true \
		-PenableSigning=true \
		-x test \
		-x jvmTest \
		-x jsTest;

clean:  ## Clean build outputs and caches.
	@echo "Cleaning targets..."
	$(CMD)$(RM) -fr$(POSIX_FLAGS) $(TARGET)
	$(CMD)$(GRADLE) clean $(_ARGS)
	$(CMD)$(FIND) . -name .DS_Store -delete

docs: $(DOCS)  ## Generate docs for all library modules.
	@echo "Generating docs..."
	$(CMD)$(RM) -fr$(POSIX_FLAGS) docs/kotlin docs/reports
	$(CMD)$(GRADLE) docs $(_ARGS)
	$(CMD)$(MKDIR) -p $(DOCS) $(DOCS)/kotlin $(DOCS)/kotlin/javadoc
	$(CMD)cd $(TARGET)/docs \
		&& $(CP) -fr$(POSIX_FLAGS) ./* $(PWD)/docs/
	$(CMD)cd packages/server/build/dokka \
		&& $(CP) -fr$(POSIX_FLAGS) ./javadoc/* $(PWD)/docs/kotlin/javadoc/
	$(CMD)cd packages/rpc-jvm/build/dokka \
		&& $(CP) -fr$(POSIX_FLAGS) ./javadoc/* $(PWD)/docs/kotlin/javadoc/
	@echo "Docs update complete."

reports:  ## Generate reports for tests, coverage, etc.
	@echo "Generating reports..."
	$(CMD)$(GRADLE) \
		:reports \
		:tools:reports:reports \
		-x nativeCompile \
		-x test
	$(CMD)$(MKDIR) -p $(REPORTS) $(TARGET)/reports
	@echo "Copying merged reports to '$(REPORTS)'..."
	$(CMD)-cd $(TARGET)/reports && $(CP) -fr$(POSIX_FLAGS) ./* $(REPORTS)/
	@echo "Copying test reports to '$(REPORTS)'..."
	$(CMD)$(MKDIR) -p tools/reports/build/reports
	$(CMD)cd tools/reports/build/reports && $(CP) -fr$(POSIX_FLAGS) ./* $(REPORTS)/
	$(CMD)$(RM) -f docs/reports/project/properties.txt docs/reports/project/tasks.txt
	@echo "Reports synced."

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
	$(CMD)cd docs \
		&& open http://localhost:8000 \
		&& python -m SimpleHTTPServer

distclean: clean  ## DANGER: Clean and remove any persistent caches. Drops changes.
	@echo "Cleaning caches..."
	$(CMD)$(RM) -fr$(POSIX_FLAGS) kotlin-js-store .buildstate.tar.gz

forceclean: forceclean  ## DANGER: Clean, distclean, and clear untracked files.
	@echo "Resetting codebase..."
	$(CMD)$(GIT) reset --hard
	@echo "Cleaning untracked files..."
	$(CMD)$(GIT) clean -xdf

help:  ## Show this help text ('make help').
	$(info Elide:)
	@grep -E '^[a-z1-9A-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'

.PHONY: all build test clean distclean forceclean docs
