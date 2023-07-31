FROM debian:bookworm-slim@sha256:5bbfcb9f36a506f9c9c2fb53205f15f6e9d1f0e032939378ddc049a2d26d651e

LABEL org.opencontainers.image.vendor=Elide
LABEL org.opencontainers.image.title="Elide Base (Native)"
LABEL org.opencontainers.image.description="Base image for native Elide server apps"
LABEL org.opencontainers.image.version=v3-alpha1
LABEL org.opencontainers.image.url=https://github.com/elide-dev/v3
LABEL org.opencontainers.image.base.name=gcr.io/distroless/base
LABEL org.opencontainers.image.source=https://github.com/elide-dev/v3/blob/v3/tools/images/native/Dockerfile

RUN apt-get update && apt-get install -y zlib1g libc6 && rm -rf /var/lib/apt/lists/*
