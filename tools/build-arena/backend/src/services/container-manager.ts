import { spawn } from 'child_process';
import Docker from 'dockerode';

const docker = new Docker();

export interface StartContainerOptions {
  image: string;
  repoUrl: string;
  jobId: string;
  buildType: 'elide' | 'standard';
  apiKey: string;
}

/**
 * Start a Docker container for a build race
 */
export async function startContainer(options: StartContainerOptions): Promise<string> {
  const { image, repoUrl, jobId, buildType, apiKey } = options;

  return new Promise((resolve, reject) => {
    // Start container with interactive bash
    // The minder will connect via WebSocket and send the claude command
    const docker = spawn('docker', [
      'run',
      '-d',
      '--rm',
      '-i', // Interactive mode
      '-e',
      `ANTHROPIC_API_KEY=${apiKey}`,
      '-e',
      `REPOSITORY_URL=${repoUrl}`,
      '-e',
      `JOB_ID=${jobId}`,
      '-e',
      `BUILD_TYPE=${buildType}`,
      '--name',
      `race-${jobId}-${buildType}`,
      image,
      '/bin/bash', // Just start bash, minder will send commands
    ]);

    let containerId = '';
    let errorOutput = '';

    docker.stdout.on('data', (data) => {
      containerId += data.toString().trim();
    });

    docker.stderr.on('data', (data) => {
      errorOutput += data.toString();
    });

    docker.on('close', (code) => {
      if (code === 0 && containerId) {
        resolve(containerId);
      } else {
        reject(new Error(`Failed to start container: ${errorOutput}`));
      }
    });
  });
}

/**
 * Stop and remove a Docker container
 */
export async function stopContainer(containerId: string): Promise<void> {
  try {
    const container = docker.getContainer(containerId);

    // Get container info to check if it exists and is running
    const info = await container.inspect();

    // Stop container if running
    if (info.State.Running) {
      console.log(`[Container] Stopping container ${containerId.substring(0, 12)}`);
      await container.stop({ t: 10 }); // 10 second grace period
    }

    // Remove container (if not using --rm flag)
    // Note: containers started with --rm will auto-remove when stopped
    console.log(`[Container] Container ${containerId.substring(0, 12)} stopped`);
  } catch (error: any) {
    // Ignore errors if container doesn't exist or already stopped
    if (error.statusCode === 404) {
      console.log(`[Container] Container ${containerId.substring(0, 12)} already removed`);
    } else if (error.statusCode === 304) {
      console.log(`[Container] Container ${containerId.substring(0, 12)} already stopped`);
    } else {
      console.error(`[Container] Error stopping container ${containerId.substring(0, 12)}:`, error.message);
      throw error;
    }
  }
}

/**
 * Cleanup containers for a job (both elide and standard)
 */
export async function cleanupJobContainers(jobId: string): Promise<void> {
  console.log(`[Container] Cleaning up containers for job ${jobId}`);

  try {
    // List containers with this job ID label or name pattern
    const containers = await docker.listContainers({
      all: true,
      filters: {
        name: [`race-${jobId}-`]
      }
    });

    // Stop each container
    await Promise.all(
      containers.map(async (containerInfo) => {
        try {
          await stopContainer(containerInfo.Id);
        } catch (error) {
          console.error(`[Container] Failed to stop container ${containerInfo.Id.substring(0, 12)}:`, error);
        }
      })
    );

    console.log(`[Container] Cleaned up ${containers.length} containers for job ${jobId}`);
  } catch (error) {
    console.error(`[Container] Error cleaning up containers for job ${jobId}:`, error);
    throw error;
  }
}
