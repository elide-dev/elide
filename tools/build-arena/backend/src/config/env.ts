import { envSchema, type EnvConfig } from '../validation/schemas.js';

/**
 * Validate and parse environment variables
 */
function validateEnv(): EnvConfig {
  try {
    return envSchema.parse(process.env);
  } catch (error) {
    console.error('❌ Environment validation failed:');
    if (error instanceof Error) {
      console.error(error.message);
    }
    console.error('\nRequired environment variables:');
    console.error('  - ANTHROPIC_API_KEY: Your Anthropic API key');
    console.error('\nOptional environment variables:');
    console.error('  - NODE_ENV: development | production | test (default: development)');
    console.error('  - PORT: Server port (default: 3001)');
    console.error('  - HOST: Server host (default: localhost)');
    process.exit(1);
  }
}

/**
 * Validated environment configuration
 */
export const env = validateEnv();

/**
 * Check if running in development mode
 */
export const isDevelopment = env.NODE_ENV === 'development';

/**
 * Check if running in production mode
 */
export const isProduction = env.NODE_ENV === 'production';

/**
 * Check if running in test mode
 */
export const isTest = env.NODE_ENV === 'test';
