ARG BASE_ARCH="amd64"

FROM --platform=linux/${BASE_ARCH} mcr.microsoft.com/vscode/devcontainers/base:ubuntu-20.04

ARG DEV_USER=dev
ARG IBAZEL_VERSION=v0.15.10

ARG NODE_VERSION="17"
ARG YARN_VERSION="berry"
ARG GRADLE_VERSION=7.6
ARG KOTLIN_VERSION=1.7.22
ARG GRADLE_DIST=bin
ARG INSTALL_ZSH="true"
ARG UPGRADE_PACKAGES="true"
ARG USERNAME=${DEV_USER}
ARG USER_UID=1000
ARG USER_GID=$USER_UID
ARG GRAAL_EDITION=ce
ARG GRAAL_VERSION=22.3.0
ARG GRAAL_ARCH=amd64
ARG JAVA_VERSION=java19

RUN apt-get update \
  && apt-get -y upgrade \
  && DEBIAN_FRONTEND=noninteractive apt-get -y install --no-install-recommends \
    ca-certificates \
    curl \
    git \
    gnupg \
    libc-dev \
  && apt-get update \
  && apt-get install -y wget tar \
  && set -eux \
  && apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 0xB1998361219BD9C9 \
  && DEBIAN_FRONTEND=noninteractive apt-get install -y \
    apt-transport-https \
    autoconf \
    automake \
    autotools-dev \
    bash \
    build-essential \
    ca-certificates \
    clang \
    curl \
    g++ \
    gcc \
    git \
    gnupg \
    libc6-dev \
    libpng-dev \
    libtool \
    make \
    pkg-config \
    qt5-default \
    software-properties-common \
    tar \
    time \
    cron \
    htop \
    jq \
    lcov \
    logrotate \
    lsb-release \
    sudo \
    fakeroot \
    multitail \
    python2 \
    python2-dev \
    python3 \
    python3-dev \
    python3-pip \
    python3-wheel \
    python3-venv \
    python3-setuptools \
    rsyslog \
    tree \
    tmux \
    vim \
    unzip \
    wget \
    zlib1g \
  && cd ~ \
  && mkdir -p /tmp/gvm \
  && cd /tmp/gvm \
  && export GRAAL_DOWNLOAD_URL="https://github.com/graalvm/graalvm-$GRAAL_EDITION-builds/releases/download/vm-$GRAAL_VERSION/graalvm-$GRAAL_EDITION-$JAVA_VERSION-linux-$GRAAL_ARCH-$GRAAL_VERSION.tar.gz" \
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
  && echo "Installing NodeJS (v$NODE_VERSION / Yarn $YARN_VERSION)..." \
  && curl -sL "https://deb.nodesource.com/setup_$NODE_VERSION.x" | bash - \
  && DEBIAN_FRONTEND=noninteractive apt-get install -y nodejs \
  && npm install -g "yarn@$YARN_VERSION" \
  && echo "Installing Gradle..." \
  && mkdir -p /opt/gradle \
  && cd /opt/gradle \
  && wget -q "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-$GRADLE_DIST.zip" \
  && unzip gradle*.zip \
  && ls -d */ | sed 's/\/*$//g' | xargs -I{} mv {} gradle \
  && rm gradle*.zip \
  && echo "Installing Kotlin..." \
  && mkdir -p /opt/kotlin \
  && cd /opt/kotlin \
  && wget -q "https://github.com/JetBrains/kotlin/releases/download/v$KOTLIN_VERSION/kotlin-compiler-$KOTLIN_VERSION.zip" \
  && unzip *kotlin*.zip \
  && rm *kotlin*.zip \
  && echo "Installing Chrome..." \
  && wget -q https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb \
  && apt install -y ./google-chrome-stable_current_amd64.deb \
  && rm -f google-chrome-stable_current_amd64.deb \
  && apt-key adv --keyserver keyserver.ubuntu.com --recv-keys A040830F7FAC5991 \
  && echo "Installing Bazelisk..." \
  && curl -LO "https://github.com/bazelbuild/bazelisk/releases/download/v1.11.0/bazelisk-linux-amd64" \
  && mv bazelisk-linux-amd64 "/usr/bin/bazel" \
  && chmod +x /usr/bin/bazel \
  && ln -s /usr/bin/bazel /usr/bin/bazelisk \
  && echo "Installing Google Cloud SDK..." \
  && mkdir -p /var/lib/gcloud \
  && cd /var/lib/gcloud \
  && curl https://sdk.cloud.google.com > install_gcloud.sh \
  && bash install_gcloud.sh --disable-prompts --install-dir=/var/lib/gcloud \
  && rm -fv install_gcloud.sh \
  && cd ~ \
  && /var/lib/gcloud/google-cloud-sdk/bin/gcloud auth configure-docker us-docker.pkg.dev \
  && export GOPATH="/go" \
  && mkdir -p "$GOPATH/src" "$GOPATH/bin" \
  && chmod -R 777 "$GOPATH" \
  && echo "Installing GraalVM modules..." \
  && $GRAALVM_HOME/bin/gu install native-image js \
  && rm -rf /var/lib/apt/lists/* \
  && echo "Setting up Debian-based codespace..." \
  && yes | unminimize 2>&1 \
  && echo "Installing updates and dev packages..." \
  && groupadd engineering \
  && useradd -g engineering -m dev \
  && usermod -aG sudo dev \
  && mkdir -p /home/dev \
  && chown dev:engineering /home/dev \
  && echo "dev  ALL=(ALL) NOPASSWD:ALL" >> /etc/sudoers \
  && echo "%engineering  ALL=(ALL) NOPASSWD:ALL" >> /etc/sudoers \
  && echo "$DEV_USER  ALL=(ALL) NOPASSWD:ALL" >> /etc/sudoers \
  && echo "%dev  ALL=(ALL) NOPASSWD:ALL" >> /etc/sudoers \
  && chsh -s /bin/zsh "$DEV_USER" \
  && echo "Build tools ready."

ENV GRADLE_HOME="/opt/gradle" \
  KOTLIN_HOME="/opt/kotlinc" \
  JAVA_HOME="/usr/lib/jvm/gvm" \
  GRAALVM_HOME="/usr/lib/gvm" \
  GOPATH="/go" \
  QTWEBENGINE_DISABLE_SANDBOX="1" \
  PATH="$GRADLE_HOME/bin:$KOTLIN_HOME/bin:$GOPATH/bin:/home/$DEV_USER/bin:/usr/local/go/bin:/var/lib/gcloud/google-cloud-sdk/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/usr/lib/gvm/bin:/sbin:/bin" \
  EDITOR=code \
  SHELL=zsh \
  LANG=en_US.UTF-8 \
  LC_CTYPE=en_US.UTF-8 \
  LC_ALL=en_US.UTF-8 \
  TZ=America/Los_Angeles \
  DOCKER_BUILDKIT=1 \
  ELIDE_TEST_LOCAL=true

COPY alias /home/$DEV_USER/.alias

RUN echo "source ~/.profile" >> ~/.zshrc \
  && rm -rf /var/lib/apt/lists/* \
  && mkdir -p /home/dev/bin \
  && curl -o /home/dev/bin/ibazel "https://github.com/bazelbuild/bazel-watcher/releases/download/$IBAZEL_VERSION/ibazel_linux_amd64" \
  && yes Y | gcloud components install kubectl \
  && echo "Codespace image ready."

COPY zshrc /home/$DEV_USER/.zshrc
USER ${DEV_USER}
WORKDIR /home/${DEV_USER}
