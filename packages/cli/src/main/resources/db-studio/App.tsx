import { DatabaseStudio } from "./components/DatabaseStudio.tsx";
import { TableDetail, type TableRow } from "./components/TableDetail.tsx";

export type AppProps = {
  title: string;
  children: any;
}

export function App({ title, children }: AppProps) {
  return (
    <html lang="en">
      <head>
        <meta charSet="UTF-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1.0" />
        <title>{title}</title>
        <style>{`
          * { margin: 0; padding: 0; box-sizing: border-box; }
          :root {
            --bg-dark: #0f0f0f; --bg-sidebar: #1a1a1a; --bg-main: #0f0f0f;
            --bg-hover: #252525; --text-primary: #e5e7eb; --text-muted: #9ca3af;
            --border-color: #2a2a2a; --accent: #404040;
          }
          body {
            font-family: -apple-system, BlinkMacSystemFont, Segoe UI, Roboto, Helvetica Neue, Arial, sans-serif;
            background: var(--bg-main); min-height: 100vh; color: var(--text-primary);
            line-height: 1.6; overflow: hidden;
          }
          .app-layout { display: flex; height: 100vh; }
          .sidebar {
            width: 280px; background: var(--bg-sidebar); border-right: 1px solid var(--border-color);
            display: flex; flex-direction: column; overflow: hidden;
          }
          .sidebar-header {
            padding: 1.5rem 1rem; border-bottom: 1px solid var(--border-color);
            display: flex; align-items: center; gap: 0.75rem;
          }
          .sidebar-header svg {
            width: 20px; height: 20px; flex-shrink: 0;
          }
          .sidebar-title { font-size: 1.1rem; font-weight: 700; color: var(--text-primary); }
          .sidebar-section { padding: 1rem 0.5rem; flex: 1; overflow-y: auto; }
          .section-label {
            padding: 0.5rem 0.75rem; font-size: 0.75rem; font-weight: 600;
            color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.05em;
          }
          .table-list { list-style: none; }
          .table-item { margin: 0.25rem 0; }
          .table-link {
            display: flex; align-items: center; gap: 0.75rem; padding: 0.65rem 0.75rem;
            color: var(--text-primary); text-decoration: none; border-radius: 6px;
            transition: all 0.15s ease; font-size: 0.9rem;
          }
          .table-link:hover { background: var(--bg-hover); color: white; }
          .table-link.active { background: var(--accent); color: white; }
          .table-icon { width: 16px; height: 16px; color: var(--text-muted); flex-shrink: 0; }
          .table-link:hover .table-icon, .table-link.active .table-icon { color: white; }
          .welcome-container {
            flex: 1; display: flex; align-items: center; justify-content: center;
            background: var(--bg-main);
          }
          .welcome-content { text-align: center; max-width: 500px; padding: 2rem; }
          .welcome-content svg { margin: 0 auto 1.5rem; width: 64px; height: 64px; }
          .welcome-content h1 {
            font-size: 2.5rem; font-weight: 700; margin-bottom: 0.5rem; color: var(--text-primary);
          }
          .welcome-subtitle { color: var(--text-muted); font-size: 0.9rem; margin-bottom: 2rem; }
          .welcome-stats { display: flex; justify-content: center; gap: 2rem; margin-bottom: 2rem; }
          .stat { text-align: center; }
          .stat-value {
            font-size: 2.5rem; font-weight: 700; color: var(--text-primary);
            line-height: 1; margin-bottom: 0.5rem;
          }
          .stat-label {
            font-size: 0.85rem; color: var(--text-muted);
            text-transform: uppercase; letter-spacing: 0.05em;
          }
          .db-path-display {
            background: var(--bg-sidebar); border: 1px solid var(--border-color);
            border-radius: 8px; padding: 1rem; font-family: Monaco, monospace;
            font-size: 0.85rem; color: var(--text-muted); word-break: break-all;
            display: flex; align-items: center; gap: 0.5rem;
          }
          .db-path-label { font-size: 1.25rem; flex-shrink: 0; }
          .db-path-value { flex: 1; }
          .main-content { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
          .toolbar {
            background: var(--bg-sidebar); border-bottom: 1px solid var(--border-color);
            padding: 1rem 1.5rem; display: flex; align-items: center; justify-content: space-between;
          }
          .toolbar-left { display: flex; align-items: center; gap: 1rem; }
          .table-name-display {
            font-size: 1.25rem; font-weight: 700; color: var(--text-primary);
            font-family: Monaco, monospace;
          }
          .row-count {
            font-size: 0.85rem; color: var(--text-muted);
            padding: 0.25rem 0.75rem; background: var(--bg-dark); border-radius: 4px;
          }
          .table-wrapper { flex: 1; overflow: auto; background: var(--bg-main); }
          .data-table-container { min-width: 100%; display: inline-block; }
          .data-table { width: 100%; border-collapse: collapse; font-size: 0.9rem; }
          .data-table thead { background: var(--bg-sidebar); position: sticky; top: 0; z-index: 10; }
          .data-table th {
            padding: 0.75rem 1rem; text-align: left; font-weight: 600; color: var(--text-muted);
            border-bottom: 1px solid var(--border-color); font-family: Monaco, monospace;
            font-size: 0.8rem; letter-spacing: 0.05em;
          }
          .data-table td {
            padding: 0.75rem 1rem; border-bottom: 1px solid var(--border-color);
            color: var(--text-primary); font-family: Monaco, monospace;
            font-size: 0.85rem; white-space: nowrap;
          }
          .data-table tbody tr:hover { background: var(--bg-hover); }
          .data-table td.null { color: var(--text-muted); font-style: italic; }
          .empty-state { text-align: center; padding: 4rem 2rem; color: var(--text-muted); }
          .empty-icon { font-size: 3rem; margin-bottom: 1rem; opacity: 0.5; }
        `}</style>
      </head>
      <body>
        {children}
      </body>
    </html>
  );
}

export type HomeViewProps = {
  dbPath: string;
  tables: string[];
}

export function HomeView({ dbPath, tables }: HomeViewProps) {
  return (
    <App title="Database Studio · Elide">
      <DatabaseStudio dbPath={dbPath} tables={tables} />
    </App>
  );
}

export type TableViewProps = {
  dbPath: string;
  tableName: string;
  columns: string[];
  rows: TableRow[];
  totalRows: number;
  allTables: string[];
}

export function TableView({ dbPath, tableName, columns, rows, totalRows, allTables }: TableViewProps) {
  return (
    <App title={`${tableName} · Database Studio · Elide`}>
      <TableDetail
        dbPath={dbPath}
        tableName={tableName}
        columns={columns}
        rows={rows}
        totalRows={totalRows}
        allTables={allTables}
      />
    </App>
  );
}
