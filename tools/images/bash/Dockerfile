FROM debian:stable-slim@sha256:2b2e35d67c8fda0ba853d40cd18e57b99ab12d82fd3200607015eb09784068bd AS base

LABEL org.opencontainers.image.vendor=Elide
LABEL org.opencontainers.image.title="Elide Runtime with Bash"
LABEL org.opencontainers.image.description="Elide runtime CLI as a container image, with Bash and several tools"
LABEL org.opencontainers.image.version=1.0.0-alpha9
LABEL org.opencontainers.image.url=https://github.com/elide-dev/elide
LABEL org.opencontainers.image.base.name=debian:stable-slim
LABEL org.opencontainers.image.base.digest=sha256:2b2e35d67c8fda0ba853d40cd18e57b99ab12d82fd3200607015eb09784068bd
LABEL org.opencontainers.image.source=https://github.com/elide-dev/elide/blob/main/tools/images/bash/Dockerfile

COPY ./elide /home/dev/.elide

RUN apt-get update \
  && DEBIAN_FRONTEND=noninteractive apt-get -y install --no-install-recommends \
    bash \
    ca-certificates \
    curl \
    uuid-runtime \
    locales \
  && sed -i '/en_US.UTF-8/s/^# //g' /etc/locale.gen \
  && locale-gen en_US.UTF-8 \
  && rm -rf /var/lib/apt/lists/* \
  && groupadd elide \
  && useradd dev -g elide -m -s /bin/bash \
  && ln -s /home/dev/elide/elide /bin/elide \
  && chown -R dev /home/dev

ENV LANG=en_US.UTF-8
USER dev

RUN echo "Installing Elide..." \
  && curl -sSL elide.sh | bash -s - \
  && which elide \
  && elide --help \
  && elide selftest \
  && elide info \
  && echo "Elide/Bash container image ready."
