FROM debian:stable-slim@sha256:2b2e35d67c8fda0ba853d40cd18e57b99ab12d82fd3200607015eb09784068bd AS builder

RUN apt-get update \
  && apt-get install -y \
    bash \
    ca-certificates \
    curl \
    uuid-runtime \
  && groupadd elide \
  && useradd dev -g elide -m -s /bin/bash \
  && rm -rf /var/lib/apt/lists/*

USER dev
RUN echo "Installing Elide..." && curl -sSL elide.sh | bash -s -

FROM debian:stable-slim@sha256:2b2e35d67c8fda0ba853d40cd18e57b99ab12d82fd3200607015eb09784068bd AS runtime

LABEL org.opencontainers.image.vendor=Elide
LABEL org.opencontainers.image.title="Elide Runtime"
LABEL org.opencontainers.image.description="Elide runtime CLI as a container image"
LABEL org.opencontainers.image.version=1.0.0-alpha9
LABEL org.opencontainers.image.url=https://github.com/elide-dev/elide
LABEL org.opencontainers.image.base.name=debian:stable-slim
LABEL org.opencontainers.image.base.digest=sha256:2b2e35d67c8fda0ba853d40cd18e57b99ab12d82fd3200607015eb09784068bd
LABEL org.opencontainers.image.source=https://github.com/elide-dev/elide/blob/main/tools/images/elide/Dockerfile

RUN groupadd elide \
  && useradd dev -g elide -m -s /bin/bash \
  && ln -s /home/dev/elide/elide /bin/elide \
  && apt-get update \
  && DEBIAN_FRONTEND=noninteractive apt-get -y install --no-install-recommends \
    libc6 \
    zlib1g \
    bash \
    ca-certificates \
    curl \
    uuid-runtime \
    locales \
  && sed -i '/en_US.UTF-8/s/^# //g' /etc/locale.gen \
  && locale-gen en_US.UTF-8

ENV LANG=en_US.UTF-8

COPY --from=builder --chown=dev:elide /home/dev/elide /home/dev/elide
USER dev
COPY ./elide /home/dev/.elide
COPY entrypoint.sh /home/dev/elide/entrypoint.sh

RUN echo "Setting up Elide..." \
  && mkdir -p /home/dev/.elide /home/dev/workdir \
  && touch /home/dev/.env \
  && echo '{}' > /home/dev/package.json \
  && which elide \
  && elide --help \
  && elide selftest \
  && elide info \
  && echo "Elide container image ready."

STOPSIGNAL SIGTERM
VOLUME /home/dev/workdir
WORKDIR /home/dev/workdir
ENTRYPOINT ["bash", "/home/dev/elide/entrypoint.sh"]
