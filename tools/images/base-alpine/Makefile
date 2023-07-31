
#
# Elide: Docker Images
# --------------------
# Image: Base (Alpine)
#

IMAGE ?= base/alpine
PROVENANCE ?= yes
PUB_IMAGE ?= base-alpine
REPOSITORY ?= tools/$(IMAGE)
PUB_IMAGE_PATH ?= ghcr.io/elide-dev/base/alpine:$(VERSION)
ORIGIN_IMAGE ?= $(PUB_IMAGE_PATH)

include ../Docker.mk
