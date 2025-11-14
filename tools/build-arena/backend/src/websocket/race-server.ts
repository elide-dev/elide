import { WebSocketServer, WebSocket } from 'ws';
import { spawn } from 'child_process';
import { WebSocketRecorder, generateCacheKey, type CacheKeyParams } from '../services/websocket-recorder.js';
import { db } from '../db/index.js';
import { jobs, buildResults, buildCache } from '../db/schema.js';
import { eq } from 'drizzle-orm';
import { updateRaceStatistics } from '../routes/race-api.js';
import * as path from 'path';

interface RaceConnection {
  ws: WebSocket;
  jobId: string;
  buildType: 'elide' | 'standard';
  containerId: string;
  recorder: WebSocketRecorder;
  startTime: number;
}

const connections = new Map<string, RaceConnection>();
const RECORDINGS_DIR = path.join(process.cwd(), 'recordings');
const CLAUDE_VERSION = '2.0.35'; // TODO: Get from environment or config

/**
 * Setup WebSocket server for race streaming
 */
export function setupRaceWebSocketServer(wss: WebSocketServer): void {
  console.log('[RaceWS] Race WebSocket server initialized');

  wss.on('connection', (ws, req) => {
    const url = req.url;

    if (!url) return;

    // Match: /ws/race/:jobId/:buildType
    const raceMatch = url.match(/^\/ws\/race\/([^\/]+)\/(elide|standard)$/);

    if (raceMatch) {
      const [, jobId, buildType] = raceMatch;
      handleRaceConnection(ws, jobId, buildType as 'elide' | 'standard');
    }
  });
}

/**
 * Handle a new race WebSocket connection
 */
