ARG BASE_IMAGE=gvm17
ARG BASE_VERSION=":latest"
ARG BASE_HASH="@sha256:7db6e37eb898adc48fc1251997374ecc765b9a85a191d89c30adbfc41cc83883"

FROM us-docker.pkg.dev/elide-fw/tools/${BASE_IMAGE}${BASE_VERSION}${BASE_HASH}

ARG JDK_VERSION="17"
ARG NODE_VERSION="18"
ARG YARN_VERSION="berry"
ARG GRADLE_VERSION=7.6
ARG KOTLIN_VERSION=1.8.0-RC
ARG BAZELISK_VERSION=v1.15.0
ARG GRADLE_DIST=bin

LABEL org.opencontainers.image.vendor=Elide
LABEL org.opencontainers.image.title="Elide Builder"
LABEL org.opencontainers.image.description="Builder image for Elide apps and tools"
LABEL org.opencontainers.image.version=v3-alpha1
LABEL org.opencontainers.image.url=https://github.com/elide-dev/v3
LABEL org.opencontainers.image.base.name=us-docker.pkg.dev/elide-fw/tools/base
LABEL org.opencontainers.image.source=https://github.com/elide-dev/v3/blob/v3/tools/images/builder/Dockerfile

RUN echo "Building Elide builder image..." \
  && set -eux \
  && apt-get update \
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
    unzip \
    wget \
    sudo \
    zlib1g \
  && export ARCH="$(uname -m)" \
  && export FORMAL_ARCH="$(uname -m)" \
  && export COMPONENTS="native-image js" \
  && if [ "$ARCH" = "x86_64" ] ; then ARCH="amd64"; COMPONENTS="native-image js wasm"; fi \
  && if [ "$ARCH" = "arm64" ] ; then FORMAL_ARCH="arm64"; fi \
  && if [ "$ARCH" = "aarch64" ] ; then FORMAL_ARCH="arm64"; fi \
  && cd ~ \
  && echo "Installing NodeJS (v$NODE_VERSION / Yarn $YARN_VERSION)..." \
  && curl -sL "https://deb.nodesource.com/setup_$NODE_VERSION.x" | bash - \
  && DEBIAN_FRONTEND=noninteractive apt-get install -y nodejs \
  && npm install -g "yarn@$YARN_VERSION" "pnpm" \
  && cd /opt \
  && echo "Installing Docker..." \
  && curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg \
  && echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu \
         $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null \
  && apt-get update \
  && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends docker-ce docker-ce-cli containerd.io \
  && echo "Installing GraalVM modules..." \
  && $GRAALVM_HOME/bin/gu install $COMPONENTS \
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
  && curl -fsLO "https://github.com/bazelbuild/bazelisk/releases/download/$BAZELISK_VERSION/bazelisk-linux-$FORMAL_ARCH" \
  && mv "bazelisk-linux-$FORMAL_ARCH" "/usr/bin/bazel" \
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
  && rm -rf /var/lib/apt/lists/* \
  && echo "Builder image ready."

ENV GRADLE_HOME="/opt/gradle" \
  KOTLIN_HOME="/opt/kotlinc" \
  JAVA_HOME="/usr/lib/gvm" \
  GRAALVM_HOME="/usr/lib/gvm" \
  QTWEBENGINE_DISABLE_SANDBOX="1" \
  PATH="$GRADLE_HOME/bin:$KOTLIN_HOME/bin:$GOPATH/bin:/usr/local/go/bin:/var/lib/gcloud/google-cloud-sdk/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/usr/lib/gvm/bin:/sbin:/bin"
