
FROM us-docker.pkg.dev/elide-fw/tools/builder:latest

ARG DEV_USER=dev
ARG IBAZEL_VERSION=v0.15.10

ENV EDITOR=vim \
    SHELL=zsh \
    LANG=en_US.UTF-8 \
    TZ=America/Los_Angeles \
    PATH="/home/$DEV_USER/bin:$PATH"

RUN groupadd dev \
    && useradd -g dev -m ${DEV_USER} \
    && usermod -aG sudo ${DEV_USER} \
    && apt-get update \
    && apt-get -y install \
        apt-transport-https \
        bash \
        ca-certificates \
        cron \
        htop \
        jq \
        lcov \
        logrotate \
        lsb-release \
        maven \
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
        software-properties-common \
        tree \
        tmux \
        vim \
        zsh \
    && echo "$DEV_USER  ALL=(ALL) NOPASSWD:ALL" >> /etc/sudoers \
    && echo "%dev  ALL=(ALL) NOPASSWD:ALL" >> /etc/sudoers \
    && rm -rf /var/lib/apt/lists/* \
    && chsh -s /bin/zsh "$DEV_USER" \
    && su "$DEV_USER" \
    && sh -c "$(curl -fsSL https://raw.github.com/ohmyzsh/ohmyzsh/master/tools/install.sh)" \
    && go install github.com/bazelbuild/buildtools/buildifier@latest \
    && go install github.com/bazelbuild/buildtools/buildozer@latest \
    && go install github.com/fullstorydev/grpcurl/cmd/grpcurl@latest \
    && go install github.com/fullstorydev/grpcui/cmd/grpcui@latest \
    && go install github.com/bazelbuild/bazelisk@latest \
    && mkdir -p /home/dev/bin \
    && curl -o /home/dev/bin/ibazel "https://github.com/bazelbuild/bazel-watcher/releases/download/$IBAZEL_VERSION/ibazel_linux_amd64" \
    && echo "Codespace image ready."

USER ${DEV_USER}
WORKDIR /home/${DEV_USER}
