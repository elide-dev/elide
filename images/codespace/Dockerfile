
ARG BASE_ARCH="amd64"

FROM --platform=linux/${BASE_ARCH} mcr.microsoft.com/vscode/devcontainers/base:ubuntu-20.04

ARG DEV_USER=dev
ARG IBAZEL_VERSION=v0.15.10

ARG JAVA_VERSION=java17
ARG NODE_VERSION="17"
ARG YARN_VERSION="berry"
ARG GRADLE_VERSION=7.4.2
ARG KOTLIN_VERSION=1.7.0
ARG GRADLE_DIST=bin
ARG INSTALL_ZSH="true"
ARG UPGRADE_PACKAGES="true"
ARG USERNAME=${DEV_USER}
ARG USER_UID=1000
ARG USER_GID=$USER_UID
ARG GRAAL_EDITION=ce
ARG GRAAL_VERSION=22.1.0
ARG GRAAL_ARCH=amd64
ARG JAVA_VERSION=java17

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
    && apt-get install -y zulu17-jdk wget tar \
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
    && echo "Installing Go..."; \
        arch="$(dpkg --print-architecture)"; arch="${arch##*-}"; \
        url=; \
        case "${arch}" in \
            'amd64') \
                url='https://dl.google.com/go/go1.18.3.linux-amd64.tar.gz'; \
                sha256='956f8507b302ab0bb747613695cdae10af99bbd39a90cae522b7c0302cc27245'; \
                ;; \
            'armel') \
                export GOARCH='arm' GOARM='5' GOOS='linux'; \
                ;; \
            'armhf') \
                url='https://dl.google.com/go/go1.18.3.linux-armv6l.tar.gz'; \
                sha256='b8f0b5db24114388d5dcba7ca0698510ea05228b0402fcbeb0881f74ae9cb83b'; \
                ;; \
            'arm64') \
                url='https://dl.google.com/go/go1.18.3.linux-arm64.tar.gz'; \
                sha256='beacbe1441bee4d7978b900136d1d6a71d150f0a9bb77e9d50c822065623a35a'; \
                ;; \
            'i386') \
                url='https://dl.google.com/go/go1.18.3.linux-386.tar.gz'; \
                sha256='72b73da021397a3a1ce182c19d2a890a5346bfe80885d9dd7d1ff04ce6597938'; \
                ;; \
            'mips64el') \
                export GOARCH='mips64le' GOOS='linux'; \
                ;; \
            'ppc64el') \
                url='https://dl.google.com/go/go1.18.3.linux-ppc64le.tar.gz'; \
                sha256='5d42bd252e7af9f854df92e46bb2e88be7b2fb310cc937c0fe091afd8c4f2016'; \
                ;; \
            's390x') \
                url='https://dl.google.com/go/go1.18.3.linux-s390x.tar.gz'; \
                sha256='ebb4efddec5bbd22bdd9c87137cb3dd59e874b5dfcf93d00bef351c60d2c7401'; \
                ;; \
            *) echo >&2 "error: unsupported architecture '$arch' (likely packaging update needed)"; exit 1 ;; \
        esac; \
        build=; \
        if [ -z "$url" ]; then \
            # https://github.com/golang/go/issues/38536#issuecomment-616897960
            build=1; \
            url='https://dl.google.com/go/go1.18.3.src.tar.gz'; \
            sha256='0012386ddcbb5f3350e407c679923811dbd283fcdc421724931614a842ecbc2d'; \
            echo >&2; \
            echo >&2 "warning: current architecture ($arch) does not have a compatible Go binary release; will be building from source"; \
            echo >&2; \
        fi; \
        \
        wget -O go.tgz.asc "$url.asc"; \
        wget -O go.tgz "$url" --progress=dot:giga; \
        echo "$sha256 *go.tgz" | sha256sum -c -; \
        \
        # https://github.com/golang/go/issues/14739#issuecomment-324767697
        GNUPGHOME="$(mktemp -d)"; export GNUPGHOME; \
        # https://www.google.com/linuxrepositories/
        gpg --batch --keyserver keyserver.ubuntu.com --recv-keys 'EB4C 1BFD 4F04 2F6D DDCC  EC91 7721 F63B D38B 4796'; \
        # let's also fetch the specific subkey of that key explicitly that we expect "go.tgz.asc" to be signed by, just to make sure we definitely have it
        gpg --batch --keyserver keyserver.ubuntu.com --recv-keys '2F52 8D36 D67B 69ED F998  D857 78BD 6547 3CB3 BD13'; \
        gpg --batch --verify go.tgz.asc go.tgz; \
        gpgconf --kill all; \
        rm -rf "$GNUPGHOME" go.tgz.asc; \
        \
        tar -C /usr/local -xzf go.tgz; \
        rm go.tgz; \
        \
        if [ -n "$build" ]; then \
            savedAptMark="$(apt-mark showmanual)"; \
            apt-get update; \
            apt-get install -y --no-install-recommends golang-go; \
            \
            export GOCACHE='/tmp/gocache'; \
            \
            ( \
                cd /usr/local/go/src; \
                # set GOROOT_BOOTSTRAP + GOHOST* such that we can build Go successfully
                export GOROOT_BOOTSTRAP="$(go env GOROOT)" GOHOSTOS="$GOOS" GOHOSTARCH="$GOARCH"; \
                ./make.bash; \
            ); \
            \
            apt-mark auto '.*' > /dev/null; \
            apt-mark manual $savedAptMark > /dev/null; \
            apt-get purge -y --auto-remove -o APT::AutoRemove::RecommendsImportant=false; \
            rm -rf /var/lib/apt/lists/*; \
            \
            # remove a few intermediate / bootstrapping files the official binary release tarballs do not contain
            rm -rf \
                /usr/local/go/pkg/*/cmd \
                /usr/local/go/pkg/bootstrap \
                /usr/local/go/pkg/obj \
                /usr/local/go/pkg/tool/*/api \
                /usr/local/go/pkg/tool/*/go_bootstrap \
                /usr/local/go/src/cmd/dist/dist \
                "$GOCACHE" \
            ; \
        fi; \
        \
	  /usr/local/go/bin/go version \
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
    && echo "Installing GraalVM modules..." \
    && $GRAALVM_HOME/bin/gu install native-image espresso \
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
    && echo "Build tools ready.";

