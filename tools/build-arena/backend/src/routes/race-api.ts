import { Router, type Router as ExpressRouter } from 'express';
import { db } from '../db/index.js';
import { jobs, buildResults, raceStatistics } from '../db/schema.js';
import { eq, desc, and, notLike } from 'drizzle-orm';
import { v4 as uuidv4 } from 'uuid';
import {
  generateCacheKey,
  findCachedRecording,
  loadRecording,
  type CacheKeyParams,
} from '../services/websocket-recorder.js';
import { RaceMinder } from '../services/race-minder.js';
import { startContainer, cleanupJobContainers } from '../services/container-manager.js';
import { updateRaceStatistics as updateStats, determineWinner } from '../services/race-statistics.js';
import { startRaceSchema, buildTypeSchema } from '../validation/schemas.js';
import { CONFIG, ERROR_MESSAGES, HTTP_STATUS } from '../config/constants.js';
import { env } from '../config/env.js';
import * as path from 'path';

export const raceApiRouter: ExpressRouter = Router();

const RECORDINGS_DIR = path.join(process.cwd(), CONFIG.RECORDINGS.DIR);

// Check if a race exists for a repository
raceApiRouter.get('/check', async (req, res) => {
  try {
    const repoUrl = req.query.repo as string;

    if (!repoUrl) {
      res.status(400).json({ error: 'Repository URL is required' });
      return;
    }

    // PRIORITY 1: Check for cached recordings first (fastest path)
    const elideCacheKey = generateCacheKey({
      repositoryUrl: repoUrl,
      tool: 'elide',
      claudeVersion: CONFIG.CLAUDE.VERSION,
      dockerImage: CONFIG.DOCKER.IMAGES.ELIDE,
    });

    const standardCacheKey = generateCacheKey({
      repositoryUrl: repoUrl,
      tool: 'standard',
      claudeVersion: CONFIG.CLAUDE.VERSION,
      dockerImage: CONFIG.DOCKER.IMAGES.STANDARD,
    });

    const [elideRecordingPath, standardRecordingPath] = await Promise.all([
      findCachedRecording(elideCacheKey, RECORDINGS_DIR),
      findCachedRecording(standardCacheKey, RECORDINGS_DIR),
    ]);

    // If both recordings exist, load metadata and return immediately
    if (elideRecordingPath && standardRecordingPath) {
      const [elideRecording, standardRecording] = await Promise.all([
        loadRecording(elideRecordingPath),
        loadRecording(standardRecordingPath),
      ]);

      // Determine winner from recording durations
      const winner = determineWinner(elideRecording.duration, standardRecording.duration);

      console.log(`[Race Check] Cache hit for ${repoUrl} - serving recordings`);

      res.json({
        exists: true,
        hasRecording: true,
        race: {
          jobId: 'cached', // No job ID for cached content
          repositoryUrl: repoUrl,
          repositoryName: repoUrl.split('/').pop()?.replace('.git', '') || 'unknown',
          status: 'completed',
          elide: {
            status: 'completed',
            duration: Math.floor(elideRecording.duration / 1000), // Convert ms to seconds
            hasRecording: true,
          },
          standard: {
            status: 'completed',
            duration: Math.floor(standardRecording.duration / 1000), // Convert ms to seconds
            hasRecording: true,
          },
          winner,
          stats: null, // Could load from DB if needed
        },
      });
      return;
    }

    console.log(`[Race Check] Cache miss for ${repoUrl} - checking database`);

    // PRIORITY 2: Check database for completed races (if no cached recordings)
    const recentJobs = await db
      .select({
        job: jobs,
        buildResults: buildResults,
      })
      .from(jobs)
      .leftJoin(buildResults, eq(jobs.id, buildResults.jobId))
      .where(
        and(
          eq(jobs.repositoryUrl, repoUrl),
          eq(jobs.status, 'completed'),
          notLike(jobs.id, 'mock-job-%') // Exclude mock data
        )
      )
      .orderBy(desc(jobs.completedAt))
      .limit(1);

    if (recentJobs.length === 0) {
      res.json({ exists: false });
      return;
    }

    const job = recentJobs[0].job;

    // Get both build results
    const results = await db.select().from(buildResults).where(eq(buildResults.jobId, job.id));

    const elideResult = results.find((r) => r.buildType === 'elide');
    const standardResult = results.find((r) => r.buildType === 'standard');

    if (!elideResult || !standardResult) {
      res.json({ exists: false });
      return;
    }

    // Get statistics
    const stats = await db
      .select()
      .from(raceStatistics)
      .where(eq(raceStatistics.repositoryUrl, repoUrl))
      .limit(1);

    // Determine winner
    const winner = determineWinner(elideResult.duration, standardResult.duration);

    res.json({
      exists: true,
      race: {
        jobId: job.id,
        repositoryUrl: job.repositoryUrl,
        repositoryName: job.repositoryName,
        status: 'completed',
        elide: {
          status: elideResult.status === 'success' ? 'completed' : 'failed',
          duration: elideResult.duration,
        },
        standard: {
          status: standardResult.status === 'success' ? 'completed' : 'failed',
          duration: standardResult.duration,
        },
        winner,
        stats: stats[0] || null,
      },
    });
  } catch (error) {
    console.error('Error checking race:', error);
    res.status(500).json({ error: 'Failed to check race' });
  }
});

