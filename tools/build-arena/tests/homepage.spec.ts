import { test, expect } from '@playwright/test'

test.describe('Build Arena Homepage', () => {
  test('should load homepage with correct title', async ({ page }) => {
    await page.goto('/')

    // Check title
    await expect(page).toHaveTitle(/Build Arena/)

    // Check main heading
    const heading = page.getByRole('heading', { name: /Build Arena/i })
    await expect(heading).toBeVisible()

    // Check description
    await expect(page.getByText(/AI agents battle it out/i)).toBeVisible()
  })

  test('should display repository form', async ({ page }) => {
    await page.goto('/')

    // Check form elements
    await expect(page.getByText('Submit a Java Repository')).toBeVisible()
    await expect(page.getByLabel('Repository URL')).toBeVisible()
    await expect(page.getByRole('button', { name: /Start AI Build Competition/i })).toBeVisible()
  })

  test('should display example repositories', async ({ page }) => {
    await page.goto('/')

    // Check example repos
    await expect(page.getByText('Example Repositories')).toBeVisible()
    await expect(page.getByText('Spring PetClinic')).toBeVisible()
    await expect(page.getByText('Apache Commons Lang')).toBeVisible()
  })

  test('should populate form when clicking example repository', async ({ page }) => {
    await page.goto('/')

    // Click example repo
    await page.getByText('Spring PetClinic').click()

    // Check if URL is populated
    const input = page.getByLabel('Repository URL')
    await expect(input).toHaveValue(/spring-petclinic/)
  })

  test('should validate repository URL', async ({ page }) => {
    await page.goto('/')

    const input = page.getByLabel('Repository URL')
    const submitButton = page.getByRole('button', { name: /Start AI Build Competition/i })

    // Button should be disabled when empty
    await expect(submitButton).toBeDisabled()

    // Enter invalid URL
    await input.fill('not-a-url')
    await expect(submitButton).toBeEnabled() // HTML5 validation will catch this

    // Enter valid URL
    await input.fill('https://github.com/test/repo.git')
    await expect(submitButton).toBeEnabled()
  })
})
