ARG ELIDE_VERSION=1.0.0-beta7
ARG ELIDE_DIGEST=4600633661497a050270e35428e3d5bcc8ed48ed27e0197def683f38b63b600c

FROM debian:unstable-slim@sha256:5a1fa4e7ca7e4a8ea0449d0e5e6ac6593b4aebac12f658e69af354c0b3cb073a AS builder

RUN apt-get update \
  && apt-get install -y \
    bash \
    ca-certificates=20250419 \
    curl=8.13.0-5 \
    uuid-runtime=2.41-5 \
    libgomp1=14.2.0-19 \
    libc-bin=2.41-8 \
    libc6=2.41-8 \
    libstdc++6=14.2.0-19 \
  && groupadd elide \
  && useradd dev -g elide -m -s /bin/bash \
  && rm -rf /var/lib/apt/lists/*

USER dev
RUN echo "Installing Elide..." \
      && mkdir -p /tmp/elide-install \
      && cd /tmp/elide-install \
      && curl -sSL elide.sh | bash -s - -- \
        --install-rev=1.0.0-beta7 \
        --install-digest=4600633661497a050270e35428e3d5bcc8ed48ed27e0197def683f38b63b600c \
        --preserve-installer \
      && cd - \
      && rm -rf /tmp/elide-install

FROM debian:unstable-slim@sha256:5a1fa4e7ca7e4a8ea0449d0e5e6ac6593b4aebac12f658e69af354c0b3cb073a AS runtime

LABEL org.opencontainers.image.vendor=Elide
LABEL org.opencontainers.image.title="Elide Runtime"
LABEL org.opencontainers.image.description="Elide runtime CLI as a container image"
LABEL org.opencontainers.image.version=1.0.0-beta7
LABEL org.opencontainers.image.url=https://github.com/elide-dev/elide
LABEL org.opencontainers.image.base.name=debian:unstable-slim
LABEL org.opencontainers.image.base.digest=sha256:93700abbae646fd44d84e6e9fd85f024e6dd010e4acec250e48fc7d1b3690392
LABEL org.opencontainers.image.source=https://github.com/elide-dev/elide/blob/main/tools/images/elide/Dockerfile

RUN groupadd elide \
  && useradd dev -g elide -m -s /bin/bash \
  && ln -s /home/dev/elide/elide /bin/elide \
  && apt-get update \
  && DEBIAN_FRONTEND=noninteractive apt-get -y install --no-install-recommends \
    bash \
    ca-certificates=20250419 \
    curl=8.13.0-5 \
    uuid-runtime=2.41-5 \
    libgomp1=14.2.0-19 \
    libc-bin=2.41-8 \
    libc6=2.41-8 \
    libstdc++6=14.2.0-19

COPY --from=builder --chown=dev:elide /home/dev/elide /home/dev/elide
COPY entrypoint.sh /home/dev/elide/entrypoint.sh
USER dev

RUN echo "Setting up Elide..." \
  && mkdir -p /home/dev/.elide /home/dev/workdir \
  && touch /home/dev/.env \
  && echo '{}' > /home/dev/package.json \
  && which elide \
  && elide --help \
  && elide info \
  && echo "Elide container image ready."

STOPSIGNAL SIGTERM
VOLUME /home/dev/workdir
WORKDIR /home/dev/workdir
ENTRYPOINT ["bash", "/home/dev/elide/entrypoint.sh"]
