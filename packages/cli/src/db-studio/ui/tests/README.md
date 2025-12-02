# DB Studio E2E Tests

End-to-end tests for the Elide Database Studio UI using Playwright.

## Prerequisites

Before running tests, ensure you have:

1. **Elide backend server** running on port 4984
2. **Test database** (`test.db`) available and connected

## Setup

### One-Time Setup

1. **Install Playwright browsers** (if not already installed):
   ```bash
   npx playwright install
   ```

2. **Verify test database exists**:
   ```bash
   # Navigate to the db directory
   cd packages/cli/src/db-studio/db

   # Check if test.db exists
   ls -la test.db

   # If it doesn't exist, create it:
   sqlite3 test.db < create-test-db.sql
   ```

### Running Tests

Tests require the Elide backend server to be running before execution.

#### Terminal 1: Start Elide Backend
```bash
# From the db-studio directory
cd packages/cli/src/db-studio

# Start Elide pointing to the db/ directory containing test.db
elide db studio db/
```

The server should start on port 4984 and discover the test.db database.

#### Terminal 2: Run Playwright Tests
```bash
# From the UI directory
cd packages/cli/src/db-studio/ui

# Run all tests (headless mode)
npm run test:e2e

# Run tests with UI mode (recommended for debugging)
npm run test:e2e:ui

# Run tests in headed mode (see browser)
npm run test:e2e:headed

# Run tests in debug mode
npm run test:e2e:debug
```

## Test Configuration

Tests are configured to run across **3 browsers**:
- Chromium (Chrome/Edge)
- Firefox
- WebKit (Safari)

Configuration file: `playwright.config.ts`

## Test Structure

```
tests/
├── e2e/
│   └── smoke.spec.ts          # Basic smoke tests
└── README.md                   # This file
```

### Current Tests

**smoke.spec.ts** - Basic functionality tests:
- Navigate to database and view tables
- View table data (users, products, orders)
- Navigate to query editor
- Verify foreign key relationships

## Test Database

The test database (`test.db`) contains:

**Schema**:
- `users` table (id, name, email, created_at)
- `products` table (id, name, price, stock)
- `orders` table (id, user_id, product_id, quantity, created_at)
  - Foreign keys to users and products

**Seed Data**:
- 5 users
- 5 products
- 5 orders

## Resetting Test Data

If tests modify the database and you need to reset it:

```bash
cd packages/cli/src/db-studio/db

# Delete existing test.db
rm test.db

# Recreate from SQL script
sqlite3 test.db < create-test-db.sql
```

Then restart the Elide backend server to pick up the fresh database.

## Troubleshooting

### "Backend server not reachable"
- Ensure Elide is running: `elide db studio db/`
- Check server is on port 4984
- Verify test.db is in the db/ directory

### "test.db not found in database list"
- Restart Elide backend after creating test.db
- Ensure you're pointing to the correct directory: `elide db studio db/`
- Check server logs for database discovery messages

### Tests timeout
- Increase timeout in test files if needed
- Check Vite dev server is starting correctly (port 5173)
- Ensure backend API is responding (visit http://localhost:4984/health)

### Browser installation issues
```bash
# Reinstall all browsers
npx playwright install --with-deps

# Install specific browser
npx playwright install chromium
```

## Adding New Tests

1. Create a new `.spec.ts` file in `tests/e2e/`
2. Import test and expect from `@playwright/test`
3. Follow the existing smoke test patterns
4. Run tests to verify

Example:
```typescript
import { test, expect } from '@playwright/test';

test.describe('My Feature Tests', () => {
  test('should do something', async ({ page }) => {
    await page.goto('/');
    // ... test code
  });
});
```

## CI/CD

CI/CD integration with GitHub Actions will be added in a future phase.

## Resources

- [Playwright Documentation](https://playwright.dev/docs/intro)
- [Playwright Best Practices](https://playwright.dev/docs/best-practices)
- [Debugging Tests](https://playwright.dev/docs/debug)
