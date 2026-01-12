# Standard Builder Docker Image
# This image contains Claude Code, Maven, Gradle, and Java for AI-driven build comparisons
# Multi-platform build for linux/amd64 and linux/arm64
# Uses Ubuntu 24.04 (Noble) for GLIBC 2.39+ (for consistency)

# Use Eclipse Temurin (AdoptOpenJDK) - official OpenJDK builds with full Java toolchain
FROM eclipse-temurin:17-jdk-noble

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

# Install Claude Code CLI (trying version 2.0.30 - before apiKeyHelper changes)
RUN npm install -g @anthropic-ai/claude-code@2.0.30

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
# Disable Claude Code auto-update (correct env var from docs)
ENV DISABLE_AUTOUPDATER=1

# Create non-root user for Claude Code
# Note: Ubuntu 24.04 (Noble) already has UID 1000, so we use the next available UID
RUN useradd -m -s /bin/bash builder && \
    mkdir -p /workspace && \
    chown -R builder:builder /workspace

# Set working directory
WORKDIR /workspace

# Copy Claude Code instructions (Maven/Gradle-specific)
COPY CLAUDE-STANDARD.md /workspace/CLAUDE.md
RUN chown builder:builder /workspace/CLAUDE.md

# Pre-configure Claude Code to skip prompts and enable non-interactive mode
# Create directory structure for Claude Code configuration
RUN mkdir -p /home/builder/.claude /home/builder/.config/claude-code && \
    echo '{"permissionMode":"dangerouslySkipPermissions"}' > /home/builder/.claude/settings.json && \
    echo '{"permissionMode":"dangerouslySkipPermissions"}' > /home/builder/.config/claude-code/settings.json && \
    chown -R builder:builder /home/builder/.claude /home/builder/.config

# Create a startup script that will configure the API key from environment variable
# This creates .claude.json with the API key before Claude Code starts
# Based on workaround from https://github.com/anthropics/claude-code/issues/441
RUN echo '#!/bin/bash' > /usr/local/bin/init-claude.sh && \
    echo 'if [ -n "$ANTHROPIC_API_KEY" ]; then' >> /usr/local/bin/init-claude.sh && \
    echo '  cat > $HOME/.claude.json <<EOF' >> /usr/local/bin/init-claude.sh && \
    echo '{' >> /usr/local/bin/init-claude.sh && \
    echo '  "changelogLastFetched": 1000000000000,' >> /usr/local/bin/init-claude.sh && \
    echo '  "primaryApiKey": "$ANTHROPIC_API_KEY",' >> /usr/local/bin/init-claude.sh && \
    echo '  "hasCompletedOnboarding": true,' >> /usr/local/bin/init-claude.sh && \
    echo '  "lastOnboardingVersion": "2.0.30"' >> /usr/local/bin/init-claude.sh && \
    echo '}' >> /usr/local/bin/init-claude.sh && \
    echo 'EOF' >> /usr/local/bin/init-claude.sh && \
    echo 'fi' >> /usr/local/bin/init-claude.sh && \
    echo 'exec "$@"' >> /usr/local/bin/init-claude.sh && \
    chmod +x /usr/local/bin/init-claude.sh

# Create .bashrc with helpful startup message
RUN echo '# Display welcome message and Claude Code info' >> /home/builder/.bashrc && \
    echo 'if [ -t 1 ]; then' >> /home/builder/.bashrc && \
    echo '  echo -e "\033[1;32m=== Standard Build Arena Container ===\033[0m"' >> /home/builder/.bashrc && \
    echo '  echo -e "\033[1;36mClaude Code:\033[0m $(claude --version 2>/dev/null || echo \"not installed\")"' >> /home/builder/.bashrc && \
    echo '  echo -e "\033[1;36mJava:\033[0m $(java -version 2>&1 | head -1)"' >> /home/builder/.bashrc && \
    echo '  echo -e "\033[1;36mMaven:\033[0m $(mvn --version 2>&1 | head -1)"' >> /home/builder/.bashrc && \
    echo '  echo -e "\033[1;36mGradle:\033[0m $(gradle --version 2>&1 | grep Gradle)"' >> /home/builder/.bashrc && \
    echo '  echo -e "\033[1;36mWorking Directory:\033[0m /workspace"' >> /home/builder/.bashrc && \
    echo '  echo -e "\033[1;36mInstructions:\033[0m cat /workspace/CLAUDE.md"' >> /home/builder/.bashrc && \
    echo '  echo ""' >> /home/builder/.bashrc && \
    echo '  echo -e "\033[0;33mTip: Type \047claude \"your task\"\047 to run Claude Code\033[0m"' >> /home/builder/.bashrc && \
    echo '  echo ""' >> /home/builder/.bashrc && \
    echo 'fi' >> /home/builder/.bashrc

# Switch to non-root user
USER builder

# Use init script as entrypoint to configure API key before starting
ENTRYPOINT ["/usr/local/bin/init-claude.sh"]

# Default command - interactive bash with Claude Code available
CMD ["/bin/bash", "-l"]
