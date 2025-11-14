import { Router, type Router as ExpressRouter } from 'express';
import Docker from 'dockerode';
import { spawn, ChildProcess } from 'child_process';
import path from 'path';
import { fileURLToPath } from 'url';
import { dirname } from 'path';

// ES module compatibility for __dirname
const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

export const testApiRouter: ExpressRouter = Router();
const docker = new Docker();

// Store active test containers and their minder processes
const testContainers = new Map<string, Docker.Container>();
const minderProcesses = new Map<string, ChildProcess>();

/**
 * Helper function to create and start a container
 */
async function createAndStartContainer(image: string = 'elide-builder:latest'): Promise<Docker.Container> {
  const container = await docker.createContainer({
    Image: image,
    Tty: true,
    OpenStdin: true,
    StdinOnce: false,
    Cmd: ['/bin/bash', '-l'],  // -l for login shell (sources .bashrc/.profile)
    WorkingDir: '/workspace',
    Env: [
      'TERM=xterm-256color',
      'LANG=C.UTF-8',
      `ANTHROPIC_API_KEY=${process.env.ANTHROPIC_API_KEY || ''}`,
    ],
  });

  await container.start();
  return container;
}

/**
 * Start a test container for terminal testing (basic version - no minder)
 */
testApiRouter.post('/test/start-container', async (req, res) => {
  try {
    const { image = 'elide-builder:latest' } = req.body;

    console.log(`Starting test container with image: ${image}`);

    const container = await createAndStartContainer(image);
    const containerId = container.id;
    testContainers.set(containerId, container);

    console.log(`Test container started: ${containerId}`);

    res.json({
      containerId,
      image,
      status: 'running',
    });
  } catch (error) {
    console.error('Error starting test container:', error);
    res.status(500).json({
      error: 'Failed to start container',
      details: error instanceof Error ? error.message : 'Unknown error',
    });
  }
});

/**
 * Start a test container WITH minder process for autonomous operation
 */
testApiRouter.post('/test/start-container-with-minder', async (req, res) => {
  try {
    const { image = 'elide-builder:latest', repoUrl = 'https://github.com/google/gson.git' } = req.body;

    console.log(`Starting test container with minder for image: ${image}`);

    // Create and start container
    const container = await createAndStartContainer(image);
    const containerId = container.id;
    testContainers.set(containerId, container);

    console.log(`Test container started: ${containerId}`);

    // Start minder process
    // The minder script path is relative to the backend directory
    const minderScriptPath = path.resolve(__dirname, '../../../test-claude-auto-approve.js');
    console.log(`Starting minder process with script: ${minderScriptPath}`);

    const minderProcess = spawn('node', [minderScriptPath], {
      env: {
        ...process.env,
        BACKEND_URL: 'http://localhost:3001',
        REPO_URL: repoUrl,
        CONTAINER_ID: containerId,
      },
      stdio: ['ignore', 'pipe', 'pipe'], // Capture stdout and stderr
      detached: false, // Keep attached so we can kill it
    });

    minderProcesses.set(containerId, minderProcess);

    // Log minder output
    minderProcess.stdout?.on('data', (data) => {
      console.log(`[Minder ${containerId.substring(0, 12)}] ${data.toString()}`);
    });

    minderProcess.stderr?.on('data', (data) => {
      console.error(`[Minder ${containerId.substring(0, 12)} ERROR] ${data.toString()}`);
    });

    minderProcess.on('exit', (code) => {
      console.log(`[Minder ${containerId.substring(0, 12)}] Process exited with code ${code}`);
      minderProcesses.delete(containerId);
    });

    console.log(`Minder process started for container: ${containerId} (PID: ${minderProcess.pid})`);

    res.json({
      containerId,
      image,
      status: 'running',
      minderPid: minderProcess.pid,
      minderStatus: 'running',
    });
  } catch (error) {
    console.error('Error starting test container with minder:', error);
    res.status(500).json({
      error: 'Failed to start container with minder',
      details: error instanceof Error ? error.message : 'Unknown error',
    });
  }
});

/**
 * Stop a test container (and its minder process if any)
 */
