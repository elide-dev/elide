import { jsonResponse, handleSQLError, errorResponse } from "../http/responses.ts";
import { withDatabase } from "../http/middleware.ts";
import { parseRequestBody } from "../utils/request.ts";
import { executeQuery } from "../database.ts";
import { ExecuteQueryRequestSchema } from "../http/schemas.ts";

/**
 * Execute a raw SQL query with rich metadata
 */
export const executeQueryRoute = withDatabase(async (context) => {
  const { db, body } = context;
  const data = parseRequestBody(body);
  const result = ExecuteQueryRequestSchema.safeParse(data);

  if (!result.success) {
    return errorResponse(
      `Invalid request body: ${result.error.errors.map(e => e.message).join(", ")}`,
      400
    );
  }

  const { sql, params: queryParams } = result.data;
  const trimmedSql = sql.trim();
  const startTime = performance.now();

  try {
    const params = queryParams || [];
    const sqlLower = trimmedSql.toLowerCase();

    // Determine if this is a SELECT query
    if (sqlLower.startsWith("select")) {
      // For SELECT queries, use our enhanced executeQuery function
      const { columns, data: rows } = executeQuery(db, trimmedSql);
      const endTime = performance.now();

      return jsonResponse({
        success: true,
        data: rows,
        columns,
        metadata: {
          executionTimeMs: Number((endTime - startTime).toFixed(2)),
          sql: trimmedSql,
          rowCount: rows.length,
        },
      });
    } else {
      // For INSERT/UPDATE/DELETE/CREATE/DROP etc.
      const stmt = db.prepare(trimmedSql);
      const info = stmt.run(...(params as any));
      const endTime = performance.now();

      return jsonResponse({
        success: true,
        rowsAffected: info.changes,
        lastInsertRowid: info.lastInsertRowid,
        metadata: {
          executionTimeMs: Number((endTime - startTime).toFixed(2)),
          sql: trimmedSql,
          rowCount: info.changes,
        },
      });
    }
  } catch (err) {
    return handleSQLError(err, trimmedSql, startTime);
  }
});

