ARG ELIDE_VERSION=1.0.0-beta3-rc4
ARG ELIDE_DIGEST=a907d6750d9cfad1d81ad33ce436e0e19065a638b516f2b5d633a58843a89cbf

FROM debian:unstable-slim@sha256:93700abbae646fd44d84e6e9fd85f024e6dd010e4acec250e48fc7d1b3690392 AS base

LABEL org.opencontainers.image.vendor=Elide
LABEL org.opencontainers.image.title="Elide Runtime with Bash"
LABEL org.opencontainers.image.description="Elide runtime CLI as a container image, with Bash and several tools"
LABEL org.opencontainers.image.version=1.0.0-beta3-rc4
LABEL org.opencontainers.image.url=https://github.com/elide-dev/elide
LABEL org.opencontainers.image.base.name=debian:unstable-slim
LABEL org.opencontainers.image.base.digest=sha256:93700abbae646fd44d84e6e9fd85f024e6dd010e4acec250e48fc7d1b3690392
LABEL org.opencontainers.image.source=https://github.com/elide-dev/elide/blob/main/tools/images/bash/Dockerfile

RUN apt-get update \
  && DEBIAN_FRONTEND=noninteractive apt-get -y install --no-install-recommends \
    bash \
    ca-certificates=20250419 \
    curl=8.13.0-5 \
    uuid-runtime=2.41-5 \
    libgomp1=14.2.0-19 \
    libc-bin=2.41-8 \
    libc6=2.41-8 \
    libstdc++6=14.2.0-19 \
  && rm -rf /var/lib/apt/lists/* \
  && groupadd elide \
  && useradd dev -g elide -m -s /bin/bash \
  && ln -s /home/dev/elide/elide /bin/elide

USER dev

RUN echo "Installing Elide..." \
  && curl -sSL elide.sh | bash -s - -- \
    --install-rev=1.0.0-beta3-rc4 \
    --install-digest=a907d6750d9cfad1d81ad33ce436e0e19065a638b516f2b5d633a58843a89cbf \
    --installer-format=txz \
    --preserve-installer \
  && rm -fv elide.txz elide.txz.sha256 \
  && which elide \
  && elide --help \
  && elide info \
  && echo "Elide/Bash container image ready."
