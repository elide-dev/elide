import Docker from 'dockerode';
import { EventEmitter } from 'events';
import { readFileSync } from 'fs';
import { join } from 'path';
import type { BuildTool, BuildResult, TerminalOutput } from '../../../shared/types.js';

/**
 * Interactive sandbox runner with PTY support for Claude Code agents
 */
export class InteractiveSandboxRunner extends EventEmitter {
  private docker: Docker;
  private runningContainers: Map<string, { container: Docker.Container; exec?: Docker.Exec }> = new Map();
  private inputBuffers: Map<string, string[]> = new Map();

  constructor() {
    super();
    this.docker = new Docker();
  }

  /**
   * Run an interactive build session with Claude Code
   */
  async runInteractiveBuild(
    jobId: string,
    repositoryUrl: string,
    tool: BuildTool
  ): Promise<BuildResult> {
    const startTime = new Date().toISOString();
    const containerId = `${jobId}-${tool}`;

    this.emit('build:started', { jobId, tool, timestamp: startTime });

    try {
      // Create and start container
      const container = await this.createInteractiveContainer(repositoryUrl, tool);
      this.runningContainers.set(containerId, { container });
      this.inputBuffers.set(containerId, []);

      await container.start();

      // Start Claude Code agent in the container
      await this.startClaudeCodeAgent(container, containerId, jobId, tool, repositoryUrl);

      // Monitor container until completion
      const exitCode = await this.waitForCompletion(container, containerId, jobId, tool);

      // Get metrics
      const stats = await this.getContainerStats(container);

      const endTime = new Date().toISOString();
      const duration = new Date(endTime).getTime() - new Date(startTime).getTime();

      // Cleanup
      await this.cleanupContainer(container);
      this.runningContainers.delete(containerId);
      this.inputBuffers.delete(containerId);

      const result: BuildResult = {
        tool,
        status: exitCode === 0 ? 'success' : 'failure',
        startTime,
        endTime,
        duration,
        exitCode,
        metrics: {
          buildTime: duration,
          memoryUsage: stats.memoryUsage,
          cpuUsage: stats.cpuUsage,
        },
      };

      this.emit('build:completed', { jobId, tool, result });

      return result;
    } catch (error) {
      const endTime = new Date().toISOString();
      const duration = new Date(endTime).getTime() - new Date(startTime).getTime();

      console.error(`Interactive build failed for ${tool}:`, error);

      this.runningContainers.delete(containerId);
      this.inputBuffers.delete(containerId);

      const result: BuildResult = {
        tool,
        status: 'failure',
        startTime,
        endTime,
        duration,
        exitCode: -1,
      };

      this.emit('build:failed', { jobId, tool, error });

      return result;
    }
  }

  /**
   * Send input to an interactive terminal
   */
  async sendInput(jobId: string, tool: BuildTool, input: string): Promise<void> {
    const containerId = `${jobId}-${tool}`;
    const containerInfo = this.runningContainers.get(containerId);

    if (!containerInfo) {
      throw new Error(`Container ${containerId} not found`);
    }

    // Buffer input for the exec session
    const buffer = this.inputBuffers.get(containerId);
    if (buffer) {
      buffer.push(input);
    }

    // If there's an active exec session, write to it
    if (containerInfo.exec) {
      // For now, we'll handle input via environment variables and command injection
      // In a production system, you'd want a proper PTY implementation
      console.log(`Buffering input for ${containerId}: ${input}`);
    }
  }

  /**
   * Stop a running build
   */
  async stopBuild(jobId: string): Promise<void> {
    const promises: Promise<void>[] = [];

    for (const [containerId, { container }] of this.runningContainers.entries()) {
      if (containerId.startsWith(jobId)) {
        promises.push(this.cleanupContainer(container));
        this.runningContainers.delete(containerId);
        this.inputBuffers.delete(containerId);
      }
    }

    await Promise.all(promises);
  }

  /**
   * Create an interactive Docker container
   */
  private async createInteractiveContainer(
    repositoryUrl: string,
    tool: BuildTool
  ): Promise<Docker.Container> {
    const image = tool === 'elide' ? 'elide-builder:latest' : 'standard-builder:latest';

    await this.ensureImage(image);

    const container = await this.docker.createContainer({
      Image: image,
      Tty: true, // Allocate a pseudo-TTY
      OpenStdin: true, // Keep STDIN open
      AttachStdin: true,
      AttachStdout: true,
      AttachStderr: true,
      Env: [
        `REPO_URL=${repositoryUrl}`,
        `BUILD_TOOL=${tool}`,
        'TERM=xterm-256color',
        `ANTHROPIC_API_KEY=${process.env.ANTHROPIC_API_KEY || ''}`,
      ],
      HostConfig: {
        Memory: 4 * 1024 * 1024 * 1024, // 4GB memory limit
        MemorySwap: 4 * 1024 * 1024 * 1024,
        CpuQuota: 200000, // 2 CPU cores
        NetworkMode: 'bridge',
        AutoRemove: false,
      },
      WorkingDir: '/workspace',
    });

    return container;
  }

