/**
 * Database API Layer
 *
 * Provides abstraction over database operations for the DB Studio.
 * This layer isolates all database-specific logic, making it easy to:
 * - Swap SQLite for other databases (PostgreSQL, MySQL, etc.)
 * - Add caching, connection pooling, or other optimizations
 * - Centralize error handling and validation
 */

import type { Database, Statement } from "elide:sqlite";
import type { ColumnMetadata, ForeignKeyReference, Filter } from "./http/schemas.ts";

/**
 * Log SQL queries to console
 */
function logQuery(sql: string, params?: unknown[]): void {
  const timestamp = new Date().toISOString();
  const paramsStr = params && params.length > 0 ? ` [${params.join(", ")}]` : "";
  console.log(`[${timestamp}] SQL: ${sql}${paramsStr}`);
}

export interface DiscoveredDatabase {
  path: string;
  name: string;
  size: number;
  lastModified: number;
}

export type TableType = 'table' | 'view';

export interface TableInfo {
  name: string;
  type: TableType;
  rowCount: number;
}

export type TableData = {
  name: string;
  columns: ColumnMetadata[];
  rows: unknown[][];
  totalRows: number;
  metadata: {
    executionTimeMs: number;
    sql: string;
    rowCount: number;
  };
};

export interface DatabaseInfo {
  path: string;
  name: string;
  size: number;
  lastModified: number;
  tableCount: number;
}

interface TableNameRow {
  name: string;
  type: 'table' | 'view';
}

interface CountRow {
  count: number;
}

/**
 * Get list of tables and views in a database
 */
export function getTables(db: Database): TableInfo[] {
  // First query: get all table and view names with their types
  // Exclude SQLite system tables (sqlite_sequence, sqlite_stat1, etc.)
  const tableNamesQuery = "SELECT name, type FROM sqlite_master WHERE type IN ('table', 'view') AND name NOT LIKE 'sqlite_%' ORDER BY name";
  logQuery(tableNamesQuery);
  const tablesQuery: Statement<TableNameRow> = db.query(tableNamesQuery);
  const tables = tablesQuery.all();

  if (tables.length === 0) {
    return [];
  }

  // Second query: get counts for all tables and views in a single UNION ALL query
  const tableCountQuery = tables.map(({ name }) =>
    `SELECT '${name}' as tableName, COUNT(*) as count FROM "${name}"`
  ).join(' UNION ALL ');
  
  logQuery(tableCountQuery);
  
  interface CountResultRow {
    tableName: string;
    count: number;
  }
  
  const countsQuery: Statement<CountResultRow> = db.query(tableCountQuery);
  const counts = countsQuery.all();

  // Create a map for O(1) lookup
  const countMap = new Map(counts.map(({ tableName, count }) => [tableName, count]));

  return tables.map(({ name, type }) => ({
    name,
    type,
    rowCount: countMap.get(name) ?? 0,
  }));
}

interface PragmaTableInfoRow {
  cid: number;
  name: string;
  type: string;
  notnull: number;
  dflt_value: string | null;
  pk: number;
}

interface PragmaForeignKeyRow {
  id: number;
  seq: number;
  table: string;
  from: string;
  to: string;
  on_update: string;
  on_delete: string;
  match: string;
}

interface PragmaIndexListRow {
  seq: number;
  name: string;
  unique: number;
  origin: string;
  partial: number;
}

interface PragmaIndexInfoRow {
  seqno: number;
  cid: number;
  name: string;
}

/**
 * Get comprehensive column metadata for a table
 */
