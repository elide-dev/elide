import type { DiscoveredDatabase } from "../database.ts";
import type { Database } from "elide:sqlite";

/**
 * Database constructor type
 */
export type DatabaseConstructor = typeof Database;

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
  Database: DatabaseConstructor;
};

/**
 * Route handler function signature
 */
export type RouteHandler = (
  params: Record<string, string>,
  context: RouteContext,
  body: string
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
};

/**
 * Database-specific handler function signature
 */
export type DatabaseHandler = (
  params: Record<string, string>,
  context: DatabaseHandlerContext,
  body: string
) => Promise<ApiResponse>;

