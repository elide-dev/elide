import type { DiscoveredDatabase } from "../database.ts";
import type { ApiResponse, RouteContext, DatabaseConstructor } from "./types.ts";
import { matchRoute } from "./router.ts";
import { routes } from "../routes/index.ts";
import { errorResponse } from "./responses.ts";

/**
 * Handle API requests using Node.js primitives
 */
export async function handleApiRequest(
  url: string,
  method: string,
  body: string,
  databases: DiscoveredDatabase[],
  Database: DatabaseConstructor
): Promise<ApiResponse> {
  // Parse URL path (remove query string if present)
  const path = url.split('?')[0];

  try {
    // Try to match against registered routes
    const context: RouteContext = { databases, Database };

    for (const route of routes) {
      if (route.method !== method) continue;

      const params = matchRoute(route.pattern, path);
      if (params) {
        return await route.handler(params, context, body);
      }
    }

    // No route matched - return 404
    return { status: 404, headers: {}, body: '' };

  } catch (err) {
    const errorMessage = err instanceof Error ? err.message : "Unknown error";
    console.error("Error handling request:", err);
    return errorResponse(errorMessage, 500);
  }
}

