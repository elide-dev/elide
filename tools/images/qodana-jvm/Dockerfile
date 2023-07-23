FROM --platform=amd64 us-docker.pkg.dev/elide-fw/tools/jdk17 AS jvm17

FROM --platform=amd64 jetbrains/qodana-jvm:2022.1-eap@sha256:da4f13e2b9efb1adadffb9abaaafdcf9ff3dbc7f3c88218aac59dd71935764c0

COPY --from=jvm17 /usr/lib/jvm/zulu17 /usr/lib/jvm/zulu17
