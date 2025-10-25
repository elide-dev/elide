import { renderToString } from "react-dom/server";
import { Database } from "elide:sqlite";
import { HomeView, TableView } from "./App.tsx";
import type { TableRow } from "./components/TableDetail.tsx";

/**
 * Database Studio - Entry Point
 *
 * A web-based database UI for SQLite databases, built with Elide SSR.
 */

// Configuration injected by DbStudioCommand.kt
const port = __PORT__;
const dbPath = "__DB_PATH__";


export interface ServerConfig {
  port: number;
  dbPath: string;
}

async function renderHome(dbPath: string): Promise<string> {
  const db = new Database(dbPath);
  const query = db.query("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name");
  const results = query.all();
  const tables = results.map((row: any) => row.name as string);

  return renderToString(<HomeView dbPath={dbPath} tables={tables} />);
}

async function renderTable(dbPath: string, tableName: string): Promise<string> {
  const db = new Database(dbPath);

  const tablesQuery = db.query("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name");
  const tablesResults = tablesQuery.all();
  const allTables = tablesResults.map((row: any) => row.name as string);

  const dataQuery = db.query(`SELECT * FROM ${tableName} LIMIT 100`);
  const rows = dataQuery.all() as TableRow[];

  const schemaQuery = db.query(`SELECT name FROM pragma_table_info('${tableName}') ORDER BY cid`);
  /* type any here since there's some weird internal issue about symbols not being  resolved to a concrete type */
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
    />
  );
}

export function startServer({ port, dbPath }: ServerConfig): void {
  if (!Elide.http) {
    throw new Error("Running under Elide but no server is available: please run with `elide serve`");
  }

  Elide.http.router.handle("GET", "/", async (request, response) => {
    try {
      const html = await renderHome(dbPath);
      response.header("Content-Type", "text/html; charset=utf-8");
      response.send(200, html);
    } catch (err: any) {
      console.error("Error rendering home page:", err);
      response.header("Content-Type", "text/plain");
      response.send(500, `Error: ${err.message}\n${err.stack}`);
    }
  });

  Elide.http.router.handle("GET", "/table/:tableName", async (request, response, context) => {
    try {
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
    console.log(`Database: ${dbPath}`);
  });

  Elide.http.start();
}

startServer({ port, dbPath });
