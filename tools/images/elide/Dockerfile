FROM debian:sid-slim@sha256:7e8e5af19ae9e22c326f52aa30a65dd635c0e45430e322fc2aa64455c9ad624c AS builder

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

ENV ELIDE_VERSION=1.0.0-beta9
ENV ELIDE_DIGEST_LINUX_AMD64=a5430b7e398c12b66fc6a13a1ac3fd1a7622db48b0a312ccce6232aafa40d23d
ENV ELIDE_DIGEST_LINUX_ARM64=f5bdc8845726cb32564cd8ea97c286e8c5ef243662bb52480ff553c29c7231d7

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

FROM debian:sid-slim@sha256:7e8e5af19ae9e22c326f52aa30a65dd635c0e45430e322fc2aa64455c9ad624c AS runtime

LABEL org.opencontainers.image.vendor=Elide
LABEL org.opencontainers.image.title="Elide Runtime"
LABEL org.opencontainers.image.description="Elide runtime CLI as a container image"
LABEL org.opencontainers.image.version=1.0.0-beta9
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

ENV ELIDE_VERSION=1.0.0-beta9

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
