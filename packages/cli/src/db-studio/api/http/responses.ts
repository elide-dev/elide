import type { ApiResponse } from "./types.ts";

const SUCCESS_STATUS = 200;
const ERROR_STATUS = 500;
const NOT_FOUND_STATUS = 404;

/**
 * CORS headers to allow cross-origin requests
 */
const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, POST, PUT, DELETE, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type, Authorization",
};

/**
 * Extract error message from various error types
 * Handles standard Error, SQLite errors, and unknown error shapes
 */
export function extractErrorMessage(err: unknown): string {
  if (err instanceof Error) {
    return err.message;
  }
  
  // Handle objects with message property (common in SQLite bindings)
  if (typeof err === 'object' && err !== null) {
    const errObj = err as Record<string, unknown>;
    
    // Try common error message properties
    if (typeof errObj.message === 'string') {
      return errObj.message;
    }
    if (typeof errObj.error === 'string') {
      return errObj.error;
    }
    if (typeof errObj.msg === 'string') {
      return errObj.msg;
    }
    
    // Try toString if available
    if (typeof errObj.toString === 'function') {
      const str = errObj.toString();
      if (str !== '[object Object]') {
        return str;
      }
    }
    
    // Last resort: stringify the object
    try {
      return JSON.stringify(err);
    } catch {
      return 'Unknown error (could not serialize)';
    }
  }
  
  // Handle primitive errors (strings, numbers)
  if (typeof err === 'string') {
    return err;
  }
  
  return `Unknown error: ${String(err)}`;
}

/**
 * Create a JSON response
 */
export function jsonResponse(data: unknown, status: number = SUCCESS_STATUS): ApiResponse {
  // console.log("returning json response", data);
  return {
    status,
    headers: { "Content-Type": "application/json", ...corsHeaders },
    body: JSON.stringify(data),
  };
}

/**
 * Create a CORS preflight response for OPTIONS requests
 */
export function corsPreflightResponse(): ApiResponse {
  return {
    status: 204,
    headers: corsHeaders,
    body: "",
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
  console.error(`Database error during ${operation}:`, err);
  const errorMessage = extractErrorMessage(err);
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
  console.error("SQL error:", err);
  const errorMessage = extractErrorMessage(err);
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