export function getColumnMetadata(db: Database, tableName: string): ColumnMetadata[] {
  // Get basic column information
  const tableInfoSql = `SELECT * FROM pragma_table_info('${tableName}')`;
  logQuery(tableInfoSql);
  const tableInfoQuery: Statement<PragmaTableInfoRow> = db.prepare(tableInfoSql);
  const columns = tableInfoQuery.all();

  // Get foreign key information
  const foreignKeySql = `SELECT * FROM pragma_foreign_key_list('${tableName}')`;
  logQuery(foreignKeySql);
  const foreignKeyQuery: Statement<PragmaForeignKeyRow> = db.prepare(foreignKeySql);
  const foreignKeys = foreignKeyQuery.all();

  // Build foreign key map: column name -> foreign key reference
  const foreignKeyMap = new Map<string, ForeignKeyReference>();
  for (const fk of foreignKeys) {
    foreignKeyMap.set(fk.from, {
      table: fk.table,
      column: fk.to,
      onUpdate: fk.on_update,
      onDelete: fk.on_delete,
    });
  }

  // Get unique constraint information from indexes
  const indexListSql = `SELECT * FROM pragma_index_list('${tableName}')`;
  logQuery(indexListSql);
  const indexListQuery: Statement<PragmaIndexListRow> = db.prepare(indexListSql);
  const indexes = indexListQuery.all();

  // Build set of columns that have unique constraints
  const uniqueColumns = new Set<string>();
  for (const index of indexes) {
    if (index.unique === 1) {
      const indexInfoSql = `SELECT * FROM pragma_index_info('${index.name}')`;
      logQuery(indexInfoSql);
      const indexInfoQuery: Statement<PragmaIndexInfoRow> = db.prepare(indexInfoSql);
      const indexInfo = indexInfoQuery.all();
      
      // Only mark as unique if it's a single-column index
      if (indexInfo.length === 1) {
        const colName = indexInfo[0].name;
        if (colName) {
          uniqueColumns.add(colName);
        }
      }
    }
  }

  // Build the column metadata array
  return columns.map((col): ColumnMetadata => {
    const isAutoIncrement = col.pk === 1 && col.type.toUpperCase() === "INTEGER";
    
    return {
      name: col.name,
      type: col.type,
      nullable: col.notnull === 0,
      primaryKey: col.pk > 0,
      defaultValue: col.dflt_value,
      foreignKey: foreignKeyMap.get(col.name),
      unique: uniqueColumns.has(col.name),
      autoIncrement: isAutoIncrement,
    };
  });
}

/**
 * Build WHERE clause and parameter values from filters
 * Returns an object with the WHERE clause (without the WHERE keyword) and the parameter values
 */
function buildWhereClause(
  filters: Filter[],
  columns: ColumnMetadata[]
): { whereClause: string; params: unknown[] } {
  if (filters.length === 0) {
    return { whereClause: '', params: [] };
  }

  const columnNames = new Set(columns.map(col => col.name));
  const conditions: string[] = [];
  const params: unknown[] = [];

  for (const filter of filters) {
    // Validate column exists
    if (!columnNames.has(filter.column)) {
      throw new Error(`Invalid filter column: "${filter.column}" does not exist in table`);
    }

    const quotedColumn = `"${filter.column}"`;

    switch (filter.operator) {
      case 'eq':
        conditions.push(`${quotedColumn} = ?`);
        params.push(filter.value);
        break;
      case 'neq':
        conditions.push(`${quotedColumn} <> ?`);
        params.push(filter.value);
        break;
      case 'gt':
        conditions.push(`${quotedColumn} > ?`);
        params.push(filter.value);
        break;
      case 'gte':
        conditions.push(`${quotedColumn} >= ?`);
        params.push(filter.value);
        break;
      case 'lt':
        conditions.push(`${quotedColumn} < ?`);
        params.push(filter.value);
        break;
      case 'lte':
        conditions.push(`${quotedColumn} <= ?`);
        params.push(filter.value);
        break;
      case 'like':
        conditions.push(`${quotedColumn} LIKE ?`);
        params.push(filter.value);
        break;
      case 'not_like':
        conditions.push(`${quotedColumn} NOT LIKE ?`);
        params.push(filter.value);
        break;
      case 'in':
        if (!Array.isArray(filter.value)) {
          throw new Error(`Filter operator 'in' requires an array value for column "${filter.column}"`);
        }
        if (filter.value.length === 0) {
          throw new Error(`Filter operator 'in' requires at least one value for column "${filter.column}"`);
        }
        const placeholders = filter.value.map(() => '?').join(', ');
        conditions.push(`${quotedColumn} IN (${placeholders})`);
        params.push(...filter.value);
        break;
      case 'is_null':
        conditions.push(`${quotedColumn} IS NULL`);
        break;
      case 'is_not_null':
        conditions.push(`${quotedColumn} IS NOT NULL`);
        break;
      default:
        throw new Error(`Unsupported filter operator: ${filter.operator}`);
    }
  }

  return {
    whereClause: conditions.join(' AND '),
    params,
  };
}

/**
 * Get table data with schema and rows
 * Supports optional sorting by column name and direction and filtering via WHERE clause
 */
