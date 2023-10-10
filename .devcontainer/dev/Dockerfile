FROM ghcr.io/elide-dev/codespace@sha256:4ee81688a0be011baeca66129ebd73b14e5fa0c8bb72ae5ac67d8696f413d24d

USER root

RUN apt-get update && apt-get install -y uuid-runtime build-essential cmake ruby3.0 libyaml-dev

USER dev

RUN go env -w GOPROXY=https://go.stage.pkg.st,direct;

RUN curl -sSL --tlsv1.2 "dl.elide.dev/cli/install.sh" | bash -s -

RUN curl -sSL --tlsv1.2 "https://github.com/buildless/cli/releases/download/1.0.0-beta1/buildless.linux-amd64.tar.gz" > buildless.tar.gz \
    && tar -xzvf buildless.tar.gz \
    && sudo mv buildless.linux-amd64/buildless /usr/local/bin/buildless \
    && rm -frv buildless.tar.gz buildless.linux-amd64;

ENV GRAALVM_HOME=/usr/lib/gvm \
    SYSTEM_RUBY=/bin/ruby

COPY buildless.yaml /home/dev/.config/buildless/buildless.yaml
COPY init.gradle.kts /home/dev/.gradle/init.gradle.kts
ENTRYPOINT ["/usr/local/share/docker-init.sh"]
CMD ["sleep", "infinity"]
