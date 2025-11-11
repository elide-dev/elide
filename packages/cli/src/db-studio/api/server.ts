/// <reference path="../../../../../types/index.d.ts" />

import type { Database } from "elide:sqlite";
import { Database as DatabaseConstructor } from "elide:sqlite";
import type { DiscoveredDatabase } from "./database.ts";
import { getDatabaseInfo, getTables, getTableData } from "./database.ts";

type ApiResponse = {
  status: number;
  headers: Record<string, string>;
  body: string;
};

type RouteContext = {
  databases: DiscoveredDatabase[];
  Database: typeof DatabaseConstructor;
};

type RouteHandler = (params: Record<string, string>, context: RouteContext, body: string) => Promise<ApiResponse>;

type Route = {
  method: string;
  pattern: string;
  handler: RouteHandler;
};

type DatabaseHandlerContext = {
  database: DiscoveredDatabase;
  db: Database;
  databases: DiscoveredDatabase[];
  Database: typeof DatabaseConstructor;
};

type DatabaseHandler = (params: Record<string, string>, context: DatabaseHandlerContext, body: string) => Promise<ApiResponse>;

function jsonResponse(data: unknown, status: number = 200): ApiResponse {
  console.log("returning json response", data);
  return {
    status,
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  };
}

function errorResponse(message: string, status: number = 500): ApiResponse {
  return jsonResponse({ error: message }, status);
}

function validateDatabaseIndex(
  dbIndexStr: string,
  databases: DiscoveredDatabase[]
): { database: DiscoveredDatabase } | { error: ApiResponse } {
  const dbIndex = parseInt(dbIndexStr, 10);

  if (isNaN(dbIndex)) {
    return { error: errorResponse("Invalid database index", 400) };
  }

  if (dbIndex < 0 || dbIndex >= databases.length) {
    return { error: errorResponse(`Database not found at index ${dbIndex}`, 404) };
  }

  return { database: databases[dbIndex] };
}

function withDatabase(handler: DatabaseHandler): RouteHandler {
  return async (params: Record<string, string>, context: RouteContext, body: string): Promise<ApiResponse> => {
    const result = validateDatabaseIndex(params.dbIndex, context.databases);
    if ("error" in result) return result.error;

    const { database } = result;
    const db = new context.Database(database.path);

    return handler(params, { ...context, database, db }, body);
  };
}

function requireTableName(params: Record<string, string>): ApiResponse | null {
  const tableName = params.tableName;
  if (!tableName) {
    return errorResponse("Table name is required", 400);
  }
  return null;
}

function handleDatabaseError(err: unknown, operation: string): ApiResponse {
  const errorMessage = err instanceof Error ? err.message : "Unknown error";
  return errorResponse(`Failed to ${operation}: ${errorMessage}`, 500);
}

function buildWhereClause(where: Record<string, unknown>): { clause: string; values: unknown[] } {
  const conditions: string[] = [];
  const values: unknown[] = [];

  for (const [key, value] of Object.entries(where)) {
    conditions.push(`${key} = ?`);
    values.push(value);
  }

  return {
    clause: conditions.length > 0 ? `WHERE ${conditions.join(" AND ")}` : "",
    values,
  };
}

function isDestructiveQuery(sql: string): boolean {
  const normalized = sql.trim().toLowerCase();
  return (
    normalized.startsWith("drop") ||
    normalized.startsWith("truncate") ||
    (normalized.startsWith("delete") && !normalized.includes("where"))
  );
}

function parseRequestBody(body: string): Record<string, unknown> {
  if (!body || body.trim() === "") {
    return {};
  }
  try {
    return JSON.parse(body);
  } catch {
    return {};
  }
}

function matchRoute(pattern: string, path: string): Record<string, string> | null {
  const patternParts = pattern.split("/").filter(p => p);
  const pathParts = path.split("/").filter(p => p);

  if (patternParts.length !== pathParts.length) return null;

  const params: Record<string, string> = {};

  for (let i = 0; i < patternParts.length; i++) {
    const patternPart = patternParts[i];
    const pathPart = pathParts[i];

    if (patternPart.startsWith(":")) {
      // Parameter segment
      const paramName = patternPart.slice(1);
      params[paramName] = pathPart;
    } else if (patternPart !== pathPart) {
      // Literal segment doesn't match
      return null;
    }
  }

  return params;
}

async function healthCheck(): Promise<ApiResponse> {
  return jsonResponse({ status: "ok" });
}

