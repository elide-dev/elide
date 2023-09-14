FROM ghcr.io/elide-dev/codespace@sha256:4ee81688a0be011baeca66129ebd73b14e5fa0c8bb72ae5ac67d8696f413d24d

RUN go env -w GOPROXY=https://go.stage.pkg.st,direct \
    && chown -R dev /home/dev/.gradle \
    && curl -sSL --tlsv1.2 "dl.elide.dev/cli/install.sh" | bash -s - \
    && curl -sSL --tlsv1.2 "https://github.com/buildless/cli/releases/download/1.0.0-beta1/buildless.linux-amd64.tar.gz" > buildless.tar.gz \
    && tar -xzvf buildless.tar.gz \
    && mv buildless /usr/bin/buildless \
    && rm -fv buildless.tar.gz;


ENV GRAALVM_HOME=/usr/lib/gvm

COPY init.gradle.kts /home/dev/.gradle/init.gradle.kts
ENTRYPOINT ["/usr/local/share/docker-init.sh"]
CMD ["sleep", "infinity"]
