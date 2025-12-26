import type { RouteContext, ApiResponse } from "../http/types.ts";
import { jsonResponse } from "../http/responses.ts";
import { withDatabase } from "../http/middleware.ts";
import { getDatabaseInfo } from "../database.ts";

/**
 * List all databases
 */
export async function listDatabases(context: RouteContext): Promise<ApiResponse> {
  const { databases } = context;
  return jsonResponse({ databases });
}

/**
 * Get database info
 */
export const getDatabaseInfoRoute = withDatabase(async (context) => {
  const { db, database } = context;
  const info = getDatabaseInfo(db, database.path);

  const fullInfo = {
    ...info,
    size: database.size,
    lastModified: database.lastModified,
    tableCount: info.tableCount,
  };

  return jsonResponse(fullInfo);
});

