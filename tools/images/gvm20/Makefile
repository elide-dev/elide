
#
# Elide: Docker Images
# -----------------------
# Image: GraalVM (JDK 20)
#

IMAGE ?= gvm20
PROVENANCE ?= yes
PUB_IMAGE ?= tools-$(IMAGE)
REPOSITORY ?= tools/$(IMAGE)
PUB_IMAGE_PATH ?= ghcr.io/elide-dev/tools/$(IMAGE):$(VERSION)
ORIGIN_IMAGE ?= $(PUB_IMAGE_PATH)

include ../Docker.mk
