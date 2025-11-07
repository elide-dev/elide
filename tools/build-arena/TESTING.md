# Build Arena Testing Guide

Comprehensive guide to testing Build Arena with Playwright.

## Overview

Build Arena uses **Playwright** for end-to-end testing with support for multiple browsers and devices. Tests run in headless mode by default for CI/CD, but can also run in headed mode for debugging.

## Quick Start

```bash
# Run all tests (headless)
pnpm test

# Run tests with UI (interactive)
pnpm test:ui

# Run tests in headed mode (see browser)
pnpm test:headed
```

## Test Structure

```
tests/
├── homepage.spec.ts          # Homepage tests
├── job-submission.spec.ts    # Job submission flow
└── build-arena.spec.ts       # Build arena view tests
```

## Test Categories

### 1. Homepage Tests (`homepage.spec.ts`)

Tests the initial landing page:

- ✓ Page loads with correct title
- ✓ Repository form is displayed
- ✓ Example repositories are shown
- ✓ Form validation works
- ✓ Example repos populate form when clicked

**Run**:
```bash
pnpm exec playwright test homepage
```

### 2. Job Submission Tests (`job-submission.spec.ts`)

Tests the job creation flow:

- ✓ Form submission creates job
- ✓ Navigation to build arena after submission
- ✓ API error handling
- ✓ Loading states during submission

**Run**:
```bash
pnpm exec playwright test job-submission
```

### 3. Build Arena Tests (`build-arena.spec.ts`)

Tests the build comparison view:

- ✓ Job information displayed
- ✓ Dual terminals rendered
- ✓ Navigation back to form
- ✓ Metrics displayed when build completes

**Run**:
```bash
pnpm exec playwright test build-arena
```

## Running Tests

### Headless Mode (CI/CD)

```bash
# Run all tests
pnpm test

# Run specific test file
pnpm exec playwright test homepage.spec.ts

# Run tests matching pattern
pnpm exec playwright test --grep "should display"
```

### Headed Mode (Debugging)

```bash
# Run all tests in headed mode
pnpm test:headed

# Run specific browser
pnpm exec playwright test --project=chromium --headed

# Run single test
pnpm exec playwright test -g "should load homepage" --headed
```

### UI Mode (Interactive)

```bash
# Open Playwright UI
pnpm test:ui
```

Features:
- Visual test picker
- Time travel debugging
- Watch mode
- Network inspection
- Console logs

## Browser Testing

### Supported Browsers

Tests run on multiple browsers by default:

- **Desktop**: Chromium, Firefox, WebKit (Safari)
- **Mobile**: Mobile Chrome (Pixel 5), Mobile Safari (iPhone 12)

### Run Specific Browser

```bash
# Chromium only
pnpm exec playwright test --project=chromium

# Firefox only
pnpm exec playwright test --project=firefox

# WebKit only
pnpm exec playwright test --project=webkit

# Mobile Chrome
pnpm exec playwright test --project="Mobile Chrome"

# Mobile Safari
pnpm exec playwright test --project="Mobile Safari"
```

## Debugging Tests

### Debug Mode

```bash
# Open Playwright Inspector
pnpm exec playwright test --debug

# Debug specific test
pnpm exec playwright test -g "should submit" --debug
```

### Debugging Tools

**Playwright Inspector**:
- Step through test execution
- Inspect page state
- Edit selectors live
- Record new tests

**VS Code Extension**:
```bash
# Install Playwright extension
code --install-extension ms-playwright.playwright
```

Then use:
- Test explorer sidebar
- Run/debug tests inline
- Record tests
- Pick selectors

### Screenshots & Videos

Screenshots are automatically captured on test failure.

**Force screenshot**:
```typescript
await page.screenshot({ path: 'screenshot.png' })
```

**Enable video**:
```typescript
// playwright.config.ts
use: {
  video: 'on' // or 'retain-on-failure'
}
```

## Mocking & Fixtures

### API Mocking

Tests use route mocking to simulate backend responses:

```typescript
// Mock successful job creation
await page.route('**/api/jobs', async (route) => {
  await route.fulfill({
    status: 201,
    body: JSON.stringify({
      jobId: 'test-job-123',
      message: 'Job created successfully',
    }),
  })
})
```

### WebSocket Mocking

For testing WebSocket connections:

```typescript
// Mock WebSocket
await page.evaluate(() => {
  window.WebSocket = class MockWebSocket {
    send(data: string) { /* mock */ }
    close() { /* mock */ }
  }
})
```

## CI/CD Integration

### GitHub Actions

```yaml
name: Tests
on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: pnpm/action-setup@v2
      - run: pnpm install
      - run: pnpm exec playwright install --with-deps
      - run: pnpm test
      - uses: actions/upload-artifact@v3
        if: always()
        with:
          name: playwright-report
          path: playwright-report/
```

### GitLab CI

```yaml
test:
  image: mcr.microsoft.com/playwright:v1.48.2
  script:
    - npm install -g pnpm
    - pnpm install
    - pnpm test
  artifacts:
    when: always
    paths:
      - playwright-report/
```

## Configuration

### `playwright.config.ts`

Key configuration options:

