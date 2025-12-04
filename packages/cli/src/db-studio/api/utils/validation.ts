import type { ApiResponse } from "../http/types.ts";
import type { DiscoveredDatabase } from "../database.ts";
import { errorResponse } from "../http/responses.ts";

/**
 * Validate database ID parameter
 */
export function validateDatabaseId(
  dbId: string,
  databases: DiscoveredDatabase[]
): { database: DiscoveredDatabase } | { error: ApiResponse } {
  const database = databases.find(db => db.id === dbId);
  if (!database) {
    return { error: errorResponse(`Database not found with ID: ${dbId}`, 404) };
  }
  return { database };
}

/**
 * Validate database index parameter
 * @deprecated Use validateDatabaseId instead
 */
export function validateDatabaseIndex(
  dbId: string,
  databases: DiscoveredDatabase[]
): { database: DiscoveredDatabase } | { error: ApiResponse } {
  const database = databases.find(db => db.id === dbId);
  if (!database) {
    return { error: errorResponse(`Database not found with ID: ${dbId}`, 404) };
  }
  return { database };
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

