/**
 * Application configuration constants
 */

export const CONFIG = {
  // API endpoints
  API_BASE_URL: import.meta.env.VITE_API_URL || '',

  // WebSocket configuration
  WS_RECONNECT_DELAY: 3000, // ms
  WS_MAX_RECONNECT_ATTEMPTS: 5,

  // Terminal configuration
  TERMINAL_FONT_SIZE: 13,
  TERMINAL_ROWS: 30,
  TERMINAL_FONT_FAMILY: 'Menlo, Monaco, "Courier New", monospace',

  // Terminal theme
  TERMINAL_THEME: {
    background: '#1e293b',
    foreground: '#e2e8f0',
  },

  // Replay configuration
  REPLAY_MAX_DELAY: 100, // Cap replay delays at 100ms for faster playback

  // Timer configuration
  TIMER_UPDATE_INTERVAL: 1000, // Update timer every second

  // UI thresholds
  TIE_THRESHOLD_SECONDS: 1, // Difference < 1s is considered a tie
} as const;

/**
 * URL validation patterns
 */
export const VALIDATION = {
  GITHUB_URL_PATTERN: /^https?:\/\/(www\.)?github\.com\/[\w-]+\/[\w.-]+(?:\.git)?$/,
  GITLAB_URL_PATTERN: /^https?:\/\/(www\.)?gitlab\.com\/[\w-]+\/[\w.-]+(?:\.git)?$/,
  GENERIC_GIT_URL_PATTERN: /^https?:\/\/.+\.git$/,
} as const;

/**
 * Error messages
 */
export const ERROR_MESSAGES = {
  INVALID_REPO_URL: 'Please enter a valid repository URL',
  REPO_URL_REQUIRED: 'Repository URL is required',
  RACE_START_FAILED: 'Failed to start race. Please try again.',
  RECORDING_LOAD_FAILED: 'Failed to load race replay',
  TERMINALS_NOT_READY: 'Terminals not initialized',
  WEBSOCKET_CONNECTION_FAILED: 'Failed to connect to race server',
} as const;

/**
 * Build info injected at build time
 */
export const BUILD_INFO = {
  COMMIT_HASH: __COMMIT_HASH__,
  BUILD_TIME: __BUILD_TIME__,
} as const;
