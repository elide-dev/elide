import { test, expect } from '@playwright/test';

test.describe('Race Closed-Loop Integration Test', () => {
  test.use({
    baseURL: 'http://localhost:3000',
    timeout: 10 * 60 * 1000 // 10 minutes for full race
  });

  test('should complete a full race with bell detection', async ({ page }) => {
    console.log('\n🏁 Starting Race Closed-Loop Test...\n');

    // Navigate to race page
    await page.goto('/race');
    console.log('✅ Loaded race page');

    // Enter repository URL
    const repoUrl = 'https://github.com/google/gson';
    await page.fill('input[placeholder*="github.com"]', repoUrl);
    console.log(`📦 Entered repository: ${repoUrl}`);

    // Start race
    await page.getByRole('button', { name: /start race|race/i }).click();
    console.log('🚀 Started race...');

    // Wait for race to initialize (should redirect to race/:jobId)
    await page.waitForURL(/\/race\/[a-f0-9-]+/, { timeout: 30000 });
    const jobId = page.url().match(/\/race\/([a-f0-9-]+)/)?.[1];
    console.log(`✅ Race initialized with job ID: ${jobId}`);

    // Set up WebSocket message interceptor for both terminals
    const terminalMessages: { [key: string]: any[] } = {
      elide: [],
      standard: []
    };

    await page.evaluate(() => {
      const originalWebSocket = window.WebSocket;
      (window as any).WebSocket = function(url: string) {
        const ws = new originalWebSocket(url);

        // Detect which terminal this is for
        const isElide = url.includes('elide');
        const isStandard = url.includes('standard');
        const terminalType = isElide ? 'elide' : isStandard ? 'standard' : 'unknown';

        ws.addEventListener('message', (event) => {
          try {
            const data = JSON.parse(event.data);
            if (!(window as any).terminalMessages) {
              (window as any).terminalMessages = { elide: [], standard: [] };
            }
            if (terminalType !== 'unknown') {
              (window as any).terminalMessages[terminalType].push({
                timestamp: Date.now(),
                type: data.type,
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

    // Check for race status indicator
    await expect(page.locator('text=/Live Race|Race Status/i')).toBeVisible({ timeout: 10000 });
    console.log('✅ Race status visible');

    // Monitor race progress
    const startTime = Date.now();
    const maxDuration = 10 * 60 * 1000; // 10 minutes
    const pollInterval = 2000; // Check every 2 seconds

    let raceComplete = false;
    let elideComplete = false;
    let standardComplete = false;
    let lastElideIndex = 0;
    let lastStandardIndex = 0;

    console.log('\n📺 === MONITORING RACE OUTPUT ===\n');

    while (Date.now() - startTime < maxDuration && !raceComplete) {
      await page.waitForTimeout(pollInterval);

      // Get new terminal messages
      const messages = await page.evaluate(() => {
        return (window as any).terminalMessages || { elide: [], standard: [] };
      });

      // Process new Elide messages
      for (let i = lastElideIndex; i < messages.elide.length; i++) {
        const msg = messages.elide[i];
        if (msg.type === 'output' && msg.data) {
          // Check for completion signals
          if (checkForBellSignal(msg.data)) {
            console.log('\n🔔 ELIDE: Bell rung!');
            elideComplete = true;
          }
        }
      }
      lastElideIndex = messages.elide.length;

      // Process new Standard messages
      for (let i = lastStandardIndex; i < messages.standard.length; i++) {
        const msg = messages.standard[i];
        if (msg.type === 'output' && msg.data) {
          // Check for completion signals
          if (checkForBellSignal(msg.data)) {
            console.log('\n🔔 STANDARD: Bell rung!');
            standardComplete = true;
          }
        }
      }
      lastStandardIndex = messages.standard.length;

      // Check if both are complete
      if (elideComplete && standardComplete) {
        console.log('\n✅ Both runners completed!');
        raceComplete = true;
      }

      // Log progress
      const elapsed = Math.round((Date.now() - startTime) / 1000);
      if (elapsed % 10 === 0) {
        console.log(`⏱️  ${elapsed}s elapsed - Elide: ${elideComplete ? '✅' : '⏳'}, Standard: ${standardComplete ? '✅' : '⏳'}`);
      }
    }

    if (!raceComplete) {
      console.log('\n⚠️  Race did not complete within timeout');
    }

    // Verify race results via API
    const response = await page.request.get(`/api/races/${jobId}`);
    expect(response.ok()).toBeTruthy();
    const raceData = await response.json();

    console.log('\n📊 === RACE RESULTS ===');
    console.log(`Status: ${raceData.status}`);
    console.log(`Repository: ${raceData.repositoryName}`);
    console.log(`\nElide Runner:`);
    console.log(`  Status: ${raceData.elide.status}`);
    console.log(`  Duration: ${raceData.elide.duration}s`);
    console.log(`  Container: ${raceData.elide.containerId}`);
    console.log(`  Recording: ${raceData.elide.hasRecording ? '✅' : '❌'}`);
    console.log(`\nStandard Runner:`);
    console.log(`  Status: ${raceData.standard.status}`);
    console.log(`  Duration: ${raceData.standard.duration}s`);
    console.log(`  Container: ${raceData.standard.containerId}`);
    console.log(`  Recording: ${raceData.standard.hasRecording ? '✅' : '❌'}`);

    if (raceData.stats) {
      console.log(`\nStatistics:`);
      console.log(`  Winner: ${raceData.stats.winner}`);
      console.log(`  Time difference: ${raceData.stats.timeDifferenceSeconds}s`);
      console.log(`  Speed improvement: ${raceData.stats.speedupPercentage}%`);
    }

    // Assertions
    expect(raceData.status).toBe('completed');
    expect(raceData.elide.hasRecording).toBe(true);
    expect(raceData.standard.hasRecording).toBe(true);

    // At least one should have succeeded
    const hasSuccess = raceData.elide.status === 'success' ||
                      raceData.standard.status === 'success';
    expect(hasSuccess).toBeTruthy();

    console.log('\n✅ Race test completed successfully!\n');
  });

  test('should connect to race WebSocket and receive status updates', async ({ page }) => {
    console.log('\n📡 Testing Race WebSocket Connection...\n');

    // Start a race first
    await page.goto('/race');
    await page.fill('input[placeholder*="github.com"]', 'https://github.com/google/gson');
    await page.getByRole('button', { name: /start race/i }).click();
    await page.waitForURL(/\/race\/[a-f0-9-]+/, { timeout: 30000 });

    const jobId = page.url().match(/\/race\/([a-f0-9-]+)/)?.[1];
    console.log(`Race job ID: ${jobId}`);

    // Connect to race WebSocket via backend
    const wsUrl = `ws://localhost:3001/ws/race/${jobId}/elide`;
    console.log(`Connecting to: ${wsUrl}`);

    // Use page.evaluate to test WebSocket in browser context
    const wsConnected = await page.evaluate((url) => {
      return new Promise<boolean>((resolve) => {
        const ws = new WebSocket(url);

        ws.onopen = () => {
          console.log('WebSocket connected');
          resolve(true);
        };

        ws.onerror = (error) => {
          console.error('WebSocket error:', error);
          resolve(false);
        };

        // Store for cleanup
        (window as any).testWs = ws;

        // Auto-close after 5 seconds
        setTimeout(() => {
          ws.close();
        }, 5000);
      });
    }, wsUrl);

    expect(wsConnected).toBe(true);
    console.log('✅ Race WebSocket connection successful\n');
  });
});

/**
 * Check if output contains bell/completion signals
 */
function checkForBellSignal(text: string): boolean {
  const patterns = [
    /🔔/,
    /BUILD COMPLETE/i,
    /\[BUILD COMPLETE\]/i,
    /Build succeeded/i,
    /Build failed/i,
    /BUILD SUCCESS/i,
    /BUILD FAILURE/i,
    /BUILD SUCCEEDED/i,
    /BUILD FAILED/i,
    /\[SUCCESS\]/i,
    /\[FAILED\]/i,
    /Total time:/i,
    /BUILD SUCCESSFUL/i,
  ];

  return patterns.some(p => p.test(text));
}
