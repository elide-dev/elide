/// <reference path="../../../../../types/index.d.ts" />

import type { Database } from "elide:sqlite";
import type { DiscoveredDatabase } from "./database.ts";
import { getDatabaseInfo, getTables, getTableData } from "./database.ts";

/**
 * HTTP Server Layer - JSON API Only
 *
 * Provides RESTful JSON API endpoints for database operations.
 * The React UI is served separately via a static file server.
 */

export type ServerConfig = {
  port: number;
  databases: DiscoveredDatabase[];
  Database: typeof Database;
};

// Extended HTTP types for route parameters
// Note: These extend the base ElideHttp types from http.d.ts
type RouteContext = {
  params?: Record<string, string>;
};

type ElideHttpResponseExtended = {
  header(name: string, value: string): void;
  send(status: number, body: string): void;
};

/**
 * Get database by index from the discovered list
 */
function getDatabaseByIndex(databases: DiscoveredDatabase[], index: number): DiscoveredDatabase | null {
  if (index < 0 || index >= databases.length) return null;
  return databases[index];
}

/**
 * Start the HTTP server with API and SSR routes
 */
export function startServer({ port, databases, Database }: ServerConfig): void {
  if (!Elide.http) {
    throw new Error("Running under Elide but no server is available: please run with `elide serve`");
  }

  console.log("Server starting with config:", { port, databaseCount: databases?.length });

  // ============================================================================
  // JSON API Routes
  // ============================================================================

  /**
   * GET /api/databases
   * List all discovered databases
   */
  Elide.http.router.handle("GET", "/api/databases", async (request, response: ElideHttpResponseExtended) => {
    try {
      response.header("Content-Type", "application/json");
      response.send(200, JSON.stringify({ databases }));
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : "Unknown error";
      console.error("Error getting databases:", err);
      response.header("Content-Type", "application/json");
      response.send(500, JSON.stringify({ error: errorMessage }));
    }
  });

  /**
   * GET /api/databases/:dbIndex
   * Get metadata for a specific database
   */
  Elide.http.router.handle("GET", "/api/databases/:dbIndex", async (request, response: ElideHttpResponseExtended, context: RouteContext) => {
    try {
      const dbIndexStr = context?.params?.dbIndex || "";
      const dbIndex = parseInt(dbIndexStr, 10);

      if (isNaN(dbIndex)) {
        response.header("Content-Type", "application/json");
        response.send(400, JSON.stringify({ error: "Invalid database index" }));
        return;
      }

      const database = getDatabaseByIndex(databases, dbIndex);
      if (!database) {
        response.header("Content-Type", "application/json");
        response.send(404, JSON.stringify({ error: `Database not found at index ${dbIndex}` }));
        return;
      }

      const db = new Database(database.path);
      const info = getDatabaseInfo(db, database.path);

      const fullInfo = {
        ...info,
        size: database.size,
        lastModified: database.lastModified,
        isLocal: database.isLocal,
        tableCount: info.tableCount, // From getDatabaseInfo
      };

      response.header("Content-Type", "application/json");
      response.send(200, JSON.stringify(fullInfo));
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : "Unknown error";
      console.error("Error getting database info:", err);
      response.header("Content-Type", "application/json");
      response.send(500, JSON.stringify({ error: errorMessage }));
    }
  });

  /**
   * GET /api/databases/:dbIndex/tables
   * List all tables in a database
   */
  Elide.http.router.handle("GET", "/api/databases/:dbIndex/tables", async (request, response: ElideHttpResponseExtended, context: RouteContext) => {
    try {
      const dbIndexStr = context?.params?.dbIndex || "";
      const dbIndex = parseInt(dbIndexStr, 10);

      if (isNaN(dbIndex)) {
        response.header("Content-Type", "application/json");
        response.send(400, JSON.stringify({ error: "Invalid database index" }));
        return;
      }

      const database = getDatabaseByIndex(databases, dbIndex);
      if (!database) {
        response.header("Content-Type", "application/json");
        response.send(404, JSON.stringify({ error: `Database not found at index ${dbIndex}` }));
        return;
      }

      const db = new Database(database.path);
      const tables = getTables(db);
      response.header("Content-Type", "application/json");
      response.send(200, JSON.stringify({ tables }));
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : "Unknown error";
      console.error("Error getting tables:", err);
      response.header("Content-Type", "application/json");
      response.send(500, JSON.stringify({ error: errorMessage }));
    }
  });

  /**
   * GET /api/databases/:dbIndex/tables/:tableName
   * Get data from a specific table
   */
  Elide.http.router.handle("GET", "/api/databases/:dbIndex/tables/:tableName", async (request, response: ElideHttpResponseExtended, context: RouteContext) => {
    try {
      const dbIndexStr = context?.params?.dbIndex || "";
      const dbIndex = parseInt(dbIndexStr, 10);

      if (isNaN(dbIndex)) {
        response.header("Content-Type", "application/json");
        response.send(400, JSON.stringify({ error: "Invalid database index" }));
        return;
      }

      const database = getDatabaseByIndex(databases, dbIndex);
      if (!database) {
        response.header("Content-Type", "application/json");
        response.send(404, JSON.stringify({ error: `Database not found at index ${dbIndex}` }));
        return;
      }

      const tableName = context?.params?.tableName || "";
      if (!tableName) {
        response.header("Content-Type", "application/json");
        response.send(400, JSON.stringify({ error: "Table name is required" }));
        return;
      }

      const db = new Database(database.path);
      const tableData = getTableData(db, tableName);
      response.header("Content-Type", "application/json");
      response.send(200, JSON.stringify(tableData));
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : "Unknown error";
      console.error("Error getting table data:", err);
      response.header("Content-Type", "application/json");
      response.send(500, JSON.stringify({ error: errorMessage }));
    }
  });

  /**
   * GET /health
   * Health check endpoint
   */
  Elide.http.router.handle("GET", "/health", (request, response: ElideHttpResponseExtended) => {
    response.header("Content-Type", "application/json");
    response.send(200, JSON.stringify({ status: "ok" }));
  });


  // ============================================================================
  // Server Configuration
  // ============================================================================

  Elide.http.config.port = port;

  Elide.http.config.onBind(() => {
    console.log(`Database Studio listening at "http://localhost:${port}"! 🚀`);
    console.log(`${databases.length} database(s) available`);
    console.log();
    console.log("API Endpoints:");
    console.log(`  GET /api/databases`);
    console.log(`  GET /api/databases/:dbIndex`);
    console.log(`  GET /api/databases/:dbIndex/tables`);
    console.log(`  GET /api/databases/:dbIndex/tables/:tableName`);
    console.log(`  GET /health`);
  });

  Elide.http.start();
}
