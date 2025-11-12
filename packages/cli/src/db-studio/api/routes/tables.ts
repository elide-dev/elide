import { jsonResponse, handleDatabaseError, errorResponse } from "../http/responses.ts";
import { withDatabase } from "../http/middleware.ts";
import { requireTableName } from "../utils/validation.ts";
import { parseRequestBody } from "../utils/request.ts";
import { getTables, getTableData } from "../database.ts";

/**
 * Get list of tables in a database
 */
export const getTablesRoute = withDatabase(async (_params, context, _body) => {
  const tables = getTables(context.db);
  return jsonResponse({ tables });
});

/**
 * Get table data
 */
export const getTableDataRoute = withDatabase(async (params, context, _body) => {
  const tableNameError = requireTableName(params);
  if (tableNameError) return tableNameError;

  const tableData = getTableData(context.db, params.tableName);
  console.log(tableData);
  return jsonResponse(tableData);
});

/**
 * Create a new table
 */
export const createTableRoute = withDatabase(async (_params, context, body) => {
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
    return `${col.name} ${col.type}${constraints}`;
  }).join(", ");

  const sql = `CREATE TABLE ${tableName} (${columns})`;

  try {
    context.db.exec(sql);
    return jsonResponse({ success: true, message: `Table '${tableName}' created successfully` });
  } catch (err) {
    return handleDatabaseError(err, "create table");
  }
});

/**
 * Drop a table
 */
export const dropTableRoute = withDatabase(async (params, context, body) => {
  const tableNameError = requireTableName(params);
  if (tableNameError) return tableNameError;

  const data = parseRequestBody(body);
  const confirm = data.confirm as boolean | undefined;

  if (!confirm) {
    return errorResponse("Must set 'confirm: true' in request body to drop table (safety check)", 400);
  }

  const sql = `DROP TABLE ${params.tableName}`;

  try {
    context.db.exec(sql);
    return jsonResponse({ success: true, message: `Table '${params.tableName}' dropped successfully` });
  } catch (err) {
    return handleDatabaseError(err, "drop table");
  }
});

