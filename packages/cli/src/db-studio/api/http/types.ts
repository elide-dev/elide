import type { Database } from "elide:sqlite";
import type { DiscoveredDatabase } from "../database.ts";

/**
 * API response structure
 */
export type ApiResponse = {
  status: number;
  headers: Record<string, string>;
  body: string;
};

/**
 * Context passed to all route handlers
 */
export type RouteContext = {
  databases: DiscoveredDatabase[];
  params: Record<string, string>;
  body: string;
  url: string;
};

/**
 * Route handler function signature
 */
export type RouteHandler = (
  context: RouteContext
) => Promise<ApiResponse>;

/**
 * Route definition
 */
export type Route = {
  method: string;
  pattern: string;
  handler: RouteHandler;
};

/**
 * Extended context for database-specific handlers
 */
export type DatabaseHandlerContext = {
  database: DiscoveredDatabase;
  db: Database;
  databases: DiscoveredDatabase[];
  params: Record<string, string>;
  body: string;
  url: string;
};

/**
 * Database-specific handler function signature
 */
export type DatabaseHandler = (
  context: DatabaseHandlerContext
) => Promise<ApiResponse>;

