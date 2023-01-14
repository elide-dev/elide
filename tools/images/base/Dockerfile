ARG UBUNTU_VERSION=20.04
ARG UBUNTU_HASH="@sha256:8eb87f3d6c9f2feee114ff0eff93ea9dfd20b294df0a0353bd6a4abf403336fe"

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
