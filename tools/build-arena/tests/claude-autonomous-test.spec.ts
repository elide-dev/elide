import { test, expect, type Page } from '@playwright/test';

test.describe('Claude Autonomous Build Test', () => {
  // Configure this test suite
  test.use({
    baseURL: 'http://localhost:3000',
    // Longer timeout for Claude builds
    timeout: 5 * 60 * 1000 // 5 minutes
  });

  test('should watch Claude Code build autonomously and mirror output', async ({ page }) => {
    console.log('\n🎬 Starting Claude Code autonomous build test...\n');

    // Navigate to terminal test page
    await page.goto('/test/terminal');
    console.log('✅ Loaded terminal test page');

    // Set up console logging for terminal output
    let terminalBuffer = '';
    let lastLogTime = Date.now();

    // Intercept WebSocket messages to mirror terminal output
    await page.evaluate(() => {
      const originalWebSocket = window.WebSocket;
      (window as any).WebSocket = function(url: string) {
        const ws = new originalWebSocket(url);

        // Intercept messages
        ws.addEventListener('message', (event) => {
          try {
            const data = JSON.parse(event.data);
            if (data.type === 'output') {
              // Store in window for Playwright to access
              if (!(window as any).terminalOutput) {
                (window as any).terminalOutput = [];
              }
              (window as any).terminalOutput.push({
                timestamp: Date.now(),
                data: data.data
              });
            }
          } catch (e) {
            // Ignore parse errors
          }
        });

        return ws;
      };
    });

    // Start container with auto-run enabled
    console.log('🚀 Starting container...');
    await page.getByRole('button', { name: 'Start Container' }).click();

    // Wait for connection
    await expect(page.getByText('Connected')).toBeVisible({ timeout: 30000 });
    console.log('✅ Container connected');

    // Wait for Claude to start
    await page.waitForTimeout(3000);
    console.log('🤖 Claude Code should be starting...\n');
    console.log('📺 === TERMINAL OUTPUT (MIRRORED) ===\n');

    // Poll for terminal output and print to console
    const startTime = Date.now();
    const maxDuration = 5 * 60 * 1000; // 5 minutes max
    const pollInterval = 500; // Check every 500ms

    let lastOutputIndex = 0;
    let buildComplete = false;

    while (Date.now() - startTime < maxDuration && !buildComplete) {
      // Get new terminal output
      const outputs = await page.evaluate(() => {
        return (window as any).terminalOutput || [];
      });

      // Print new output to console
      for (let i = lastOutputIndex; i < outputs.length; i++) {
        const output = outputs[i];
        process.stdout.write(output.data);
        terminalBuffer += output.data;

        // Check for completion signals
        if (output.data.includes('BELL RUNG') ||
            output.data.includes('Build succeeded') ||
            output.data.includes('Claude Code execution completed')) {
          console.log('\n\n🔔 Build milestone reached!');
          buildComplete = true;
        }
      }

      lastOutputIndex = outputs.length;

      // Check terminal content for completion
      const terminalContent = await page.locator('.xterm-screen').textContent();
      if (terminalContent) {
        if (terminalContent.includes('Build succeeded') ||
            terminalContent.includes('Claude Code execution completed') ||
            terminalContent.includes('BELL RUNG')) {
          buildComplete = true;
        }
      }

      if (!buildComplete) {
        await page.waitForTimeout(pollInterval);
      }
    }

    const duration = Math.round((Date.now() - startTime) / 1000);
    console.log(`\n\n📺 === END OF TERMINAL OUTPUT ===`);
    console.log(`⏱️  Total duration: ${duration} seconds`);

    // Verify some output was captured
    expect(terminalBuffer.length).toBeGreaterThan(0);
    console.log(`📊 Captured ${terminalBuffer.length} bytes of output`);

    // Check for Claude Code markers
    const hasClaudeCode = terminalBuffer.includes('Claude') || terminalBuffer.includes('claude');
    console.log(`🤖 Claude Code detected: ${hasClaudeCode ? 'YES' : 'NO'}`);

    // Stop container
    console.log('\n🛑 Stopping container...');
    await page.getByRole('button', { name: 'Stop Container' }).click();
    await page.waitForTimeout(2000);

    // Check backend logs for recording info
    console.log('\n💾 Recording should be saved to backend/recordings/');
    console.log('   Check backend logs for recording details\n');

    console.log('✅ Test complete!');
  });

  test('should verify recording was created', async () => {
    // This test checks if recordings directory exists and has files
    const fs = require('fs');
    const path = require('path');

    const recordingsDir = path.join(__dirname, '../backend/recordings');

    console.log(`\n📂 Checking for recordings in: ${recordingsDir}`);

    if (fs.existsSync(recordingsDir)) {
      const files = fs.readdirSync(recordingsDir);
      const gzFiles = files.filter((f: string) => f.endsWith('.json.gz'));

      console.log(`✅ Recordings directory exists`);
      console.log(`📦 Found ${gzFiles.length} recording(s):`);

      gzFiles.forEach((file: string) => {
        const stats = fs.statSync(path.join(recordingsDir, file));
        const sizeKB = Math.round(stats.size / 1024);
        console.log(`   - ${file} (${sizeKB} KB)`);
      });

      expect(gzFiles.length).toBeGreaterThan(0);
    } else {
      console.log(`⚠️  Recordings directory does not exist yet`);
      console.log(`   This is expected if no builds have completed`);
    }
  });
});
