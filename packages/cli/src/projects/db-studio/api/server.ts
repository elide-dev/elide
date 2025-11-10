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

type RouteHandler = (params: Record<string, string>, context: RouteContext) => Promise<ApiResponse>;

type Route = {
  method: string;
  pattern: string;
  handler: RouteHandler;
};

function jsonResponse(data: unknown, status: number = 200): ApiResponse {
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

async function listDatabases(_params: Record<string, string>, context: RouteContext): Promise<ApiResponse> {
  return jsonResponse({ databases: context.databases });
}

async function getDatabaseInfoRoute(params: Record<string, string>, context: RouteContext): Promise<ApiResponse> {
  const result = validateAndGetDatabase(params.dbIndex, context.databases);
  if ("error" in result) return result.error;

  const { database } = result;
  const db = new context.Database(database.path);
  const info = getDatabaseInfo(db, database.path);

  const fullInfo = {
    ...info,
    size: database.size,
    lastModified: database.lastModified,
    isLocal: database.isLocal,
    tableCount: info.tableCount,
  };

  return jsonResponse(fullInfo);
}

async function getTablesRoute(params: Record<string, string>, context: RouteContext): Promise<ApiResponse> {
  const result = validateAndGetDatabase(params.dbIndex, context.databases);
  if ("error" in result) return result.error;

  const { database } = result;
  const db = new context.Database(database.path);
  const tables = getTables(db);

  return jsonResponse({ tables });
}

async function getTableDataRoute(params: Record<string, string>, context: RouteContext): Promise<ApiResponse> {
  const result = validateAndGetDatabase(params.dbIndex, context.databases);
  if ("error" in result) return result.error;

  const tableName = params.tableName;
  if (!tableName) {
    return errorResponse("Table name is required", 400);
  }

  const { database } = result;
  const db = new context.Database(database.path);
  const tableData = getTableData(db, tableName);

  return jsonResponse(tableData);
}

/**
 * Route Registry
 */
const routes: Route[] = [
  { method: "GET", pattern: "/api/databases", handler: listDatabases },
  { method: "GET", pattern: "/api/databases/:dbIndex", handler: getDatabaseInfoRoute },
  { method: "GET", pattern: "/api/databases/:dbIndex/tables", handler: getTablesRoute },
  { method: "GET", pattern: "/api/databases/:dbIndex/tables/:tableName", handler: getTableDataRoute },
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
        return await route.handler(params, context);
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
