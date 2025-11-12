import type { ApiResponse } from "./types.ts";

/**
 * Create a JSON response
 */
export function jsonResponse(data: unknown, status: number = 200): ApiResponse {
  console.log("returning json response", data);
  return {
    status,
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  };
}

/**
 * Create an error response
 */
export function errorResponse(message: string, status: number = 500): ApiResponse {
  return jsonResponse({ error: message }, status);
}

/**
 * Handle database operation errors
 */
export function handleDatabaseError(err: unknown, operation: string): ApiResponse {
  const errorMessage = err instanceof Error ? err.message : "Unknown error";
  return errorResponse(`Failed to ${operation}: ${errorMessage}`, 500);
}

