import type { Route } from "../http/types.ts";
import { healthCheck } from "./health.ts";
import { listDatabases, getDatabaseInfoRoute } from "./databases.ts";
import { getTablesRoute, getTableDataRoute, createTableRoute, dropTableRoute, truncateTableRoute } from "./tables.ts";
import { executeQueryRoute } from "./query.ts";
import { deleteRowsRoute, insertRowRoute } from "./rows.ts";

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

  // Table operations - write (structure-level operations)
  { method: "POST", pattern: "/api/databases/:dbIndex/tables", handler: createTableRoute },
  { method: "DELETE", pattern: "/api/databases/:dbIndex/tables/:tableName", handler: dropTableRoute },
  { method: "POST", pattern: "/api/databases/:dbIndex/tables/:tableName/truncate", handler: truncateTableRoute },

  // Row operations - write (data-level operations)
  { method: "POST", pattern: "/api/databases/:dbIndex/tables/:tableName/rows", handler: insertRowRoute },
  { method: "DELETE", pattern: "/api/databases/:dbIndex/tables/:tableName/rows", handler: deleteRowsRoute },

  // Raw SQL query endpoint (for custom queries)
  { method: "POST", pattern: "/api/databases/:dbIndex/query", handler: executeQueryRoute },
];

