/**
 * Backend configuration constants
 */

export const CONFIG = {
  // Docker configuration
  DOCKER: {
    IMAGES: {
      ELIDE: 'elide-builder:latest',
      STANDARD: 'standard-builder:latest',
    },
    MEMORY_LIMIT: 4 * 1024 * 1024 * 1024, // 4GB
    CPU_QUOTA: 200000, // 2 CPU cores
    NETWORK_MODE: 'bridge',
    STOP_TIMEOUT: 10, // seconds
  },

  // Container configuration
  CONTAINER: {
    WORKING_DIR: '/workspace',
    TERM: 'xterm-256color',
  },

  // Claude Code configuration
  CLAUDE: {
    VERSION: '2.0.35', // TODO: Get from environment
    MAX_TURNS: 50,
    OUTPUT_FORMAT: 'json',
    INSTRUCTIONS_DIR: '../docker',
  },

  // Recording configuration
  RECORDINGS: {
    DIR: 'recordings',
    VERSION: 1,
  },

  // Race configuration
  RACE: {
    TIE_THRESHOLD_MS: 1000, // Difference < 1s is a tie
    CONTAINER_CHECK_INTERVAL: 1000, // Check container status every 1s
  },

  // Timeouts
  TIMEOUTS: {
    CONTAINER_START: 30000, // 30s
    BUILD: 900000, // 15 minutes
    DOCKER_SPAWN: 10000, // 10s
  },
} as const;

/**
 * Build types
 */
export const BUILD_TYPES = ['elide', 'standard'] as const;
export type BuildType = typeof BUILD_TYPES[number];

/**
 * Error messages
 */
export const ERROR_MESSAGES = {
  REPO_URL_REQUIRED: 'Repository URL is required',
  INVALID_BUILD_TYPE: 'Invalid build type. Must be "elide" or "standard"',
  JOB_NOT_FOUND: 'Job not found',
  RECORDING_NOT_FOUND: 'Recording not found',
  CONTAINER_START_FAILED: 'Failed to start container',
  DOCKER_IMAGE_NOT_FOUND: 'Docker image not available. Run: cd docker && ./build-images.sh',
  API_KEY_MISSING: 'ANTHROPIC_API_KEY environment variable is required',
  CONTAINER_NOT_FOUND: 'Container not found',
} as const;

/**
 * HTTP status codes
 */
export const HTTP_STATUS = {
  OK: 200,
  CREATED: 201,
  BAD_REQUEST: 400,
  NOT_FOUND: 404,
  INTERNAL_ERROR: 500,
} as const;
