ARG BASE_IMAGE=gvm19
ARG BASE_VERSION=":latest"
ARG BASE_ARCH="amd64"
ARG BASE_HASH="@sha256:b7bd0dc21cac38533076d7c268c93d66ea8268a0cd64867da115ea339930da26"

FROM --platform=linux/${BASE_ARCH} us-docker.pkg.dev/elide-fw/tools/${BASE_IMAGE}${BASE_VERSION}${BASE_HASH}

ARG JDK_VERSION="19"
ARG NODE_VERSION="18"
ARG YARN_VERSION="berry"
ARG GRADLE_VERSION=7.6
ARG KOTLIN_VERSION=1.7.22
ARG GRADLE_DIST=bin

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
    zlib1g \
  && cd ~ \
  && echo "Installing NodeJS (v$NODE_VERSION / Yarn $YARN_VERSION)..." \
  && curl -sL "https://deb.nodesource.com/setup_$NODE_VERSION.x" | bash - \
  && DEBIAN_FRONTEND=noninteractive apt-get install -y nodejs \
  && npm install -g "yarn@$YARN_VERSION" \
  && cd /opt \
  && echo "Installing Docker..." \
  && curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg \
  && echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu \
         $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null \
  && apt-get update \
  && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends docker-ce docker-ce-cli containerd.io \
  && echo "Installing GraalVM modules..." \
  && $GRAALVM_HOME/bin/gu install native-image js \
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
  && rm -rf /var/lib/apt/lists/* \
  && export GOPATH="/go" \
  && mkdir -p "$GOPATH/src" "$GOPATH/bin" \
  && chmod -R 777 "$GOPATH" \
  && echo "Builder image ready."

ENV GRADLE_HOME="/opt/gradle" \
  KOTLIN_HOME="/opt/kotlinc" \
  JAVA_HOME="/usr/lib/jvm/gvm" \
  GRAALVM_HOME="/usr/lib/gvm" \
  GOPATH="/go" \
  QTWEBENGINE_DISABLE_SANDBOX="1" \
  PATH="$GRADLE_HOME/bin:$KOTLIN_HOME/bin:$GOPATH/bin:/usr/local/go/bin:/var/lib/gcloud/google-cloud-sdk/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/usr/lib/gvm/bin:/sbin:/bin"
