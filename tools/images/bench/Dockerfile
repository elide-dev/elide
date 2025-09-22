FROM --platform=linux/amd64 debian:stable-slim AS runtime

LABEL org.opencontainers.image.vendor=Elide
LABEL org.opencontainers.image.title="Elide Runtime"
LABEL org.opencontainers.image.description="Elide bench container image"
LABEL org.opencontainers.image.version=1.0.0-beta9-bench1
LABEL org.opencontainers.image.url=https://github.com/elide-dev/elide
LABEL org.opencontainers.image.base.name=ghcr.io/elide-dev/base
LABEL org.opencontainers.image.base.digest=sha256:ce2fe9474ca168eebfd35b0c7682db11045b0f10418ec46ae9b0c7610c295913
LABEL org.opencontainers.image.source=https://github.com/elide-dev/elide/blob/main/tools/images/elide/Dockerfile

RUN groupadd elide \
  && useradd dev -g elide -m -s /bin/bash \
  && ln -s /home/dev/elide/elide /bin/elide \
  && apt-get update \
  && DEBIAN_FRONTEND=noninteractive apt-get -y install --no-install-recommends \
    libc6 \
    zlib1g \
    bash

COPY entrypoint.sh /bin/entrypoint.sh
USER dev

RUN echo "Setting up Elide..." \
  && mkdir -p /home/dev/.elide /home/dev/workdir /home/dev/elide/caches \
  && touch /home/dev/.env \
  && echo '{}' > /home/dev/package.json \
  && chmod -R 755 /home/dev/elide/caches;

ADD --chown=dev elide-edge.tgz /home/dev/elide

VOLUME /home/dev/workdir
WORKDIR /home/dev/workdir

RUN which elide \
  && elide --version \
  && elide --help \
  && elide info \
  && echo "Elide container image ready."

ENTRYPOINT ["bash", "/bin/entrypoint.sh"]
