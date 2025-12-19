import { z } from 'zod';

/**
 * Repository URL validation
 */
export const repositoryUrlSchema = z
  .string()
  .url('Invalid repository URL')
  .regex(
    /^https?:\/\/(www\.)?(github|gitlab)\.com\/[\w-]+\/[\w.-]+(?:\.git)?$/,
    'Must be a valid GitHub or GitLab repository URL'
  )
  .transform((url) => url.replace(/\.git$/, '')); // Normalize URL

/**
 * Build type validation
 */
export const buildTypeSchema = z.enum(['elide', 'standard']);

/**
 * Start race request validation
 */
export const startRaceSchema = z.object({
  repositoryUrl: repositoryUrlSchema,
});

/**
 * Check race request validation
 */
export const checkRaceSchema = z.object({
  repo: repositoryUrlSchema,
});

/**
 * Get recording request validation
 */
export const getRecordingSchema = z.object({
  jobId: z.string().uuid('Invalid job ID'),
  buildType: buildTypeSchema,
});

/**
 * Container start options validation
 */
export const containerStartOptionsSchema = z.object({
  image: z.string().min(1, 'Image name is required'),
  repoUrl: repositoryUrlSchema,
  jobId: z.string().uuid('Invalid job ID'),
  buildType: buildTypeSchema,
  apiKey: z.string().min(1, 'API key is required'),
});

/**
 * Environment variables validation
 */
export const envSchema = z.object({
  ANTHROPIC_API_KEY: z.string().min(1, 'ANTHROPIC_API_KEY is required'),
  NODE_ENV: z.enum(['development', 'production', 'test']).default('development'),
  PORT: z.string().regex(/^\d+$/).transform(Number).default('3001'),
  HOST: z.string().default('localhost'),
});

/**
 * Type exports
 */
export type StartRaceInput = z.infer<typeof startRaceSchema>;
export type CheckRaceInput = z.infer<typeof checkRaceSchema>;
export type GetRecordingInput = z.infer<typeof getRecordingSchema>;
export type ContainerStartOptions = z.infer<typeof containerStartOptionsSchema>;
export type EnvConfig = z.infer<typeof envSchema>;