export function getTableData(
  db: Database,
  tableName: string,
  limit: number = 100,
  offset: number = 0,
  sortColumn: string | null = null,
  sortDirection: 'asc' | 'desc' | null = null,
  filters: Filter[] | null = null
): TableData {
  const startTime = performance.now();
  
  // Get column metadata
  const columns = getColumnMetadata(db, tableName);

  // Validate sort column if provided
  if (sortColumn && sortDirection) {
    const columnExists = columns.some(col => col.name === sortColumn);
    if (!columnExists) {
      throw new Error(`Invalid sort column: "${sortColumn}" does not exist in table "${tableName}"`);
    }
  }

  // Build WHERE clause from filters
  const { whereClause, params: whereParams } = filters && filters.length > 0
    ? buildWhereClause(filters, columns)
    : { whereClause: '', params: [] };

  // Build SQL query with optional WHERE and ORDER BY clauses
  let dataSql = `SELECT * FROM "${tableName}"`;
  
  if (whereClause) {
    dataSql += ` WHERE ${whereClause}`;
  }
  
  if (sortColumn && sortDirection) {
    // Column name is validated above, but we still quote it for safety
    dataSql += ` ORDER BY "${sortColumn}" ${sortDirection.toUpperCase()}`;
  }
  
  dataSql += ` LIMIT ${limit} OFFSET ${offset}`;
  
  logQuery(dataSql, whereParams);
  const dataQuery = db.query(dataSql);
  const rows = whereParams.length > 0 ? dataQuery.all(...(whereParams as (string | number | null)[])) : dataQuery.all();

  // Get total row count with same WHERE clause
  let countSql = `SELECT COUNT(*) as count FROM "${tableName}"`;
  if (whereClause) {
    countSql += ` WHERE ${whereClause}`;
  }
  logQuery(countSql, whereParams);
  const countQuery: Statement<CountRow> = db.query(countSql);
  const countResult = whereParams.length > 0 ? countQuery.get(...(whereParams as (string | number | null)[])) : countQuery.get();
  const totalRows = countResult?.count ?? 0;

  const endTime = performance.now();

  return {
    name: tableName,
    columns,
    rows: rows.map((row: unknown) => columns.map(col => (row as Record<string, unknown>)[col.name])),
    totalRows,
    metadata: {
      executionTimeMs: Number((endTime - startTime).toFixed(2)),
      sql: dataSql,
      rowCount: rows.length,
    },
  };
}

/**
 * Get database metadata
 */
export function getDatabaseInfo(db: Database, dbPath: string): DatabaseInfo {
  // Exclude SQLite system tables from count
  const sql = "SELECT COUNT(*) as count FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'";
  logQuery(sql);
  const tablesQuery: Statement<CountRow> = db.query(sql);
  const tablesResult = tablesQuery.get();

  // Extract name from path
  const pathParts = dbPath.split('/');
  const name = pathParts[pathParts.length - 1];

  return {
    path: dbPath,
    name,
    size: 0, // Will be populated by calling code if available
    lastModified: 0, // Will be populated by calling code if available
    tableCount: tablesResult?.count ?? 0,
  };
}

/**
 * Execute a raw SQL query and extract column metadata where possible
 * For ad-hoc queries, we can't extract full metadata, so we provide basic info
 */
export function executeQuery(
  db: Database,
  sql: string
): { columns: ColumnMetadata[], rows: unknown[][], data: Record<string, unknown>[] } {
  logQuery(sql);
  const query = db.query(sql);
  const results = query.all();

  if (results.length === 0) {
    return { columns: [], rows: [], data: [] };
  }

  const firstRow = results[0] as Record<string, unknown>;
  const columnNames = Object.keys(firstRow);
  
  // For ad-hoc queries, we can only infer basic column info from the data
  const columns: ColumnMetadata[] = columnNames.map(name => ({
    name,
    type: inferColumnType(firstRow[name]),
    nullable: true, // We can't know for sure
    primaryKey: false, // We can't determine this for arbitrary queries
  }));

  const rows = results.map((row: unknown) => 
    columnNames.map(col => (row as Record<string, unknown>)[col])
  );

  return { columns, rows, data: results as Record<string, unknown>[] };
}

/**
 * Infer a SQLite type from a JavaScript value
 */
function inferColumnType(value: unknown): string {
  if (value === null || value === undefined) return "NULL";
  if (typeof value === "number") {
    return Number.isInteger(value) ? "INTEGER" : "REAL";
  }
  if (typeof value === "string") return "TEXT";
  if (typeof value === "boolean") return "INTEGER"; // SQLite stores booleans as integers
  return "BLOB";
}

/**
 * Validate that a database path is accessible
 */
export function validateDatabase(db: Database): boolean {
  try {
    // Try a simple query to verify the database is valid
    const sql = "SELECT 1";
    const query = db.query(sql);
    query.get();
    return true;
  } catch (err) {
    return false;
  }
}

/**
 * Format a value for SQL display (shows actual values instead of placeholders)
 */