async function listDatabases(_params: Record<string, string>, context: RouteContext, _body: string): Promise<ApiResponse> {
  return jsonResponse({ databases: context.databases });
}

const getDatabaseInfoRoute = withDatabase(async (_params, context, _body) => {
  const info = getDatabaseInfo(context.db, context.database.path);

  const fullInfo = {
    ...info,
    size: context.database.size,
    lastModified: context.database.lastModified,
    tableCount: info.tableCount,
  };

  return jsonResponse(fullInfo);
});

const getTablesRoute = withDatabase(async (_params, context, _body) => {
  const tables = getTables(context.db);
  return jsonResponse({ tables });
});

const getTableDataRoute = withDatabase(async (params, context, _body) => {
  const tableNameError = requireTableName(params);
  if (tableNameError) return tableNameError;

  const tableData = getTableData(context.db, params.tableName);
  console.log(tableData);
  return jsonResponse(tableData);
});

const insertRowsRoute = withDatabase(async (params, context, body) => {
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

const updateRowsRoute = withDatabase(async (params, context, body) => {
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

const deleteRowsRoute = withDatabase(async (params, context, body) => {
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

const createTableRoute = withDatabase(async (_params, context, body) => {
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

const dropTableRoute = withDatabase(async (params, context, body) => {
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

const executeQueryRoute = withDatabase(async (_params, context, body) => {
  const data = parseRequestBody(body);
  const sql = data.sql as string | undefined;
  const queryParams = data.params as unknown[] | undefined;

  if (!sql) {
    return errorResponse("Request body must contain 'sql' string", 400);
  }

  if (isDestructiveQuery(sql)) {
    console.warn(`Warning: Executing potentially destructive query: ${sql}`);
  }

  try {
    const stmt = context.db.prepare(sql);
    const params = queryParams || [];

    if (sql.trim().toLowerCase().startsWith("select")) {
      const rows = stmt.all(...(params as any));
      return jsonResponse({
        success: true,
        rows,
        rowCount: Array.isArray(rows) ? rows.length : 0,
      });
    } else {
      const info = stmt.run(...(params as any));
      return jsonResponse({
        success: true,
        rowsAffected: info.changes,
        lastInsertRowid: info.lastInsertRowid,
      });
    }
  } catch (err) {
    return handleDatabaseError(err, "execute query");
  }
});

/**
 * Route Registry
 */
const routes: Route[] = [
  // Read operations
  { method: "GET", pattern: "/api/databases", handler: listDatabases },
  { method: "GET", pattern: "/api/databases/:dbIndex", handler: getDatabaseInfoRoute },
  { method: "GET", pattern: "/api/databases/:dbIndex/tables", handler: getTablesRoute },
  { method: "GET", pattern: "/api/databases/:dbIndex/tables/:tableName", handler: getTableDataRoute },

  // Write operations - rows
  { method: "POST", pattern: "/api/databases/:dbIndex/tables/:tableName/rows", handler: insertRowsRoute },
  { method: "PATCH", pattern: "/api/databases/:dbIndex/tables/:tableName/rows", handler: updateRowsRoute },
  { method: "DELETE", pattern: "/api/databases/:dbIndex/tables/:tableName/rows", handler: deleteRowsRoute },

  // Write operations - tables
  { method: "POST", pattern: "/api/databases/:dbIndex/tables", handler: createTableRoute },
  { method: "DELETE", pattern: "/api/databases/:dbIndex/tables/:tableName", handler: dropTableRoute },

  // Raw SQL query endpoint
  { method: "POST", pattern: "/api/databases/:dbIndex/query", handler: executeQueryRoute },
];

/**
 * Handle API requests using Node.js primitives
 */
export async function handleApiRequest(
  url: string,
  method: string,
  body: string,
  databases: DiscoveredDatabase[],
  Database: typeof DatabaseConstructor
): Promise<ApiResponse> {
  // Parse URL path (remove query string if present)
  const path = url.split('?')[0];

  try {
    // Special case: health check endpoint (no params needed)
    if (method === "GET" && path === "/health") {
      return healthCheck();
    }

    // Try to match against registered routes
    const context: RouteContext = { databases, Database };

    for (const route of routes) {
      if (route.method !== method) continue;

      const params = matchRoute(route.pattern, path);
      if (params) {
        return await route.handler(params, context, body);
      }
    }

    // No route matched - return 404
    return { status: 404, headers: {}, body: '' };

  } catch (err) {
    const errorMessage = err instanceof Error ? err.message : "Unknown error";
    console.error("Error handling request:", err);
    return errorResponse(errorMessage, 500);
  }
}
