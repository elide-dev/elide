
#
# Elide: Docker Images
# ----------------------
# Image: Codespaces
#

IMAGE ?= codespace

PUSH ?= yes
REMOTE ?= yes
VERSION ?= latest
PROJECT ?= elide-fw
REGISTRY ?= us-docker.pkg.dev
REPOSITORY ?= tools/$(IMAGE)
DOCKER ?= $(shell which docker)
GCLOUD ?= $(shell which gcloud) beta

ifeq ($(PUSH),yes)
all: image push
else
all: image
endif

ifeq ($(REMOTE),no)
image:
	$(DOCKER) build -t $(REGISTRY)/$(PROJECT)/$(REPOSITORY):$(VERSION) .

push:
	$(DOCKER) push $(REGISTRY)/$(PROJECT)/$(REPOSITORY):$(VERSION)

.PHONY: image push
else
image:
	$(GCLOUD) builds submit . --config cloudbuild.yaml

push:
	@echo "Remote image build complete: $(REGISTRY)/$(PROJECT)/$(REPOSITORY):$(VERSION)"

.PHONY: image
endif
