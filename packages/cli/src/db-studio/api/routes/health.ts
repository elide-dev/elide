import type { ApiResponse } from "../http/types.ts";
import { jsonResponse } from "../http/responses.ts";

/**
 * Health check endpoint
 */
export async function healthCheck(): Promise<ApiResponse> {
  return jsonResponse({ status: "ok" });
}