function formatSqlValue(value: unknown): string {
  if (value === null) return 'NULL';
  if (value === undefined) return 'DEFAULT';
  if (typeof value === 'string') return `'${value.replace(/'/g, "''")}'`;
  if (typeof value === 'number' || typeof value === 'bigint') return String(value);
  if (typeof value === 'boolean') return value ? '1' : '0';
  return `'${String(value)}'`;
}

/**
 * Build a human-readable SQL string with actual values (for error messages)
 */
function buildDisplaySql(sql: string, values: unknown[]): string {
  let displaySql = sql;
  for (const value of values) {
    displaySql = displaySql.replace('?', formatSqlValue(value));
  }
  return displaySql;
}

/**
 * Custom error class that includes SQL context
 */
export class SQLError extends Error {
  public readonly sql: string;
  
  constructor(message: string, sql: string) {
    super(message);
    this.name = 'SQLError';
    this.sql = sql;
  }
}

/**
 * Delete rows from a table based on primary key values
 * @param db Database instance
 * @param tableName Name of the table
 * @param primaryKeys Array of primary key objects (e.g., [{ id: 1 }, { id: 2, name: "foo" }])
 * @returns Object with sql (the first DELETE statement for context)
 * 
 * Note: rowsAffected is not currently available from the elide sqlite library
 */
export function deleteRows(
  db: Database,
  tableName: string,
  primaryKeys: Record<string, unknown>[]
): { sql?: string } {
  if (primaryKeys.length === 0) {
    return {};
  }

  // Get column metadata to identify primary key columns
  const columns = getColumnMetadata(db, tableName);
  const pkColumns = columns.filter(col => col.primaryKey);

  if (pkColumns.length === 0) {
    throw new Error(`Table "${tableName}" has no primary key`);
  }

  // Validate that all primary keys have the required columns
  const pkColumnNames = pkColumns.map(col => col.name);
  for (const pk of primaryKeys) {
    for (const colName of pkColumnNames) {
      if (!(colName in pk)) {
        throw new Error(`Primary key missing required column: "${colName}"`);
      }
    }
  }

  // Build display SQL for the first delete (for error context)
  const firstPk = primaryKeys[0];
  const firstConditions = pkColumns.map(col => `"${col.name}" = ?`).join(' AND ');
  const firstValues = pkColumns.map(col => firstPk[col.name]);
  const firstSql = `DELETE FROM "${tableName}" WHERE ${firstConditions}`;
  const displaySql = buildDisplaySql(firstSql, firstValues);

  try {
    // Execute all delete operations in a transaction for atomicity
    db.transaction(() => {
      for (const pk of primaryKeys) {
        const conditions = pkColumns.map(col => `"${col.name}" = ?`).join(' AND ');
        const values = pkColumns.map(col => pk[col.name]);
        const sql = `DELETE FROM "${tableName}" WHERE ${conditions}`;

        logQuery(sql, values);
        const stmt = db.prepare(sql);
        stmt.run(...(values as (string | number)[]));
      }
    })(); // Execute immediately

    return { sql: displaySql };
  } catch (err) {
    // Re-throw with SQL context
    const message = err instanceof Error ? err.message : String(err);
    throw new SQLError(message, displaySql);
  }
}

/**
 * Insert a new row into a table
 * @param db Database instance
 * @param tableName Name of the table to insert into
 * @param row Object mapping column names to values (undefined values are omitted to use DEFAULT)
 * @returns Object with sql
 * 
 * Note: rowsAffected and lastInsertRowid are not currently available from the elide sqlite library
 */
export function insertRow(
  db: Database,
  tableName: string,
  row: Record<string, unknown>
): { sql: string } {
  // Filter out undefined values (these will use DEFAULT)
  const entries = Object.entries(row).filter(([_, value]) => value !== undefined);

  if (entries.length === 0) {
    throw new Error("No columns specified for insert");
  }

  // Build INSERT query with parameterized values
  const columnNames = entries.map(([key]) => `"${key}"`).join(', ');
  const placeholders = entries.map(() => '?').join(', ');
  const values = entries.map(([_, value]) => value);

  const sql = `INSERT INTO "${tableName}" (${columnNames}) VALUES (${placeholders})`;
  const displaySql = buildDisplaySql(sql, values);

  logQuery(sql, values);
  
  try {
    const stmt = db.prepare(sql);
    stmt.run(...(values as (string | number | null)[]));

    return {
      sql: displaySql,
    };
  } catch (err) {
    // Re-throw with SQL context
    const message = err instanceof Error ? err.message : String(err);
    throw new SQLError(message, displaySql);
  }
}
