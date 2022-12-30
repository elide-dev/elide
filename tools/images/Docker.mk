
#
# Elide: Docker Images (Abstract)
# -------------------------------
# Defines shared Makefile logic for Docker image builds.
#

PUSH ?= yes
REMOTE ?= yes
VERSION ?= latest
REGISTRY ?= us-docker.pkg.dev
PROJECT ?= elide-fw
DOCKER ?= $(shell which docker)
GCLOUD ?= $(shell which gcloud) beta
PLATFORMS ?= linux/arm64,linux/amd64
PUB_REGISTRY ?= ghcr.io/elide-dev
HUB_REGISTRY ?= elidetools

ORIGIN_IMAGE_PATH ?= $(REGISTRY)/$(PROJECT)/$(REPOSITORY):$(VERSION)
PUB_IMAGE_PATH ?= $(PUB_REGISTRY)/$(REPOSITORY):$(VERSION)
HUB_IMAGE_PATH ?= $(HUB_REGISTRY)/$(PUB_IMAGE):$(VERSION)

ifeq ($(VERBOSE),yes)
CMD ?=
else
CMD ?= @
endif

ifeq ($(PUSH),yes)
DOCKER_ARGS ?= --push
else
DOCKER_ARGS ?=
endif

ifeq ($(RELEASE),yes)
all: image retag
else
all: image
endif

ifeq ($(REMOTE),no)
image:
	$(CMD)$(DOCKER) buildx build --platform $(PLATFORMS) $(DOCKER_ARGS) --tag $(ORIGIN_IMAGE) .

push:
	$(CMD)$(DOCKER) buildx push $(ORIGIN_IMAGE)

.PHONY: image push
else
image:
	$(CMD)$(GCLOUD) builds submit . --config cloudbuild.yaml

push:
	@echo "Remote image build complete: $(ORIGIN_IMAGE)"

.PHONY: image
endif

retag:
	@echo ""; echo "Republishing image '$(ORIGIN_IMAGE_PATH)' → '$(PUB_IMAGE_PATH)'..."
	$(CMD)$(DOCKER) pull $(ORIGIN_IMAGE_PATH) && \
		$(DOCKER) tag $(ORIGIN_IMAGE_PATH) $(PUB_IMAGE_PATH) && \
		$(DOCKER) push $(PUB_IMAGE_PATH);

	@echo ""; echo "Republishing image '$(ORIGIN_IMAGE_PATH)' → '$(HUB_IMAGE_PATH)'..."
	$(CMD)$(DOCKER) tag $(ORIGIN_IMAGE_PATH) $(HUB_IMAGE_PATH) && \
		$(DOCKER) push $(HUB_IMAGE_PATH);
