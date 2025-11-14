import type Docker from 'dockerode';

export interface ContainerStats {
  memoryUsage: number;
  cpuUsage: number;
}

/**
 * Get container resource usage statistics
 */
export async function getContainerStats(container: Docker.Container): Promise<ContainerStats> {
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
 * Wait for container to complete and return exit code
 */
export async function waitForContainerCompletion(container: Docker.Container): Promise<number> {
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
 * Cleanup container (stop and remove)
 */
export async function cleanupContainer(container: Docker.Container): Promise<void> {
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
