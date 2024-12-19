FROM debian:bookworm-slim@sha256:67f3931ad8cb1967beec602d8c0506af1e37e8d73c2a0b38b181ec5d8560d395

LABEL org.opencontainers.image.vendor=Elide
LABEL org.opencontainers.image.title="Elide Base (Native)"
LABEL org.opencontainers.image.description="Base image for native Elide server apps"
LABEL org.opencontainers.image.version=v1.0.0-alpha11
LABEL org.opencontainers.image.url=https://github.com/elide-dev/elide
LABEL org.opencontainers.image.base.name=gcr.io/distroless/base
LABEL org.opencontainers.image.source=https://github.com/elide-dev/elide/blob/main/tools/images/native/Dockerfile

RUN apt-get update && apt-get install -y zlib1g libc6 && rm -rf /var/lib/apt/lists/*
