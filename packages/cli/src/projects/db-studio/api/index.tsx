/// <reference path="../../../../../types/index.d.ts" />

import { Database } from "elide:sqlite";
import { startServer } from "./server.ts";
import type { DiscoveredDatabase } from "./database.ts";

/**
 * Database Studio - Entry Point
 *
 * Serves a JSON API wrapper for SQL queries.
 * Delegates to the server module which handles all HTTP routing and API logic.
 */

export { Database };

// Configuration injected by DbStudioCommand.kt
// These are global variables that will be replaced at runtime
declare const __PORT__: number;
declare const __DATABASES__: string;

const port: number = __PORT__;
const databases: DiscoveredDatabase[] = JSON.parse(__DATABASES__) as DiscoveredDatabase[];

// Start the server with injected configuration
startServer({ port, databases, Database });