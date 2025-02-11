ARG BASE_VERSION=":latest"
ARG BASE_HASH="@sha256:6bd4fc792e7a0705c1df80ba4be73a647b3e90ed41ec0559a04148f81ccb17c5"

FROM ghcr.io/elide-dev/base${BASE_VERSION}${BASE_HASH}

ARG JAVA_VERSION=zulu22

LABEL org.opencontainers.image.vendor=Elide
LABEL org.opencontainers.image.title="Elide Tools: Zulu JDK (Java 22)"
LABEL org.opencontainers.image.description="Java 22 tools for Elide."
LABEL org.opencontainers.image.version=v1.0.0-alpha13
LABEL org.opencontainers.image.url=https://github.com/elide-dev/elide
LABEL org.opencontainers.image.base.name=us-docker.pkg.dev/elide-fw/tools/base
LABEL org.opencontainers.image.source=https://github.com/elide-dev/elide/blob/main/tools/images/jdk22/Dockerfile

RUN apt-get update \
  && apt-get -y upgrade \
  && DEBIAN_FRONTEND=noninteractive apt-get -y install --no-install-recommends \
    ca-certificates \
    curl \
    git \
    gnupg \
    libc-dev \
  && apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 0xB1998361219BD9C9 \
  && curl -O https://cdn.azul.com/zulu/bin/zulu-repo_1.0.0-3_all.deb \
  && apt-get install ./zulu-repo_1.0.0-3_all.deb \
  && rm -fv /zulu-repo_1.0.0-3_all.deb \
  && apt-get update \
  && apt-get install -y "${JAVA_VERSION}-jdk" wget tar \
  && rm -rf /var/lib/apt/lists/* \
  && echo "JDK image ready."

ENV LC_CTYPE=en_US.UTF-8 \
  LC_ALL=en_US.UTF-8 \
  JAVA_HOME="/usr/lib/jvm/${JAVA_VERSION}" \
  PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
