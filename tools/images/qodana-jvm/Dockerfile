FROM --platform=amd64 us-docker.pkg.dev/elide-fw/tools/jdk17 AS jvm17

FROM --platform=amd64 jetbrains/qodana-jvm:2022.1-eap@sha256:fc913039837ba34ee8ce516c91d14331693ff62332db4445f6b5f48d7e5e769f

COPY --from=jvm17 /usr/lib/jvm/zulu17 /usr/lib/jvm/zulu17
