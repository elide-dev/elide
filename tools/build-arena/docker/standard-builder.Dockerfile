# Standard Builder Docker Image
# This image contains Claude Code, Maven, Gradle, and Java for AI-driven build comparisons
# Multi-platform build for linux/amd64 and linux/arm64

# Use Eclipse Temurin (AdoptOpenJDK) - official OpenJDK builds with full Java toolchain
FROM eclipse-temurin:17-jdk-jammy

# Prevent interactive prompts during installation
ENV DEBIAN_FRONTEND=noninteractive

# Install dependencies (including tools for internet access, package management, etc.)
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    git \
    maven \
    build-essential \
    ca-certificates \
    gnupg \
    lsb-release \
    unzip \
    zip \
    vim \
    nano \
    jq \
    apt-transport-https \
    software-properties-common \
    python3 \
    python3-pip \
    && rm -rf /var/lib/apt/lists/*

# Install Node.js (required for Claude Code)
RUN curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
    && apt-get install -y nodejs \
    && rm -rf /var/lib/apt/lists/*

# Install Claude Code CLI
RUN npm install -g @anthropic-ai/claude-code

# Install Gradle
ENV GRADLE_VERSION=8.5
RUN curl -L https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip -o gradle.zip \
    && unzip gradle.zip -d /opt \
    && rm gradle.zip \
    && ln -s /opt/gradle-${GRADLE_VERSION}/bin/gradle /usr/bin/gradle

# Verify installations
RUN java -version && \
    mvn --version && \
    gradle --version && \
    node --version && \
    claude --version

# Set environment variables
ENV TERM=xterm-256color
ENV LANG=C.UTF-8

# Set working directory
WORKDIR /workspace

# Copy CLAUDE.md instruction file (will be added at runtime)
# COPY CLAUDE.md /workspace/CLAUDE.md

# Default command - interactive bash with Claude Code available
CMD ["/bin/bash", "-l"]
