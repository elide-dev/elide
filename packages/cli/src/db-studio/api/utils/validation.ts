import type { ApiResponse } from "../http/types.ts";
import type { DiscoveredDatabase } from "../database.ts";
import { errorResponse } from "../http/responses.ts";

/**
 * Validate database index parameter
 */
export function validateDatabaseIndex(
  dbIndexStr: string,
  databases: DiscoveredDatabase[]
): { database: DiscoveredDatabase } | { error: ApiResponse } {
  const dbIndex = parseInt(dbIndexStr, 10);

  if (isNaN(dbIndex)) {
    return { error: errorResponse("Invalid database index", 400) };
  }

  if (dbIndex < 0 || dbIndex >= databases.length) {
    return { error: errorResponse(`Database not found at index ${dbIndex}`, 404) };
  }

  return { database: databases[dbIndex] };
}

/**
 * Require table name parameter
 */
export function requireTableName(params: Record<string, string>): ApiResponse | null {
  const tableName = params.tableName;
  if (!tableName) {
    return errorResponse("Table name is required", 400);
  }
  return null;
}

/**
 * Check if a SQL query is potentially destructive
 */
export function isDestructiveQuery(sql: string): boolean {
  const normalized = sql.trim().toLowerCase();
  return (
    normalized.startsWith("drop") ||
    normalized.startsWith("truncate") ||
    (normalized.startsWith("delete") && !normalized.includes("where"))
  );
}

