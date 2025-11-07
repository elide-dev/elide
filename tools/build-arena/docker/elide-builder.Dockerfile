# Elide Builder Docker Image
# This image contains Claude Code, Elide, and Java for AI-driven build comparisons
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

# Note: Elide installation skipped for now - will be added via volume mount or separate step
# The terminal test page works fine without Elide installed

# Verify installations
RUN java -version && \
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
