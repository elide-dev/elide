import type { ApiResponse } from "../http/types.ts";
import { jsonResponse, handleDatabaseError, errorResponse } from "../http/responses.ts";
import { withDatabase } from "../http/middleware.ts";
import { requireTableName } from "../utils/validation.ts";
import { parseRequestBody, buildWhereClause } from "../utils/request.ts";

/**
 * Insert rows into a table
 */
export const insertRowsRoute = withDatabase(async (params, context, body) => {
  const tableNameError = requireTableName(params);
  if (tableNameError) return tableNameError;

  const data = parseRequestBody(body);
  const values = data.values as Record<string, unknown> | undefined;

  if (!values || typeof values !== "object") {
    return errorResponse("Request body must contain 'values' object", 400);
  }

  const columns = Object.keys(values);
  const placeholders = columns.map(() => "?").join(", ");
  const sql = `INSERT INTO ${params.tableName} (${columns.join(", ")}) VALUES (${placeholders})`;

  try {
    const stmt = context.db.prepare(sql);
    stmt.run(...(Object.values(values) as any));
    return jsonResponse({ success: true, message: "Row inserted successfully" });
  } catch (err) {
    return handleDatabaseError(err, "insert row");
  }
});

/**
 * Update rows in a table
 */
export const updateRowsRoute = withDatabase(async (params, context, body) => {
  const tableNameError = requireTableName(params);
  if (tableNameError) return tableNameError;

  const data = parseRequestBody(body);
  const values = data.values as Record<string, unknown> | undefined;
  const where = data.where as Record<string, unknown> | undefined;

  if (!values || typeof values !== "object") {
    return errorResponse("Request body must contain 'values' object", 400);
  }

  if (!where || typeof where !== "object" || Object.keys(where).length === 0) {
    return errorResponse("Request body must contain 'where' object with at least one condition", 400);
  }

  const setColumns = Object.keys(values).map(key => `${key} = ?`).join(", ");
  const { clause: whereClause, values: whereValues } = buildWhereClause(where);
  const sql = `UPDATE ${params.tableName} SET ${setColumns} ${whereClause}`;

  try {
    const stmt = context.db.prepare(sql);
    const allValues = [...Object.values(values), ...whereValues];
    const info = stmt.run(...(allValues as any));
    return jsonResponse({ success: true, rowsAffected: info.changes, message: "Rows updated successfully" });
  } catch (err) {
    return handleDatabaseError(err, "update rows");
  }
});

/**
 * Delete rows from a table
 */
export const deleteRowsRoute = withDatabase(async (params, context, body) => {
  const tableNameError = requireTableName(params);
  if (tableNameError) return tableNameError;

  const data = parseRequestBody(body);
  const where = data.where as Record<string, unknown> | undefined;

  if (!where || typeof where !== "object" || Object.keys(where).length === 0) {
    return errorResponse("Request body must contain 'where' object with at least one condition (safety check)", 400);
  }

  const { clause: whereClause, values: whereValues } = buildWhereClause(where);
  const sql = `DELETE FROM ${params.tableName} ${whereClause}`;

  try {
    const stmt = context.db.prepare(sql);
    const info = stmt.run(...(whereValues as any));
    return jsonResponse({ success: true, rowsAffected: info.changes, message: "Rows deleted successfully" });
  } catch (err) {
    return handleDatabaseError(err, "delete rows");
  }
});

