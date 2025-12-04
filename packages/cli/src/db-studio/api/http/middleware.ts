import { Database } from "elide:sqlite";
import type { ApiResponse, RouteContext, RouteHandler, DatabaseHandler } from "./types.ts";
import { validateDatabaseId } from "../utils/validation.ts";

/**
 * Middleware that validates database ID and provides database instance to handler
 */
export function withDatabase(handler: DatabaseHandler): RouteHandler {
  return async (context: RouteContext): Promise<ApiResponse> => {
    const { params, databases } = context;
    const result = validateDatabaseId(params.dbId, databases);
    if ("error" in result) return result.error;

    const { database } = result;
    const db = new Database(database.path);

    return handler({ ...context, database, db });
  };
}

