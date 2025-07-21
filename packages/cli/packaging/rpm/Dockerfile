# This is designed to be run from the root project directory in order to test the built rpm for Elide.
FROM fedora:rawhide@sha256:e6903d9b1cc68be9bece455ee2529911b3d8efbad0516ea11bee1e93adff590f

RUN yum update \
    && yum install -y sudo ca-certificates which \
    && groupadd elide \
    && useradd -g elide -m elide -s /bin/bash \
    && echo '%elide   ALL=(ALL) NOPASSWD: ALL' >> /etc/sudoers

ENV DPKG_MAINTSCRIPT_DEBUG=1 \
    ELIDE_DEB_DEBUG=1

USER elide
WORKDIR /home/elide
COPY build/*.rpm elide.rpm
RUN sudo yum install -y ./elide.rpm && rm -f elide.rpm