// Start a new race
raceApiRouter.post('/start', async (req, res) => {
  try {
    // Validate request body
    const validationResult = startRaceSchema.safeParse(req.body);

    if (!validationResult.success) {
      res.status(HTTP_STATUS.BAD_REQUEST).json({
        error: validationResult.error.errors[0]?.message || ERROR_MESSAGES.REPO_URL_REQUIRED,
      });
      return;
    }

    const { repositoryUrl } = validationResult.data;

    // Extract repository name from URL
    const repoName = repositoryUrl.split('/').pop()?.replace('.git', '') || 'unknown';

    // Create job
    const jobId = uuidv4();
    await db.insert(jobs).values({
      id: jobId,
      repositoryUrl,
      repositoryName: repoName,
      status: 'running',
      createdAt: new Date(),
      startedAt: new Date(),
    });

    // Start Docker containers for both runners
    console.log(`[Race] Starting containers for job ${jobId}`);
    const elideContainerId = await startContainer({
      image: CONFIG.DOCKER.IMAGES.ELIDE,
      repoUrl: repositoryUrl,
      jobId,
      buildType: 'elide',
      apiKey: env.ANTHROPIC_API_KEY,
    });
    console.log(`[Race] Elide container started: ${elideContainerId.substring(0, 12)}`);

    const standardContainerId = await startContainer({
      image: CONFIG.DOCKER.IMAGES.STANDARD,
      repoUrl: repositoryUrl,
      jobId,
      buildType: 'standard',
      apiKey: env.ANTHROPIC_API_KEY,
    });
    console.log(`[Race] Standard container started: ${standardContainerId.substring(0, 12)}`);

    // Create initial build_results entries with container IDs
    await db.insert(buildResults).values([
      {
        jobId,
        buildType: 'elide',
        containerId: elideContainerId,
        status: 'pending',
        duration: 0,
        bellRung: false,
        createdAt: new Date(),
      },
      {
        jobId,
        buildType: 'standard',
        containerId: standardContainerId,
        status: 'pending',
        duration: 0,
        bellRung: false,
        createdAt: new Date(),
      },
    ]);

    // Start minders for both runners
    // Minders connect to WebSocket and drive the build with auto-approval
    // Frontend connects to same WebSocket in view-only mode
    const host = process.env.HOST || 'localhost';
    const port = process.env.PORT || 3001;

    // Build WebSocket URLs with metadata for recording
    const elideWsUrl = `ws://${host}:${port}/ws/terminal/${elideContainerId}?` + new URLSearchParams({
      record: 'true',
      jobId,
      buildType: 'elide',
      repoUrl: repositoryUrl,
      claudeVersion: CONFIG.CLAUDE.VERSION,
      dockerImage: CONFIG.DOCKER.IMAGES.ELIDE
    }).toString();

    const standardWsUrl = `ws://${host}:${port}/ws/terminal/${standardContainerId}?` + new URLSearchParams({
      record: 'true',
      jobId,
      buildType: 'standard',
      repoUrl: repositoryUrl,
      claudeVersion: CONFIG.CLAUDE.VERSION,
      dockerImage: CONFIG.DOCKER.IMAGES.STANDARD
    }).toString();

    // Track completion of both minders
    const minderResults = {
      elide: null as any,
      standard: null as any,
    };

    let raceFinalized = false;

    const finalizeRace = async () => {
      if (raceFinalized || !minderResults.elide || !minderResults.standard) {
        return; // Not ready yet or already finalized
      }

      raceFinalized = true;
      console.log(`[Race] Both minders completed for job ${jobId} - finalizing race`);

      try {
        // Update build results with final durations
        await db.update(buildResults)
          .set({
            status: minderResults.elide.success ? 'success' : 'failure',
            duration: minderResults.elide.duration,
            bellRung: minderResults.elide.bellRung,
          })
          .where(and(
            eq(buildResults.jobId, jobId),
            eq(buildResults.buildType, 'elide')
          ));

        await db.update(buildResults)
          .set({
            status: minderResults.standard.success ? 'success' : 'failure',
            duration: minderResults.standard.duration,
            bellRung: minderResults.standard.bellRung,
          })
          .where(and(
            eq(buildResults.jobId, jobId),
            eq(buildResults.buildType, 'standard')
          ));

        // Update job as completed
        await db.update(jobs)
          .set({
            status: 'completed',
            completedAt: new Date(),
          })
          .where(eq(jobs.id, jobId));

        // Update race statistics
        await updateStats(jobId);

        console.log(`[Race] Race ${jobId} finalized successfully`);

        // Cleanup containers after a delay to allow recordings to save
        setTimeout(async () => {
          try {
            await cleanupJobContainers(jobId);
            console.log(`[Race] Containers cleaned up for job ${jobId}`);
          } catch (error) {
            console.error(`[Race] Error cleaning up containers for job ${jobId}:`, error);
          }
        }, 5000); // 5 second delay to ensure recordings are saved
      } catch (error) {
        console.error(`[Race] Error finalizing race ${jobId}:`, error);
      }
    };

    const elideMinder = new RaceMinder({
      containerId: elideContainerId,
      repoUrl: repositoryUrl,
      buildType: 'elide',
      wsUrl: elideWsUrl,
      onComplete: async (result) => {
        console.log(`[Race] Elide minder completed:`, result);
        minderResults.elide = result;
        await finalizeRace();
      },
    });

    const standardMinder = new RaceMinder({
      containerId: standardContainerId,
      repoUrl: repositoryUrl,
      buildType: 'standard',
      wsUrl: standardWsUrl,
      onComplete: async (result) => {
        console.log(`[Race] Standard minder completed:`, result);
        minderResults.standard = result;
        await finalizeRace();
      },
    });

    // Start minders in background (don't await - they run autonomously)
    elideMinder.start().catch((err) => console.error(`[Race] Elide minder error:`, err));
    standardMinder.start().catch((err) => console.error(`[Race] Standard minder error:`, err));

    console.log(`[Race] Started race ${jobId} with minders`);

    res.json({
      jobId,
      repositoryUrl,
      repositoryName: repoName,
      status: 'running',
      elide: {
        status: 'running',
        containerId: elideContainerId,
      },
      standard: {
        status: 'running',
        containerId: standardContainerId,
      },
    });
  } catch (error) {
    console.error('Error starting race:', error);
    res.status(500).json({ error: 'Failed to start race' });
  }
});

