import { spawn } from 'child_process';
import * as path from 'path';

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
    const instructionsFile = buildType === 'elide' ? 'CLAUDE-ELIDE.md' : 'CLAUDE-STANDARD.md';
    const instructionsPath = path.join(process.cwd(), '../docker', instructionsFile);

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
      '-v',
      `${instructionsPath}:/instructions.md:ro`,
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
