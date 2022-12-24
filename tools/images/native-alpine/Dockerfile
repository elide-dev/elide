ARG BASE_IMAGE=base/alpine
ARG BASE_VERSION=""
ARG BASE_HASH="@sha256:f3cb1a2bc54e15cb42351e7666f8d09cbf95e5c75a742b397229c5a9968f6998"

FROM us-docker.pkg.dev/elide-fw/tools/${BASE_IMAGE}${BASE_VERSION}${BASE_HASH}

LABEL org.opencontainers.image.vendor=Elide
LABEL org.opencontainers.image.title="Elide Base (Native/Alpine)"
LABEL org.opencontainers.image.description="Base image for native Elide server apps on Alpine Linux"
LABEL org.opencontainers.image.version=v3-alpha1
LABEL org.opencontainers.image.url=https://github.com/elide-dev/v3
LABEL org.opencontainers.image.base.name=busybox:glibc${BASE_VERSION}${BASE_HASH}
LABEL org.opencontainers.image.source=https://github.com/elide-dev/v3/blob/v3/tools/images/native-alpine/Dockerfile

## GraalVM requires libstdc++
RUN apk --no-cache update && apk add libstdc++
