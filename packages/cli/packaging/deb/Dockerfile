# This is designed to be run from the root project directory in order to test the built deb for Elide.
FROM debian:sid-slim@sha256:5e814211fbdb43856892292a315c740e8882387709a1f51ee266884931ff5eac

RUN apt-get update \
    && apt-get install -y sudo ca-certificates \
    && groupadd elide \
    && useradd -g elide -m elide -s /bin/bash \
    && echo '%elide   ALL=(ALL) NOPASSWD: ALL' >> /etc/sudoers

ENV DPKG_MAINTSCRIPT_DEBUG=1 \
    ELIDE_DEB_DEBUG=1

USER elide
WORKDIR /home/elide
COPY build/*.deb elide.deb
RUN sudo apt install -y ./elide.deb && rm -f elide.deb
