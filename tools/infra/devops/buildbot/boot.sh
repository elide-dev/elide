#!/bin/bash

#
# Copyright (c) 2023 Elide Ventures, LLC.
#
# Licensed under the MIT license (the "License"); you may not use this file except in compliance
# with the License. You may obtain a copy of the License at
#
#   https://opensource.org/license/mit/
#
# Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
# an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations under the License.
#

set -e

echo "Booting CI/CD machine for Elide..."

LANG=en_US.UTF-8
JAVA_VERSION=java19
GRAALVM_COMPONENTS="native-image js"
GRAALVM_PKG=gs://elide-runtime/buildbot/r1a/gvm/graalvm-ce-$JAVA_VERSION-GRAALVM_ARCH-$GRAALVM_VERSION.tar.gz
JAVA_HOME=/opt/graalvm-ce-$JAVA_VERSION-$GRAALVM_VERSION
CACHE_USERNAME=$(gcloud secrets versions access --project=elide-fw --secret=gradle-cache-username latest)
CACHE_PASSWORD=$(gcloud secrets versions access --project=elide-fw --secret=gradle-cache-password latest)
RUNNER_TOKEN=$(gcloud secrets versions access --project=elide-fw --secret=buildbot-runner-token latest)

set -x
apt-get update \
  && echo "LANG=$LANG" >> /etc/environment \
  && apt-get upgrade -y --no-install-recommends \
  && apt install -y \
    apt-transport-https \
    build-essential \
    ca-certificates \
    curl \
    gcc \
    g++ \
    make \
    git \
    gnupg \
    libssl-dev \
    lsb-release \
    make \
    software-properties-common \
    zlib1g-dev \
  && mkdir -p /etc/apt/keyrings \
  && rm -f /etc/apt/keyrings/docker.gpg \
  && curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg \
  && echo \
    "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
       $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null \
  && apt-get update \
  && apt-get install -y --no-install-recommends \
    docker-ce \
    docker-ce-cli \
    containerd.io \
    docker-compose-plugin \
  && apt-get -y autoremove \
  && if [ "$TARGETPLATFORM" == "linux/amd64" ]; then GRAALVM_PKG=${GRAALVM_PKG/GRAALVM_ARCH/linux-amd64}; fi \
  && if [ "$TARGETPLATFORM" == "linux/arm64" ]; then GRAALVM_PKG=${GRAALVM_PKG/GRAALVM_ARCH/linux-aarch64}; fi \
  && gsutil cp ${GRAALVM_PKG} - | gunzip | tar x -C /opt/ \
  && mkdir -p "/usr/java" \
  && ln -sfT "$JAVA_HOME" /usr/java/default \
  && ln -sfT "$JAVA_HOME" /usr/java/latest \
  && echo "CI=true" >> /etc/environment \
  && echo "GRAAL_HOME=$JAVA_HOME" >> /etc/environment \
  && echo "GRAALVM_HOME=$JAVA_HOME" >> /etc/environment \
  && echo "JAVA_HOME=$JAVA_HOME" >> /etc/environment \
  && echo "GRADLE_CACHE_REMOTE=true" >> /etc/environment \
  && echo "GRADLE_CACHE_USERNAME=$CACHE_USERNAME" >> /etc/environment \
  && echo "GRADLE_CACHE_PASSWORD=\"$CACHE_PASSWORD\"" >> /etc/environment \
  && echo "GRADLE_CACHE_PUSH=true" >> /etc/environment \
  && for bin in "$JAVA_HOME/bin/"*; do
    base="$(basename "$bin")"
    [ ! -e "/usr/bin/$base" ]
    update-alternatives --install "/usr/bin/$base" "$base" "$bin" 20000
  done \
  && gu install $GRAALVM_COMPONENTS \
  && curl -sL https://deb.nodesource.com/setup_18.x | bash \
  && curl -sL https://dl.yarnpkg.com/debian/pubkey.gpg | gpg --dearmor | tee /usr/share/keyrings/yarnkey.gpg > /dev/null \
  && apt-get update \
  && apt-get install -y nodejs yarn \
  && mkdir -p /usr/local/actions-runner \
  && mkdir -p /workspace \
  && cd /usr/local/actions-runner \
  && curl --retry 10 -o "actions-runner-linux.tar.gz" -L "https://github.com/actions/runner/releases/download/v$RUNNER_VERSION/actions-runner-linux-$RUNNER_ARCH-$RUNNER_VERSION.tar.gz" \
  && tar xzf ./actions-runner-linux.tar.gz \
  && groupadd devops \
  && useradd -m buildbot -g devops \
  && echo "deb http://packages.azlux.fr/debian/ buster main" | tee /etc/apt/sources.list.d/azlux.list \
  && wget -qO - https://azlux.fr/repo.gpg.key | apt-key add - \
  && apt update \
  && apt install oha \
  && sudo -u buildbot mkdir /home/buildbot/.ssh \
  && sudo -u buildbot ssh-keyscan github.com >> /home/buildbot/.ssh/known_hosts \
  && gcloud secrets versions access --project=elide-fw --secret=buildbot-ssh-key-private latest > /home/buildbot/.ssh/id_rsa \
  && gcloud secrets versions access --project=elide-fw --secret=buildbot-ssh-key-public latest > /home/buildbot/.ssh/id_rsa.pub \
  && chmod 600 /home/buildbot/.ssh/id_rsa \
  && chmod 600 /home/buildbot/.ssh/known_hosts \
  && mkdir -p /home/buildbot/.gradle \
  && echo "versions.java.language=19" >> /home/buildbot/.gradle/gradle.properties \
  && echo "org.gradle.java.home=$JAVA_HOME" >> /home/buildbot/.gradle/gradle.properties \
  && echo "org.gradle.jvmargs=-Xmx12g -XX:MaxMetaspaceSize=1024m -XX:+UseParallelGC" >> /home/buildbot/.gradle/gradle.properties \
  && echo "elide.ci=true" >> /home/buildbot/.gradle/gradle.properties \
  && echo "elide.publish.repo.maven=gcs://elide-snapshots/repository/v3" >> /home/buildbot/.gradle/gradle.properties \
  && echo "elide.publish.repo.docker.tools=us-docker.pkg.dev/elide-fw/tools" >> /home/buildbot/.gradle/gradle.properties \
  && echo "elide.publish.repo.docker.samples=us-docker.pkg.dev/elide-fw/samples" >> /home/buildbot/.gradle/gradle.properties \
  && echo "kotlin.build.report.enable=true" >> /home/buildbot/.gradle/gradle.properties \
  && echo "kotlin.build.report.verbose=true" >> /home/buildbot/.gradle/gradle.properties \
  && echo "kotlin.build.report.metrics=true" >> /home/buildbot/.gradle/gradle.properties \
  && chown -R buildbot:devops /home/buildbot \
  && chown -R buildbot:devops /workspace \
  && chmod -R 755 /workspace \
  && chown -R buildbot:devops /usr/local/actions-runner \
  && sudo -u buildbot bash ./config.sh --unattended --url https://github.com/elide-dev --token $RUNNER_TOKEN --replace \
  && bash ./svc.sh install buildbot \
  && ./svc.sh start \
  && ./svc.sh status \
  && echo "CI/CD builder ready."
