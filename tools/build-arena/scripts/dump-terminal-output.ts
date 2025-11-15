#!/usr/bin/env tsx
/**
 * Dump terminal output from a running race container
 * Usage: pnpm exec tsx scripts/dump-terminal-output.ts <containerId>
 */

import WebSocket from 'ws';

const containerId = process.argv[2];

if (!containerId) {
  console.error('Usage: tsx scripts/dump-terminal-output.ts <containerId>');
  process.exit(1);
}

const wsUrl = `ws://localhost:3001/ws/terminal/${containerId}?interactive=false`;

console.log(`Connecting to ${wsUrl}...`);

const ws = new WebSocket(wsUrl);

let outputBuffer = '';
let messageCount = 0;

ws.on('open', () => {
  console.log('Connected! Listening for output...\n');
  console.log('='.repeat(80));
});

ws.on('message', (data: Buffer) => {
  try {
    const message = JSON.parse(data.toString());

    if (message.type === 'output') {
      outputBuffer += message.data.toString();
      messageCount++;

      // Print the raw output
      process.stdout.write(message.data.toString());
    }
  } catch (err) {
    // Ignore parse errors
  }
});

ws.on('error', (error) => {
  console.error('\nWebSocket error:', error.message);
  process.exit(1);
});

ws.on('close', () => {
  console.log('\n' + '='.repeat(80));
  console.log(`\nConnection closed. Received ${messageCount} messages.`);
  console.log(`Total output length: ${outputBuffer.length} characters`);
  process.exit(0);
});

// Close after 10 seconds
setTimeout(() => {
  console.log('\n' + '='.repeat(80));
  console.log(`\nTimeout after 10 seconds. Received ${messageCount} messages.`);
  console.log(`Total output length: ${outputBuffer.length} characters`);
  ws.close();
  process.exit(0);
}, 10000);
