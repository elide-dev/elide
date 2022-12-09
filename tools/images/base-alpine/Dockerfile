ARG ALPINE_VERSION=3.17.0
ARG ALPINE_ARCH=amd64
ARG ALPINE_HASH=sha256:c0d488a800e4127c334ad20d61d7bc21b4097540327217dfab52262adc02380c

FROM --platform=linux/${ALPINE_ARCH} alpine:${ALPINE_VERSION}@${ALPINE_HASH}

LABEL org.opencontainers.image.vendor=Elide
LABEL org.opencontainers.image.title="Elide Base (Alpine)"
LABEL org.opencontainers.image.description="Base image for Elide apps and tools built on top of musl-libc"
LABEL org.opencontainers.image.version=v3-alpha1
LABEL org.opencontainers.image.url=https://github.com/elide-dev/v3
LABEL org.opencontainers.image.base.name=ubuntu:${UBUNTU_VERSION}
LABEL org.opencontainers.image.source=https://github.com/elide-dev/v3/blob/v3/tools/images/base-alpine/Dockerfile

RUN apk add --no-cache --update musl

ENV LC_CTYPE=en_US.UTF-8 \
  LC_ALL=en_US.UTF-8 \
  PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/usr/lib/gvm/bin:/sbin:/bin
