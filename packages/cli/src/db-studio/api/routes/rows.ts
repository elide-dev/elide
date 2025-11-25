import { jsonResponse, errorResponse } from "../http/responses.ts";
import { withDatabase } from "../http/middleware.ts";
import { requireTableName } from "../utils/validation.ts";
import { parseRequestBody } from "../utils/request.ts";
import { deleteRows } from "../database.ts";
import { DeleteRowsRequestSchema } from "../http/schemas.ts";

/**
 * Delete rows from a table based on primary key values
 * DELETE /api/databases/:dbIndex/tables/:tableName/rows
 *
 * Request body: { primaryKeys: [{ id: 1 }, { id: 2 }] }
 * Response: { success: true, rowsAffected: 2 }
 */
export const deleteRowsRoute = withDatabase(async (context) => {
  const { params, db, body } = context;
  const tableNameError = requireTableName(params);
  if (tableNameError) return tableNameError;

  // Parse and validate request body
  const g = DeleteRowsRequestSchema.safeParse(body);
  console.log(g)
  const data = parseRequestBody(body);
  const primaryKeys = data.primaryKeys as Array<Record<string, unknown>> | undefined;

  if (!primaryKeys || !Array.isArray(primaryKeys) || primaryKeys.length === 0) {
    return errorResponse(
      "Request body must contain 'primaryKeys' array with at least one primary key",
      400
    );
  }

  try {
    const result = deleteRows(db, params.tableName, primaryKeys);
    return jsonResponse({
      success: true,
      rowsAffected: result.rowsAffected,
    });
  } catch (err) {
    const errorMessage = err instanceof Error ? err.message : "Unknown error";
    return errorResponse(errorMessage, 400);
  }
});
