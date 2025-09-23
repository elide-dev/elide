ARG ALPINE_VERSION=3.18
ARG ALPINE_HASH="@sha256:de0eb0b3f2a47ba1eb89389859a9bd88b28e82f5826b6969ad604979713c2d4f"

FROM alpine:${ALPINE_VERSION}${ALPINE_HASH}

LABEL org.opencontainers.image.vendor=Elide
LABEL org.opencontainers.image.title="Elide Base (Alpine)"
LABEL org.opencontainers.image.description="Base image for Elide apps and tools built on top of musl-libc"
LABEL org.opencontainers.image.version=1.0.0-beta9
LABEL org.opencontainers.image.url=https://github.com/elide-dev/elide
LABEL org.opencontainers.image.base.name=alpine:3.18
LABEL org.opencontainers.image.source=https://github.com/elide-dev/elide/blob/main/tools/images/base-alpine/Dockerfile

RUN apk add --no-cache --update libstdc++=12.2.1_git20220924-r10 musl=1.2.4-r3 libc6-compat=1.2.4-r3

ENV LC_CTYPE=en_US.UTF-8 \
  LC_ALL=en_US.UTF-8 \
  PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/usr/lib/gvm/bin:/sbin:/bin
