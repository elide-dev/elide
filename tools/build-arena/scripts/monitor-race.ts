/**
 * Monitor race page console output and minder status
 */
import { chromium } from 'playwright';

const RACE_ID = 'dc18b1e7-f742-4c93-86f4-e378251a1cb7';
const RACE_URL = `http://localhost:3000/race/${RACE_ID}`;

async function monitorRace() {
  console.log(`[Monitor] Starting browser for race: ${RACE_ID}`);

  const browser = await chromium.launch({
    headless: true,
    args: ['--no-sandbox']
  });

  const context = await browser.newContext();
  const page = await context.newPage();

  // Monitor console messages
  page.on('console', (msg) => {
    const type = msg.type();
    const text = msg.text();

    // Only log relevant messages
    if (text.includes('WS') || text.includes('terminal') || text.includes('Minder') || text.includes('Race')) {
      console.log(`[Browser Console ${type}] ${text}`);
    }
  });

  // Monitor errors
  page.on('pageerror', (error) => {
    console.error(`[Page Error] ${error.message}`);
  });

  // Navigate to race page
  console.log(`[Monitor] Loading ${RACE_URL}`);
  await page.goto(RACE_URL, { waitUntil: 'networkidle' });

  console.log(`[Monitor] Page loaded, waiting for terminals to initialize...`);
  await page.waitForTimeout(2000);

  // Check minder status via API
  const checkMinderStatus = async () => {
    try {
      const response = await page.evaluate(async () => {
        const res = await fetch('/api/races/minders');
        return await res.json();
      });

      console.log(`\n[Minder Status] Active minders: ${response.count}`);
      response.minders.forEach((minder: any) => {
        console.log(`  - ${minder.buildType.toUpperCase()}`);
        console.log(`    Container: ${minder.containerId.substring(0, 12)}`);
        console.log(`    Connected: ${minder.connected}`);
        console.log(`    Uptime: ${minder.uptime}s`);
        console.log(`    Last Activity: ${minder.lastActivity}`);
        console.log(`    Approvals: ${minder.approvalCount}`);
        console.log(`    State: claudeStarted=${minder.state.claudeStarted}, apiKey=${minder.state.apiKeyHandled}, trust=${minder.state.workspaceTrustHandled}`);
        console.log(`    Last Output: ${minder.lastOutputSnippet.substring(0, 100)}...`);
        console.log('');
      });
    } catch (error) {
      console.error(`[Monitor] Error fetching minder status:`, error);
    }
  };

  // Check race status
  const checkRaceStatus = async () => {
    try {
      const response = await page.evaluate(async (raceId) => {
        const res = await fetch(`/api/races/${raceId}`);
        return await res.json();
      }, RACE_ID);

      console.log(`\n[Race Status]`);
      console.log(`  Job ID: ${response.jobId}`);
      console.log(`  Status: ${response.status}`);
      console.log(`  Elide: ${response.elide.status} (${response.elide.duration}s)`);
      console.log(`  Standard: ${response.standard.status} (${response.standard.duration}s)`);
      console.log(`  Winner: ${response.winner || 'TBD'}`);
      console.log('');
    } catch (error) {
      console.error(`[Monitor] Error fetching race status:`, error);
    }
  };

  // Initial status check
  await checkMinderStatus();
  await checkRaceStatus();

  // Monitor every 5 seconds
  console.log(`[Monitor] Monitoring race... (Ctrl+C to exit)`);
  const interval = setInterval(async () => {
    await checkMinderStatus();
    await checkRaceStatus();
  }, 5000);

  // Keep running until interrupted
  process.on('SIGINT', async () => {
    console.log('\n[Monitor] Stopping...');
    clearInterval(interval);
    await browser.close();
    process.exit(0);
  });
}

monitorRace().catch(console.error);
