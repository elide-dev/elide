import { Router } from 'express';
import Docker from 'dockerode';
import { v4 as uuidv4 } from 'uuid';

export const testApiRouter = Router();
const docker = new Docker();

// Store active test containers
const testContainers = new Map<string, Docker.Container>();

/**
 * Start a test container for terminal testing
 */
testApiRouter.post('/test/start-container', async (req, res) => {
  try {
    const { image = 'elide-builder:latest' } = req.body;

    console.log(`Starting test container with image: ${image}`);

    // Create container
    const container = await docker.createContainer({
      Image: image,
      Tty: true,
      OpenStdin: true,
      StdinOnce: false,
      Cmd: ['/bin/bash'],
      WorkingDir: '/workspace',
      Env: [
        'TERM=xterm-256color',
        'LANG=C.UTF-8',
        `ANTHROPIC_API_KEY=${process.env.ANTHROPIC_API_KEY || ''}`,
      ],
    });

    // Start container
    await container.start();

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
 * Stop a test container
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

    // Stop and remove container
    await container.stop();
    await container.remove();

    testContainers.delete(containerId);

    console.log(`Test container stopped: ${containerId}`);

    res.json({
      containerId,
      status: 'stopped',
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
testApiRouter.get('/test/containers', async (req, res) => {
  try {
    const containers = Array.from(testContainers.entries()).map(([id, container]) => ({
      id,
      // We can add more details here if needed
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
 * Cleanup all test containers (for development)
 */
testApiRouter.post('/test/cleanup', async (req, res) => {
  try {
    console.log(`Cleaning up ${testContainers.size} test containers`);

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
  console.log('Cleaning up test containers on shutdown...');
  for (const [id, container] of testContainers.entries()) {
    try {
      await container.stop();
      await container.remove();
    } catch (error) {
      console.error(`Error cleaning up container ${id}:`, error);
    }
  }
});
