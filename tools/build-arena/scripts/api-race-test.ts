#!/usr/bin/env tsx

/**
 * API-based race test runner
 *
 * This script runs a race test purely via API calls and WebSocket monitoring,
 * without requiring a browser. Useful for CI/CD and headless testing.
 *
 * Usage:
 *   tsx scripts/api-race-test.ts [repository-url]
 *
 * Example:
 *   tsx scripts/api-race-test.ts https://github.com/google/gson
 */

import WebSocket from 'ws';

const API_BASE = process.env.API_BASE || 'http://localhost:3001';
const WS_BASE = process.env.WS_BASE || 'ws://localhost:3001';
const REPO_URL = process.argv[2] || 'https://github.com/google/gson';
const MAX_WAIT_TIME = 10 * 60 * 1000; // 10 minutes

interface RaceStatus {
  jobId: string;
  repositoryUrl: string;
  repositoryName: string;
  status: 'pending' | 'running' | 'completed' | 'failed';
  startedAt: string;
  elide: RunnerStatus;
  standard: RunnerStatus;
  stats?: RaceStats;
}

interface RunnerStatus {
  status: 'pending' | 'running' | 'success' | 'failed';
  duration: number;
  containerId?: string;
  hasRecording: boolean;
}

interface RaceStats {
  winner: 'elide' | 'standard' | 'tie';
  timeDifferenceSeconds: number;
  speedupPercentage: number;
}

interface BellDetectionResult {
  elide: boolean;
  standard: boolean;
}

async function startRace(repoUrl: string): Promise<string> {
  console.log(`\n🚀 Starting race for: ${repoUrl}\n`);

  const response = await fetch(`${API_BASE}/api/races/start`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ repositoryUrl: repoUrl })
  });

  if (!response.ok) {
    const error = await response.text();
    throw new Error(`Failed to start race: ${response.status} ${error}`);
  }

  const data = await response.json();
  console.log(`✅ Race started with job ID: ${data.jobId}`);
  console.log(`   Elide container: ${data.elide.containerId}`);
  console.log(`   Standard container: ${data.standard.containerId}\n`);

  return data.jobId;
}

async function getRaceStatus(jobId: string): Promise<RaceStatus> {
  const response = await fetch(`${API_BASE}/api/races/${jobId}`);

  if (!response.ok) {
    throw new Error(`Failed to get race status: ${response.status}`);
  }

  return response.json();
}

function monitorTerminalWebSocket(
  containerId: string,
  runnerName: string
): Promise<boolean> {
  return new Promise((resolve, reject) => {
    const wsUrl = `${WS_BASE}/ws/terminal/${containerId}?interactive=false`;
    const ws = new WebSocket(wsUrl);

    let outputBuffer = '';
    let bellRung = false;

    console.log(`📡 Connecting to ${runnerName} WebSocket...`);

    ws.on('open', () => {
      console.log(`✅ Connected to ${runnerName} terminal`);
    });

    ws.on('message', (data: Buffer) => {
      try {
        const message = JSON.parse(data.toString());

        if (message.type === 'output') {
          outputBuffer += message.data;

          // Check for bell patterns
          const bellPatterns = [
            /🔔/,
            /BUILD COMPLETE/i,
            /\[BUILD COMPLETE\]/i,
            /BUILD SUCCESS/i,
            /BUILD FAILURE/i,
            /BUILD SUCCEEDED/i,
            /BUILD FAILED/i,
            /Total time:/i,
            /BUILD SUCCESSFUL/i,
          ];

          if (!bellRung && bellPatterns.some(p => p.test(outputBuffer))) {
            bellRung = true;
            console.log(`🔔 ${runnerName}: Bell rung!`);
            ws.close();
            resolve(true);
          }
        } else if (message.type === 'complete') {
          console.log(`✅ ${runnerName}: Container completed`);
          ws.close();
          resolve(bellRung);
        }
      } catch (err) {
        // Ignore parse errors
      }
    });

    ws.on('error', (error) => {
      console.error(`❌ ${runnerName} WebSocket error:`, error.message);
      ws.close();
      reject(error);
    });

    ws.on('close', () => {
      console.log(`📴 ${runnerName} WebSocket closed`);
      if (!bellRung) {
        // Connection closed before bell was rung
        resolve(false);
      }
    });

    // Timeout after 10 minutes
    setTimeout(() => {
      if (!bellRung) {
        console.log(`⏱️  ${runnerName}: Timeout waiting for bell`);
        ws.close();
        resolve(false);
      }
    }, MAX_WAIT_TIME);
  });
}

