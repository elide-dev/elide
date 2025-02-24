ARG BASE_VERSION=":latest"
ARG BASE_HASH="@sha256:89ed76666b460e342e3103d3498dd8b75a027b1273714547831cace58b74b590"
FROM ghcr.io/elide-dev/base${BASE_VERSION}${BASE_HASH}

ARG GRAAL_VERSION=23.0.1
ARG JAVA_VERSION=17
ARG GRAAL_RELEASE="17.0.11+7.1"

LABEL org.opencontainers.image.vendor=Elide
LABEL org.opencontainers.image.title="Elide Tools: GraalVM (Java 17)"
LABEL org.opencontainers.image.description="Java 17 tools for Elide/GraalVM"
LABEL org.opencontainers.image.version=v1.0.0-alpha13
LABEL org.opencontainers.image.url=https://github.com/elide-dev/elide
LABEL org.opencontainers.image.base.name=us-docker.pkg.dev/elide-fw/tools/base
LABEL org.opencontainers.image.source=https://github.com/elide-dev/elide/blob/main/tools/images/gvm17/Dockerfile

RUN apt-get update \
  && apt-get -y upgrade \
  && DEBIAN_FRONTEND=noninteractive apt-get -y install --no-install-recommends \
    ca-certificates \
    curl \
    git \
    gnupg \
    libc-dev \
    build-essential \
  && mkdir -p /tmp/gvm \
  && cd /tmp/gvm \
  && export ARCH="$(uname -m)" \
  && export COMPONENTS="native-image js" \
  && if [ "$ARCH" = "x86_64" ]; then \
    ARCH="x64" \
    COMPONENTS="native-image js espresso"; \
  fi \
  && if [ "$ARCH" = "arm64" ]; then ARCH="aarch64"; fi \
  && export GRAAL_DOWNLOAD_URL="https://download.oracle.com/graalvm/$JAVA_VERSION/latest/graalvm-jdk-${JAVA_VERSION}_linux-${ARCH}_bin.tar.gz" \
  && echo "GraalVM Download URL: $GRAAL_DOWNLOAD_URL" \
  && curl --progress-bar -SL "$GRAAL_DOWNLOAD_URL" > ./graalvm.tar.gz \
  && curl --progress-bar -sSL "$GRAAL_DOWNLOAD_URL.sha256" > ./graalvm.tar.gz.sha256 \
  && ls -la ./graalvm.tar.gz ./graalvm.tar.gz.sha256 \
  && echo "Downloaded checksum for GraalVM: $(cat ./graalvm.tar.gz.sha256)" \
  && echo "$(cat ./graalvm.tar.gz.sha256) graalvm.tar.gz" | sha256sum --check --status \
  && tar -xzvf ./graalvm.tar.gz \
  && mv "./graalvm-jdk-${GRAAL_RELEASE}" /usr/lib/gvm \
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
