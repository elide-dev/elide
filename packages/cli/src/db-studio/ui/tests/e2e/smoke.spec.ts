import { test, expect } from '@playwright/test';

/**
 * Smoke tests to verify the basic setup and functionality of DB Studio.
 *
 * Prerequisites:
 * - Elide backend server must be running on port 4984
 * - Backend should be connected to test.db (start with: elide db studio db/)
 */

test.describe('DB Studio Smoke Tests', () => {
  test('can navigate to database and view tables', async ({ page }) => {
    await page.goto('/');

    // Should see databases list
    // Note: The database might be named "test" without extension, or "test.db"
    // We'll check for either variant
    const testDbLocator = page.locator('text=/test/i').first();
    await expect(testDbLocator).toBeVisible({ timeout: 10000 });

    // Navigate to database by clicking on it
    await testDbLocator.click();

    // Should see tables in sidebar after navigation
    await expect(page.getByText('users')).toBeVisible({ timeout: 5000 });
    await expect(page.getByText('products')).toBeVisible();
    await expect(page.getByText('orders')).toBeVisible();
  });

  test('can view table data', async ({ page }) => {
    await page.goto('/');

    // Navigate to database
    const testDbLocator = page.locator('text=/test/i').first();
    await testDbLocator.click();

    // Click on users table
    await page.getByText('users').click();

    // Should see data grid with column headers
    await expect(page.getByRole('columnheader', { name: /id/i })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: /name/i })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: /email/i })).toBeVisible();

    // Should see some user data (we seeded 5 users)
    await expect(page.getByText('Alice Johnson')).toBeVisible();
    await expect(page.getByText('alice@example.com')).toBeVisible();
  });

  test('can navigate to query editor', async ({ page }) => {
    await page.goto('/');

    // Navigate to database
    const testDbLocator = page.locator('text=/test/i').first();
    await testDbLocator.click();

    // Look for "Query" or "Query Editor" link/button
    const queryLink = page.getByRole('link', { name: /query/i });
    await expect(queryLink).toBeVisible();

    // Click to navigate to query editor
    await queryLink.click();

    // Should see SQL editor (CodeMirror)
    // CodeMirror creates a contenteditable element
    await expect(page.locator('.cm-content')).toBeVisible();
  });

  test('can view products table data', async ({ page }) => {
    await page.goto('/');

    // Navigate to database
    const testDbLocator = page.locator('text=/test/i').first();
    await testDbLocator.click();

    // Click on products table
    await page.getByText('products').click();

    // Should see products column headers
    await expect(page.getByRole('columnheader', { name: /name/i })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: /price/i })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: /stock/i })).toBeVisible();

    // Should see seeded product data
    await expect(page.getByText('Laptop')).toBeVisible();
    await expect(page.getByText('999.99')).toBeVisible();
  });

  test('can view orders table with foreign key relationships', async ({ page }) => {
    await page.goto('/');

    // Navigate to database
    const testDbLocator = page.locator('text=/test/i').first();
    await testDbLocator.click();

    // Click on orders table
    await page.getByText('orders').click();

    // Should see orders column headers
    await expect(page.getByRole('columnheader', { name: /user_id/i })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: /product_id/i })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: /quantity/i })).toBeVisible();

    // Should see at least one order (we seeded 5 orders)
    const dataGrid = page.locator('[role="table"]');
    await expect(dataGrid).toBeVisible();
  });
});
