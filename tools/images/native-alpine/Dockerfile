ARG BASE_IMAGE=base/alpine
ARG BASE_VERSION=""
ARG BASE_HASH="@sha256:40f914496cdbebe42fb944c58685da2d2cb626b1d4e2da2a66edc847a674ec81"

FROM ghcr.io/elide-dev/base/alpine${BASE_VERSION}${BASE_HASH}

LABEL org.opencontainers.image.vendor=Elide
LABEL org.opencontainers.image.title="Elide Base (Native/Alpine)"
LABEL org.opencontainers.image.description="Base image for native Elide server apps on Alpine Linux"
LABEL org.opencontainers.image.version=v1.0.0-alpha11
LABEL org.opencontainers.image.url=https://github.com/elide-dev/elide
LABEL org.opencontainers.image.base.name=busybox:glibc${BASE_VERSION}${BASE_HASH}
LABEL org.opencontainers.image.source=https://github.com/elide-dev/elide/blob/main/tools/images/native-alpine/Dockerfile

## GraalVM requires libstdc++
RUN apk --no-cache update && apk add libstdc++
