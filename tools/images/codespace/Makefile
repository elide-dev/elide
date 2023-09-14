
#
# Elide: Docker Images
# ----------------------
# Image: Codespaces
#

IMAGE ?= codespace
PLATFORMS = linux/amd64

PUB_IMAGE ?= codespace
PROVENANCE ?= yes
REPOSITORY ?= tools/$(IMAGE)
PUB_IMAGE_PATH ?= ghcr.io/elide-dev/codespace:$(VERSION)
ORIGIN_IMAGE ?= $(PUB_IMAGE_PATH)

include ../Docker.mk
