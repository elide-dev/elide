
#
# Elide: Docker Images
# ----------------------
# Image: CI Builder
#

IMAGE ?= builder

PUSH ?= yes
REMOTE ?= no
VERSION ?= latest
PROJECT ?= elide-dev
REGISTRY ?= ghcr.io
REPOSITORY ?= tools/$(IMAGE)
DOCKER ?= $(shell which docker)
GCLOUD ?= $(shell which gcloud) beta
PLATFORMS ?= linux/arm64,linux/amd64

all: image

ifeq ($(PUSH),yes)
DOCKER_ARGS ?= --push
else
DOCKER_ARGS ?=
endif

ifeq ($(REMOTE),no)
image:
	$(DOCKER) buildx build --platform $(PLATFORMS) $(DOCKER_ARGS) --tag $(REGISTRY)/$(PROJECT)/$(REPOSITORY):$(VERSION) .

push:
	$(DOCKER) buildx push $(REGISTRY)/$(PROJECT)/$(REPOSITORY):$(VERSION)

.PHONY: image push
else
image:
	$(GCLOUD) builds submit . --config cloudbuild.yaml

push:
	@echo "Remote image build complete: $(REGISTRY)/$(PROJECT)/$(REPOSITORY):$(VERSION)"

.PHONY: image
endif
