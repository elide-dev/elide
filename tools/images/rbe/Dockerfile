ARG BASE_IMAGE=gvm17
ARG BASE_VERSION=":latest"
ARG BASE_HASH="@sha256:90f12ce02cac21f3c90850966d4829ed53f4757d8dd20c799e73401dce06dc21"

FROM us-docker.pkg.dev/elide-fw/tools/${BASE_IMAGE}${BASE_VERSION}${BASE_HASH}

ARG JDK_VERSION="17"
ARG NODE_VERSION="18"
ARG YARN_VERSION="berry"
ARG GRADLE_VERSION=7.6
ARG KOTLIN_VERSION=1.9.20-Beta2
ARG BAZELISK_VERSION=v1.15.0
ARG GRADLE_DIST=bin
ARG GRAALVM_HOME=/usr/lib/gvm
ARG ZULU_VERSION=zulu17

LABEL org.opencontainers.image.vendor=Elide
LABEL org.opencontainers.image.title="Elide Builder: RBE"
LABEL org.opencontainers.image.description="Remote build executor for Elide projects"
LABEL org.opencontainers.image.version=v3-alpha1
LABEL org.opencontainers.image.url=https://github.com/elide-dev/v3
LABEL org.opencontainers.image.base.name=us-docker.pkg.dev/elide-fw/tools/base
LABEL org.opencontainers.image.source=https://github.com/elide-dev/v3/blob/v3/tools/images/rbe/Dockerfile

RUN echo "Building Elide builder image..." \
  && set -eux \
  && echo "Installing Zulu 17 repository..." \
  && curl -O https://cdn.azul.com/zulu/bin/zulu-repo_1.0.0-3_all.deb \
  && apt-get install ./zulu-repo_1.0.0-3_all.deb \
  && rm -fv /zulu-repo_1.0.0-3_all.deb \
  && apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 0xB1998361219BD9C9 \
  && apt-get update \
  && DEBIAN_FRONTEND=noninteractive apt-get install -y \
    apt-transport-https \
    autoconf \
    automake \
    autotools-dev \
    bash \
    ${ZULU_VERSION}-jdk \
    build-essential \
    ca-certificates \
    clang \
    curl \
    dirmngr \
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
    zlib1g \
  && cd ~ \
  && apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 0xB1998361219BD9C9 \
  && echo "Installing LLVM repositories..." \
  && wget --no-check-certificate -O llvm.key https://apt.llvm.org/llvm-snapshot.gpg.key \
  && apt-key add llvm.key \
  && rm -fv llvm.key \
  && add-apt-repository 'deb https://apt.llvm.org/bionic/ llvm-toolchain-bionic-15 main' \
  && echo "Installing NodeJS (v$NODE_VERSION / Yarn $YARN_VERSION)..." \
  && curl -sL "https://deb.nodesource.com/setup_$NODE_VERSION.x" | bash - \
  && DEBIAN_FRONTEND=noninteractive apt-get install -y nodejs llvm \
  && npm install -g "yarn@$YARN_VERSION" "pnpm" \
  && export ARCH="$(uname -m)" \
  && export COMPONENTS="native-image js espresso" \
  && if [ "$ARCH" = "x86_64" ] ; then ARCH="amd64"; COMPONENTS="native-image js wasm llvm llvm-toolchain native-image-llvm-backend"; fi \
  && if [ "$ARCH" = "arm64" ] ; then ARCH="aarch64"; fi \
  && echo "Installing GraalVM modules..." \
  && $GRAALVM_HOME/bin/gu install $COMPONENTS \
  && echo "Installing Gradle..." \
  && mkdir -p /opt/gradle \
  && cd /opt/gradle \
  && wget -q "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-$GRADLE_DIST.zip" \
  && unzip gradle*.zip \
  && ls -d */ | sed 's/\/*$//g' | xargs -I{} mv {} gradle \
  && rm gradle*.zip \
  && echo "Installing Bazelisk..." \
  && curl -LO "https://github.com/bazelbuild/bazelisk/releases/download/$BAZELISK_VERSION/bazelisk-linux-$ARCH" \
  && mv "bazelisk-linux-$ARCH" "/usr/bin/bazel" \
  && echo "Installing Binaryen..." \
  && curl -sSL https://github.com/WebAssembly/binaryen/releases/download/version_111/binaryen-version_111-x86_64-linux.tar.gz | tar -C /usr/local -xz --strip-components=1 \
  && echo "Testing Binaryen..." \
  && which wasm-opt \
  && chmod +x /usr/bin/bazel \
  && ln -s /usr/bin/bazel /usr/bin/bazelisk \
  && cd ~ \
  && rm -rf /var/lib/apt/lists/* \
  && echo "RBE builder image ready."

ENV GRADLE_HOME="/opt/gradle" \
  JAVA_HOME="/usr/lib/jvm/${ZULU_VERSION}" \
  GRAALVM_HOME="/usr/lib/gvm" \
  GOPATH="/go" \
  PATH="$GRADLE_HOME/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/usr/lib/gvm/bin:/sbin:/bin"
