FROM mcr.microsoft.com/vscode/devcontainers/base:ubuntu-24.04

ARG DEV_USER=dev
ARG IBAZEL_VERSION=v0.15.10

ARG NODE_VERSION="23"
ARG YARN_VERSION="berry"
ARG GRADLE_VERSION=8.12
ARG KOTLIN_VERSION=2.2.0
ARG LLVM_VERSION=18
ARG GRADLE_DIST=bin
ARG INSTALL_ZSH="true"
ARG UPGRADE_PACKAGES="true"
ARG USERNAME=${DEV_USER}
ARG USER_UID=1000
ARG USER_GID=$USER_UID
ARG GRAAL_EDITION=ce
ARG GRAAL_VERSION=24.1.2
ARG JAVA_VERSION=23
ARG JAVA_VM_VERSION=graalvm-jdk-23.0.2+7.1

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
  && export ARCH="$(uname -m)" \
  && export FORMAL_ARCH="$(uname -m)" \
  && export COMPONENTS="native-image js" \
  && if [ "$ARCH" = "x86_64" ]; then \
    ARCH="x64"; \
    FORMAL_ARCH="amd64"; \
    BAZELISK_ARCH="x86_64"; \
  fi \
  && if [ "$ARCH" = "aarch64" ]; then \
    ARCH="aarch64"; \
    FORMAL_ARCH="arm64"; \
    BAZELISK_ARCH="arm64"; \
  fi \
  && if [ "$ARCH" = "arm64" ]; then \
    ARCH="aarch64"; \
    FORMAL_ARCH="arm64"; \
    BAZELISK_ARCH="arm64"; \
  fi \
  && mkdir -p /tmp/gvm \
  && cd /tmp/gvm \
  && export GRAAL_DOWNLOAD_URL="https://download.oracle.com/graalvm/$JAVA_VERSION/latest/graalvm-jdk-${JAVA_VERSION}_linux-${ARCH}_bin.tar.gz" \
  && echo "GraalVM Download URL: $GRAAL_DOWNLOAD_URL" \
  && curl --progress-bar -SL "$GRAAL_DOWNLOAD_URL" > ./graalvm.tar.gz \
  && tar -xzvf ./graalvm.tar.gz \
  && mv "./${JAVA_VM_VERSION}" /usr/lib/gvm \
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
  && apt-key adv --keyserver keyserver.ubuntu.com --recv-keys A040830F7FAC5991 \
  && echo "Installing Bazelisk..." \
  && curl -fLO https://github.com/bazelbuild/bazelisk/releases/download/v1.15.0/bazelisk-linux-$FORMAL_ARCH \
  && mv "bazelisk-linux-$FORMAL_ARCH" "/usr/bin/bazel" \
  && chmod +x /usr/bin/bazel \
  && ln -s /usr/bin/bazel /usr/bin/bazelisk \
  && curl -o /usr/bin/bazel "https://github.com/bazelbuild/bazel-watcher/releases/download/$IBAZEL_VERSION/ibazel_linux_$FORMAL_ARCH" \
  && chmod +x /usr/bin/bazel \
  && echo "Installing Google Cloud SDK..." \
  && mkdir -p /var/lib/gcloud \
  && cd /var/lib/gcloud \
  && curl https://sdk.cloud.google.com > install_gcloud.sh \
  && bash install_gcloud.sh --disable-prompts --install-dir=/var/lib/gcloud \
  && rm -fv install_gcloud.sh \
  && cd ~ \
  && /var/lib/gcloud/google-cloud-sdk/bin/gcloud auth configure-docker us-docker.pkg.dev \
  && echo "Installing Go..." \
  && curl -fLO https://go.dev/dl/go1.19.linux-${FORMAL_ARCH}.tar.gz \
  && tar -C /usr/local -xzf go1.19.linux-${FORMAL_ARCH}.tar.gz \
  && rm go1.19.linux-${FORMAL_ARCH}.tar.gz \
  && export GOPATH="/go" \
  && mkdir -p "$GOPATH/src" "$GOPATH/bin" \
  && chmod -R 777 "$GOPATH" \
  && echo "Installing Buf..." \
  && curl -fLSO https://github.com/bufbuild/buf/releases/download/v1.11.0/buf-Linux-${BAZELISK_ARCH} \
  && mv buf-Linux-${BAZELISK_ARCH} /usr/local/bin/buf \
  && chmod +x /usr/local/bin/buf \
  && echo "Setting up Debian-based codespace..." \
  && yes | unminimize 2>&1 \
  && rm -rf /var/lib/apt/lists/*

RUN echo "Preparing dev root..." \
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
  && rm -rf /var/lib/apt/lists/* \
  && chown -R "$DEV_USER" /home/dev \
  && chmod 755 /usr/local \
  && chown "$DEV_USER" /usr/local \
  && sudo -u "$DEV_USER" mkdir -p /home/dev/.ssh \
  && sudo -u "$DEV_USER" bash -c 'sh -c "$(curl -fsSL https://raw.githubusercontent.com/ohmyzsh/ohmyzsh/master/tools/install.sh)"' \
  && echo "Installing Rust..." \
  && sudo -u "$DEV_USER" bash -c "curl https://sh.rustup.rs -sSf | bash -s -- -y" \
  && echo 'source $HOME/.cargo/env' >> $HOME/.bashrc \
  && echo 'source $HOME/.cargo/env' >> $HOME/.zshrc \
  && sudo -u "$DEV_USER" bash -c "/home/dev/.cargo/bin/rustup toolchain install nightly" \
  && sudo -u "$DEV_USER" bash -c "/home/dev/.cargo/bin/rustup default nightly" \
  && sudo -u "$DEV_USER" bash -c "/home/dev/.cargo/bin/rustup target add wasm32-unknown-unknown" \
  && sudo -u "$DEV_USER" bash -c "/home/dev/.cargo/bin/rustup target add wasm32-wasip1" \
  && echo "Codespace image ready."

ENV GRADLE_HOME="/opt/gradle/gradle" \
  KOTLIN_HOME="/opt/kotlin/kotlincc" \
  JAVA_HOME="/usr/lib/gvm" \
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
COPY zshrc /home/$DEV_USER/.zshrc

USER ${DEV_USER}
WORKDIR /home/${DEV_USER}
