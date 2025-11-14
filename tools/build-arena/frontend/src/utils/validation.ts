import { VALIDATION, ERROR_MESSAGES } from '../constants/config';

/**
 * Validate repository URL
 */
export function validateRepositoryUrl(url: string): { valid: boolean; error?: string } {
  if (!url || url.trim() === '') {
    return { valid: false, error: ERROR_MESSAGES.REPO_URL_REQUIRED };
  }

  const trimmedUrl = url.trim();

  // Check if it matches any of our patterns
  const isValid =
    VALIDATION.GITHUB_URL_PATTERN.test(trimmedUrl) ||
    VALIDATION.GITLAB_URL_PATTERN.test(trimmedUrl) ||
    VALIDATION.GENERIC_GIT_URL_PATTERN.test(trimmedUrl);

  if (!isValid) {
    return { valid: false, error: ERROR_MESSAGES.INVALID_REPO_URL };
  }

  return { valid: true };
}

/**
 * Normalize repository URL (remove trailing .git, etc.)
 */
export function normalizeRepositoryUrl(url: string): string {
  return url
    .trim()
    .replace(/\.git$/, '')
    .replace(/\/$/, '');
}

/**
 * Sanitize user input to prevent XSS
 */
export function sanitizeInput(input: string): string {
  return input
    .trim()
    .replace(/[<>]/g, '') // Remove angle brackets
    .slice(0, 500); // Limit length
}
