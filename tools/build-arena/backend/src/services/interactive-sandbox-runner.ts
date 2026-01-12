import Docker from 'dockerode';
import { EventEmitter } from 'events';
import { readFileSync } from 'fs';
import { join } from 'path';
import type { BuildTool, BuildResult, TerminalOutput } from '../../../shared/types.js';
import { generateClaudeAgentScript } from './build-scripts/index.js';
import { getContainerStats, waitForContainerCompletion, cleanupContainer } from '../utils/docker-stats.js';

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
      const exitCode = await waitForContainerCompletion(container);

      // Get metrics
      const stats = await getContainerStats(container);

      const endTime = new Date().toISOString();
      const duration = new Date(endTime).getTime() - new Date(startTime).getTime();

      // Cleanup
      await cleanupContainer(container);
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
        promises.push(cleanupContainer(container));
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

    // Generate startup script using extracted script generator
    const startupScript = generateClaudeAgentScript(tool, repositoryUrl, claudeInstructions);

    // Create exec instance with interactive TTY
    const exec = await container.exec({
      Cmd: ['/bin/bash', '-c', startupScript],
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

    stream.on('error', (error: Error) => {
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
