ARG UBUNTU_VERSION=24.04
ARG UBUNTU_HASH="@sha256:6015f66923d7afbc53558d7ccffd325d43b4e249f41a6e93eef074c9505d2233"

FROM ubuntu:${UBUNTU_VERSION}${UBUNTU_HASH}

LABEL org.opencontainers.image.vendor=Elide
LABEL org.opencontainers.image.title="Elide Distribution Builder"
LABEL org.opencontainers.image.description="Builder image for Elide dists like deb, rpm, and apk"
LABEL org.opencontainers.image.url=https://github.com/elide-dev/elide
LABEL org.opencontainers.image.base.name=ubuntu
LABEL org.opencontainers.image.base.digest=sha256:6015f66923d7afbc53558d7ccffd325d43b4e249f41a6e93eef074c9505d2233
LABEL org.opencontainers.image.source=https://github.com/elide-dev/elide/blob/main/tools/images/dist/Dockerfile

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        ca-certificates=20240203 \
        curl=8.5.0-2ubuntu10.6 \
        gnupg2=2.4.4-2ubuntu17.3 \
        lsb-release=12.0-2 \
        software-properties-common=0.99.49.2 \
        debsigs=0.1.26 \
        rpm=4.18.2+dfsg-2.1build2 \
        ruby=1:3.2~ubuntu1 \
        golang=2:1.22~2build1 \
        unzip=6.0-28ubuntu4.1 \
        fakeroot=1.33-1 \
        sudo=1.9.15p5-3ubuntu5.24.04.1 \
        wget=1.21.4-1ubuntu4.1 \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd elide \
    && useradd -m -g elide -s /bin/bash elide \
    && echo "elide ALL=(ALL) NOPASSWD: ALL" >> /etc/sudoers

RUN curl -O https://downloads.rclone.org/rclone-current-linux-amd64.zip \
    && unzip rclone-current-linux-amd64.zip \
    && cd rclone-*-linux-amd64 \
    && cp rclone /usr/bin/ \
    && chown root:root /usr/bin/rclone \
    && chmod 755 /usr/bin/rclone \
    && cd .. \
    && rm -rf rclone-*-linux-amd64 rclone-*.zip

RUN (type -p wget >/dev/null || (apt update && apt install wget -y)) \
    && mkdir -p -m 755 /etc/apt/keyrings \
    && out=$(mktemp) && wget -nv -O$out https://cli.github.com/packages/githubcli-archive-keyring.gpg \
    && cat $out | tee /etc/apt/keyrings/githubcli-archive-keyring.gpg > /dev/null \
    && chmod go+r /etc/apt/keyrings/githubcli-archive-keyring.gpg \
    && mkdir -p -m 755 /etc/apt/sources.list.d \
    && echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/githubcli-archive-keyring.gpg] https://cli.github.com/packages stable main" | tee /etc/apt/sources.list.d/github-cli.list > /dev/null \
    && apt update \
    && apt install gh -y

RUN gem install fpm

USER elide

RUN rclone --version \
    && fpm --version \
    && echo "Dist builder ready."
