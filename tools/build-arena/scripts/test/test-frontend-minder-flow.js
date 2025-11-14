#!/usr/bin/env node
/**
 * Test the frontend + minder flow
 * Simulates what the frontend does:
 * 1. Call /api/test/start-container-with-minder
 * 2. Immediately connect WebSocket in view-only mode
 * 3. Watch the output from minder's Claude session
 */

import WebSocket from 'ws';
import fetch from 'node-fetch';

const BACKEND_URL = 'http://localhost:3001';
const REPO_URL = 'https://github.com/google/gson.git';

console.log('\n🧪 Testing Frontend + Minder Flow\n');

let containerId = null;
let messageCount = 0;

async function startContainerWithMinder() {
  console.log('1️⃣  Starting container with minder...');

  const response = await fetch(`${BACKEND_URL}/api/test/start-container-with-minder`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      image: 'elide-builder:latest',
      repoUrl: REPO_URL
    })
  });

  if (!response.ok) {
    throw new Error(`Failed to start container: ${response.statusText}`);
  }

  const data = await response.json();
  containerId = data.containerId;

  console.log(`✅ Container started: ${containerId.substring(0, 12)}`);
  console.log(`✅ Minder started: PID ${data.minderPid}\n`);
  return data;
}

async function connectAsViewer(containerId) {
  return new Promise((resolve, reject) => {
    console.log('2️⃣  Connecting frontend WebSocket (view-only mode)...');

    // Connect in non-interactive mode (like the frontend does)
    const wsUrl = `ws://localhost:3001/ws/terminal/${containerId}?record=true&interactive=false`;
    const ws = new WebSocket(wsUrl);

    let startTime = Date.now();
    let gotClaudeOutput = false;

    ws.on('open', () => {
      console.log('✅ Frontend connected to terminal session\n');
      console.log('📺 === WATCHING MINDER\'S TERMINAL OUTPUT ===\n');
    });

    ws.on('message', (data) => {
      try {
        const message = JSON.parse(data);

        if (message.type === 'output') {
          // Write output to console
          process.stdout.write(message.data);
          messageCount++;

          const output = message.data.toString();

          // Detect when Claude Code starts
          if (!gotClaudeOutput && output.includes('claude')) {
            gotClaudeOutput = true;
            console.log('\n\n🎉 SUCCESS: Frontend is receiving minder\'s Claude Code output!\n');
          }

          // Stop after 30 seconds or when Claude finishes
          const elapsed = Date.now() - startTime;
          if (elapsed > 30000) {
            console.log('\n\n⏰ 30 seconds elapsed - stopping test');
            ws.close();
            resolve({ success: gotClaudeOutput, messageCount });
          }

          // Check for early completion
          if (output.includes('Welcome to Claude Code') || output.includes('BUILD COMPLETE')) {
            console.log('\n\n✅ Detected significant Claude activity - test successful!');
            setTimeout(() => {
              ws.close();
              resolve({ success: true, messageCount });
            }, 3000);
          }
        }
      } catch (err) {
        // Ignore parse errors
      }
    });

    ws.on('error', (error) => {
      console.error('\n\n❌ WebSocket error:', error.message);
      reject(error);
    });

    ws.on('close', () => {
      const duration = Math.round((Date.now() - startTime) / 1000);
      console.log('\n\n📺 === END OF TERMINAL OUTPUT ===');
      console.log(`\n📊 Statistics:`);
      console.log(`   - Duration: ${duration} seconds`);
      console.log(`   - Messages received: ${messageCount}`);
      console.log(`   - Got Claude output: ${gotClaudeOutput ? '✅ YES' : '❌ NO'}`);
      resolve({ success: gotClaudeOutput, messageCount });
    });

    // Timeout after 60 seconds
    setTimeout(() => {
      ws.close();
    }, 60000);
  });
}

async function stopContainer(containerId) {
  console.log('\n3️⃣  Stopping container and minder...');

  try {
    const response = await fetch(`${BACKEND_URL}/api/test/stop-container/${containerId}`, {
      method: 'POST'
    });

    if (!response.ok) {
      console.warn('⚠️  Failed to stop container via API');
    } else {
      const data = await response.json();
      console.log(`✅ Container stopped`);
      if (data.minderKilled) {
        console.log(`✅ Minder process killed\n`);
      }
    }
  } catch (err) {
    console.error('⚠️  Error stopping container:', err.message);
  }
}

// Main execution
async function main() {
  try {
    // Check backend is running
    const healthCheck = await fetch(`${BACKEND_URL}/health`);
    if (!healthCheck.ok) {
      throw new Error('Backend is not running. Start with: pnpm dev');
    }

    // Start container with minder
    const data = await startContainerWithMinder();
    containerId = data.containerId;

    // Connect frontend (immediately, to catch all output)
    const result = await connectAsViewer(containerId);

    // Stop container
    await stopContainer(containerId);

    // Report results
    if (result.success) {
      console.log('\n🎉 TEST PASSED: Frontend successfully received minder output!\n');
      process.exit(0);
    } else {
      console.log('\n❌ TEST FAILED: Frontend did not receive Claude Code output\n');
      console.log('   This means the WebSocket broadcast is not working as expected.');
      process.exit(1);
    }

  } catch (error) {
    console.error('\n❌ Error:', error.message);

    if (containerId) {
      await stopContainer(containerId);
    }

    process.exit(1);
  }
}

// Handle Ctrl+C gracefully
process.on('SIGINT', async () => {
  console.log('\n\n⚠️  Interrupted by user');
  if (containerId) {
    await stopContainer(containerId);
  }
  process.exit(0);
});

main();