async function handleRaceConnection(
  ws: WebSocket,
  jobId: string,
  buildType: 'elide' | 'standard'
): Promise<void> {
  const connectionId = `${jobId}-${buildType}`;
  console.log(`[RaceWS] New race connection: ${connectionId}`);

  try {
    // Get repository URL from job
    const jobResults = await db.select().from(jobs).where(eq(jobs.id, jobId)).limit(1);

    if (jobResults.length === 0) {
      ws.send(JSON.stringify({ type: 'error', message: 'Job not found' }));
      ws.close();
      return;
    }

    const job = jobResults[0];

    // Find the running container for this job and build type
    const containerName = `race-${jobId}-${buildType}`;

    // Create recorder
    const dockerImage = buildType === 'elide' ? 'elide-builder:latest' : 'standard-builder:latest';
    const recorder = new WebSocketRecorder(jobId, buildType, {
      jobId,
      tool: buildType,
      repositoryUrl: job.repositoryUrl,
      claudeVersion: CLAUDE_VERSION,
      dockerImage
    });

    recorder.start();

    // Store connection with output buffer for bell detection
    const connection: RaceConnection = {
      ws,
      jobId,
      buildType,
      containerId: containerName,
      recorder,
      startTime: Date.now()
    };
    connections.set(connectionId, connection);

    // Track all output for bell detection
    let allOutput = '';

    // Attach to container and stream logs
    const dockerLogs = spawn('docker', [
      'logs',
      '-f',
      containerName
    ]);

    // Stream stdout
    dockerLogs.stdout.on('data', (data) => {
      const dataStr = data.toString();
      allOutput += dataStr;

      const message = {
        type: 'output',
        data: dataStr
      };

      // Record message
      recorder.record(message);

      // Send to client
      if (ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify(message));
      }
    });

    // Stream stderr
    dockerLogs.stderr.on('data', (data) => {
      const dataStr = data.toString();
      allOutput += dataStr;

      const message = {
        type: 'output',
        data: dataStr
      };

      // Record message
      recorder.record(message);

      // Send to client
      if (ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify(message));
      }
    });

    // Handle container exit
    dockerLogs.on('close', async (code) => {
      console.log(`[RaceWS] Container ${containerName} exited with code ${code}`);

      // Detect if bell was rung
      const bellPatterns = [
        /🔔.*BUILD COMPLETE.*🔔/i,
        /\[BUILD COMPLETE\]/i,
        /BUILD SUCCEEDED/i,
        /BUILD FAILED/i,
        /Total time:/i
      ];

      const bellRung = bellPatterns.some(pattern => pattern.test(allOutput));
      console.log(`[RaceWS] Bell detection for ${containerName}: ${bellRung}`);

      const duration = Math.round((Date.now() - connection.startTime) / 1000);

      // Only mark as success if bell was rung and exit code was 0
      const status = bellRung && code === 0 ? 'success' : 'failure';

      // Stop recording
      recorder.stop();

      // Save recording
      try {
        const cacheKeyParams: CacheKeyParams = {
          repositoryUrl: job.repositoryUrl,
          tool: buildType,
          claudeVersion: CLAUDE_VERSION,
          dockerImage
        };
        const cacheKey = generateCacheKey(cacheKeyParams);
        const recordingPath = await recorder.save(cacheKey, RECORDINGS_DIR);

        // Only save to database if recording was saved (build succeeded)
        if (recordingPath) {
          console.log(`[RaceWS] Saving successful build recording to database`);
          await db.insert(buildCache).values({
            cacheKey,
            jobId,
            repositoryUrl: job.repositoryUrl,
            buildTool: buildType,
            recordingPath,
            fileSizeBytes: recorder.getMessageCount() * 100, // Rough estimate
            durationMs: recorder.getDuration(),
            messageCount: recorder.getMessageCount(),
            claudeVersion: CLAUDE_VERSION,
            dockerImage,
            createdAt: new Date(),
            accessCount: 0
          });
        } else {
          console.log(`[RaceWS] Recording not saved - build did not complete successfully (bellRung: ${recorder.isBellRung()}, success: ${recorder.isBuildSuccessful()})`);
        }

      } catch (error) {
        console.error(`[RaceWS] Error saving recording:`, error);
      }

      // Save build result (only if bell was rung)
      if (bellRung) {
        await db.insert(buildResults).values({
          jobId,
          buildType,
          status,
          duration,
          bellRung: true,
          createdAt: new Date()
        });
      } else {
        // Bell not rung - mark as incomplete/failed
        console.log(`[RaceWS] Skipping build result - bell not rung for ${containerName}`);
        await db.insert(buildResults).values({
          jobId,
          buildType,
          status: 'failure',
          duration,
          bellRung: false,
          createdAt: new Date()
        });
      }

      // Send completion message
      const completeMessage = {
        type: 'complete',
        status,
        duration
      };

      if (ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify(completeMessage));
      }

      // Check if both builds are complete
      const allResults = await db
        .select()
        .from(buildResults)
        .where(eq(buildResults.jobId, jobId));

      if (allResults.length === 2) {
        // Update race statistics
        await updateRaceStatistics(jobId);

        // Update job status
        await db
          .update(jobs)
          .set({
            status: 'completed',
            completedAt: new Date()
          })
          .where(eq(jobs.id, jobId));

        // Clean up containers after race completes
        console.log(`[RaceWS] 🧹 Race complete, cleaning up containers for job ${jobId}`);
        const { spawn: spawnCleanup } = await import('child_process');

        // Stop and remove elide container
        const elideContainerName = `race-${jobId}-elide`;
        spawnCleanup('docker', ['stop', elideContainerName]).on('close', (code) => {
          console.log(`[RaceWS] Stopped ${elideContainerName}`);
          spawnCleanup('docker', ['rm', elideContainerName]).on('close', () => {
            console.log(`[RaceWS] Removed ${elideContainerName}`);
          });
        });

        // Stop and remove standard container
        const standardContainerName = `race-${jobId}-standard`;
        spawnCleanup('docker', ['stop', standardContainerName]).on('close', (code) => {
          console.log(`[RaceWS] Stopped ${standardContainerName}`);
          spawnCleanup('docker', ['rm', standardContainerName]).on('close', () => {
            console.log(`[RaceWS] Removed ${standardContainerName}`);
          });
        });
      }

      // Clean up
      connections.delete(connectionId);
      ws.close();
    });

    // Handle WebSocket close
    ws.on('close', () => {
      console.log(`[RaceWS] Client disconnected: ${connectionId}`);
      dockerLogs.kill();
      connections.delete(connectionId);
    });

    // Handle WebSocket errors
    ws.on('error', (error) => {
      console.error(`[RaceWS] WebSocket error for ${connectionId}:`, error);
      dockerLogs.kill();
      connections.delete(connectionId);
    });

  } catch (error) {
    console.error(`[RaceWS] Error handling race connection:`, error);
    ws.send(JSON.stringify({
      type: 'error',
      message: error instanceof Error ? error.message : 'Unknown error'
    }));
    ws.close();
  }
}
