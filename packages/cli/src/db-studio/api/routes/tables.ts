import { jsonResponse, handleSQLError, errorResponse } from "../http/responses.ts";
import { withDatabase } from "../http/middleware.ts";
import { requireTableName } from "../utils/validation.ts";
import { parseRequestBody, parseQueryParams } from "../utils/request.ts";
import { getTables, getTableData } from "../database.ts";

/**
 * Get list of tables in a database
 */
export const getTablesRoute = withDatabase(async (context) => {
  const { db } = context;
  const tables = getTables(db);
  return jsonResponse({ tables });
});

/**
 * Get table data with enhanced column metadata
 * Supports query parameters: limit (default: 100), offset (default: 0)
 */
export const getTableDataRoute = withDatabase(async (context) => {
  const { params, db, url } = context;
  const tableNameError = requireTableName(params);
  if (tableNameError) return tableNameError;

  // Parse query parameters for pagination
  const queryParams = parseQueryParams(url);
  const limitParam = queryParams.get('limit');
  const offsetParam = queryParams.get('offset');
  
  const limit = limitParam ? parseInt(limitParam, 10) : 100;
  const offset = offsetParam ? parseInt(offsetParam, 10) : 0;
  
  // Validate pagination parameters
  if (isNaN(limit) || limit < 1 || limit > 1000) {
    return errorResponse("Invalid limit parameter (must be between 1 and 1000)", 400);
  }
  if (isNaN(offset) || offset < 0) {
    return errorResponse("Invalid offset parameter (must be >= 0)", 400);
  }

  const tableData = getTableData(db, params.tableName, limit, offset);
  return jsonResponse(tableData);
});

/**
 * Create a new table
 */
export const createTableRoute = withDatabase(async (context) => {
  const { db, body } = context;
  const data = parseRequestBody(body);
  const tableName = data.name as string | undefined;
  const schema = data.schema as Array<{ name: string; type: string; constraints?: string }> | undefined;

  if (!tableName) {
    return errorResponse("Request body must contain 'name' for the table", 400);
  }

  if (!schema || !Array.isArray(schema) || schema.length === 0) {
    return errorResponse("Request body must contain 'schema' array with at least one column", 400);
  }

  const columns = schema.map(col => {
    const constraints = col.constraints ? ` ${col.constraints}` : "";
    return `"${col.name}" ${col.type}${constraints}`;
  }).join(", ");

  const sql = `CREATE TABLE "${tableName}" (${columns})`;
  const startTime = performance.now();

  try {
    db.exec(sql);
    return jsonResponse({ success: true, message: `Table '${tableName}' created successfully` });
  } catch (err) {
    return handleSQLError(err, sql, startTime);
  }
});

/**
 * Drop a table
 */
export const dropTableRoute = withDatabase(async (context) => {
  const { params, db, body } = context;
  const tableNameError = requireTableName(params);
  if (tableNameError) return tableNameError;

  const data = parseRequestBody(body);
  const confirm = data.confirm as boolean | undefined;

  if (!confirm) {
    return errorResponse("Must set 'confirm: true' in request body to drop table (safety check)", 400);
  }

  const sql = `DROP TABLE "${params.tableName}"`;
  const startTime = performance.now();

  try {
    db.exec(sql);
    return jsonResponse({ success: true, message: `Table '${params.tableName}' dropped successfully` });
  } catch (err) {
    return handleSQLError(err, sql, startTime);
  }
});