// Get recording for playback
raceApiRouter.get('/:jobId/recording/:buildType', async (req, res) => {
  try {
    // Validate build type
    const buildTypeResult = buildTypeSchema.safeParse(req.params.buildType);

    if (!buildTypeResult.success) {
      res.status(HTTP_STATUS.BAD_REQUEST).json({ error: ERROR_MESSAGES.INVALID_BUILD_TYPE });
      return;
    }

    const { jobId } = req.params;
    const buildType = buildTypeResult.data;

    // Get job and build result
    const job = await db.select().from(jobs).where(eq(jobs.id, jobId)).limit(1);
    if (job.length === 0) {
      res.status(HTTP_STATUS.NOT_FOUND).json({ error: ERROR_MESSAGES.JOB_NOT_FOUND });
      return;
    }

    // Find cache key for this build
    const cacheKeyParams: CacheKeyParams = {
      repositoryUrl: job[0].repositoryUrl,
      tool: buildType,
      claudeVersion: CONFIG.CLAUDE.VERSION,
      dockerImage:
        buildType === 'elide' ? CONFIG.DOCKER.IMAGES.ELIDE : CONFIG.DOCKER.IMAGES.STANDARD,
    };

    const cacheKey = generateCacheKey(cacheKeyParams);
    const recordingPath = await findCachedRecording(cacheKey, RECORDINGS_DIR);

    if (!recordingPath) {
      res.status(HTTP_STATUS.NOT_FOUND).json({ error: ERROR_MESSAGES.RECORDING_NOT_FOUND });
      return;
    }

    // Load and return recording
    const recording = await loadRecording(recordingPath);
    res.json(recording);
  } catch (error) {
    console.error('Error fetching recording:', error);
    res.status(500).json({ error: 'Failed to fetch recording' });
  }
});

