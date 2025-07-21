# This is designed to be run from the root project directory in order to test the built apk for Elide.
ARG ALPINE_VERSION=3.18
ARG ALPINE_HASH="@sha256:de0eb0b3f2a47ba1eb89389859a9bd88b28e82f5826b6969ad604979713c2d4f"

FROM alpine:${ALPINE_VERSION}${ALPINE_HASH}

# RUN apk add --no-cache --update libstdc++=12.2.1_git20220924-r10 musl=1.2.4-r3 libc6-compat=1.2.4-r3

RUN apk add --no-cache --update \
     ca-certificates \
     bash \
     file \
     libstdc++ \
     musl

COPY build/*.apk .
RUN file ./*.apk && apk add --update --allow-untrusted ./*.apk && rm -f *.apk
