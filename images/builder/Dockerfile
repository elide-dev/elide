ARG BASE_IMAGE=gvm17
ARG BASE_VERSION=":latest"
ARG BASE_ARCH="amd64"
ARG BASE_HASH="@sha256:2d25a613ed3764d375557f709f5d02423eae097abae05eaeca073bedcdb49863"

FROM --platform=linux/${BASE_ARCH} us-docker.pkg.dev/elide-fw/tools/${BASE_IMAGE}${BASE_VERSION}${BASE_HASH}

ARG JDK_VERSION="17"
ARG NODE_VERSION="17"
ARG YARN_VERSION="berry"
ARG GRADLE_VERSION=7.4.2
ARG KOTLIN_VERSION=1.7.0
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
  && echo "Installing Go..."
arch="$(dpkg --print-architecture)"
arch="${arch##*-}"
url=
case "${arch}" in
  'amd64')
    url='https://dl.google.com/go/go1.18.3.linux-amd64.tar.gz'
    sha256='956f8507b302ab0bb747613695cdae10af99bbd39a90cae522b7c0302cc27245'
    ;;
  'armel')
    export GOARCH='arm' GOARM='5' GOOS='linux'
    ;;
  'armhf')
    url='https://dl.google.com/go/go1.18.3.linux-armv6l.tar.gz'
    sha256='b8f0b5db24114388d5dcba7ca0698510ea05228b0402fcbeb0881f74ae9cb83b'
    ;;
  'arm64')
    url='https://dl.google.com/go/go1.18.3.linux-arm64.tar.gz'
    sha256='beacbe1441bee4d7978b900136d1d6a71d150f0a9bb77e9d50c822065623a35a'
    ;;
  'i386')
    url='https://dl.google.com/go/go1.18.3.linux-386.tar.gz'
    sha256='72b73da021397a3a1ce182c19d2a890a5346bfe80885d9dd7d1ff04ce6597938'
    ;;
  'mips64el')
    export GOARCH='mips64le' GOOS='linux'
    ;;
  'ppc64el')
    url='https://dl.google.com/go/go1.18.3.linux-ppc64le.tar.gz'
    sha256='5d42bd252e7af9f854df92e46bb2e88be7b2fb310cc937c0fe091afd8c4f2016'
    ;;
  's390x')
    url='https://dl.google.com/go/go1.18.3.linux-s390x.tar.gz'
    sha256='ebb4efddec5bbd22bdd9c87137cb3dd59e874b5dfcf93d00bef351c60d2c7401'
    ;;
  *)
    echo >&2 "error: unsupported architecture '$arch' (likely packaging update needed)"
    exit 1
    ;;
esac
build=
if [ -z "$url" ]; then
  # https://github.com/golang/go/issues/38536#issuecomment-616897960
  build=1
  url='https://dl.google.com/go/go1.18.3.src.tar.gz'
  sha256='0012386ddcbb5f3350e407c679923811dbd283fcdc421724931614a842ecbc2d'
  echo >&2
  echo >&2 "warning: current architecture ($arch) does not have a compatible Go binary release; will be building from source"
  echo >&2
fi

wget -O go.tgz.asc "$url.asc"
wget -O go.tgz "$url" --progress=dot:giga
echo "$sha256 *go.tgz" | sha256sum -c -

# https://github.com/golang/go/issues/14739#issuecomment-324767697
GNUPGHOME="$(mktemp -d)"
export GNUPGHOME
# https://www.google.com/linuxrepositories/
gpg --batch --keyserver keyserver.ubuntu.com --recv-keys 'EB4C 1BFD 4F04 2F6D DDCC  EC91 7721 F63B D38B 4796'
# let's also fetch the specific subkey of that key explicitly that we expect "go.tgz.asc" to be signed by, just to make sure we definitely have it
gpg --batch --keyserver keyserver.ubuntu.com --recv-keys '2F52 8D36 D67B 69ED F998  D857 78BD 6547 3CB3 BD13'
gpg --batch --verify go.tgz.asc go.tgz
gpgconf --kill all
rm -rf "$GNUPGHOME" go.tgz.asc

tar -C /usr/local -xzf go.tgz
rm go.tgz

if [ -n "$build" ]; then
  savedAptMark="$(apt-mark showmanual)"
  apt-get update
  apt-get install -y --no-install-recommends golang-go

  export GOCACHE='/tmp/gocache'

  (
    cd /usr/local/go/src
    # set GOROOT_BOOTSTRAP + GOHOST* such that we can build Go successfully
    export GOROOT_BOOTSTRAP="$(go env GOROOT)" GOHOSTOS="$GOOS" GOHOSTARCH="$GOARCH"
    ./make.bash
  )

  apt-mark auto '.*' > /dev/null
  apt-mark manual $savedAptMark > /dev/null
  apt-get purge -y --auto-remove -o APT::AutoRemove::RecommendsImportant=false
  rm -rf /var/lib/apt/lists/*

  # remove a few intermediate / bootstrapping files the official binary release tarballs do not contain
  rm -rf \
    /usr/local/go/pkg/*/cmd \
    /usr/local/go/pkg/bootstrap \
    /usr/local/go/pkg/obj \
    /usr/local/go/pkg/tool/*/api \
    /usr/local/go/pkg/tool/*/go_bootstrap \
    /usr/local/go/src/cmd/dist/dist \
    "$GOCACHE" \
    ;
fi

/usr/local/go/bin/go version \
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
  JAVA_HOME="/usr/lib/jvm/zulu17" \
  GRAALVM_HOME="/usr/lib/gvm" \
  GOPATH="/go" \
  QTWEBENGINE_DISABLE_SANDBOX="1" \
  PATH="$GRADLE_HOME/bin:$KOTLIN_HOME/bin:$GOPATH/bin:/usr/local/go/bin:/var/lib/gcloud/google-cloud-sdk/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/usr/lib/gvm/bin:/sbin:/bin"
