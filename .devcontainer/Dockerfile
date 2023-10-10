FROM ghcr.io/elide-dev/elide AS elide

FROM mcr.microsoft.com/devcontainers/base:ubuntu-22.04

LABEL org.opencontainers.image.vendor=Elide
LABEL org.opencontainers.image.title="Elide Codespace"
LABEL org.opencontainers.image.description="Elide runtime within a GitHub Codespace"
LABEL org.opencontainers.image.version=1.0.0-alpha7
LABEL org.opencontainers.image.url=https://github.com/elide-dev/elide
LABEL org.opencontainers.image.base.name=mcr.microsoft.com/devcontainers/base
LABEL org.opencontainers.image.source=https://github.com/elide-dev/elide/blob/main/.devcontainer/Elide.devcontainer

RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get -y install --no-install-recommends \
      bash \
      ca-certificates \
      curl \
      uuid-runtime \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /usr/lib/elide /home/vscode/.elide \
    && echo "Installing Elide..." \
    && curl -sSL elide.sh | bash -s - --install-dir=/usr/lib/elide \
    && ln -s /usr/lib/elide/elide /bin/elide \
    && which elide \
    && elide --help \
    && elide selftest \
    && elide info \
    && echo "Elide codespace container image ready."

ENTRYPOINT ["/usr/local/share/docker-init.sh"]
CMD ["sleep", "infinity"]
