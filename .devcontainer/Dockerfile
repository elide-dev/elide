FROM us-docker.pkg.dev/elide-fw/tools/codespace:latest
RUN go env -w GOPROXY=https://go.stage.pkg.st,direct
COPY init.gradle.kts /home/dev/.gradle/init.gradle.kts
ENTRYPOINT ["/usr/local/share/docker-init.sh"]
CMD ["sleep", "infinity"]
