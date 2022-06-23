
FROM us-docker.pkg.dev/elide-fw/tools/builder:latest

ARG DEV_USER=dev
ARG IBAZEL_VERSION=v0.15.10

ARG INSTALL_ZSH="true"
ARG UPGRADE_PACKAGES="true"
ARG USERNAME=${DEV_USER}
ARG USER_UID=1000
ARG USER_GID=$USER_UID

ENV EDITOR=code \
    SHELL=zsh \
    LANG=en_US.UTF-8 \
    LC_CTYPE=en_US.UTF-8 \
    LC_ALL=en_US.UTF-8 \
    TZ=America/Los_Angeles \
    PATH="/home/$DEV_USER/bin:$PATH"

COPY library-scripts/*.sh library-scripts/*.env /tmp/library-scripts/
COPY zshrc /home/$DEV_USER/.zshrc
COPY alias /home/$DEV_USER/.alias

RUN echo "Setting up Debian-based codespace..." \
    && yes | unminimize 2>&1 \
    && bash /tmp/library-scripts/common-debian.sh "${INSTALL_ZSH}" "${USERNAME}" "${USER_UID}" "${USER_GID}" "${UPGRADE_PACKAGES}" "true" "true" \
    && echo "Installing updates and dev packages..." \
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
    && chsh -s /bin/zsh "$DEV_USER" \
    && su "$DEV_USER" \
    && go install github.com/bazelbuild/buildtools/buildifier@latest \
    && go install github.com/bazelbuild/buildtools/buildozer@latest \
    && go install github.com/fullstorydev/grpcurl/cmd/grpcurl@latest \
    && go install github.com/fullstorydev/grpcui/cmd/grpcui@latest \
    && go install github.com/bazelbuild/bazelisk@latest \
    && rm -rf /var/lib/apt/lists/* \
    && chown "$DEV_USER" "/home/$DEV_USER/.zshrc" "/home/$DEV_USER/.alias" \
    && mkdir -p /home/dev/bin \
    && curl -o /home/dev/bin/ibazel "https://github.com/bazelbuild/bazel-watcher/releases/download/$IBAZEL_VERSION/ibazel_linux_amd64" \
    && gcloud components install -y kubectl \
    && echo "Codespace image ready."

USER ${DEV_USER}
WORKDIR /home/${DEV_USER}
