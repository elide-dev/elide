#!/usr/bin/env node
/**
 * Test script with automatic approval of Claude Code permission requests
 * Watches WebSocket output for approval prompts and automatically responds
 */

import WebSocket from 'ws';
import fetch from 'node-fetch';

const BACKEND_URL = process.env.BACKEND_URL || 'http://localhost:3001';
const REPO_URL = process.env.REPO_URL || 'https://github.com/google/gson.git';
const EXISTING_CONTAINER_ID = process.env.CONTAINER_ID || null;

console.log('\n🎬 Claude Code Autonomous Build Test (Auto-Approve)\n');
console.log(`📦 Repository: ${REPO_URL}`);
console.log(`🔗 Backend: ${BACKEND_URL}`);
if (EXISTING_CONTAINER_ID) {
  console.log(`📦 Using existing container: ${EXISTING_CONTAINER_ID}`);
}
console.log('\n');

let startTime = Date.now();
let containerId = EXISTING_CONTAINER_ID;
let messageCount = 0;
let ws = null;

// Approval patterns - ONLY match actual tool permission prompts
// These should be specific to Claude Code's permission system, not general questions
const APPROVAL_PATTERNS = [
  // REMOVED overly broad patterns that match during thinking
  // Only keeping specific tool permission patterns that Claude Code actually shows
];

/**
 * Check if output contains an approval request
 */
function containsApprovalRequest(text) {
  return APPROVAL_PATTERNS.some(pattern => pattern.test(text));
}

/**
 * Automatically send approval response
 */
function sendApproval(reason) {
  if (!ws || ws.readyState !== WebSocket.OPEN) {
    console.log(`⚠️  Cannot send approval - WebSocket not ready`);
    return;
  }

  console.log(`\n🤖 AUTO-APPROVING: ${reason}`);

  // Send 'y' followed by Enter
  ws.send(JSON.stringify({
    type: 'input',
    data: 'y\n'
  }));

  console.log(`✅ Sent approval response\n`);
}