ENV GRADLE_HOME="/opt/gradle" \
    KOTLIN_HOME="/opt/kotlinc" \
    JAVA_HOME="/usr/lib/jvm/zulu17" \
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

RUN sh -c "$(curl -fsSL https://raw.github.com/ohmyzsh/ohmyzsh/master/tools/install.sh)" \
    && echo "source ~/.profile" >> ~/.zshrc \
    && go install github.com/bazelbuild/buildtools/buildifier@latest \
    && go install github.com/bazelbuild/buildtools/buildozer@latest \
    && go install github.com/fullstorydev/grpcurl/cmd/grpcurl@latest \
    && go install github.com/fullstorydev/grpcui/cmd/grpcui@latest \
    && go install github.com/bazelbuild/bazelisk@latest \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /home/dev/bin \
    && curl -o /home/dev/bin/ibazel "https://github.com/bazelbuild/bazel-watcher/releases/download/$IBAZEL_VERSION/ibazel_linux_amd64" \
    && yes Y | gcloud components install kubectl \
    && echo "Codespace image ready."

COPY zshrc /home/$DEV_USER/.zshrc
USER ${DEV_USER}
WORKDIR /home/${DEV_USER}

RUN BUF_PREFIX="/usr/local" && \
    BUF_VERSION="1.6.0" && \
      curl -sSL \
        "https://github.com/bufbuild/buf/releases/download/v${BUF_VERSION}/buf-$(uname -s)-$(uname -m).tar.gz" | \
        tar -xvzf - -C "${BUF_PREFIX}" --strip-components 1
