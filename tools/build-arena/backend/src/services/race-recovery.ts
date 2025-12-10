/**
 * Race Recovery Service
 * 
 * Handles reconnection to orphaned races after backend restarts.
 * Detects races with running containers but no active minders and recreates them.
 */

import { db } from '../db/index.js';
import { jobs, buildResults } from '../db/schema.js';
import { eq } from 'drizzle-orm';
import { RaceMinder } from './race-minder.js';
import { env } from '../config/env.js';
import { spawn } from 'child_process';

interface OrphanedRace {
  jobId: string;
  repositoryUrl: string;
  elideContainerId: string;
  standardContainerId: string;
}

/**
 * Find races that have running containers but no active minders
 */
export async function findOrphanedRaces(): Promise<OrphanedRace[]> {
  // Get all running jobs from database
  const runningJobs = await db
    .select()
    .from(jobs)
    .where(eq(jobs.status, 'running'));

  const orphaned: OrphanedRace[] = [];

  for (const job of runningJobs) {
    // Get build results for this job
    const results = await db
      .select()
      .from(buildResults)
      .where(eq(buildResults.jobId, job.id));

    const elideResult = results.find(r => r.buildType === 'elide');
    const standardResult = results.find(r => r.buildType === 'standard');

    if (!elideResult?.containerId || !standardResult?.containerId) {
      continue;
    }

    // Check if containers are actually running
    const elideRunning = await isContainerRunning(elideResult.containerId);
    const standardRunning = await isContainerRunning(standardResult.containerId);

    if (elideRunning && standardRunning) {
      orphaned.push({
        jobId: job.id,
        repositoryUrl: job.repositoryUrl,
        elideContainerId: elideResult.containerId,
        standardContainerId: standardResult.containerId,
      });
    }
  }

  return orphaned;
}

/**
 * Check if a Docker container is running
 */
async function isContainerRunning(containerId: string): Promise<boolean> {
  return new Promise((resolve) => {
    const docker = spawn('docker', ['inspect', '-f', '{{.State.Running}}', containerId]);
    let output = '';

    docker.stdout.on('data', (data) => {
      output += data.toString().trim();
    });

    docker.on('close', (code) => {
      resolve(code === 0 && output === 'true');
    });

    docker.on('error', () => {
      resolve(false);
    });
  });
}

/**
 * Reconnect minders to orphaned races
 */
export async function reconnectOrphanedRaces(): Promise<number> {
  const orphaned = await findOrphanedRaces();

  if (orphaned.length === 0) {
    return 0;
  }

  console.log(`[Recovery] Found ${orphaned.length} orphaned race(s), reconnecting minders...`);

  for (const race of orphaned) {
    try {
      const host = process.env.HOST || 'localhost';
      const port = process.env.PORT || 3001;

      // Create new minders for the existing containers
      const elideMinder = new RaceMinder({
        containerId: race.elideContainerId,
        repoUrl: race.repositoryUrl,
        buildType: 'elide',
        wsUrl: `ws://${host}:${port}/ws/terminal/${race.elideContainerId}?record=true`,
      });

      const standardMinder = new RaceMinder({
        containerId: race.standardContainerId,
        repoUrl: race.repositoryUrl,
        buildType: 'standard',
        wsUrl: `ws://${host}:${port}/ws/terminal/${race.standardContainerId}?record=true`,
      });

      // Start minders in background
      elideMinder.start().catch((err) => 
        console.error(`[Recovery] Elide minder error for job ${race.jobId}:`, err)
      );
      
      standardMinder.start().catch((err) => 
        console.error(`[Recovery] Standard minder error for job ${race.jobId}:`, err)
      );

      console.log(`[Recovery] Reconnected minders for race ${race.jobId}`);
    } catch (error) {
      console.error(`[Recovery] Failed to reconnect race ${race.jobId}:`, error);
    }
  }

  return orphaned.length;
}

/**
 * Get recovery status for status API
 */
export async function getRecoveryStatus() {
  const orphaned = await findOrphanedRaces();
  
  return {
    orphanedRacesDetected: orphaned.length,
    races: orphaned.map(r => ({
      jobId: r.jobId,
      repositoryUrl: r.repositoryUrl,
      elideContainer: r.elideContainerId.substring(0, 12),
      standardContainer: r.standardContainerId.substring(0, 12),
    })),
  };
}
