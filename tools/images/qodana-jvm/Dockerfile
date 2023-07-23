FROM --platform=amd64 us-docker.pkg.dev/elide-fw/tools/jdk17 AS jvm17

FROM --platform=amd64 jetbrains/qodana-jvm:2022.3-eap@sha256:5a20fbc7609cab0b78c38a6ee12a265d4ff51fa5617f0a576e4219d4a8a9637b

COPY --from=jvm17 /usr/lib/jvm/zulu17 /usr/lib/jvm/zulu17
