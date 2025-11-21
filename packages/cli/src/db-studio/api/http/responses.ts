import type { ApiResponse } from "./types.ts";

const SUCCESS_STATUS = 200;
const ERROR_STATUS = 500;
const NOT_FOUND_STATUS = 404;

/**
 * Create a JSON response
 */
export function jsonResponse(data: unknown, status: number = SUCCESS_STATUS): ApiResponse {
  // console.log("returning json response", data);
  return {
    status,
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  };
}

/**
 * Create an error response
 */
export function errorResponse(message: string, status: number = ERROR_STATUS): ApiResponse {
  return jsonResponse({ success: false, error: message }, status);
}

/**
 * Handle database operation errors
 */
export function handleDatabaseError(err: unknown, operation: string): ApiResponse {
  const errorMessage = err instanceof Error ? err.message : "Unknown error";
  return errorResponse(`Failed to ${operation}: ${errorMessage}`, ERROR_STATUS);
}

/**
 * Handle SQL query errors with context
 */
export function handleSQLError(
  err: unknown,
  sql: string,
  startTime?: number
): ApiResponse {
  const errorMessage = err instanceof Error ? err.message : "Unknown error";
  const endTime = startTime ? performance.now() : undefined;
  
  return jsonResponse({
    success: false,
    error: errorMessage,
    sql: sql.trim(),
    executionTimeMs: endTime && startTime ? Number((endTime - startTime).toFixed(2)) : undefined,
  }, ERROR_STATUS);
}

/**
 * Creates a 404 Not Found response
 */
export function notFoundResponse(): ApiResponse {
  return jsonResponse({ error: "Not Found" }, NOT_FOUND_STATUS);
}
