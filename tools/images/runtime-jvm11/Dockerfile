ARG BASE_IMAGE=base
ARG BASE_VERSION=":latest"
ARG BASE_HASH="@sha256:e350485a408c626aba6e143a18bf912bb490fcc2711b5f5df0cd2cc6f12ad4b4"

FROM us-docker.pkg.dev/elide-fw/tools/${BASE_IMAGE}${BASE_VERSION}${BASE_HASH}

ARG GRAAL_EDITION=ce
ARG GRAAL_VERSION=22.3.0
ARG JAVA_VERSION=java11

LABEL org.opencontainers.image.vendor=Elide
LABEL org.opencontainers.image.title="Elide Runtime: GraalVM (JVM 11)"
LABEL org.opencontainers.image.description="JVM 11 optimized runtime for Elide."
LABEL org.opencontainers.image.version=v3-alpha1
LABEL org.opencontainers.image.url=https://github.com/elide-dev/v3
LABEL org.opencontainers.image.base.name=us-docker.pkg.dev/elide-fw/tools/base
LABEL org.opencontainers.image.source=https://github.com/elide-dev/v3/blob/v3/tools/images/runtime-jvm11/Dockerfile

RUN apt-get update \
  && apt-get -y upgrade \
  && DEBIAN_FRONTEND=noninteractive apt-get -y install --no-install-recommends \
    ca-certificates \
    curl \
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
  && rm ./graalvm.tar.gz ./graalvm.tar.gz.sha256 \
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
  PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/usr/lib/gvm/bin:/sbin:/bin
