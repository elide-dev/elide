
#
# Elide: Docker Images
# -----------------------
# Image: Elide with Bash
#

IMAGE ?= bash
PROVENANCE ?= yes
PUB_IMAGE ?= $(IMAGE)
REPOSITORY ?= tools/$(IMAGE)
PUB_IMAGE_PATH ?= ghcr.io/elide-dev/$(IMAGE):$(VERSION)
ORIGIN_IMAGE ?= $(PUB_IMAGE_PATH)
PLATFORMS = linux/amd64

include ../Docker.mk
