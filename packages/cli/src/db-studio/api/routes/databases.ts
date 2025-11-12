import type { RouteContext, ApiResponse } from "../http/types.ts";
import { jsonResponse } from "../http/responses.ts";
import { withDatabase } from "../http/middleware.ts";
import { getDatabaseInfo } from "../database.ts";

/**
 * List all databases
 */
export async function listDatabases(_params: Record<string, string>, context: RouteContext, _body: string): Promise<ApiResponse> {
  return jsonResponse({ databases: context.databases });
}

/**
 * Get database info
 */
export const getDatabaseInfoRoute = withDatabase(async (_params, context, _body) => {
  const info = getDatabaseInfo(context.db, context.database.path);

  const fullInfo = {
    ...info,
    size: context.database.size,
    lastModified: context.database.lastModified,
    tableCount: info.tableCount,
  };

  return jsonResponse(fullInfo);
});

