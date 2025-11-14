#!/usr/bin/env node
/**
 * Simple test script for Claude Code without git operations
 * Tests basic file operations to verify dangerouslySkipPermissions works
 */

import WebSocket from 'ws';
import fetch from 'node-fetch';

const BACKEND_URL = 'http://localhost:3001';

console.log('\n🎬 Claude Code Simple Test (No Git)\n');
console.log(`🔗 Backend: ${BACKEND_URL}\n`);

let startTime = Date.now();
let containerId = null;
let messageCount = 0;

async function startContainer() {
  console.log('🚀 Starting Docker container...');

  const response = await fetch(`${BACKEND_URL}/api/test/start-container`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ image: 'elide-builder:latest' })
  });

  if (!response.ok) {
    throw new Error(`Failed to start container: ${response.statusText}`);
  }

  const data = await response.json();
  containerId = data.containerId;

  console.log(`✅ Container started: ${containerId.substring(0, 12)}`);
  return containerId;
}

async function connectWebSocket(containerId) {
  return new Promise((resolve, reject) => {
    const wsUrl = `ws://localhost:3001/ws/terminal/${containerId}?record=true`;
    console.log(`🔌 Connecting to WebSocket (recording enabled)...`);

    const ws = new WebSocket(wsUrl);

    ws.on('open', () => {
      console.log('✅ WebSocket connected\n');
      console.log('📺 === TERMINAL OUTPUT (LIVE) ===\n');

      // Wait for bash to initialize
      setTimeout(() => {
        // Test with simple commands that don't require permissions
        const command = `claude "List files in /workspace, check Java version, and create a test file called hello.txt with the content 'Hello from Claude Code!'. Then read it back and verify the content."\n`;

        console.log(`🤖 Sending simple test command to Claude Code...\n`);
        ws.send(JSON.stringify({
          type: 'input',
          data: command
        }));
      }, 2000);
    });

    ws.on('message', (data) => {
      try {
        const message = JSON.parse(data);

        if (message.type === 'output') {
          // Write directly to stdout (preserves terminal colors/formatting)
          process.stdout.write(message.data);
          messageCount++;

          // Check for completion signals
          const output = message.data.toString();
          if (output.includes('Hello from Claude Code') ||
              output.includes('test file') ||
              output.includes('verified')) {
            console.log('\n\n✅ Test operations detected!');
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
      console.log(`   - Recording saved to: backend/recordings/test-${containerId.substring(0, 12)}.json.gz`);
      resolve();
    });
  });
}

async function stopContainer(containerId) {
  console.log('\n🛑 Stopping container...');

  try {
    await fetch(`${BACKEND_URL}/api/test/stop-container/${containerId}`, {
      method: 'POST'
    });
    console.log('✅ Container stopped\n');
  } catch (err) {
    console.error('⚠️  Error stopping container:', err.message);
  }
}

async function checkRecording() {
  console.log('🔍 Checking for recording file...');

  const fs = await import('fs');
  const path = await import('path');

  const recordingsDir = path.join(process.cwd(), 'backend', 'recordings');

  if (fs.existsSync(recordingsDir)) {
    const files = fs.readdirSync(recordingsDir);
    const gzFiles = files.filter(f => f.endsWith('.json.gz'));

    if (gzFiles.length > 0) {
      console.log(`✅ Found ${gzFiles.length} recording(s):`);
      gzFiles.forEach(file => {
        const stats = fs.statSync(path.join(recordingsDir, file));
        const sizeKB = Math.round(stats.size / 1024);
        console.log(`   - ${file} (${sizeKB} KB)`);
      });
    } else {
      console.log('⚠️  No recordings found yet');
    }
  } else {
    console.log('⚠️  Recordings directory does not exist');
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

    // Start container
    const containerId = await startContainer();

    // Connect and watch (with timeout)
    const timeoutPromise = new Promise((_, reject) =>
      setTimeout(() => reject(new Error('Test timeout after 2 minutes')), 120000)
    );

    await Promise.race([
      connectWebSocket(containerId),
      timeoutPromise
    ]);

    // Stop container
    await stopContainer(containerId);

    // Check for recording
    await checkRecording();

    console.log('\n✅ Test complete!\n');
    process.exit(0);

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
