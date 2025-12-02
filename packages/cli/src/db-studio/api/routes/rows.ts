import { jsonResponse, errorResponse } from "../http/responses.ts";
import { withDatabase } from "../http/middleware.ts";
import { requireTableName } from "../utils/validation.ts";
import { parseRequestBody } from "../utils/request.ts";
import { deleteRows, insertRow } from "../database.ts";
import { DeleteRowsRequestSchema, InsertRowRequestSchema } from "../http/schemas.ts";

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
  const data = parseRequestBody(body);
  const result = DeleteRowsRequestSchema.safeParse(data);

  if (!result.success) {
    return errorResponse(
      `Invalid request body: ${result.error.errors.map(e => e.message).join(", ")}`,
      400
    );
  }

  const { primaryKeys } = result.data;

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

/**
 * Insert a new row into a table
 * POST /api/databases/:dbIndex/tables/:tableName/rows
 *
 * Request body: { row: { column1: value1, column2: value2 } }
 * Response: { success: true, rowsAffected: 1, lastInsertRowid: 5 }
 */
export const insertRowRoute = withDatabase(async (context) => {
  const { params, db, body } = context;
  const tableNameError = requireTableName(params);
  if (tableNameError) return tableNameError;

  // Parse and validate request body
  const data = parseRequestBody(body);
  const result = InsertRowRequestSchema.safeParse(data);

  if (!result.success) {
    return errorResponse(
      `Invalid request body: ${result.error.errors.map(e => e.message).join(", ")}`,
      400
    );
  }

  const { row } = result.data;

  try {
    const result = insertRow(db, params.tableName, row);
    return jsonResponse({
      success: true,
      rowsAffected: result.rowsAffected,
      lastInsertRowid: result.lastInsertRowid,
    });
  } catch (err) {
    const errorMessage = err instanceof Error ? err.message : "Unknown error";
    return errorResponse(errorMessage, 400);
  }
});
