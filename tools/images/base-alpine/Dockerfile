ARG ALPINE_VERSION=3.17.8
ARG ALPINE_HASH="@sha256:a6063e988bcd597b4f1f7cfd4ec38402b02edd0c79250f00c9e14dc1e94bebbc"

FROM alpine:${ALPINE_VERSION}${ALPINE_HASH}

LABEL org.opencontainers.image.vendor=Elide
LABEL org.opencontainers.image.title="Elide Base (Alpine)"
LABEL org.opencontainers.image.description="Base image for Elide apps and tools built on top of musl-libc"
LABEL org.opencontainers.image.version=v1.0.0-alpha11
LABEL org.opencontainers.image.url=https://github.com/elide-dev/elide
LABEL org.opencontainers.image.base.name=alpine:${ALPINE_VERSION}
LABEL org.opencontainers.image.source=https://github.com/elide-dev/elide/blob/main/tools/images/base-alpine/Dockerfile

RUN apk add --no-cache --update libstdc++ musl

ENV LC_CTYPE=en_US.UTF-8 \
  LC_ALL=en_US.UTF-8 \
  PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/usr/lib/gvm/bin:/sbin:/bin
