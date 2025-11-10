/// <reference path="../../../../../types/index.d.ts" />

import type { Database } from "elide:sqlite";
import type { DiscoveredDatabase } from "./database.ts";
import { getDatabaseInfo, getTables, getTableData } from "./database.ts";

type ApiResponse = {
  status: number;
  headers: Record<string, string>;
  body: string;
};

type RouteContext = {
  databases: DiscoveredDatabase[];
  Database: typeof Database;
};

type RouteHandler = (params: Record<string, string>, context: RouteContext, body: string) => Promise<ApiResponse>;

type Route = {
  method: string;
  pattern: string;
  handler: RouteHandler;
};

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

function validateAndGetDatabase(
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

async function getDatabaseInfoRoute(params: Record<string, string>, context: RouteContext, _body: string): Promise<ApiResponse> {
  const result = validateAndGetDatabase(params.dbIndex, context.databases);
  if ("error" in result) return result.error;

  const { database } = result;
  const db = new context.Database(database.path);
  const info = getDatabaseInfo(db, database.path);

  const fullInfo = {
    ...info,
    size: database.size,
    lastModified: database.lastModified,
    tableCount: info.tableCount,
  };

  return jsonResponse(fullInfo);
}

async function getTablesRoute(params: Record<string, string>, context: RouteContext, _body: string): Promise<ApiResponse> {
  const result = validateAndGetDatabase(params.dbIndex, context.databases);
  if ("error" in result) return result.error;

  const { database } = result;
  const db = new context.Database(database.path);
  const tables = getTables(db);

  return jsonResponse({ tables });
}

async function getTableDataRoute(params: Record<string, string>, context: RouteContext, _body: string): Promise<ApiResponse> {
  const result = validateAndGetDatabase(params.dbIndex, context.databases);
  if ("error" in result) return result.error;

  const tableName = params.tableName;
  if (!tableName) {
    return errorResponse("Table name is required", 400);
  }

  const { database } = result;
  const db = new context.Database(database.path);
  const tableData = getTableData(db, tableName);

  console.log(tableData);

  return jsonResponse(tableData);
}

async function insertRowsRoute(params: Record<string, string>, context: RouteContext, body: string): Promise<ApiResponse> {
  const result = validateAndGetDatabase(params.dbIndex, context.databases);
  if ("error" in result) return result.error;

  const tableName = params.tableName;
  if (!tableName) {
    return errorResponse("Table name is required", 400);
  }

  const data = parseRequestBody(body);
  const values = data.values as Record<string, unknown> | undefined;

  if (!values || typeof values !== "object") {
    return errorResponse("Request body must contain 'values' object", 400);
  }

  const { database } = result;
  const db = new context.Database(database.path);

  const columns = Object.keys(values);
  const placeholders = columns.map(() => "?").join(", ");
  const sql = `INSERT INTO ${tableName} (${columns.join(", ")}) VALUES (${placeholders})`;

  try {
    const stmt = db.prepare(sql);
    stmt.run(...Object.values(values));
    return jsonResponse({ success: true, message: "Row inserted successfully" });
  } catch (err) {
    const errorMessage = err instanceof Error ? err.message : "Unknown error";
    return errorResponse(`Failed to insert row: ${errorMessage}`, 500);
  }
}

async function updateRowsRoute(params: Record<string, string>, context: RouteContext, body: string): Promise<ApiResponse> {
  const result = validateAndGetDatabase(params.dbIndex, context.databases);
  if ("error" in result) return result.error;

  const tableName = params.tableName;
  if (!tableName) {
    return errorResponse("Table name is required", 400);
  }

  const data = parseRequestBody(body);
  const values = data.values as Record<string, unknown> | undefined;
  const where = data.where as Record<string, unknown> | undefined;

  if (!values || typeof values !== "object") {
    return errorResponse("Request body must contain 'values' object", 400);
  }

  if (!where || typeof where !== "object" || Object.keys(where).length === 0) {
    return errorResponse("Request body must contain 'where' object with at least one condition", 400);
  }

  const { database } = result;
  const db = new context.Database(database.path);

  const setColumns = Object.keys(values).map(key => `${key} = ?`).join(", ");
  const { clause: whereClause, values: whereValues } = buildWhereClause(where);
  const sql = `UPDATE ${tableName} SET ${setColumns} ${whereClause}`;

  try {
    const stmt = db.prepare(sql);
    const allValues = [...Object.values(values), ...whereValues];
    const info = stmt.run(...allValues);
    return jsonResponse({ success: true, rowsAffected: info.changes, message: "Rows updated successfully" });
  } catch (err) {
    const errorMessage = err instanceof Error ? err.message : "Unknown error";
    return errorResponse(`Failed to update rows: ${errorMessage}`, 500);
  }
}

async function deleteRowsRoute(params: Record<string, string>, context: RouteContext, body: string): Promise<ApiResponse> {
  const result = validateAndGetDatabase(params.dbIndex, context.databases);
  if ("error" in result) return result.error;

  const tableName = params.tableName;
  if (!tableName) {
    return errorResponse("Table name is required", 400);
  }

  const data = parseRequestBody(body);
  const where = data.where as Record<string, unknown> | undefined;

  if (!where || typeof where !== "object" || Object.keys(where).length === 0) {
    return errorResponse("Request body must contain 'where' object with at least one condition (safety check)", 400);
  }

  const { database } = result;
  const db = new context.Database(database.path);

  const { clause: whereClause, values: whereValues } = buildWhereClause(where);
  const sql = `DELETE FROM ${tableName} ${whereClause}`;

  try {
    const stmt = db.prepare(sql);
    const info = stmt.run(...whereValues);
    return jsonResponse({ success: true, rowsAffected: info.changes, message: "Rows deleted successfully" });
  } catch (err) {
    const errorMessage = err instanceof Error ? err.message : "Unknown error";
    return errorResponse(`Failed to delete rows: ${errorMessage}`, 500);
  }
}

async function createTableRoute(params: Record<string, string>, context: RouteContext, body: string): Promise<ApiResponse> {
  const result = validateAndGetDatabase(params.dbIndex, context.databases);
  if ("error" in result) return result.error;

  const data = parseRequestBody(body);
  const tableName = data.name as string | undefined;
  const schema = data.schema as Array<{ name: string; type: string; constraints?: string }> | undefined;

  if (!tableName) {
    return errorResponse("Request body must contain 'name' for the table", 400);
  }

  if (!schema || !Array.isArray(schema) || schema.length === 0) {
    return errorResponse("Request body must contain 'schema' array with at least one column", 400);
  }

  const { database } = result;
  const db = new context.Database(database.path);

  const columns = schema.map(col => {
    const constraints = col.constraints ? ` ${col.constraints}` : "";
    return `${col.name} ${col.type}${constraints}`;
  }).join(", ");

  const sql = `CREATE TABLE ${tableName} (${columns})`;

  try {
    db.exec(sql);
    return jsonResponse({ success: true, message: `Table '${tableName}' created successfully` });
  } catch (err) {
    const errorMessage = err instanceof Error ? err.message : "Unknown error";
    return errorResponse(`Failed to create table: ${errorMessage}`, 500);
  }
}

async function dropTableRoute(params: Record<string, string>, context: RouteContext, body: string): Promise<ApiResponse> {
  const result = validateAndGetDatabase(params.dbIndex, context.databases);
  if ("error" in result) return result.error;

  const tableName = params.tableName;
  if (!tableName) {
    return errorResponse("Table name is required", 400);
  }

  const data = parseRequestBody(body);
  const confirm = data.confirm as boolean | undefined;

  if (!confirm) {
    return errorResponse("Must set 'confirm: true' in request body to drop table (safety check)", 400);
  }

  const { database } = result;
  const db = new context.Database(database.path);

  const sql = `DROP TABLE ${tableName}`;

  try {
    db.exec(sql);
    return jsonResponse({ success: true, message: `Table '${tableName}' dropped successfully` });
  } catch (err) {
    const errorMessage = err instanceof Error ? err.message : "Unknown error";
    return errorResponse(`Failed to drop table: ${errorMessage}`, 500);
  }
}

async function executeQueryRoute(params: Record<string, string>, context: RouteContext, body: string): Promise<ApiResponse> {
  const result = validateAndGetDatabase(params.dbIndex, context.databases);
  if ("error" in result) return result.error;

  const data = parseRequestBody(body);
  const sql = data.sql as string | undefined;
  const queryParams = data.params as unknown[] | undefined;

  if (!sql) {
    return errorResponse("Request body must contain 'sql' string", 400);
  }

  if (isDestructiveQuery(sql)) {
    console.warn(`Warning: Executing potentially destructive query: ${sql}`);
  }

  const { database } = result;
  const db = new context.Database(database.path);

  try {
    const stmt = db.prepare(sql);
    const params = queryParams || [];

    if (sql.trim().toLowerCase().startsWith("select")) {
      const rows = stmt.all(...params);
      return jsonResponse({
        success: true,
        rows,
        rowCount: Array.isArray(rows) ? rows.length : 0,
      });
    } else {
      const info = stmt.run(...params);
      return jsonResponse({
        success: true,
        rowsAffected: info.changes,
        lastInsertRowid: info.lastInsertRowid,
      });
    }
  } catch (err) {
    const errorMessage = err instanceof Error ? err.message : "Unknown error";
    return errorResponse(`Failed to execute query: ${errorMessage}`, 500);
  }
}

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
  Database: typeof Database
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
