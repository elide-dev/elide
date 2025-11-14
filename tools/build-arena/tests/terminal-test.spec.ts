import { test, expect, type Page } from '@playwright/test';

test.describe('Terminal Test Page', () => {
  let page: Page;

  test.beforeEach(async ({ browser }) => {
    page = await browser.newPage();
    await page.goto('http://localhost:3000/test/terminal');
  });

  test.afterEach(async () => {
    await page.close();
  });

  test('should load terminal test page', async () => {
    await expect(page.getByRole('heading', { name: 'Terminal Test' })).toBeVisible();
    await expect(page.getByText('Direct terminal connection to Docker container')).toBeVisible();
  });

  test('should have control buttons', async () => {
    await expect(page.getByRole('button', { name: 'Start Container' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Stop Container' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Clear Terminal' })).toBeVisible();
  });

  test('should show disconnected status initially', async () => {
    await expect(page.getByText('Disconnected')).toBeVisible();
  });

  test('should start container and connect terminal', async () => {
    // Click start button
    const startButton = page.getByRole('button', { name: 'Start Container' });
    await startButton.click();

    // Wait for container to start (this may take a few seconds)
    await expect(page.getByText('Connected')).toBeVisible({ timeout: 30000 });

    // Check for container ID in the DOM (it appears above the terminal)
    await expect(page.locator('text=Container ID:')).toBeVisible({ timeout: 5000 });

    // Verify terminal shows connection messages
    const terminal = page.locator('.xterm-rows');
    await page.waitForTimeout(2000);
    const terminalContent = await terminal.innerText();
    expect(terminalContent).toContain('Container started');
  });

  test('should show view-only terminal messages', async () => {
    // Start container
    const startButton = page.getByRole('button', { name: 'Start Container' });
    await startButton.click();

    // Wait for connection
    await expect(page.getByText('Connected')).toBeVisible({ timeout: 30000 });
    await page.waitForTimeout(2000);

    // Check terminal shows view-only messages
    const terminal = page.locator('.xterm-rows');
    const terminalContent = await terminal.innerText();

    // Verify view-only mode messages
    expect(terminalContent).toContain('View-Only Mode');
    expect(terminalContent).toContain('Container started');
    expect(terminalContent).toContain('WebSocket connected');
  });

  test('should display container configuration options', async () => {
    // Verify repository URL input exists
    await expect(page.locator('input#repoUrl')).toBeVisible();

    // Verify runner type selector exists
    await expect(page.locator('select#runnerType')).toBeVisible();

    // Verify auto-run checkbox exists
    await expect(page.locator('input#autoRun')).toBeVisible();
  });

  test('should show timer when container is running', async () => {
    // Start container
    const startButton = page.getByRole('button', { name: 'Start Container' });
    await startButton.click();

    // Wait for connection
    await expect(page.getByText('Connected')).toBeVisible({ timeout: 30000 });

    // Timer should be visible (be specific to avoid matching footer timestamp)
    await expect(page.locator('.text-blue-400.font-bold')).toBeVisible({ timeout: 5000 });
    await expect(page.locator('.text-blue-400.font-bold')).toContainText(/\d{2}:\d{2}/);
  });

  test('should stop container and disconnect', async () => {
    // Start container
    const startButton = page.getByRole('button', { name: 'Start Container' });
    await startButton.click();

    // Wait for connection
    await expect(page.getByText('Connected')).toBeVisible({ timeout: 30000 });
    await page.waitForTimeout(2000);

    // Stop container button should be enabled
    const stopButton = page.getByRole('button', { name: 'Stop Container' });
    await expect(stopButton).toBeEnabled({ timeout: 5000 });
    await stopButton.click();

    // Wait for disconnection - check for "Disconnected" status indicator
    await expect(page.getByText('Disconnected')).toBeVisible({ timeout: 10000 });

    // Verify terminal shows stop message
    await page.waitForTimeout(1000);
    const terminal = page.locator('.xterm-rows');
    const terminalContent = await terminal.innerText();
    expect(terminalContent).toContain('Container stopped');
  });

  test('should clear terminal', async () => {
    // Start container
    const startButton = page.getByRole('button', { name: 'Start Container' });
    await startButton.click();

    // Wait for connection
    await expect(page.getByText('Connected')).toBeVisible({ timeout: 30000 });
    await page.waitForTimeout(2000);

    // Type some commands
    const terminal = page.locator('.xterm-rows');
    await terminal.click();
    await page.keyboard.type('echo "test output"\n');
    await page.waitForTimeout(1000);

    // Clear terminal
    const clearButton = page.getByRole('button', { name: 'Clear Terminal' });
    await clearButton.click();

    // Terminal should be cleared (check if welcome message is gone)
    await page.waitForTimeout(500);
    const terminalContent = await terminal.innerText();
    expect(terminalContent?.trim().length).toBeLessThan(100); // Should be mostly empty
  });

  test('should display view-only terminal instructions', async () => {
    await expect(page.getByRole('heading', { name: '📺 View-Only Terminal' })).toBeVisible();
    await expect(page.getByText(/Enter a repository URL/)).toBeVisible();
    await expect(page.getByText(/Watch as Claude Code automatically/)).toBeVisible();
    await expect(page.getByText(/No keyboard input needed/)).toBeVisible();
  });
});

test.describe('Terminal Test Page - Error Handling', () => {
  test('should handle container start failure gracefully', async ({ page }) => {
    // Mock API to return error
    await page.route('**/api/test/start-container-with-minder', (route) =>
      route.fulfill({
        status: 500,
        body: JSON.stringify({ error: 'Failed to start container' }),
      })
    );

    await page.goto('http://localhost:3000/test/terminal');

    // Try to start container
    await page.getByRole('button', { name: 'Start Container' }).click();

    // Should show error in terminal
    await page.waitForTimeout(2000);
    const terminal = page.locator('.xterm-rows');
    const content = await terminal.innerText();
    expect(content).toContain('Error');

    // Status should show Failed (be specific - look for the status field, not terminal text)
    await expect(page.locator('text=/Status:.*Failed/')).toBeVisible({ timeout: 5000 });
  });
});

test.describe('Terminal Test Page - Navigation', () => {
  test('should have link to homepage', async ({ page }) => {
    await page.goto('http://localhost:3000/');

    // Should have link to terminal test (look for the card link containing "Terminal Test")
    const link = page.getByRole('link').filter({ has: page.getByRole('heading', { name: 'Terminal Test' }) });
    await expect(link).toBeVisible();

    // Click link
    await link.click();

    // Should navigate to terminal test page
    await expect(page).toHaveURL('http://localhost:3000/test/terminal');
    await expect(page.getByRole('heading', { name: 'Terminal Test' })).toBeVisible();
  });
});