// Get minder status for debugging (must be before /:jobId route)
raceApiRouter.get('/minders', async (_req, res) => {
  try {
    const { getActiveMinders } = await import('../services/race-minder.js');
    const minders = getActiveMinders();

    const minderStatuses = Array.from(minders.values()).map(minder => minder.getStatus());

    res.json({
      count: minderStatuses.length,
      minders: minderStatuses
    });
  } catch (error) {
    console.error('Error fetching minder status:', error);
    res.status(500).json({ error: 'Failed to fetch minder status' });
  }
});

// Get recent completed races (must be before /:jobId route)
raceApiRouter.get('/recent', async (req, res) => {
  try {
    const limit = Math.min(parseInt(req.query.limit as string) || 10, 50); // Max 50 races

    // Get recent completed jobs with their build results
    const recentJobs = await db
      .select({
        job: jobs,
      })
      .from(jobs)
      .where(
        and(
          eq(jobs.status, 'completed'),
          notLike(jobs.id, 'mock-job-%') // Exclude mock data
        )
      )
      .orderBy(desc(jobs.completedAt))
      .limit(limit);

    // For each job, get build results and check for recordings
    const racesWithDetails = await Promise.all(
      recentJobs.map(async ({ job }) => {
        const results = await db.select().from(buildResults).where(eq(buildResults.jobId, job.id));

        const elideResult = results.find((r) => r.buildType === 'elide');
        const standardResult = results.find((r) => r.buildType === 'standard');

        // Check for cached recordings
        const elideCacheKey = generateCacheKey({
          repositoryUrl: job.repositoryUrl,
          tool: 'elide',
          claudeVersion: CONFIG.CLAUDE.VERSION,
          dockerImage: CONFIG.DOCKER.IMAGES.ELIDE,
        });

        const standardCacheKey = generateCacheKey({
          repositoryUrl: job.repositoryUrl,
          tool: 'standard',
          claudeVersion: CONFIG.CLAUDE.VERSION,
          dockerImage: CONFIG.DOCKER.IMAGES.STANDARD,
        });

        const [elideRecordingPath, standardRecordingPath] = await Promise.all([
          findCachedRecording(elideCacheKey, RECORDINGS_DIR),
          findCachedRecording(standardCacheKey, RECORDINGS_DIR),
        ]);

        const hasRecording = !!elideRecordingPath && !!standardRecordingPath;

        // Determine winner
        let winner: 'elide' | 'standard' | 'tie' | undefined;
        if (elideResult && standardResult) {
          winner = determineWinner(elideResult.duration, standardResult.duration);
        }

        return {
          jobId: job.id,
          repositoryUrl: job.repositoryUrl,
          repositoryName: job.repositoryName,
          status: job.status,
          startedAt: job.startedAt?.toISOString(),
          completedAt: job.completedAt?.toISOString(),
          hasRecording,
          elide: elideResult
            ? {
                status: elideResult.status === 'success' ? 'completed' : 'failed',
                duration: elideResult.duration,
              }
            : null,
          standard: standardResult
            ? {
                status: standardResult.status === 'success' ? 'completed' : 'failed',
                duration: standardResult.duration,
              }
            : null,
          winner,
        };
      })
    );

    res.json({ races: racesWithDetails });
  } catch (error) {
    console.error('Error fetching recent races:', error);
    res.status(500).json({ error: 'Failed to fetch recent races' });
  }
});

