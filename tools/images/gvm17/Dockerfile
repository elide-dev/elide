ARG BASE_VERSION=":latest"
ARG BASE_HASH="@sha256:f5f4144d9f3aae5a8f848d83986e30acf6a1d8356a362685206385d19a2212d5"

FROM us-docker.pkg.dev/elide-fw/tools/base${BASE_VERSION}${BASE_HASH}

ARG GRAAL_EDITION=ce
ARG GRAAL_VERSION=22.3.0
ARG JAVA_VERSION=java17

LABEL org.opencontainers.image.vendor=Elide
LABEL org.opencontainers.image.title="Elide Tools: GraalVM (Java 17)"
LABEL org.opencontainers.image.description="Java 17 tools for Elide/GraalVM."
LABEL org.opencontainers.image.version=v3-alpha1
LABEL org.opencontainers.image.url=https://github.com/elide-dev/v3
LABEL org.opencontainers.image.base.name=us-docker.pkg.dev/elide-fw/tools/base
LABEL org.opencontainers.image.source=https://github.com/elide-dev/v3/blob/v3/tools/images/gvm17/Dockerfile

RUN apt-get update \
  && apt-get -y upgrade \
  && DEBIAN_FRONTEND=noninteractive apt-get -y install --no-install-recommends \
    ca-certificates \
    curl \
    git \
    gnupg \
    libc-dev \
  && mkdir -p /tmp/gvm \
  && cd /tmp/gvm \
  && export ARCH="$(uname -m)" \
  && export COMPONENTS="native-image js espresso" \
  && if [ "$ARCH" = "x86_64" ] ; then ARCH="amd64"; COMPONENTS="native-image js espresso wasm"; fi \
  && if [ "$ARCH" = "arm64" ] ; then ARCH="aarch64"; fi \
  && export GRAAL_DOWNLOAD_URL="https://github.com/graalvm/graalvm-$GRAAL_EDITION-builds/releases/download/vm-$GRAAL_VERSION/graalvm-$GRAAL_EDITION-$JAVA_VERSION-linux-$ARCH-$GRAAL_VERSION.tar.gz" \
  && echo "GraalVM Download URL: $GRAAL_DOWNLOAD_URL" \
  && curl --progress-bar -SL "$GRAAL_DOWNLOAD_URL" > ./graalvm.tar.gz \
  && curl --progress-bar -sSL "$GRAAL_DOWNLOAD_URL.sha256" > ./graalvm.tar.gz.sha256 \
  && ls -la ./graalvm.tar.gz ./graalvm.tar.gz.sha256 \
  && echo "Downloaded checksum for GraalVM: $(cat ./graalvm.tar.gz.sha256)" \
  && echo "$(cat ./graalvm.tar.gz.sha256) graalvm.tar.gz" | sha256sum --check --status \
  && tar -xzvf ./graalvm.tar.gz \
  && mv "./graalvm-$GRAAL_EDITION-$JAVA_VERSION-$GRAAL_VERSION" /usr/lib/gvm \
  && rm -f ./graalvm.tar.gz \
  && cd / \
  && rm -fr /tmp/gvm \
  && export JAVA_HOME=/usr/lib/gvm \
    GRAALVM_HOME=/usr/lib/gvm \
    PATH=/usr/lib/gvm/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin \
  && echo "Installing GraalVM modules..." \
  && $GRAALVM_HOME/bin/gu install $COMPONENTS \
  && rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/usr/lib/gvm \
  GRAALVM_HOME=/usr/lib/gvm \
  LC_CTYPE=en_US.UTF-8 \
  LC_ALL=en_US.UTF-8 \
  PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/usr/lib/gvm/bin:/sbin:/bin
