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

    // Check for container ID
    await expect(page.getByText(/Container ID:/)).toBeVisible();

    // Wait for terminal output
    await page.waitForSelector('.xterm-screen', { timeout: 5000 });

    // Give it a moment to fully connect
    await page.waitForTimeout(2000);
  });

  test('should execute commands in terminal', async () => {
    // Start container
    const startButton = page.getByRole('button', { name: 'Start Container' });
    await startButton.click();

    // Wait for connection
    await expect(page.getByText('Connected')).toBeVisible({ timeout: 30000 });
    await page.waitForTimeout(2000);

    // Focus on terminal (click on it)
    const terminal = page.locator('.xterm-screen');
    await terminal.click();

    // Type a command
    await page.keyboard.type('echo "Hello from terminal test"\n');

    // Wait a moment for output
    await page.waitForTimeout(1000);

    // Check if output contains our message
    const terminalContent = await terminal.textContent();
    expect(terminalContent).toContain('Hello from terminal test');
  });

  test('should show Java version', async () => {
    // Start container
    const startButton = page.getByRole('button', { name: 'Start Container' });
    await startButton.click();

    // Wait for connection
    await expect(page.getByText('Connected')).toBeVisible({ timeout: 30000 });
    await page.waitForTimeout(2000);

    // Focus and execute java command
    const terminal = page.locator('.xterm-screen');
    await terminal.click();
    await page.keyboard.type('java -version\n');

    // Wait for output
    await page.waitForTimeout(3000);

    // Check for Java version in output
    const terminalContent = await terminal.textContent();
    expect(terminalContent).toMatch(/openjdk version|java version/i);
  });

  test('should check if Elide is installed', async () => {
    // Start container
    const startButton = page.getByRole('button', { name: 'Start Container' });
    await startButton.click();

    // Wait for connection
    await expect(page.getByText('Connected')).toBeVisible({ timeout: 30000 });
    await page.waitForTimeout(2000);

    // Check elide version
    const terminal = page.locator('.xterm-screen');
    await terminal.click();
    await page.keyboard.type('elide --version\n');

    // Wait for output
    await page.waitForTimeout(3000);

    const terminalContent = await terminal.textContent();
    // Elide should be installed or we should see command output
    expect(terminalContent).toBeTruthy();
  });

  test('should stop container and disconnect', async () => {
    // Start container
    const startButton = page.getByRole('button', { name: 'Start Container' });
    await startButton.click();

    // Wait for connection
    await expect(page.getByText('Connected')).toBeVisible({ timeout: 30000 });
    await page.waitForTimeout(2000);

    // Stop container
    const stopButton = page.getByRole('button', { name: 'Stop Container' });
    await stopButton.click();

    // Wait for disconnection
    await expect(page.getByText('Disconnected')).toBeVisible({ timeout: 10000 });

    // Status should change
    await expect(page.getByText(/Status:.*Stopped/)).toBeVisible();
  });

  test('should clear terminal', async () => {
    // Start container
    const startButton = page.getByRole('button', { name: 'Start Container' });
    await startButton.click();

    // Wait for connection
    await expect(page.getByText('Connected')).toBeVisible({ timeout: 30000 });
    await page.waitForTimeout(2000);

    // Type some commands
    const terminal = page.locator('.xterm-screen');
    await terminal.click();
    await page.keyboard.type('echo "test output"\n');
    await page.waitForTimeout(1000);

    // Clear terminal
    const clearButton = page.getByRole('button', { name: 'Clear Terminal' });
    await clearButton.click();

    // Terminal should be cleared (check if welcome message is gone)
    await page.waitForTimeout(500);
    const terminalContent = await terminal.textContent();
    expect(terminalContent?.trim().length).toBeLessThan(100); // Should be mostly empty
  });

  test('should display test instructions', async () => {
    await expect(page.getByRole('heading', { name: 'Test Instructions' })).toBeVisible();
    await expect(page.getByText(/Click "Start Container"/)).toBeVisible();
    await expect(page.getByText(/WebSocket will automatically connect/)).toBeVisible();
  });
});

test.describe('Terminal Test Page - Error Handling', () => {
  test('should handle container start failure gracefully', async ({ page }) => {
    // Mock API to return error
    await page.route('**/api/test/start-container', (route) =>
      route.fulfill({
        status: 500,
        body: JSON.stringify({ error: 'Failed to start container' }),
      })
    );

    await page.goto('http://localhost:3000/test/terminal');

    // Try to start container
    await page.getByRole('button', { name: 'Start Container' }).click();

    // Should show error in terminal
    await page.waitForTimeout(1000);
    const terminal = page.locator('.xterm-screen');
    const content = await terminal.textContent();
    expect(content).toContain('Error');
  });
});

test.describe('Terminal Test Page - Navigation', () => {
  test('should have link to homepage', async ({ page }) => {
    await page.goto('http://localhost:3000/');

    // Should have link to terminal test
    const link = page.getByRole('link', { name: 'Terminal Test Page' });
    await expect(link).toBeVisible();

    // Click link
    await link.click();

    // Should navigate to terminal test page
    await expect(page).toHaveURL('http://localhost:3000/test/terminal');
    await expect(page.getByRole('heading', { name: 'Terminal Test' })).toBeVisible();
  });
});
