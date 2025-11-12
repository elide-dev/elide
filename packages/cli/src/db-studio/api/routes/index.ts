import type { Route } from "../http/types.ts";
import { healthCheck } from "./health.ts";
import { listDatabases, getDatabaseInfoRoute } from "./databases.ts";
import { getTablesRoute, getTableDataRoute, createTableRoute, dropTableRoute } from "./tables.ts";
import { insertRowsRoute, updateRowsRoute, deleteRowsRoute } from "./rows.ts";
import { executeQueryRoute } from "./query.ts";

/**
 * Route Registry
 * 
 * All application routes are defined here, mapping HTTP methods and URL patterns
 * to their corresponding handler functions.
 */
export const routes: Route[] = [
  // Health check
  { method: "GET", pattern: "/health", handler: healthCheck },

  // Database operations
  { method: "GET", pattern: "/api/databases", handler: listDatabases },
  { method: "GET", pattern: "/api/databases/:dbIndex", handler: getDatabaseInfoRoute },

  // Table operations - read
  { method: "GET", pattern: "/api/databases/:dbIndex/tables", handler: getTablesRoute },
  { method: "GET", pattern: "/api/databases/:dbIndex/tables/:tableName", handler: getTableDataRoute },

  // Table operations - write
  { method: "POST", pattern: "/api/databases/:dbIndex/tables", handler: createTableRoute },
  { method: "DELETE", pattern: "/api/databases/:dbIndex/tables/:tableName", handler: dropTableRoute },

  // Row operations
  { method: "POST", pattern: "/api/databases/:dbIndex/tables/:tableName/rows", handler: insertRowsRoute },
  { method: "PATCH", pattern: "/api/databases/:dbIndex/tables/:tableName/rows", handler: updateRowsRoute },
  { method: "DELETE", pattern: "/api/databases/:dbIndex/tables/:tableName/rows", handler: deleteRowsRoute },

  // Raw SQL query endpoint
  { method: "POST", pattern: "/api/databases/:dbIndex/query", handler: executeQueryRoute },
];

