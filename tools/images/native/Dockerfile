FROM debian:bookworm-slim@sha256:050f00e86cc4d928b21de66096126fac52c2ea47885c232932b2e4c00f0c116d

LABEL org.opencontainers.image.vendor=Elide
LABEL org.opencontainers.image.title="Elide Base (Native)"
LABEL org.opencontainers.image.description="Base image for native Elide server apps"
LABEL org.opencontainers.image.version=v3-alpha1
LABEL org.opencontainers.image.url=https://github.com/elide-dev/v3
LABEL org.opencontainers.image.base.name=gcr.io/distroless/base
LABEL org.opencontainers.image.source=https://github.com/elide-dev/v3/blob/v3/tools/images/native/Dockerfile

RUN apt-get update && apt-get install -y zlib1g libc6 && rm -rf /var/lib/apt/lists/*
