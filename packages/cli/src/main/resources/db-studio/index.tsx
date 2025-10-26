import { renderToString } from "react-dom/server";
import { Database } from "elide:sqlite";
import { HomeView, SelectionView, TableView } from "./App.tsx";
import type { TableRow } from "./components/TableDetail.tsx";
import type { DiscoveredDatabase } from "./components/DatabaseSelector.tsx";

/**
 * Database Studio - Entry Point
 *
 * A web-based database UI for SQLite databases, built with Elide SSR.
 */

// Configuration injected by DbStudioCommand.kt
const port = __PORT__;
const dbPath = "__DB_PATH__";
const databases = "__DATABASES__" as unknown as DiscoveredDatabase[];
const selectionMode = __SELECTION_MODE__;


export interface ServerConfig {
  port: number;
  dbPath: string | null;
  databases: DiscoveredDatabase[];
  selectionMode: boolean;
}

function encodeDbPath(path: string): string {
  const base64 = btoa(path);
  return base64.replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, '');
}

function decodeDbPath(encoded: string): string {
  const base64 = encoded.replace(/-/g, '+').replace(/_/g, '/');
  const padded = base64 + '='.repeat((4 - base64.length % 4) % 4);
  return atob(padded);
}

function getDatabaseByIndex(index: number): DiscoveredDatabase | null {
  if (index < 0 || index >= databases.length) return null;
  return databases[index];
}

async function renderHome(dbPath: string, dbIndex?: number): Promise<string> {
  const db = new Database(dbPath);
  const query = db.query("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name");
  const results = query.all();
  const tables = results.map((row: any) => row.name as string);

  return renderToString(<HomeView dbPath={dbPath} tables={tables} dbIndex={dbIndex} />);
}

async function renderTable(dbPath: string, tableName: string, dbIndex?: number): Promise<string> {
  const db = new Database(dbPath);

  const tablesQuery = db.query("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name");
  const tablesResults = tablesQuery.all();
  const allTables = tablesResults.map((row: any) => row.name as string);

  const dataQuery = db.query(`SELECT * FROM ${tableName} LIMIT 100`);
  const rows = dataQuery.all() as TableRow[];

  const schemaQuery = db.query(`SELECT name FROM pragma_table_info('${tableName}') ORDER BY cid`);
  const schemaResults = schemaQuery.all() as Array<any>;
  const columns = schemaResults.map((col: any) => col.name as string);

  const countQuery = db.query(`SELECT COUNT(*) as count FROM ${tableName}`);
  const countResult = countQuery.get();
  const totalRows = (countResult as any).count;

  return renderToString(
    <TableView
      dbPath={dbPath}
      tableName={tableName}
      columns={columns}
      rows={rows}
      totalRows={totalRows}
      allTables={allTables}
      dbIndex={dbIndex}
    />
  );
}

async function renderSelection(databases: DiscoveredDatabase[]): Promise<string> {
  return renderToString(<SelectionView databases={databases} />);
}

export function startServer({ port, dbPath, databases, selectionMode }: ServerConfig): void {
  if (!Elide.http) {
    throw new Error("Running under Elide but no server is available: please run with `elide serve`");
  }

  Elide.http.router.handle("GET", "/", async (request, response) => {
    try {
      if (!selectionMode && dbPath) {
        const html = await renderHome(dbPath);
        response.header("Content-Type", "text/html; charset=utf-8");
        response.send(200, html);
        return;
      }

      const html = await renderSelection(databases);
      response.header("Content-Type", "text/html; charset=utf-8");
      response.send(200, html);
    } catch (err: any) {
      console.error("Error rendering selection page:", err);
      response.header("Content-Type", "text/plain");
      response.send(500, `Error: ${err.message}\n${err.stack}`);
    }
  });

  Elide.http.router.handle("GET", "/db/:dbIndex", async (request, response, context) => {
    try {
      const dbIndexStr = context?.params?.dbIndex || "";
      const dbIndex = parseInt(dbIndexStr, 10);

      if (isNaN(dbIndex)) {
        response.header("Content-Type", "text/plain");
        response.send(400, "Invalid database index");
        return;
      }

      const database = getDatabaseByIndex(dbIndex);
      if (!database) {
        response.header("Content-Type", "text/plain");
        response.send(404, `Database not found at index ${dbIndex}`);
        return;
      }

      console.log(`[DEBUG] Rendering home for database: ${database.path}`);
      const html = await renderHome(database.path, dbIndex);
      response.header("Content-Type", "text/html; charset=utf-8");
      response.send(200, html);
    } catch (err: any) {
      console.error("Error rendering database home:", err);
      response.header("Content-Type", "text/plain");
      response.send(500, `Error: ${err.message}\n${err.stack}`);
    }
  });

  Elide.http.router.handle("GET", "/db/:dbIndex/table/:tableName", async (request, response, context) => {
    try {
      const dbIndexStr = context?.params?.dbIndex || "";
      const dbIndex = parseInt(dbIndexStr, 10);

      if (isNaN(dbIndex)) {
        response.header("Content-Type", "text/plain");
        response.send(400, "Invalid database index");
        return;
      }

      const database = getDatabaseByIndex(dbIndex);
      if (!database) {
        response.header("Content-Type", "text/plain");
        response.send(404, `Database not found at index ${dbIndex}`);
        return;
      }

      const tableName = context?.params?.tableName || "";
      if (!tableName) {
        response.send(404, "Table not found");
        return;
      }

      const html = await renderTable(database.path, tableName, dbIndex);
      response.header("Content-Type", "text/html; charset=utf-8");
      response.send(200, html);
    } catch (err: any) {
      console.error("Error rendering table:", err);
      response.header("Content-Type", "text/plain");
      response.send(500, `Error: ${err.message}\n${err.stack}`);
    }
  });

  Elide.http.router.handle("GET", "/table/:tableName", async (request, response, context) => {
    try {
      if (!dbPath) {
        response.header("Content-Type", "text/plain");
        response.send(500, "No database selected");
        return;
      }

      const tableName = context?.params?.tableName || "";
      if (!tableName) {
        response.send(404, "Table not found");
        return;
      }

      const html = await renderTable(dbPath, tableName);
      response.header("Content-Type", "text/html; charset=utf-8");
      response.send(200, html);
    } catch (err: any) {
      console.error("Error rendering table:", err);
      response.header("Content-Type", "text/plain");
      response.send(500, `Error: ${err.message}\n${err.stack}`);
    }
  });

  Elide.http.router.handle("GET", "/health", (request, response) => {
    response.header("Content-Type", "application/json");
    response.send(200, JSON.stringify({ status: "ok" }));
  });

  Elide.http.config.port = port;

  Elide.http.config.onBind(() => {
    console.log(`Database Studio listening at "http://localhost:${port}"! 🚀`);
    if (selectionMode) {
      console.log(`Selection mode: ${databases.length} database(s) available`);
    } else {
      console.log(`Database: ${dbPath}`);
    }
  });

  Elide.http.start();
}

startServer({ port, dbPath, databases, selectionMode });