async function waitForRaceCompletion(jobId: string): Promise<RaceStatus> {
  console.log('\n⏳ Waiting for race to complete...\n');

  const startTime = Date.now();
  let lastStatus: RaceStatus | null = null;

  while (Date.now() - startTime < MAX_WAIT_TIME) {
    const status = await getRaceStatus(jobId);

    // Log status changes
    if (!lastStatus ||
        status.elide.status !== lastStatus.elide.status ||
        status.standard.status !== lastStatus.standard.status) {
      console.log(`📊 Status Update:`);
      console.log(`   Elide: ${status.elide.status} (${status.elide.duration}s)`);
      console.log(`   Standard: ${status.standard.status} (${status.standard.duration}s)`);
    }

    lastStatus = status;

    if (status.status === 'completed') {
      return status;
    }

    // Wait before polling again
    await new Promise(resolve => setTimeout(resolve, 5000));
  }

  throw new Error('Race did not complete within timeout');
}

async function main() {
  const startTime = Date.now();

  try {
    console.log('╔══════════════════════════════════════════════════╗');
    console.log('║   Build Arena - API Race Test                   ║');
    console.log('╚══════════════════════════════════════════════════╝');

    // Start race
    const jobId = await startRace(REPO_URL);

    // Get initial status to get container IDs
    const initialStatus = await getRaceStatus(jobId);

    // Monitor terminals via WebSocket
    const bellDetection = await Promise.all([
      monitorTerminalWebSocket(initialStatus.elide.containerId!, 'ELIDE'),
      monitorTerminalWebSocket(initialStatus.standard.containerId!, 'STANDARD')
    ]);

    console.log('\n🔔 Bell Detection Results:');
    console.log(`   Elide: ${bellDetection[0] ? '✅' : '❌'}`);
    console.log(`   Standard: ${bellDetection[1] ? '✅' : '❌'}`);

    // Wait for race to officially complete
    const finalStatus = await waitForRaceCompletion(jobId);

    const totalTime = Math.round((Date.now() - startTime) / 1000);

    // Print results
    console.log('\n╔══════════════════════════════════════════════════╗');
    console.log('║   RACE RESULTS                                   ║');
    console.log('╚══════════════════════════════════════════════════╝\n');

    console.log(`Repository: ${finalStatus.repositoryName}`);
    console.log(`Race Status: ${finalStatus.status}`);
    console.log(`Total Test Time: ${totalTime}s\n`);

    console.log('Elide Runner:');
    console.log(`  Status: ${finalStatus.elide.status}`);
    console.log(`  Duration: ${finalStatus.elide.duration}s`);
    console.log(`  Recording: ${finalStatus.elide.hasRecording ? '✅' : '❌'}`);

    console.log('\nStandard Runner:');
    console.log(`  Status: ${finalStatus.standard.status}`);
    console.log(`  Duration: ${finalStatus.standard.duration}s`);
    console.log(`  Recording: ${finalStatus.standard.hasRecording ? '✅' : '❌'}`);

    if (finalStatus.stats) {
      console.log('\nStatistics:');
      console.log(`  🏆 Winner: ${finalStatus.stats.winner.toUpperCase()}`);
      console.log(`  ⏱️  Time Difference: ${finalStatus.stats.timeDifferenceSeconds}s`);
      console.log(`  ⚡ Speed Improvement: ${finalStatus.stats.speedupPercentage.toFixed(1)}%`);
    }

    console.log('\n');

    // Exit with success if race completed
    if (finalStatus.status === 'completed') {
      console.log('✅ Race test completed successfully!\n');
      process.exit(0);
    } else {
      console.log('❌ Race test failed!\n');
      process.exit(1);
    }

  } catch (error) {
    console.error('\n❌ Test failed with error:', error);
    process.exit(1);
  }
}

// Run if executed directly
if (require.main === module) {
  main();
}
