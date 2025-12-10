import { test, expect } from '@playwright/test'

test.describe('Job Submission Flow', () => {
  test('should submit a job and navigate to build arena', async ({ page }) => {
    await page.goto('/')

    // Fill in repository URL
    await page.getByLabel('Repository URL').fill('https://github.com/spring-projects/spring-petclinic.git')

    // Mock the API response
    await page.route('**/api/jobs', async (route) => {
      await route.fulfill({
        status: 201,
        contentType: 'application/json',
        body: JSON.stringify({
          jobId: 'test-job-123',
          message: 'Job created successfully',
        }),
      })
    })

    // Submit the form
    await page.getByRole('button', { name: /Start AI Build Competition/i }).click()

    // Wait for navigation/state change
    await page.waitForTimeout(500)

    // Should see the build arena with job details
    await expect(page.getByText(/spring-petclinic/i)).toBeVisible()
    await expect(page.getByText(/New Comparison/i)).toBeVisible()
  })

  test('should handle API errors gracefully', async ({ page }) => {
    await page.goto('/')

    await page.getByLabel('Repository URL').fill('https://github.com/invalid/repo.git')

    // Mock API error
    await page.route('**/api/jobs', async (route) => {
      await route.fulfill({
        status: 400,
        contentType: 'application/json',
        body: JSON.stringify({
          error: 'Invalid repository URL',
        }),
      })
    })

    await page.getByRole('button', { name: /Start AI Build Competition/i }).click()

    // Should display error message
    await expect(page.getByText(/Invalid repository URL/i)).toBeVisible()
  })

  test('should show loading state during submission', async ({ page }) => {
    await page.goto('/')

    await page.getByLabel('Repository URL').fill('https://github.com/test/repo.git')

    // Mock slow API response
    await page.route('**/api/jobs', async (route) => {
      await new Promise((resolve) => setTimeout(resolve, 1000))
      await route.fulfill({
        status: 201,
        contentType: 'application/json',
        body: JSON.stringify({
          jobId: 'test-job-123',
          message: 'Job created successfully',
        }),
      })
    })

    const submitButton = page.getByRole('button', { name: /Start AI Build Competition/i })
    await submitButton.click()

    // Should show loading state
    await expect(submitButton).toHaveText(/Submitting/i)
    await expect(submitButton).toBeDisabled()
  })
})
