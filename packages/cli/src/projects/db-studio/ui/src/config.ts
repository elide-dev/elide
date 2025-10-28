/**
 * API Configuration
 *
 * In production (elide db studio), the API runs on port 4984 and UI on port 4983.
 * In development, you can override API_BASE_URL via environment variable.
 */

const DEFAULT_API_PORT = 4984;

export const API_BASE_URL =
  import.meta.env.VITE_API_URL ||
  `http://localhost:${DEFAULT_API_PORT}`;
