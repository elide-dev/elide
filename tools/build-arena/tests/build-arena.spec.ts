import { test, expect } from '@playwright/test'

test.describe('Build Arena View', () => {
  test.beforeEach(async ({ page }) => {
    // Mock job submission
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

    // Mock job status endpoint
    await page.route('**/api/jobs/test-job-123', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          job: {
            id: 'test-job-123',
            repositoryUrl: 'https://github.com/spring-projects/spring-petclinic.git',
            repositoryName: 'spring-projects/spring-petclinic',
            createdAt: new Date().toISOString(),
            status: 'running',
          },
        }),
      })
    })

    // Navigate to homepage and submit job
    await page.goto('/')
    await page.getByLabel('Repository URL').fill('https://github.com/spring-projects/spring-petclinic.git')
    await page.getByRole('button', { name: /Start AI Build Competition/i }).click()
    await page.waitForTimeout(500)
  })

  test('should display job information', async ({ page }) => {
    // Check repository name is displayed
    await expect(page.getByText('spring-projects/spring-petclinic')).toBeVisible()

    // Check repository URL is displayed
    await expect(page.getByText('https://github.com/spring-projects/spring-petclinic.git')).toBeVisible()

    // Check status badge
    await expect(page.getByText(/running/i)).toBeVisible()
  })

  test('should display dual terminals', async ({ page }) => {
    // Check for terminal headers
    await expect(page.getByText('Elide')).toBeVisible()
    await expect(page.getByText('Standard Toolchain')).toBeVisible()

    // Check for terminal containers (xterm elements)
    const terminals = page.locator('.xterm')
    await expect(terminals).toHaveCount(2)
  })

  test('should allow navigation back to form', async ({ page }) => {
    const newComparisonButton = page.getByRole('button', { name: /New Comparison/i })
    await expect(newComparisonButton).toBeVisible()

    await newComparisonButton.click()

    // Should navigate back to form
    await expect(page.getByText('Submit a Java Repository')).toBeVisible()
  })

  test('should display metrics when build completes', async ({ page }) => {
    // Update mock to return completed job with results
    await page.route('**/api/jobs/test-job-123', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          job: {
            id: 'test-job-123',
            repositoryUrl: 'https://github.com/spring-projects/spring-petclinic.git',
            repositoryName: 'spring-projects/spring-petclinic',
            createdAt: new Date().toISOString(),
            status: 'completed',
            elideResult: {
              tool: 'elide',
              status: 'success',
              startTime: new Date().toISOString(),
              endTime: new Date().toISOString(),
              duration: 15000,
              exitCode: 0,
              metrics: {
                buildTime: 15000,
                memoryUsage: 1024 * 1024 * 1024,
                cpuUsage: 50,
              },
            },
            standardResult: {
              tool: 'standard',
              status: 'success',
              startTime: new Date().toISOString(),
              endTime: new Date().toISOString(),
              duration: 25000,
              exitCode: 0,
              metrics: {
                buildTime: 25000,
                memoryUsage: 1024 * 1024 * 1024 * 1.5,
                cpuUsage: 60,
              },
            },
          },
        }),
      })
    })

    // Wait for update
    await page.waitForTimeout(1500)

    // Check for metrics display
    await expect(page.getByText('Performance Comparison')).toBeVisible()
    await expect(page.getByText('Build Time')).toBeVisible()
    await expect(page.getByText(/faster/i)).toBeVisible()
  })
})
