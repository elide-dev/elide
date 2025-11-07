import Docker from 'dockerode';
import { EventEmitter } from 'events';
import type { BuildTool, BuildResult, TerminalOutput } from '../../../shared/types.js';

/**
 * Manages Docker-based sandbox execution for builds
 */
export class SandboxRunner extends EventEmitter {
  private docker: Docker;
  private runningContainers: Map<string, Docker.Container> = new Map();

  constructor() {
    super();
    this.docker = new Docker();
  }

  /**
   * Run a build in a Docker sandbox
   */
  async runBuild(
    jobId: string,
    repositoryUrl: string,
    tool: BuildTool
  ): Promise<BuildResult> {
    const startTime = new Date().toISOString();
    const containerId = `${jobId}-${tool}`;

    this.emit('build:started', { jobId, tool, timestamp: startTime });

    try {
      // Create container with appropriate build environment
      const container = await this.createBuildContainer(repositoryUrl, tool);
      this.runningContainers.set(containerId, container);

      // Start container and stream output
      await container.start();

      const exitCode = await this.streamContainerOutput(container, jobId, tool);

      // Get container stats for metrics
      const stats = await this.getContainerStats(container);

      const endTime = new Date().toISOString();
      const duration = new Date(endTime).getTime() - new Date(startTime).getTime();

      // Cleanup
      await this.cleanupContainer(container);
      this.runningContainers.delete(containerId);

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

      console.error(`Build failed for ${tool}:`, error);

      this.runningContainers.delete(containerId);

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
   * Stop a running build
   */
  async stopBuild(jobId: string): Promise<void> {
    const promises: Promise<void>[] = [];

    for (const [containerId, container] of this.runningContainers.entries()) {
      if (containerId.startsWith(jobId)) {
        promises.push(this.cleanupContainer(container));
        this.runningContainers.delete(containerId);
      }
    }

    await Promise.all(promises);
  }

  /**
   * Create a Docker container for the build
   */
  private async createBuildContainer(
    repositoryUrl: string,
    tool: BuildTool
  ): Promise<Docker.Container> {
    const image = tool === 'elide' ? 'elide-builder:latest' : 'standard-builder:latest';

    // Ensure image exists (in production, these should be pre-built)
    await this.ensureImage(image);

    const container = await this.docker.createContainer({
      Image: image,
      Cmd: ['/bin/bash', '-c', this.getBuildScript(repositoryUrl, tool)],
      Env: [
        `REPO_URL=${repositoryUrl}`,
        `BUILD_TOOL=${tool}`,
        `ANTHROPIC_API_KEY=${process.env.ANTHROPIC_API_KEY || ''}`,
      ],
      HostConfig: {
        Memory: 2 * 1024 * 1024 * 1024, // 2GB memory limit
        MemorySwap: 2 * 1024 * 1024 * 1024, // No swap
        CpuQuota: 100000, // 1 CPU core
        NetworkMode: 'bridge',
        AutoRemove: false,
      },
      AttachStdout: true,
      AttachStderr: true,
    });

    return container;
  }

  /**
   * Stream container output to WebSocket clients
   */
  private async streamContainerOutput(
    container: Docker.Container,
    jobId: string,
    tool: BuildTool
  ): Promise<number> {
    return new Promise((resolve, reject) => {
      container.attach(
        { stream: true, stdout: true, stderr: true },
        (err, stream) => {
          if (err) {
            reject(err);
            return;
          }

          if (!stream) {
            reject(new Error('No stream available'));
            return;
          }

          // Demultiplex Docker stream
          const stdout: Buffer[] = [];
          const stderr: Buffer[] = [];

          container.modem.demuxStream(stream, {
            write: (chunk: Buffer) => {
              const data = chunk.toString('utf-8');
              stdout.push(chunk);

              const output: TerminalOutput = {
                jobId,
                tool,
                type: 'stdout',
                data,
                timestamp: new Date().toISOString(),
              };

              this.emit('terminal:output', output);
            },
          } as NodeJS.WritableStream, {
            write: (chunk: Buffer) => {
              const data = chunk.toString('utf-8');
              stderr.push(chunk);

              const output: TerminalOutput = {
                jobId,
                tool,
                type: 'stderr',
                data,
                timestamp: new Date().toISOString(),
              };

              this.emit('terminal:output', output);
            },
          } as NodeJS.WritableStream);

          stream.on('end', async () => {
            const info = await container.inspect();
            resolve(info.State.ExitCode || 0);
          });

          stream.on('error', reject);
        }
      );
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
        await container.stop({ t: 10 }); // 10 second grace period
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
      // Image doesn't exist, would need to build it
      console.warn(`Image ${image} not found. In production, ensure images are pre-built.`);
      throw new Error(`Docker image ${image} not available`);
    }
  }

  /**
   * Generate build script for the container
   */
  private getBuildScript(repositoryUrl: string, tool: BuildTool): string {
    if (tool === 'elide') {
      return `
        set -e
        echo "=== Cloning repository with Elide ==="
        git clone ${repositoryUrl} /workspace
        cd /workspace
        echo "=== Detecting build system ==="
        if [ -f "build.gradle" ] || [ -f "build.gradle.kts" ]; then
          echo "Detected Gradle project"
          elide gradle build --no-daemon
        elif [ -f "pom.xml" ]; then
          echo "Detected Maven project"
          elide mvn clean package
        else
          echo "No recognized build system found"
          exit 1
        fi
        echo "=== Build completed ==="
      `;
    } else {
      return `
        set -e
        echo "=== Cloning repository with standard tools ==="
        git clone ${repositoryUrl} /workspace
        cd /workspace
        echo "=== Detecting build system ==="
        if [ -f "build.gradle" ] || [ -f "build.gradle.kts" ]; then
          echo "Detected Gradle project"
          ./gradlew build --no-daemon || gradle build --no-daemon
        elif [ -f "pom.xml" ]; then
          echo "Detected Maven project"
          mvn clean package
        else
          echo "No recognized build system found"
          exit 1
        fi
        echo "=== Build completed ==="
      `;
    }
  }
}