```typescript
{
  testDir: './tests',           // Test directory
  fullyParallel: true,          // Run tests in parallel
  retries: process.env.CI ? 2 : 0,  // Retry on CI
  workers: process.env.CI ? 1 : undefined,  // CI workers

  use: {
    baseURL: 'http://localhost:5173',  // Frontend URL
    trace: 'on-first-retry',    // Trace on retry
    screenshot: 'only-on-failure',  // Screenshots
  },

  webServer: [
    {
      command: 'pnpm dev:backend',  // Start backend
      url: 'http://localhost:3001/health',
    },
    {
      command: 'pnpm dev:frontend',  // Start frontend
      url: 'http://localhost:5173',
    },
  ],
}
```

## Writing Tests

### Test Template

```typescript
import { test, expect } from '@playwright/test'

test.describe('Feature Name', () => {
  test.beforeEach(async ({ page }) => {
    // Setup before each test
    await page.goto('/')
  })

  test('should do something', async ({ page }) => {
    // Arrange
    const button = page.getByRole('button', { name: /submit/i })

    // Act
    await button.click()

    // Assert
    await expect(page.getByText('Success')).toBeVisible()
  })
})
```

### Best Practices

**1. Use Semantic Selectors**

```typescript
// ✅ Good - Use roles and accessible names
page.getByRole('button', { name: /submit/i })
page.getByLabel('Email')
page.getByText('Welcome')

// ❌ Bad - Fragile selectors
page.locator('.btn-primary')
page.locator('#email-input')
```

**2. Wait for Elements**

```typescript
// ✅ Good - Built-in waiting
await expect(page.getByText('Loading...')).not.toBeVisible()
await expect(page.getByText('Done')).toBeVisible()

// ❌ Bad - Hard-coded waits
await page.waitForTimeout(5000)
```

**3. Mock External Dependencies**

```typescript
// Mock API calls
await page.route('**/api/**', (route) => {
  route.fulfill({ body: '{"status": "ok"}' })
})

// Mock Docker (not running in tests)
await page.route('**/ws', (route) => {
  route.abort()
})
```

**4. Clean Up After Tests**

```typescript
test.afterEach(async ({ page }) => {
  // Clear local storage
  await page.evaluate(() => localStorage.clear())

  // Close connections
  await page.close()
})
```

## Test Scenarios

### Testing Terminal Output

```typescript
test('should display terminal output', async ({ page }) => {
  // Mock WebSocket message
  await page.evaluate(() => {
    const ws = new WebSocket('ws://localhost:3001/ws')
    ws.onopen = () => {
      ws.send(JSON.stringify({
        type: 'terminal_output',
        payload: {
          jobId: 'test-123',
          tool: 'elide',
          data: 'Building project...\n'
        }
      }))
    }
  })

  // Check terminal content
  const terminal = page.locator('.xterm')
  await expect(terminal).toContainText('Building project')
})
```

### Testing Bell Notifications

```typescript
test('should show bell notification', async ({ page }) => {
  // Trigger bell event
  await page.evaluate(() => {
    window.dispatchEvent(new CustomEvent('build:bell', {
      detail: { jobId: 'test-123', tool: 'elide' }
    }))
  })

  // Check notification
  await expect(page.getByText('🔔')).toBeVisible()
})
```

### Testing Keystroke Limits

```typescript
test('should enforce keystroke limit', async ({ page }) => {
  const terminal = page.locator('.xterm')

  // Type 1001 characters
  await terminal.click()
  await page.keyboard.type('a'.repeat(1001))

  // Check warning
  await expect(page.getByText(/keystroke limit reached/i)).toBeVisible()
})
```

## Performance Testing

### Lighthouse Integration

```typescript
import { test } from '@playwright/test'
import { playAudit } from 'playwright-lighthouse'

test('should pass Lighthouse audit', async ({ page, context }) => {
  await page.goto('/')

  await playAudit({
    page,
    thresholds: {
      performance: 90,
      accessibility: 95,
      'best-practices': 90,
      seo: 90,
    },
  })
})
```

### Network Monitoring

```typescript
test('should load page efficiently', async ({ page }) => {
  let requestCount = 0

  page.on('request', () => requestCount++)

  await page.goto('/')

  expect(requestCount).toBeLessThan(50)  // Reasonable limit
})
```

## Troubleshooting

### Common Issues

**1. Tests Timeout**

```bash
# Increase timeout
pnpm exec playwright test --timeout=60000
```

Or in config:
```typescript
test.setTimeout(60000)
```

**2. Browser Not Launching**

```bash
# Reinstall browsers
pnpm exec playwright install --force
```

**3. Port Already in Use**

```bash
# Kill processes on port
lsof -ti:5173 | xargs kill -9
lsof -ti:3001 | xargs kill -9
```

**4. Screenshot Artifacts Missing**

Check `playwright-report/` directory after failed tests.

### Debug Logs

```bash
# Enable debug logs
DEBUG=pw:api pnpm test
```

## Resources

- [Playwright Documentation](https://playwright.dev)
- [Best Practices](https://playwright.dev/docs/best-practices)
- [API Reference](https://playwright.dev/docs/api/class-playwright)
- [VS Code Extension](https://playwright.dev/docs/getting-started-vscode)

## Next Steps

1. Run setup: `./dev-pnpm.sh setup`
2. Start dev servers: `./dev-pnpm.sh dev`
3. Run tests: `pnpm test`
4. View report: `pnpm exec playwright show-report`

Happy testing! 🎭
