ARG BASE_VERSION=1.35.0-musl
ARG BASE_ARCH=amd64
ARG BASE_HASH="@sha256:2994f6dc7ead76c50c26e08b0686ecd4a66ef8418c8b48c1d57cc0f604636843"

FROM --platform=linux/${BASE_ARCH} busybox:${BASE_VERSION}${BASE_HASH}

LABEL org.opencontainers.image.vendor=Elide
LABEL org.opencontainers.image.title="Elide Base (Native/Alpine)"
LABEL org.opencontainers.image.description="Base image for native Elide server apps on Alpine Linux"
LABEL org.opencontainers.image.version=v3-alpha1
LABEL org.opencontainers.image.url=https://github.com/elide-dev/v3
LABEL org.opencontainers.image.base.name=busybox:glibc${BASE_VERSION}${BASE_HASH}
LABEL org.opencontainers.image.source=https://github.com/elide-dev/v3/blob/v3/tools/images/native-alpine/Dockerfile
