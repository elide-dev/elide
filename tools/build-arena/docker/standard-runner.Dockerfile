# Standard Builder (Maven/Gradle) Runner Docker Image
# Minimal base image - Claude Code will download and install Maven/Gradle during the race
# This shows the full installation process to the user

FROM eclipse-temurin:17-jdk-jammy

ENV DEBIAN_FRONTEND=noninteractive

# Install minimal dependencies
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    git \
    build-essential \
    ca-certificates \
    gnupg \
    unzip \
    zip \
    jq \
    && rm -rf /var/lib/apt/lists/*

# Install Node.js (required for Claude Code)
RUN curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
    && apt-get install -y nodejs \
    && rm -rf /var/lib/apt/lists/*

# Install Claude Code CLI
RUN npm install -g @anthropic-ai/claude-code

# Verify installations
RUN java -version && \
    node --version && \
    claude --version

# Set environment variables
ENV TERM=xterm-256color
ENV LANG=C.UTF-8

# Create non-root user for Claude Code
RUN useradd -m -s /bin/bash -u 1000 builder && \
    mkdir -p /workspace && \
    chown -R builder:builder /workspace

# Set working directory
WORKDIR /workspace

# Copy Maven/Gradle-specific Claude Code instructions
COPY CLAUDE-STANDARD.md /workspace/CLAUDE.md
RUN chown builder:builder /workspace/CLAUDE.md

# Pre-configure Claude Code for non-interactive mode
RUN mkdir -p /home/builder/.claude && \
    echo '#!/bin/bash' > /home/builder/.claude/api-key-helper.sh && \
    echo 'echo "$ANTHROPIC_API_KEY"' >> /home/builder/.claude/api-key-helper.sh && \
    chmod +x /home/builder/.claude/api-key-helper.sh && \
    echo '{"apiKeyHelper":"/home/builder/.claude/api-key-helper.sh"}' > /home/builder/.claude/settings.json && \
    chown -R builder:builder /home/builder/.claude

# Create .bashrc with welcome message
RUN echo 'if [ -t 1 ]; then' >> /home/builder/.bashrc && \
    echo '  echo -e "\033[1;32m=== STANDARD RUNNER (Maven/Gradle) ===\033[0m"' >> /home/builder/.bashrc && \
    echo '  echo -e "\033[1;36mClaude Code:\033[0m $(claude --version 2>/dev/null)"' >> /home/builder/.bashrc && \
    echo '  echo -e "\033[1;36mJava:\033[0m $(java -version 2>&1 | head -1)"' >> /home/builder/.bashrc && \
    echo '  echo -e "\033[1;36mMaven:\033[0m NOT INSTALLED (will download during race)"' >> /home/builder/.bashrc && \
    echo '  echo -e "\033[1;36mGradle:\033[0m NOT INSTALLED (will download during race)"' >> /home/builder/.bashrc && \
    echo '  echo -e "\033[1;33mMission: Download Maven/Gradle, build project, report time\033[0m"' >> /home/builder/.bashrc && \
    echo '  echo ""' >> /home/builder/.bashrc && \
    echo 'fi' >> /home/builder/.bashrc

# Switch to non-root user
USER builder

CMD ["/bin/bash", "-l"]
