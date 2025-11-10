/// <reference path="../../../../../types/index.d.ts" />
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

import type { Database, Statement } from "elide:sqlite";

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
  const query: Statement<TableNameRow> = db.query("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name");
  const results = query.all();

  return results.map(({ name }) => {
    const tableName = name;
    const countQuery: Statement<CountRow> = db.query(`SELECT COUNT(*) as count FROM ${tableName}`);
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
  const schemaQuery: Statement<ColumnNameRow> = db.prepare(`SELECT name FROM pragma_table_info('${tableName}') ORDER BY cid`);
  const schemaResults = schemaQuery.all();
  const columns = schemaResults.map((col) => col.name);

  // Get data rows (unknown type since we don't know the schema)
  const dataQuery = db.query(`SELECT * FROM ${tableName} LIMIT ${limit} OFFSET ${offset}`);
  const rows = dataQuery.all();

  // Get total row count
  const countQuery: Statement<CountRow> = db.query(`SELECT COUNT(*) as count FROM ${tableName}`);
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
  const tablesQuery: Statement<CountRow> = db.query("SELECT COUNT(*) as count FROM sqlite_master WHERE type='table'");
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
    const query = db.query("SELECT 1");
    query.get();
    return true;
  } catch (err) {
    return false;
  }
}
