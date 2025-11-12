import type { ApiResponse, RouteContext, RouteHandler, DatabaseHandler } from "./types.ts";
import { validateDatabaseIndex } from "../utils/validation.ts";

/**
 * Middleware that validates database index and provides database instance to handler
 */
export function withDatabase(handler: DatabaseHandler): RouteHandler {
  return async (params: Record<string, string>, context: RouteContext, body: string): Promise<ApiResponse> => {
    const result = validateDatabaseIndex(params.dbIndex, context.databases);
    if ("error" in result) return result.error;

    const { database } = result;
    const db = new context.Database(database.path);

    return handler(params, { ...context, database, db }, body);
  };
}

