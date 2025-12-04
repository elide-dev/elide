import type { ApiResponse, RouteContext } from "../http/types.ts";
import { jsonResponse } from "../http/responses.ts";

/**
 * Health check endpoint
 */
export async function healthCheck(_context: RouteContext): Promise<ApiResponse> {
  return jsonResponse({ status: "ok" });
}

