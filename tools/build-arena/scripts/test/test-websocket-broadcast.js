#!/usr/bin/env node
/**
 * Test WebSocket broadcasting - verify multiple clients receive same output
 */

import WebSocket from 'ws';
import fetch from 'node-fetch';

const BACKEND_URL = 'http://localhost:3001';

console.log('\n🧪 Testing WebSocket Broadcast\n');

let containerId = null;

async function startContainer() {
  console.log('1️⃣  Starting Docker container...');

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

  console.log(`✅ Container started: ${containerId.substring(0, 12)}\n`);
  return containerId;
}

async function testBroadcast(containerId) {
  return new Promise((resolve, reject) => {
    console.log('2️⃣  Connecting ONE client first, waiting for init, then connecting second client...\n');

    let client1Messages = [];
    let client2Messages = [];
    let commandSent = false;
    let client1Ready = false;
    let client2Ready = false;

    // Client 1: Interactive (will send commands)
    const ws1 = new WebSocket(`ws://localhost:3001/ws/terminal/${containerId}?interactive=true`);

    // We'll connect Client 2 AFTER Client 1 finishes init
    let ws2 = null;

    ws1.on('open', () => {
      console.log('✅ Client 1 (interactive) connected');
      console.log('⏳ Waiting for bash prompt before connecting Client 2...');
    });

    ws1.on('message', (data) => {
      const message = JSON.parse(data);
      if (message.type === 'output') {
        client1Messages.push(message.data);
        process.stdout.write(`[Client 1] ${message.data}`);

        // Wait for bash prompt on Client 1, then connect Client 2
        // Look for either "workspace$" or the PS1 prompt format
        if (!client1Ready && (message.data.includes('workspace$') || message.data.includes('/workspace'))) {
          // Make sure we got a prompt marker (check for $ at end or bracketed prompt)
          if (message.data.includes('$') || message.data.includes('workspace')) {
            client1Ready = true;
            console.log('\n\n✅ Client 1 ready (bash prompt received)');
            console.log('\n3️⃣  Now connecting Client 2 (non-interactive)...\n');

          // Now connect Client 2 to the SAME session
          ws2 = new WebSocket(`ws://localhost:3001/ws/terminal/${containerId}?interactive=false`);

          ws2.on('open', () => {
            console.log('✅ Client 2 (non-interactive) connected to existing session');
            console.log('⏳ Waiting a moment for Client 2 to join broadcast list...\n');

            // Wait a bit, then send command
            setTimeout(() => {
              console.log('4️⃣  Client 1 sending command: echo "BROADCAST TEST"\n');
              ws1.send(JSON.stringify({
                type: 'input',
                data: 'echo "BROADCAST TEST"\n'
              }));
              commandSent = true;
            }, 1000);
          });

          ws2.on('message', (data) => {
            const message = JSON.parse(data);
            if (message.type === 'output') {
              client2Messages.push(message.data);
              process.stdout.write(`[Client 2] ${message.data}`);

              // Check if Client 2 got the broadcast
              if (message.data.includes('BROADCAST TEST')) {
                console.log('\n\n✅ Client 2 received broadcast!\n');

                // Both clients received it - test passed!
                setTimeout(() => {
                  ws1.close();
                  ws2.close();

                  console.log('5️⃣  Verifying both clients received same output...\n');

                  const client1Output = client1Messages.join('');
                  const client2Output = client2Messages.join('');

                  console.log(`Client 1 received: ${client1Messages.length} messages`);
                  console.log(`Client 2 received: ${client2Messages.length} messages`);

                  if (client1Output.includes('BROADCAST TEST') && client2Output.includes('BROADCAST TEST')) {
                    console.log('\n✅ SUCCESS: Both clients received the broadcast!');
                    resolve(true);
                  } else {
                    console.log('\n❌ FAIL: Not all clients received the broadcast');
                    console.log(`Client 1 saw "BROADCAST TEST": ${client1Output.includes('BROADCAST TEST')}`);
                    console.log(`Client 2 saw "BROADCAST TEST": ${client2Output.includes('BROADCAST TEST')}`);
                    resolve(false);
                  }
                }, 1000);
              }
            }
          });

          ws2.on('error', (error) => {
            console.error('❌ Client 2 error:', error.message);
            reject(error);
          });
          }
        }

        // Check if Client 1 got the broadcast
        if (message.data.includes('BROADCAST TEST')) {
          console.log('\n✅ Client 1 received broadcast!\n');
        }
      }
    });

    ws1.on('error', (error) => {
      console.error('❌ Client 1 error:', error.message);
      reject(error);
    });

    // Timeout after 15 seconds
    setTimeout(() => {
      if (ws1) ws1.close();
      if (ws2) ws2.close();
      reject(new Error('Test timeout'));
    }, 15000);
  });
}

async function stopContainer(containerId) {
  console.log('\n6️⃣  Stopping container...');

  try {
    await fetch(`${BACKEND_URL}/api/test/stop-container/${containerId}`, {
      method: 'POST'
    });
    console.log('✅ Container stopped\n');
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

    // Run test
    containerId = await startContainer();
    const success = await testBroadcast(containerId);
    await stopContainer(containerId);

    if (success) {
      console.log('\n🎉 All tests passed!\n');
      process.exit(0);
    } else {
      console.log('\n❌ Test failed!\n');
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
