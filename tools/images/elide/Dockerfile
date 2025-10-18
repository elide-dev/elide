FROM debian:sid-slim@sha256:3e62bb802f97815491469a127cfef29a8266977b8fde5f0c09541d761b36a6a2 AS builder

RUN apt-get update \
  && apt-get install -y \
    ca-certificates \
    curl \
    uuid-runtime \
    libgomp1 \
    libstdc++6 \
    libc-bin \
    libc6 \
  && groupadd elide \
  && useradd dev -g elide -m -s /bin/bash \
  && rm -rf /var/lib/apt/lists/*

ENV ELIDE_VERSION=1.0.0-beta10
ENV ELIDE_DIGEST_LINUX_AMD64=26eb0e66d08d97911db9d021051f87a448015a48ddfebc36e7f76f6a02af263c
ENV ELIDE_DIGEST_LINUX_ARM64=34c3aed8a9d3ac243ea62100c8ee1e17ce2d6e218f578626916feb7feb9b84ef

USER dev
RUN ELIDE_ARCH=$(uname -m) \
    && if [ "$ELIDE_ARCH" = "x86_64" ]; then \
      ELIDE_DIGEST=$ELIDE_DIGEST_LINUX_AMD64; \
    elif [ "$ELIDE_ARCH" = "aarch64" ]; then \
      ELIDE_DIGEST=$ELIDE_DIGEST_LINUX_ARM64; \
    elif [ "$ELIDE_ARCH" = "arm64" ]; then \
      ELIDE_DIGEST=$ELIDE_DIGEST_LINUX_ARM64; \
    else \
      echo "Unsupported architecture: $ELIDE_ARCH"; exit 1; \
    fi \
    && echo "Installing Elide (arch: $ELIDE_ARCH, pin: $ELIDE_DIGEST)..." \
    && mkdir -p /tmp/elide-install \
    && cd /tmp/elide-install \
    && curl -sSL elide.sh | bash -s - -- \
      --install-rev=$ELIDE_VERSION \
      --install-digest=$ELIDE_DIGEST \
      --preserve-installer \
    && cd - \
    && rm -rf /tmp/elide-install

FROM debian:sid-slim@sha256:3e62bb802f97815491469a127cfef29a8266977b8fde5f0c09541d761b36a6a2 AS runtime

LABEL org.opencontainers.image.vendor=Elide
LABEL org.opencontainers.image.title="Elide Runtime"
LABEL org.opencontainers.image.description="Elide runtime CLI as a container image"
LABEL org.opencontainers.image.version=1.0.0-beta10
LABEL org.opencontainers.image.url=https://github.com/elide-dev/elide
LABEL org.opencontainers.image.base.name=debian:sid-slim
LABEL org.opencontainers.image.base.digest=sha256:5e814211fbdb43856892292a315c740e8882387709a1f51ee266884931ff5eac
LABEL org.opencontainers.image.source=https://github.com/elide-dev/elide/blob/main/tools/images/elide/Dockerfile

RUN groupadd elide \
  && useradd dev -g elide -m -s /bin/bash \
  && ln -s /home/dev/elide/elide /bin/elide \
  && apt-get update \
  && DEBIAN_FRONTEND=noninteractive apt-get -y install --no-install-recommends \
    ca-certificates \
    curl \
    uuid-runtime \
    libgomp1 \
    libc-bin \
    libc6 \
    libstdc++6

COPY --from=builder --chown=dev:elide /home/dev/elide /home/dev/elide
COPY entrypoint.sh /home/dev/elide/entrypoint.sh
USER dev

ENV ELIDE_VERSION=1.0.0-beta10

RUN echo "Setting up Elide..." \
  && mkdir -p /home/dev/.elide /home/dev/workdir \
  && touch /home/dev/.env \
  && which elide \
  && elide --help \
  && elide info \
  && echo "Elide container image ready."

STOPSIGNAL SIGTERM
VOLUME /home/dev/workdir
WORKDIR /home/dev/workdir
ENTRYPOINT ["bash", "/home/dev/elide/entrypoint.sh"]
