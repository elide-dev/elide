ARG BASE_VERSION=""
ARG BASE_HASH="@sha256:28bfa3ccb4f29f9a745affe188551887ff999d27ad8257940d0d0e12ed818072"

FROM ghcr.io/elide-dev/base${BASE_VERSION}${BASE_HASH}

LABEL org.opencontainers.image.vendor=Elide
LABEL org.opencontainers.image.title="Elide Base (Native)"
LABEL org.opencontainers.image.description="Base image for native Elide server apps"
LABEL org.opencontainers.image.version=v1.0.0-alpha13
LABEL org.opencontainers.image.url=https://github.com/elide-dev/elide
LABEL org.opencontainers.image.base.name=ghcr.io/elide-dev/base
LABEL org.opencontainers.image.source=https://github.com/elide-dev/elide/blob/main/tools/images/native/Dockerfile

RUN apt-get update && apt-get install -y zlib1g libc6 && rm -rf /var/lib/apt/lists/*
