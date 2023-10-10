FROM --platform=linux/amd64 debian:stable-slim AS base

LABEL org.opencontainers.image.vendor=Elide
LABEL org.opencontainers.image.title="Elide Runtime with Bash"
LABEL org.opencontainers.image.description="Elide runtime CLI as a container image, with Bash and several tools"
LABEL org.opencontainers.image.version=1.0.0-alpha7
LABEL org.opencontainers.image.url=https://github.com/elide-dev/elide
LABEL org.opencontainers.image.base.name=ghcr.io/elide-dev/base
LABEL org.opencontainers.image.base.digest=sha256:ce2fe9474ca168eebfd35b0c7682db11045b0f10418ec46ae9b0c7610c295913
LABEL org.opencontainers.image.source=https://github.com/elide-dev/elide/blob/main/tools/images/bash/Dockerfile

RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get -y install --no-install-recommends \
      bash \
      ca-certificates \
      curl \
      uuid-runtime \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd elide \
    && useradd dev -g elide -m -s /bin/bash \
    && ln -s /home/dev/elide/elide /bin/elide

USER dev

RUN echo "Installing Elide..." \
    && curl -sSL elide.sh | bash -s - \
    && which elide \
    && elide --help \
    && elide selftest \
    && elide info \
    && echo "Elide/Bash container image ready."