testApiRouter.post('/test/stop-container/:containerId', async (req, res) => {
  try {
    const { containerId } = req.params;
    const container = testContainers.get(containerId);

    if (!container) {
      res.status(404).json({ error: 'Container not found' });
      return;
    }

    console.log(`Stopping test container: ${containerId}`);

    // Kill minder process if it exists
    const minderProcess = minderProcesses.get(containerId);
    if (minderProcess) {
      console.log(`Killing minder process for container: ${containerId} (PID: ${minderProcess.pid})`);
      minderProcess.kill('SIGTERM');
      minderProcesses.delete(containerId);
    }

    // Stop and remove container
    await container.stop();
    await container.remove();

    testContainers.delete(containerId);

    console.log(`Test container stopped: ${containerId}`);

    res.json({
      containerId,
      status: 'stopped',
      minderKilled: minderProcess !== undefined,
    });
  } catch (error) {
    console.error('Error stopping test container:', error);
    res.status(500).json({
      error: 'Failed to stop container',
      details: error instanceof Error ? error.message : 'Unknown error',
    });
  }
});

/**
 * List active test containers
 */
testApiRouter.get('/test/containers', async (_req, res) => {
  try {
    const containers = Array.from(testContainers.entries()).map(([id, _container]) => ({
      id,
      hasMinder: minderProcesses.has(id),
      minderPid: minderProcesses.get(id)?.pid,
    }));

    res.json({ containers });
  } catch (error) {
    console.error('Error listing test containers:', error);
    res.status(500).json({
      error: 'Failed to list containers',
      details: error instanceof Error ? error.message : 'Unknown error',
    });
  }
});

/**
 * Get logs for a specific container's minder process
 */
testApiRouter.get('/test/container/:containerId/logs', async (req, res) => {
  try {
    const { containerId } = req.params;
    const minderProcess = minderProcesses.get(containerId);

    if (!minderProcess) {
      res.status(404).json({
        error: 'Minder not found',
        message: 'No minder process found for this container'
      });
      return;
    }

    res.json({
      containerId: containerId.substring(0, 12),
      minderPid: minderProcess.pid,
      status: minderProcess.exitCode === null ? 'running' : 'exited',
      exitCode: minderProcess.exitCode,
    });
  } catch (error) {
    console.error('Error getting minder logs:', error);
    res.status(500).json({
      error: 'Failed to get logs',
      details: error instanceof Error ? error.message : 'Unknown error',
    });
  }
});

/**
 * Cleanup all test containers and minder processes (for development)
 */
testApiRouter.post('/test/cleanup', async (_req, res) => {
  try {
    console.log(`Cleaning up ${testContainers.size} test containers and ${minderProcesses.size} minder processes`);

    // Kill all minder processes first
    for (const [id, minderProcess] of minderProcesses.entries()) {
      try {
        console.log(`Killing minder process for container ${id} (PID: ${minderProcess.pid})`);
        minderProcess.kill('SIGTERM');
        minderProcesses.delete(id);
      } catch (error) {
        console.error(`Error killing minder for container ${id}:`, error);
      }
    }

    // Then clean up containers
    const cleanupPromises = Array.from(testContainers.entries()).map(async ([id, container]) => {
      try {
        await container.stop();
        await container.remove();
        testContainers.delete(id);
      } catch (error) {
        console.error(`Error cleaning up container ${id}:`, error);
      }
    });

    await Promise.all(cleanupPromises);

    res.json({
      message: 'Cleanup complete',
      removed: cleanupPromises.length,
      mindersKilled: minderProcesses.size,
    });
  } catch (error) {
    console.error('Error during cleanup:', error);
    res.status(500).json({
      error: 'Cleanup failed',
      details: error instanceof Error ? error.message : 'Unknown error',
    });
  }
});

// Cleanup on server shutdown
process.on('SIGTERM', async () => {
  console.log('Cleaning up test containers and minder processes on shutdown...');

  // Kill minder processes first
  for (const [id, minderProcess] of minderProcesses.entries()) {
    try {
      console.log(`Killing minder process for container ${id} (PID: ${minderProcess.pid})`);
      minderProcess.kill('SIGTERM');
    } catch (error) {
      console.error(`Error killing minder for container ${id}:`, error);
    }
  }

  // Then clean up containers
  for (const [id, container] of testContainers.entries()) {
    try {
      await container.stop();
      await container.remove();
    } catch (error) {
      console.error(`Error cleaning up container ${id}:`, error);
    }
  }
});