async function startContainer() {
  console.log('🚀 Starting Docker container...');

  const response = await fetch(`${BACKEND_URL}/api/test/start-container`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ image: 'elide-runner:latest' })
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

    ws = new WebSocket(wsUrl);
    let outputBuffer = '';
    let lastApprovalTime = 0;

    ws.on('open', () => {
      console.log('✅ WebSocket connected\n');
      console.log('📺 === TERMINAL OUTPUT (LIVE) ===\n');

      // Wait for bash to initialize AND for frontend to connect
      // Give frontend 5 seconds to connect after container starts
      console.log('⏳ Waiting 5 seconds for bash init and frontend connection...\n');
      setTimeout(() => {
        // Use interactive Claude Code (not --print)
        // Let Claude follow the instructions in CLAUDE.md without hard-coding the build tool
        const command = `claude "Clone ${REPO_URL}, analyze the project structure, then build it following the instructions in /workspace/CLAUDE.md. Time the build and report the results with a bell signal."\n`;

        console.log(`🤖 Sending command to Claude Code (interactive with auto-approve)...\n`);
        ws.send(JSON.stringify({
          type: 'input',
          data: command
        }));
      }, 5000);
    });

    // Auto-respond to prompts
    let themeHandled = false;
    let apiKeyHandled = false;
    let trustHandled = false;
    let gitCloneHandled = false;
    let claudeStarted = false;
    let lastErrorTime = 0;

    ws.on('message', (data) => {
      try {
        const message = JSON.parse(data);

        if (message.type === 'output') {
          const output = message.data.toString();

          // Write to stdout
          process.stdout.write(output);
          messageCount++;

          // Detect when Claude Code actually starts (not just our command echo)
          if (!claudeStarted && output.includes('Welcome to Claude Code')) {
            claudeStarted = true;
            console.log(`\n✅ Claude Code started!\n`);
          }

          // Detect when Claude is "thinking" (should NOT fill buffer during this time)
          const isThinking = output.match(/(Swooping|Pollinating|Calculating|Stewing|Thinking)…/i);

          // Add to buffer for approval detection (but only after Claude starts, setup is done, AND not thinking)
          if (claudeStarted && themeHandled && apiKeyHandled && !isThinking) {
            outputBuffer += output;

            // Keep buffer size reasonable (last 1000 chars)
            if (outputBuffer.length > 1000) {
              outputBuffer = outputBuffer.slice(-1000);
            }
          }

          // Clear buffer when Claude starts thinking to prevent stale matches
          if (isThinking) {
            outputBuffer = '';
          }

          // Handle API errors - auto-retry
          if (claudeStarted &&
              (output.includes('API Error: 500') ||
               output.includes('Internal server error') ||
               output.includes('"type":"api_error"'))) {

            const now = Date.now();
            // Debounce: only retry once per 5 seconds
            if (now - lastErrorTime > 5000) {
              console.log(`\n⚠️  API Error detected - sending retry command...\n`);

              setTimeout(() => {
                ws.send(JSON.stringify({
                  type: 'input',
                  data: 'let\'s try that again\n'
                }));
                lastErrorTime = now;
                console.log(`✅ Retry command sent\n`);
              }, 1000);
            }
          }

          // Handle theme selection prompt (only once)
          if (!themeHandled && output.includes('Choose the text style')) {
            console.log(`\n🎨 Auto-selecting default theme...\n`);
            setTimeout(() => {
              ws.send(JSON.stringify({
                type: 'input',
                data: '\r' // Send carriage return for TUI selection
              }));
              themeHandled = true;
              console.log(`✅ Theme selected, waiting for API key prompt\n`);
            }, 1000);
          }

          // Handle API key confirmation prompt (only once, after theme)
          if (themeHandled && !apiKeyHandled && output.includes('Do you want to use this API key')) {
            console.log(`\n🔑 Auto-confirming API key usage...\n`);
            setTimeout(() => {
              // Send UP arrow key to select "Yes" (currently on "No" which is option 2)
              ws.send(JSON.stringify({
                type: 'input',
                data: '\x1b[A' // UP arrow key
              }));

              // Wait a bit then send Enter to confirm
              setTimeout(() => {
                ws.send(JSON.stringify({
                  type: 'input',
                  data: '\r'
                }));
                apiKeyHandled = true;
                console.log(`✅ API key confirmed, watching for trust prompt\n`);
              }, 500);
            }, 1000);
          }

          // Handle workspace trust prompt (only once, after API key)
          // In isolated container, we always want to approve workspace access
          if (apiKeyHandled && !trustHandled &&
              (output.includes('trust') ||
               output.includes('Trust') ||
               output.includes('workspace') ||
               output.includes('Workspace'))) {
            console.log(`\n✅ Auto-trusting workspace folder...\n`);
            setTimeout(() => {
              // "Yes" option is already selected (option 1), just press Enter
              ws.send(JSON.stringify({
                type: 'input',
                data: '\r'
              }));
              trustHandled = true;
              // Enable generic handler immediately
              gitCloneHandled = true;
              console.log(`✅ Workspace trusted, generic approval handler enabled\n`);
            }, 1000);
          }

          // Handle git clone permission prompt (only once, after trust)
          // Look for the Bash tool permission menu with git clone command
          if (trustHandled && !gitCloneHandled &&
              (output.includes('git clone') && output.includes('Yes, and don\'t ask again'))) {
            console.log(`\n🔓 Auto-approving git clone permission...\n`);
            setTimeout(() => {
              // Send DOWN arrow to select "Yes, and don't ask again" (option 2)
              ws.send(JSON.stringify({
                type: 'input',
                data: '\x1b[B' // DOWN arrow key
              }));

              // Wait a bit then send Enter to confirm
              setTimeout(() => {
                ws.send(JSON.stringify({
                  type: 'input',
                  data: '\r'
                }));
                gitCloneHandled = true;
                console.log(`✅ Git clone permission granted\n`);
              }, 500);
            }, 1000);
          }

          // Handle ANY permission prompt (after initial setup)
          // In isolated container, we ALWAYS approve everything
          // Look for common permission prompt patterns
          const hasPermissionPrompt = output.includes('Do you want to proceed?') ||
                                       output.includes('Do you want to') ||
                                       output.includes('proceed?') ||
                                       output.includes('❯ 1. Yes') ||  // Option menu with Yes
                                       output.includes('Enter to confirm');

          if (gitCloneHandled && hasPermissionPrompt) {

            // Debounce: only handle if we haven't seen this prompt recently
            const now = Date.now();
            if (now - lastApprovalTime > 2000) { // 2 second debounce
              // Extract command from the output for logging
              const commandMatch = output.match(/Bash command\s+([^\n]+)/i) ||
                                   output.match(/Bash\(([^)]+)\)/);
              const command = commandMatch ? commandMatch[1].trim() : 'permission request';

              console.log(`\n🔓 Auto-approving: ${command}...\n`);

              // In isolated container, always approve with Enter
              // Option 1 is usually pre-selected
              setTimeout(() => {
                ws.send(JSON.stringify({
                  type: 'input',
                  data: '\r'  // Just press Enter on default selection
                }));
                lastApprovalTime = now;
                console.log(`✅ Permission granted for: ${command}\n`);
              }, 1000);
            }
          }

          // Check for approval requests (only after setup is complete)
          if (claudeStarted && themeHandled && apiKeyHandled) {
            const now = Date.now();
            if (now - lastApprovalTime > 2000) { // Wait at least 2 seconds between approvals
              if (containsApprovalRequest(outputBuffer)) {
                const match = APPROVAL_PATTERNS.find(p => p.test(outputBuffer));
                sendApproval(`Matched pattern: ${match}`);
                lastApprovalTime = now;
                outputBuffer = ''; // Clear buffer after approval
              }
            }
          }

          // Check for completion signals ("bell ringing")
          const completionPatterns = [
            // Explicit bell signals from CLAUDE.md instructions
            /🔔/,  // Bell emoji
            /BUILD COMPLETE/i,
            /\[BUILD COMPLETE\]/i,

            // Standard build tool completion signals
            /Build succeeded/i,
            /Build failed/i,
            /BUILD SUCCESS/i,
            /BUILD FAILURE/i,
            /BUILD SUCCEEDED/i,
            /BUILD FAILED/i,
            /\[SUCCESS\]/i,
            /\[FAILED\]/i,
            /\[FAILURE\]/i,

            // Maven completion patterns
            /Total time:/i,
            /BUILD SUCCESSFUL/i,

            // Gradle completion patterns
            /BUILD SUCCESSFUL/i,

            // General completion indicators
            /compilation.*complete/i,
            /tests.*passed/i,
            /tests.*failed/i,
            /✅.*complete/i,
            /❌.*error/i,
          ];

          const isComplete = completionPatterns.some(p => p.test(output));
          if (isComplete) {
            const match = completionPatterns.find(p => p.test(output));
            console.log(`\n\n🔔 BELL RUNG! Completion detected: ${match}\n`);
            setTimeout(() => {
              ws.close();
              resolve();
            }, 3000); // Wait a bit to capture final output
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

    // Start container (or use existing one)
    if (!containerId) {
      containerId = await startContainer();
    } else {
      console.log(`✅ Using existing container: ${containerId.substring(0, 12)}`);
    }

    // Connect and watch (with timeout)
    const timeoutPromise = new Promise((_, reject) =>
      setTimeout(() => reject(new Error('Test timeout after 5 minutes')), 300000)
    );

    await Promise.race([
      connectWebSocket(containerId),
      timeoutPromise
    ]);

    // Stop container (only if we created it)
    if (!EXISTING_CONTAINER_ID) {
      await stopContainer(containerId);
    } else {
      console.log('\n✅ Container managed externally - not stopping');
    }

    // Check for recording
    await checkRecording();

    console.log('\n✅ Test complete!\n');
    process.exit(0);

  } catch (error) {
    console.error('\n❌ Error:', error.message);

    if (containerId && !EXISTING_CONTAINER_ID) {
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
