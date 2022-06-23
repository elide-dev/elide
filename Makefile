
#
# Makefile: Elide
#

VERSION ?= $(shell cat .version)

SAMPLES ?= no

# Flags that control this makefile, along with their defaults:
#
# DEBUG ?= no
# RELEASE ?= no
# JVMDEBUG ?= no
# NATIVE ?= no
# CI ?= no
# DRY ?= no
# IGNORE_ERRORS ?= no

GRADLE ?= ./gradlew
RM ?= $(shell which rm)
FIND ?= $(shell which find)
PWD ?= $(shell pwd)
TARGET ?= $(PWD)/build

POSIX_FLAGS ?=
GRADLE_ARGS ?=
BUILD_ARGS ?=
NATIVE_TASKS ?= nativeCompile
ARGS ?=

ifeq ($(SAMPLES),yes)
BUILD_ARGS += -PbuildSamples=true
else
BUILD_ARGS += -PbuildSamples=false
endif

ifeq ($(CI),yes)
BUILD_ARGS += -Pelide.ci=true
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
CMD ?= "$(RULE)echo "
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

clean:  ## Clean build outputs and caches.
	@echo "Cleaning targets..."
	$(CMD)$(RM) -fr$(POSIX_FLAGS) $(TARGET)
	$(CMD)$(GRADLE) clean $(_ARGS)
	$(CMD)$(FIND) . -name .DS_Store -delete

docs: $(DOCS)  ## Generate docs for all library modules.
	@echo "Generating docs..."
	$(CMD)$(GRADLE) docs $(_ARGS)
	@cd $(TARGET)/docs && cp -frv ./* $(PWD)/docs/
	@echo "Docs update complete."

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