  /**
   * Start Claude Code agent inside the container
   */
  private async startClaudeCodeAgent(
    container: Docker.Container,
    containerId: string,
    jobId: string,
    tool: BuildTool,
    repositoryUrl: string
  ): Promise<void> {
    // Read the CLAUDE.md instruction file
    const claudeInstructions = readFileSync(
      join(process.cwd(), '..', 'docker', `CLAUDE-${tool}.md`),
      'utf-8'
    );

    // Create exec instance with interactive TTY
    const exec = await container.exec({
      Cmd: ['/bin/bash', '-c', this.getAgentStartupScript(tool, repositoryUrl, claudeInstructions)],
      AttachStdout: true,
      AttachStderr: true,
      Tty: true,
      Env: [`REPO_URL=${repositoryUrl}`, `BUILD_TOOL=${tool}`],
    });

    // Store exec reference
    const containerInfo = this.runningContainers.get(containerId);
    if (containerInfo) {
      containerInfo.exec = exec;
    }

    // Start the exec and stream output
    const stream = await exec.start({ Tty: true, stdin: true });

    // Handle output streaming
    stream.on('data', (chunk: Buffer) => {
      const data = chunk.toString('utf-8');

      const output: TerminalOutput = {
        jobId,
        tool,
        type: 'stdout',
        data,
        timestamp: new Date().toISOString(),
      };

      this.emit('terminal:output', output);

      // Check for bell character (completion signal)
      if (data.includes('\u0007') || data.includes('\a')) {
        this.emit('build:bell', { jobId, tool, timestamp: new Date().toISOString() });
      }
    });

    stream.on('error', (error) => {
      console.error(`Stream error for ${containerId}:`, error);
      const output: TerminalOutput = {
        jobId,
        tool,
        type: 'stderr',
        data: `Stream error: ${error.message}\n`,
        timestamp: new Date().toISOString(),
      };
      this.emit('terminal:output', output);
    });
  }

