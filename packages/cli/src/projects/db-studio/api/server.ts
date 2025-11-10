/// <reference path="../../../../../types/index.d.ts" />

import type { Database } from "elide:sqlite";
import type { DiscoveredDatabase } from "./database.ts";
import { getDatabaseInfo, getTables, getTableData } from "./database.ts";

/**
 * HTTP Server Layer - JSON API Handler
 *
 * Provides RESTful JSON API endpoints for database operations.
 * Used by the imperative Node.js HTTP server in index.tsx.
 * The React UI is served separately via a static file server.
 */

type ApiResponse = {
  status: number;
  headers: Record<string, string>;
  body: string;
};

/**
 * Get database by index from the discovered list
 */
function getDatabaseByIndex(databases: DiscoveredDatabase[], index: number): DiscoveredDatabase | null {
  if (index < 0 || index >= databases.length) return null;
  return databases[index];
}

/**
 * Helper to create JSON response
 */
function jsonResponse(data: unknown, status: number = 200): ApiResponse {
  return {
    status,
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  };
}

/**
 * Helper to create error response
 */
function errorResponse(message: string, status: number = 500): ApiResponse {
  return jsonResponse({ error: message }, status);
}

/**
 * Match a route pattern with path parameters
 * Returns null if no match, otherwise returns extracted parameters
 */
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
    // ============================================================================
    // Route: GET /health
    // ============================================================================
    if (method === "GET" && path === "/health") {
      return jsonResponse({ status: "ok" });
    }

    // ============================================================================
    // Route: GET /api/databases
    // ============================================================================
    if (method === "GET" && path === "/api/databases") {
      return jsonResponse({ databases });
    }

    // ============================================================================
    // Route: GET /api/databases/:dbIndex
    // ============================================================================
    const dbInfoMatch = matchRoute("/api/databases/:dbIndex", path);
    if (method === "GET" && dbInfoMatch) {
      const dbIndex = parseInt(dbInfoMatch.dbIndex, 10);

      if (isNaN(dbIndex)) {
        return errorResponse("Invalid database index", 400);
      }

      const database = getDatabaseByIndex(databases, dbIndex);
      if (!database) {
        return errorResponse(`Database not found at index ${dbIndex}`, 404);
      }

      const db = new Database(database.path);
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

    // ============================================================================
    // Route: GET /api/databases/:dbIndex/tables
    // ============================================================================
    const tablesMatch = matchRoute("/api/databases/:dbIndex/tables", path);
    if (method === "GET" && tablesMatch) {
      const dbIndex = parseInt(tablesMatch.dbIndex, 10);

      if (isNaN(dbIndex)) {
        return errorResponse("Invalid database index", 400);
      }

      const database = getDatabaseByIndex(databases, dbIndex);
      if (!database) {
        return errorResponse(`Database not found at index ${dbIndex}`, 404);
      }

      const db = new Database(database.path);
      const tables = getTables(db);
      return jsonResponse({ tables });
    }

    // ============================================================================
    // Route: GET /api/databases/:dbIndex/tables/:tableName
    // ============================================================================
    const tableDataMatch = matchRoute("/api/databases/:dbIndex/tables/:tableName", path);
    if (method === "GET" && tableDataMatch) {
      const dbIndex = parseInt(tableDataMatch.dbIndex, 10);

      if (isNaN(dbIndex)) {
        return errorResponse("Invalid database index", 400);
      }

      const database = getDatabaseByIndex(databases, dbIndex);
      if (!database) {
        return errorResponse(`Database not found at index ${dbIndex}`, 404);
      }

      const tableName = tableDataMatch.tableName;
      if (!tableName) {
        return errorResponse("Table name is required", 400);
      }

      const db = new Database(database.path);
      const tableData = getTableData(db, tableName);
      return jsonResponse(tableData);
    }

    // ============================================================================
    // No route matched - return 404
    // ============================================================================
    return { status: 404, headers: {}, body: '' };

  } catch (err) {
    const errorMessage = err instanceof Error ? err.message : "Unknown error";
    console.error("Error handling request:", err);
    return errorResponse(errorMessage, 500);
  }
}
