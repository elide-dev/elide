/**
 * Database API Layer
 *
 * Provides abstraction over database operations for the DB Studio.
 * This layer isolates all database-specific logic, making it easy to:
 * - Swap SQLite for other databases (PostgreSQL, MySQL, etc.)
 * - Add caching, connection pooling, or other optimizations
 * - Centralize error handling and validation
 *
 * NOTE: This module does NOT import "elide:sqlite" directly because Elide's
 * module loader can only handle that special protocol in the entry point file.
 * The Database class must be passed from index.tsx.
 */

import { Database, Statement } from "elide:sqlite";

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

export interface TableInfo {
  name: string;
  rowCount: number;
}

export type TableData = {
  name: string;
  columns: string[];
  rows: unknown[][];
  totalRows: number;
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
}

interface CountRow {
  count: number;
}

/**
 * Get list of tables in a database
 */
export function getTables(db: Database): TableInfo[] {
  const sql = "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name";
  logQuery(sql);
  const query: Statement<TableNameRow> = db.query(sql);
  const results = query.all();

  return results.map(({ name }) => {
    const tableName = name;
    const countSql = `SELECT COUNT(*) as count FROM ${tableName}`;
    logQuery(countSql);
    const countQuery: Statement<CountRow> = db.query(countSql);
    const countResult = countQuery.get();

    return {
      name: tableName,
      rowCount: countResult?.count ?? 0,
    };
  });
}


interface ColumnNameRow {
  name: string;
}

/**
 * Get table data with schema and rows
 */
export function getTableData(db: Database, tableName: string, limit: number = 100, offset: number = 0): TableData {

  // Get schema (column names)
  const schemaSql = `SELECT name FROM pragma_table_info('${tableName}') ORDER BY cid`;
  logQuery(schemaSql);
  const schemaQuery: Statement<ColumnNameRow> = db.prepare(schemaSql);
  const schemaResults = schemaQuery.all();
  const columns = schemaResults.map((col) => col.name);

  // Get data rows (unknown type since we don't know the schema)
  const dataSql = `SELECT * FROM ${tableName} LIMIT ${limit} OFFSET ${offset}`;
  logQuery(dataSql);
  const dataQuery = db.query(dataSql);
  const rows = dataQuery.all();

  // Get total row count
  const countSql = `SELECT COUNT(*) as count FROM ${tableName}`;
  logQuery(countSql);
  const countQuery: Statement<CountRow> = db.query(countSql);
  const countResult = countQuery.get();
  const totalRows = countResult?.count ?? 0;

  return {
    name: tableName,
    columns,
    rows: rows.map((row: unknown) => columns.map(col => (row as Record<string, unknown>)[col])),
    totalRows,
  };
}

/**
 * Get database metadata
 */
export function getDatabaseInfo(db: Database, dbPath: string): DatabaseInfo {
  const sql = "SELECT COUNT(*) as count FROM sqlite_master WHERE type='table'";
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
 * Execute a raw SQL query (for future query editor feature)
 */
export function executeQuery(db: Database, sql: string, limit: number = 100): { columns: string[], rows: unknown[][] } {
  logQuery(sql);
  const query = db.query(sql);
  const results = query.all();

  if (results.length === 0) {
    return { columns: [], rows: [] };
  }

  const firstRow = results[0] as Record<string, unknown>;
  const columns = Object.keys(firstRow);
  const rows = results.map((row: unknown) => columns.map(col => (row as Record<string, unknown>)[col]));

  return { columns, rows };
}

/**
 * Validate that a database path is accessible
 */
export function validateDatabase(db: Database): boolean {
  try {
    // Try a simple query to verify the database is valid
    const sql = "SELECT 1";
    logQuery(sql);
    const query = db.query(sql);
    query.get();
    return true;
  } catch (err) {
    return false;
  }
}
