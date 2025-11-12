import { jsonResponse, handleDatabaseError, errorResponse } from "../http/responses.ts";
import { withDatabase } from "../http/middleware.ts";
import { parseRequestBody } from "../utils/request.ts";

/**
 * Execute a raw SQL query
 */
export const executeQueryRoute = withDatabase(async (_params, context, body) => {
  const data = parseRequestBody(body);
  const sql = data.sql as string | undefined;
  const queryParams = data.params as unknown[] | undefined;

  if (!sql) {
    return errorResponse("Request body must contain 'sql' string", 400);
  }

  try {
    const stmt = context.db.prepare(sql);
    const params = queryParams || [];

    if (sql.trim().toLowerCase().startsWith("select")) {
      const rows = stmt.all(...(params as any));
      return jsonResponse({
        success: true,
        rows,
        rowCount: Array.isArray(rows) ? rows.length : 0,
      });
    } else {
      const info = stmt.run(...(params as any));
      return jsonResponse({
        success: true,
        rowsAffected: info.changes,
        lastInsertRowid: info.lastInsertRowid,
      });
    }
  } catch (err) {
    return handleDatabaseError(err, "execute query");
  }
});

