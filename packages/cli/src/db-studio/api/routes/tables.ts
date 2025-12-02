import { jsonResponse, handleSQLError, errorResponse } from "../http/responses.ts";
import { withDatabase } from "../http/middleware.ts";
import { requireTableName } from "../utils/validation.ts";
import { parseRequestBody, parseQueryParams } from "../utils/request.ts";
import { getTables, getTableData } from "../database.ts";
import type { Filter } from "../http/schemas.ts";
import { CreateTableRequestSchema, FiltersArraySchema } from "../http/schemas.ts";

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
 * Supports query parameters: limit (default: 100), offset (default: 0), sort (column name), order (asc/desc), where (JSON-encoded filters)
 */
export const getTableDataRoute = withDatabase(async (context) => {
  const { params, db, url } = context;
  const tableNameError = requireTableName(params);
  if (tableNameError) return tableNameError;

  // Parse query parameters for pagination and sorting
  const queryParams = parseQueryParams(url);
  const limitParam = queryParams.get('limit');
  const offsetParam = queryParams.get('offset');
  const sortParam = queryParams.get('sort');
  const orderParam = queryParams.get('order');
  const whereParam = queryParams.get('where');
  
  const limit = limitParam ? parseInt(limitParam, 10) : 100;
  const offset = offsetParam ? parseInt(offsetParam, 10) : 0;
  
  // Validate pagination parameters
  if (isNaN(limit) || limit < 1 || limit > 1000) {
    return errorResponse("Invalid limit parameter (must be between 1 and 1000)", 400);
  }
  if (isNaN(offset) || offset < 0) {
    return errorResponse("Invalid offset parameter (must be >= 0)", 400);
  }

  // Validate sorting parameters
  let sortColumn: string | null = null;
  let sortDirection: 'asc' | 'desc' | null = null;
  
  if (sortParam) {
    // Validate order parameter if sort is provided
    if (!orderParam || (orderParam !== 'asc' && orderParam !== 'desc')) {
      return errorResponse("Invalid order parameter (must be 'asc' or 'desc' when sort is provided)", 400);
    }
    sortColumn = sortParam;
    sortDirection = orderParam as 'asc' | 'desc';
  }

  // Parse and validate filters
  let filters: Filter[] | null = null;
  if (whereParam) {
    try {
      const decodedWhere = decodeURIComponent(whereParam);
      const parsedWhere = JSON.parse(decodedWhere);

      // Validate using zod schema
      const result = FiltersArraySchema.safeParse(parsedWhere);
      if (!result.success) {
        return errorResponse(
          `Invalid where parameter: ${result.error.errors.map(e => e.message).join(", ")}`,
          400
        );
      }

      filters = result.data;
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : "Unknown error";
      return errorResponse(
        `Failed to parse where parameter: ${errorMessage}`,
        400
      );
    }
  }

  try {
    const tableData = getTableData(db, params.tableName, limit, offset, sortColumn, sortDirection, filters);
    return jsonResponse(tableData);
  } catch (err) {
    const errorMessage = err instanceof Error ? err.message : "Unknown error";
    return errorResponse(errorMessage, 400);
  }
});

/**
 * Create a new table
 */
export const createTableRoute = withDatabase(async (context) => {
  const { db, body } = context;
  const data = parseRequestBody(body);
  const result = CreateTableRequestSchema.safeParse(data);

  if (!result.success) {
    return errorResponse(
      `Invalid request body: ${result.error.errors.map(e => e.message).join(", ")}`,
      400
    );
  }

  const { name: tableName, schema } = result.data;

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
  const { params, db } = context;
  const tableNameError = requireTableName(params);
  if (tableNameError) return tableNameError;

  const sql = `DROP TABLE "${params.tableName}"`;
  const startTime = performance.now();

  try {
    db.exec(sql);
    return jsonResponse({ success: true, message: `Table '${params.tableName}' dropped successfully` });
  } catch (err) {
    return handleSQLError(err, sql, startTime);
  }
});

/**
 * Truncate a table (delete all rows)
 */
export const truncateTableRoute = withDatabase(async (context) => {
  const { params, db } = context;
  const tableNameError = requireTableName(params);
  if (tableNameError) return tableNameError;

  const sql = `DELETE FROM "${params.tableName}"`;
  const startTime = performance.now();

  try {
    db.exec(sql);
    return jsonResponse({ success: true, message: `Table '${params.tableName}' truncated successfully` });
  } catch (err) {
    return handleSQLError(err, sql, startTime);
  }
});