  /**
   * Generate startup script for the Claude Code agent
   */
  private getAgentStartupScript(tool: BuildTool, repositoryUrl: string, instructions: string): string {
    // Escape instructions for embedding in bash script
    const escapedInstructions = instructions.replace(/`/g, '\\`').replace(/\$/g, '\\$');

    return `
      set -e
      cd /workspace

      # Create CLAUDE.md with instructions
      cat > CLAUDE.md << 'INSTRUCTIONS_EOF'
${escapedInstructions}
INSTRUCTIONS_EOF

      # Export environment variables
      export REPO_URL="${repositoryUrl}"
      export BUILD_TOOL="${tool}"

      echo "======================================"
      echo "Build Arena - ${tool.toUpperCase()} Team"
      echo "======================================"
      echo ""
      echo "Repository: $REPO_URL"
      echo "Tool: $BUILD_TOOL"
      echo ""
      echo "Starting Claude Code agent..."
      echo ""

      # Start Claude Code in non-interactive mode with proper flags
      # Use --print for non-interactive mode
      # --output-format json for structured output
      # --max-turns to limit iterations
      # The ANTHROPIC_API_KEY is pre-configured in the environment

      claude --print --output-format json --max-turns 50 "$(cat CLAUDE.md)" 2>&1 | tee /workspace/claude-output.log

      CLAUDE_EXIT_CODE=\${PIPESTATUS[0]}

      if [ \$CLAUDE_EXIT_CODE -ne 0 ]; then
        echo "Claude Code exited with code: \$CLAUDE_EXIT_CODE"
        echo "Falling back to direct execution..."

        ${this.getDirectExecutionScript(tool)}
      else
        echo "Claude Code execution completed successfully"
      fi

      echo ""
      echo "======================================"
      echo "Agent execution complete"
      echo "======================================"
    `;
  }

  /**
   * Get direct execution script (fallback when Claude Code isn't available)
   */
  private getDirectExecutionScript(tool: BuildTool): string {
    if (tool === 'elide') {
      return `
        # Clone repository
        git clone "$REPO_URL" project || exit 1
        cd project

        # Detect build system
        if [ -f "build.gradle" ] || [ -f "build.gradle.kts" ]; then
          BUILD_SYSTEM="gradle"
        elif [ -f "pom.xml" ]; then
          BUILD_SYSTEM="maven"
        else
          echo "Could not detect build system"
          exit 1
        fi

        echo "Detected build system: $BUILD_SYSTEM"
        START_TIME=$(date +%s)

        # Build with Elide
        if [ "$BUILD_SYSTEM" = "gradle" ]; then
          elide gradle build --no-daemon
        else
          elide mvn clean package
        fi
        BUILD_CODE=$?

        if [ $BUILD_CODE -eq 0 ]; then
          echo "✓ Build completed!"
          printf '\\a'  # Ring the bell
        fi

        # Run tests
        if [ "$BUILD_SYSTEM" = "gradle" ]; then
          elide gradle test --no-daemon
        else
          elide mvn test
        fi
        TEST_CODE=$?

        END_TIME=$(date +%s)
        echo ""
        echo "Total time: $((END_TIME - START_TIME))s"
        printf '\\a\\a'  # Ring bell twice

        exit $TEST_CODE
      `;
    } else {
      return `
        # Clone repository
        git clone "$REPO_URL" project || exit 1
        cd project

        # Detect build system
        if [ -f "build.gradle" ] || [ -f "build.gradle.kts" ]; then
          BUILD_SYSTEM="gradle"
        elif [ -f "pom.xml" ]; then
          BUILD_SYSTEM="maven"
        else
          echo "Could not detect build system"
          exit 1
        fi

        echo "Detected build system: $BUILD_SYSTEM"
        START_TIME=$(date +%s)

        # Build with standard tools
        if [ "$BUILD_SYSTEM" = "gradle" ]; then
          if [ -f "gradlew" ]; then
            chmod +x gradlew
            ./gradlew build --no-daemon
          else
            gradle build --no-daemon
          fi
        else
          if [ -f "mvnw" ]; then
            chmod +x mvnw
            ./mvnw clean package
          else
            mvn clean package
          fi
        fi
        BUILD_CODE=$?

        if [ $BUILD_CODE -eq 0 ]; then
          echo "✓ Build completed!"
          printf '\\a'  # Ring the bell
        fi

        # Run tests
        if [ "$BUILD_SYSTEM" = "gradle" ]; then
          if [ -f "gradlew" ]; then
            ./gradlew test --no-daemon
          else
            gradle test --no-daemon
          fi
        else
          if [ -f "mvnw" ]; then
            ./mvnw test
          else
            mvn test
          fi
        fi
        TEST_CODE=$?

        END_TIME=$(date +%s)
        echo ""
        echo "Total time: $((END_TIME - START_TIME))s"
        printf '\\a\\a'  # Ring bell twice

        exit $TEST_CODE
      `;
    }
  }

  /**
   * Wait for container to complete
   */
  private async waitForCompletion(
    container: Docker.Container,
    containerId: string,
    jobId: string,
    tool: BuildTool
  ): Promise<number> {
    return new Promise((resolve) => {
      const checkInterval = setInterval(async () => {
        try {
          const info = await container.inspect();

          if (!info.State.Running) {
            clearInterval(checkInterval);
            resolve(info.State.ExitCode || 0);
          }
        } catch (error) {
          console.error('Error inspecting container:', error);
          clearInterval(checkInterval);
          resolve(-1);
        }
      }, 1000);
    });
  }

  /**
   * Get container resource usage stats
   */
  private async getContainerStats(container: Docker.Container): Promise<{
    memoryUsage: number;
    cpuUsage: number;
  }> {
    try {
      const stats = await container.stats({ stream: false }) as Docker.ContainerStats;

      const memoryUsage = stats.memory_stats.usage || 0;
      const cpuDelta = stats.cpu_stats.cpu_usage.total_usage - (stats.precpu_stats.cpu_usage?.total_usage || 0);
      const systemDelta = stats.cpu_stats.system_cpu_usage - (stats.precpu_stats.system_cpu_usage || 0);
      const cpuUsage = systemDelta > 0 ? (cpuDelta / systemDelta) * 100 : 0;

      return { memoryUsage, cpuUsage };
    } catch (error) {
      console.error('Error getting container stats:', error);
      return { memoryUsage: 0, cpuUsage: 0 };
    }
  }

  /**
   * Cleanup container
   */
  private async cleanupContainer(container: Docker.Container): Promise<void> {
    try {
      const info = await container.inspect();
      if (info.State.Running) {
        await container.stop({ t: 10 });
      }
      await container.remove();
    } catch (error) {
      console.error('Error cleaning up container:', error);
    }
  }

  /**
   * Ensure Docker image exists
   */
  private async ensureImage(image: string): Promise<void> {
    try {
      await this.docker.getImage(image).inspect();
    } catch (error) {
      console.warn(`Image ${image} not found. Please build it first.`);
      throw new Error(`Docker image ${image} not available. Run: cd docker && ./build-images.sh`);
    }
  }
}
