import type { DiscoveredDatabase } from "../database.ts";
import type { ApiResponse, RouteContext } from "./types.ts";
import { matchRoute } from "./router.ts";
import { routes } from "../routes/index.ts";
import { errorResponse, notFoundResponse } from "./responses.ts";

/**
 * Extracts the path from a URL, removing query strings
 */
function parseUrlPath(url: string): string {
  return url.split('?')[0];
}

/**
 * Attempts to find and execute a matching route handler
 */
async function executeMatchingRoute(
  path: string,
  method: string,
  context: RouteContext
): Promise<ApiResponse | null> {
  for (const route of routes) {
    if (route.method !== method) continue;

    const params = matchRoute(route.pattern, path);
    if (params) {
      return await route.handler({ ...context, params });
    }
  }

  return null;
}

/**
 * Main API request handler - routes requests to appropriate handlers
 */
export async function handleApiRequest(
  url: string,
  method: string,
  body: string,
  databases: DiscoveredDatabase[]
): Promise<ApiResponse> {
  try {
    const path = parseUrlPath(url);
    const context: RouteContext = { 
      databases, 
      params: {},
      body,
      url
    };

    const response = await executeMatchingRoute(path, method, context);
    return response ?? notFoundResponse();

  } catch (err) {
    const errorMessage = err instanceof Error ? err.message : "Unknown error";
    console.error("Error handling request:", err);
    return errorResponse(errorMessage);
  }
}

