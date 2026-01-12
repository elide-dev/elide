import { test, expect } from '@playwright/test';

test.describe('Results Table', () => {
  test.beforeEach(async ({ page }) => {
    // Mock the API response with test data
    await page.route('**/api/jobs/recent/results*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          jobs: [
            {
              id: 'test-job-1',
              repositoryUrl: 'https://github.com/spring-projects/spring-petclinic.git',
              repositoryName: 'spring-projects/spring-petclinic',
              createdAt: new Date().toISOString(),
              status: 'completed',
              elideResult: {
                status: 'success',
                duration: 45,
                output: 'Build successful',
                bellRung: true,
              },
              standardResult: {
                status: 'success',
                duration: 78,
                output: 'Build successful',
                bellRung: true,
              },
            },
            {
              id: 'test-job-2',
              repositoryUrl: 'https://github.com/apache/commons-lang.git',
              repositoryName: 'apache/commons-lang',
              createdAt: new Date().toISOString(),
              status: 'completed',
              elideResult: {
                status: 'success',
                duration: 32,
                output: 'Build successful',
                bellRung: true,
              },
              standardResult: {
                status: 'success',
                duration: 55,
                output: 'Build successful',
                bellRung: true,
              },
            },
          ],
        }),
      });
    });

    await page.goto('http://localhost:3000');
  });

  test('should display results table header', async ({ page }) => {
    await expect(page.getByRole('heading', { name: 'Recent Build Comparisons' })).toBeVisible();
  });

  test('should display table columns', async ({ page }) => {
    await expect(page.getByRole('columnheader', { name: 'Repository' })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Elide' })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Standard' })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Speedup' })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Date' })).toBeVisible();
  });

  test('should display mock job results', async ({ page }) => {
    // Wait for data to load
    await page.waitForSelector('table tbody tr');

    // Check first row data
    await expect(page.getByText('spring-projects/spring-petclinic')).toBeVisible();
    await expect(page.getByText('45.0s')).toBeVisible();
    await expect(page.getByText('78.0s')).toBeVisible();
  });

  test('should calculate and display speedup correctly', async ({ page }) => {
    // Wait for data to load
    await page.waitForSelector('table tbody tr');

    // First job: (78 - 45) / 78 * 100 = 42.3% faster
    const speedupBadge = page.locator('text=/42\\.\\d+% faster/').first();
    await expect(speedupBadge).toBeVisible();
    await expect(speedupBadge).toHaveClass(/bg-green-100/);
  });

  test('should show success status for completed builds', async ({ page }) => {
    await page.waitForSelector('table tbody tr');

    const successTexts = page.getByText('Success');
    const count = await successTexts.count();
    expect(count).toBeGreaterThan(0);
  });

  test('should display repository form below results table', async ({ page }) => {
    await expect(page.getByText('Submit a Java Repository')).toBeVisible();
    await expect(page.getByPlaceholder('Enter repository URL')).toBeVisible();
  });
});

test.describe('Results Table - Empty State', () => {
  test('should show empty state when no results', async ({ page }) => {
    // Mock empty response
    await page.route('**/api/jobs/recent/results*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ jobs: [] }),
      });
    });

    await page.goto('http://localhost:3000');

    await expect(page.getByText('No completed builds yet. Submit a repository to see results!')).toBeVisible();
  });
});

test.describe('Results Table - Error State', () => {
  test('should show error state when API fails', async ({ page }) => {
    // Mock error response
    await page.route('**/api/jobs/recent/results*', async (route) => {
      await route.abort('failed');
    });

    await page.goto('http://localhost:3000');

    await expect(page.getByText('Failed to load recent results')).toBeVisible();
  });
});
