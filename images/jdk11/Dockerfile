ARG ARCH=amd64
ARG BASE_VERSION=":latest"
ARG BASE_HASH="@sha256:060b119fb2bf0f65fe5d1927c238516562638bdd2bc581d06de4d0c5443bad72"

FROM --platform=linux/${ARCH} us-docker.pkg.dev/elide-fw/tools/base${BASE_VERSION}${BASE_HASH}

ARG JAVA_VERSION=java11

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
  && apt-get install -y zulu11-jdk wget tar \
  && rm -rf /var/lib/apt/lists/* \
  && echo "JDK image ready."

ENV LC_CTYPE=en_US.UTF-8 \
  LC_ALL=en_US.UTF-8 \
  JAVA_HOME="/usr/lib/jvm/zulu11" \
  PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
