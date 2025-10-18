ARG UBUNTU_VERSION=24.04
ARG UBUNTU_HASH="@sha256:6015f66923d7afbc53558d7ccffd325d43b4e249f41a6e93eef074c9505d2233"

FROM ubuntu:${UBUNTU_VERSION}${UBUNTU_HASH}

LABEL org.opencontainers.image.vendor=Elide
LABEL org.opencontainers.image.title="Elide Base"
LABEL org.opencontainers.image.description="Base image for Elide apps and tools"
LABEL org.opencontainers.image.version=1.0.0-beta10
LABEL org.opencontainers.image.url=https://github.com/elide-dev/elide
LABEL org.opencontainers.image.base.name=ubuntu:24.04
LABEL org.opencontainers.image.source=https://github.com/elide-dev/elide/blob/main/tools/images/base/Dockerfile

RUN apt-get update \
  && apt-get -y upgrade \
  && DEBIAN_FRONTEND=noninteractive apt-get -y install --no-install-recommends libc6=2.39-0ubuntu8.4 \
  && apt-get remove perl \
  && apt-get autoremove \
  && rm -rf /var/lib/apt/lists/*

ENV LANG=en_US.UTF-8 \
  LANGUAGE=en_US \
  PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/usr/lib/gvm/bin:/sbin:/bin
