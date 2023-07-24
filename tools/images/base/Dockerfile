ARG UBUNTU_VERSION=20.04
ARG UBUNTU_HASH="@sha256:8c38f4ea0b178a98e4f9f831b29b7966d6654414c1dc008591c6ec77de3bf2c9"

FROM ubuntu:${UBUNTU_VERSION}${UBUNTU_HASH}

LABEL org.opencontainers.image.vendor=Elide
LABEL org.opencontainers.image.title="Elide Base"
LABEL org.opencontainers.image.description="Base image for Elide apps and tools"
LABEL org.opencontainers.image.version=v3-alpha1
LABEL org.opencontainers.image.url=https://github.com/elide-dev/v3
LABEL org.opencontainers.image.base.name=ubuntu:${UBUNTU_VERSION}
LABEL org.opencontainers.image.source=https://github.com/elide-dev/v3/blob/v3/tools/images/base/Dockerfile

RUN apt-get update \
  && apt-get -y upgrade \
  && DEBIAN_FRONTEND=noninteractive apt-get -y install --no-install-recommends \
    libc6 \
    zlib1g \
  && apt-get remove perl \
  && apt-get autoremove \
  && rm -rf /var/lib/apt/lists/*

ENV LC_CTYPE=en_US.UTF-8 \
  LC_ALL=en_US.UTF-8 \
  PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/usr/lib/gvm/bin:/sbin:/bin
