FROM debian:sid-slim@sha256:7e8e5af19ae9e22c326f52aa30a65dd635c0e45430e322fc2aa64455c9ad624c AS base

LABEL org.opencontainers.image.vendor=Elide
LABEL org.opencontainers.image.title="Elide Runtime with Bash"
LABEL org.opencontainers.image.description="Elide runtime CLI as a container image, with Bash and several tools"
LABEL org.opencontainers.image.version=1.0.0-beta8
LABEL org.opencontainers.image.url=https://github.com/elide-dev/elide
LABEL org.opencontainers.image.base.name=debian:sid-slim
LABEL org.opencontainers.image.base.digest=sha256:5e814211fbdb43856892292a315c740e8882387709a1f51ee266884931ff5eac
LABEL org.opencontainers.image.source=https://github.com/elide-dev/elide/blob/main/tools/images/bash/Dockerfile

ARG ELIDE_DIGEST_LINUX_AMD64=a5430b7e398c12b66fc6a13a1ac3fd1a7622db48b0a312ccce6232aafa40d23d
ARG ELIDE_DIGEST_LINUX_ARM64=f5bdc8845726cb32564cd8ea97c286e8c5ef243662bb52480ff553c29c7231d7

RUN apt-get update \
  && DEBIAN_FRONTEND=noninteractive apt-get -y install --no-install-recommends \
    ca-certificates=20250419 \
    curl=8.14.1-2 \
    uuid-runtime=2.41-5 \
    libgomp1=14.2.0-19 \
    libstdc++6=14.2.0-19 \
    libc-bin=2.41-10 \
    libc6=2.41-10 \
  && rm -rf /var/lib/apt/lists/* \
  && groupadd elide \
  && useradd dev -g elide -m -s /bin/bash \
  && ln -s /home/dev/elide/elide /bin/elide

USER dev

ENV ELIDE_VERSION=1.0.0-beta8

RUN ELIDE_ARCH=$(uname -m) \
    && if [ "$ELIDE_ARCH" = "x86_64" ]; then \
      ELIDE_DIGEST=${ELIDE_DIGEST_LINUX_AMD64}; \
    elif [ "$ELIDE_ARCH" = "aarch64" ]; then \
      ELIDE_DIGEST=${ELIDE_DIGEST_LINUX_ARM64}; \
    elif [ "$ELIDE_ARCH" = "arm64" ]; then \
      ELIDE_DIGEST=${ELIDE_DIGEST_LINUX_ARM64}; \
    else \
      echo "Unsupported architecture: $ELIDE_ARCH"; exit 1; \
    fi \
    && echo "Installing Elide (arch: $ELIDE_ARCH, pin: $ELIDE_DIGEST)..." \
    && mkdir -p /tmp/elide-install \
    && cd /tmp/elide-install \
    && curl -sSL elide.sh | bash -s - -- \
      --install-rev=$ELIDE_VERSION \
      --install-digest=$ELIDE_DIGEST \
    && cd - \
    && rm -rf /tmp/elide-install