// Get race by job ID
raceApiRouter.get('/:jobId', async (req, res) => {
  try {
    const { jobId } = req.params;

    // Get job from database
    const job = await db.select().from(jobs).where(eq(jobs.id, jobId)).limit(1);

    if (job.length === 0) {
      res.status(HTTP_STATUS.NOT_FOUND).json({ error: ERROR_MESSAGES.JOB_NOT_FOUND });
      return;
    }

    // Get build results for this job
    const results = await db.select().from(buildResults).where(eq(buildResults.jobId, jobId));

    const elideResult = results.find((r) => r.buildType === 'elide');
    const standardResult = results.find((r) => r.buildType === 'standard');

    // Get statistics
    const stats = await db
      .select()
      .from(raceStatistics)
      .where(eq(raceStatistics.repositoryUrl, job[0].repositoryUrl))
      .limit(1);

    // Determine winner if both completed
    let winner: 'elide' | 'standard' | 'tie' | undefined;
    if (elideResult && standardResult && elideResult.status === 'success' && standardResult.status === 'success') {
      winner = determineWinner(elideResult.duration, standardResult.duration);
    }

    // Map status from buildResults to frontend status
    const mapStatus = (status: string | null) => {
      if (!status || status === 'pending') return 'running';
      if (status === 'success') return 'completed';
      return 'failed';
    };

    // Check if race is ready for viewing (containers are up and have output)
    // A race is ready when:
    // 1. Both build results exist with container IDs
    // 2. Containers have been running for at least 3 seconds (to ensure terminals are connected)
    const isReady = elideResult?.containerId &&
                    standardResult?.containerId &&
                    job[0].startedAt &&
                    (Date.now() - new Date(job[0].startedAt).getTime()) > 3000;

    res.json({
      jobId: job[0].id,
      repositoryUrl: job[0].repositoryUrl,
      repositoryName: job[0].repositoryName,
      status: job[0].status,
      startedAt: job[0].startedAt?.toISOString(),
      ready: isReady, // Frontend should wait for this before connecting WebSockets
      elide: elideResult
        ? {
            status: mapStatus(elideResult.status),
            duration: elideResult.duration,
            containerId: elideResult.containerId,
            hasRecording: elideResult.status === 'success', // Only if completed successfully
          }
        : {
            status: 'running',
            duration: 0,
            containerId: undefined,
            hasRecording: false,
          },
      standard: standardResult
        ? {
            status: mapStatus(standardResult.status),
            duration: standardResult.duration,
            containerId: standardResult.containerId,
            hasRecording: standardResult.status === 'success',
          }
        : {
            status: 'running',
            duration: 0,
            containerId: undefined,
            hasRecording: false,
          },
      winner,
      stats: stats[0] || null,
    });
  } catch (error) {
    console.error('Error fetching race:', error);
    res.status(500).json({ error: 'Failed to fetch race' });
  }
});

// Get race statistics
raceApiRouter.get('/stats', async (req, res) => {
  try {
    const repoUrl = req.query.repo as string;

    if (!repoUrl) {
      // Return overall statistics
      const allStats = await db
        .select()
        .from(raceStatistics)
        .orderBy(desc(raceStatistics.lastRaceAt));
      res.json({ statistics: allStats });
    } else {
      // Return statistics for specific repo
      const stats = await db
        .select()
        .from(raceStatistics)
        .where(eq(raceStatistics.repositoryUrl, repoUrl))
        .limit(1);

      res.json({ statistics: stats[0] || null });
    }
  } catch (error) {
    console.error('Error fetching statistics:', error);
    res.status(500).json({ error: 'Failed to fetch statistics' });
  }
});

// Export the updateRaceStatistics function for use by other modules
export { updateStats as updateRaceStatistics };
