ARG BASE_VERSION=":latest"
ARG BASE_HASH="@sha256:e350485a408c626aba6e143a18bf912bb490fcc2711b5f5df0cd2cc6f12ad4b4"

FROM us-docker.pkg.dev/elide-fw/tools/base${BASE_VERSION}${BASE_HASH}

LABEL org.opencontainers.image.vendor=Elide
LABEL org.opencontainers.image.title="Elide Base (Native)"
LABEL org.opencontainers.image.description="Base image for native Elide server apps"
LABEL org.opencontainers.image.version=v3-alpha1
LABEL org.opencontainers.image.url=https://github.com/elide-dev/elide
LABEL org.opencontainers.image.base.name=gcr.io/distroless/base
LABEL org.opencontainers.image.source=https://github.com/elide-dev/elide/blob/v3/tools/images/native/Dockerfile

RUN apt-get update && apt-get install -y zlib1g libc6 && rm -rf /var/lib/apt/lists/*
