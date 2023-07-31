
#
# Elide: Docker Images
# ----------------------
# Image: Native (Alpine)
#

IMAGE ?= runtime/native/alpine
PROVENANCE ?= yes
PUB_IMAGE ?= runtime-native-alpine
REPOSITORY ?= tools/$(IMAGE)
PUB_IMAGE_PATH ?= ghcr.io/elide-dev/runtime/native/alpine:$(VERSION)
ORIGIN_IMAGE ?= $(PUB_IMAGE_PATH)

include ../Docker.mk
