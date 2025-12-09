import { jsonResponse, handleSQLError, errorResponse, extractErrorMessage } from "../http/responses.ts";
import { withDatabase } from "../http/middleware.ts";
import { requireTableName } from "../utils/validation.ts";
import { parseRequestBody, parseQueryParams } from "../utils/request.ts";
import { getTables, getTableData, getColumnMetadata } from "../database.ts";
import type { Filter } from "../http/schemas.ts";
import { CreateTableRequestSchema, FiltersArraySchema, AlterTableRequestSchema } from "../http/schemas.ts";

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
      console.error("Error parsing where parameter:", err);
      return errorResponse(
        `Failed to parse where parameter: ${extractErrorMessage(err)}`,
        400
      );
    }
  }

  try {
    const tableData = getTableData(db, params.tableName, limit, offset, sortColumn, sortDirection, filters);
    return jsonResponse(tableData);
  } catch (err) {
    console.error("Error getting table data:", err);
    return errorResponse(extractErrorMessage(err), 400);
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

  const tableName = result.data.name;
  let sql: string;

  // Check if using new format (with 'columns') or legacy format (with 'schema')
  if ('columns' in result.data) {
    // New format from table editor
    const { columns } = result.data;

    const columnDefs = columns.map(col => {
      const parts = [`"${col.name}" ${col.type}`];

      if (col.primaryKey) {
        parts.push('PRIMARY KEY');
        // AUTOINCREMENT only valid for INTEGER PRIMARY KEY
        if (col.autoIncrement && col.type === 'INTEGER') {
          parts.push('AUTOINCREMENT');
        }
      }

      if (!col.nullable && !col.primaryKey) parts.push('NOT NULL');
      if (col.unique && !col.primaryKey) parts.push('UNIQUE');

      if (col.defaultValue !== null) {
        const val = typeof col.defaultValue === 'string'
          ? `'${col.defaultValue.replace(/'/g, "''")}'`
          : col.defaultValue;
        parts.push(`DEFAULT ${val}`);
      }

      return parts.join(' ');
    });

    sql = `CREATE TABLE "${tableName}" (${columnDefs.join(', ')})`;
  } else {
    // Legacy format
    const { schema } = result.data;
    const columns = schema.map(col => {
      const constraints = col.constraints ? ` ${col.constraints}` : "";
      return `"${col.name}" ${col.type}${constraints}`;
    }).join(", ");
    sql = `CREATE TABLE "${tableName}" (${columns})`;
  }

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

/**
 * Get table schema (for editing)
 */
export const getTableSchemaRoute = withDatabase(async (context) => {
  const { params, db } = context;
  const tableNameError = requireTableName(params);
  if (tableNameError) return tableNameError;

  try {
    const columns = getColumnMetadata(db, params.tableName);

    // Map to our schema format (simplified from full metadata)
    const schema = columns.map(col => ({
      name: col.name,
      type: col.type,
      nullable: col.nullable,
      primaryKey: col.primaryKey,
      autoIncrement: col.autoIncrement || false,
      unique: col.unique || false,
      defaultValue: col.defaultValue || null,
    }));

    return jsonResponse({
      tableName: params.tableName,
      columns: schema,
    });
  } catch (err) {
    console.error("Error getting table schema:", err);
    return errorResponse(extractErrorMessage(err), 400);
  }
});

/**
 * Alter existing table structure
 */
export const alterTableRoute = withDatabase(async (context) => {
  const { params, db, body } = context;
  const tableNameError = requireTableName(params);
  if (tableNameError) return tableNameError;

  const data = parseRequestBody(body);
  const result = AlterTableRequestSchema.safeParse(data);

  if (!result.success) {
    return errorResponse(
      `Invalid request body: ${result.error.errors.map(e => e.message).join(", ")}`,
      400
    );
  }

  const { operations } = result.data;
  const tableName = params.tableName;
  const executedStatements: string[] = [];

  try {
    // Execute operations in a transaction
    db.transaction(() => {
      for (const op of operations) {
        let sql: string;

        switch (op.type) {
          case 'add_column': {
            const { column } = op;
            const parts = [`"${column.name}" ${column.type}`];

            if (!column.nullable) {
              // NOT NULL requires DEFAULT for existing rows
              if (column.defaultValue === null) {
                throw new Error(
                  `Cannot add NOT NULL column '${column.name}' without a default value`
                );
              }
              parts.push('NOT NULL');
            }

            if (column.defaultValue !== null) {
              const val = typeof column.defaultValue === 'string'
                ? `'${column.defaultValue.replace(/'/g, "''")}'`
                : column.defaultValue;
              parts.push(`DEFAULT ${val}`);
            }

            sql = `ALTER TABLE "${tableName}" ADD COLUMN ${parts.join(' ')}`;
            break;
          }

          case 'drop_column': {
            sql = `ALTER TABLE "${tableName}" DROP COLUMN "${op.columnName}"`;
            break;
          }

          case 'rename_column': {
            sql = `ALTER TABLE "${tableName}" RENAME COLUMN "${op.oldName}" TO "${op.newName}"`;
            break;
          }

          default:
            throw new Error(`Unknown operation type: ${(op as any).type}`);
        }

        db.exec(sql);
        executedStatements.push(sql);
      }
    })();

    return jsonResponse({
      success: true,
      message: `Table '${tableName}' altered successfully`,
      executedStatements,
    });
  } catch (err) {
    console.error("Error altering table:", err);
    const message = extractErrorMessage(err);
    return errorResponse(message, 400);
  }
});

