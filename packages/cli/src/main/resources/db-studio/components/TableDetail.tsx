import { Sidebar } from "./Sidebar.tsx";
import { TopToolbar } from "./TopToolbar.tsx";

export type TableRow = {
  [key: string]: any;
}

export type TableDetailProps = {
  tableName: string;
  columns: string[];
  rows: TableRow[];
  totalRows: number;
  allTables: string[];
  dbIndex?: number;
}

export function TableDetail({ tableName, columns, rows, totalRows, allTables, dbIndex }: TableDetailProps) {
  return (
    <div className="app-container">
      <TopToolbar />
      <div className="app-layout">
        <Sidebar tables={allTables} activeTable={tableName} dbIndex={dbIndex} />

        <div className="main-content">
          <div className="toolbar">
            <div className="toolbar-left">
              <div className="table-name-display">{tableName}</div>
              <div className="row-count">{totalRows} {totalRows === 1 ? 'row' : 'rows'}</div>
            </div>
          </div>

          <div className="table-wrapper">
              <div className="data-table-container">
                <table className="data-table">
                  <thead>
                    <tr>
                      {columns.map((col) => (
                        <th key={col}>{col}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {rows.map((row, idx) => (
                      <tr key={idx}>
                        {columns.map((col) => (
                          <td key={col} className={row[col] === null ? 'null' : ''}>
                            {row[col] === null ? 'NULL' : String(row[col])}
                          </td>
                        ))}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
          </div>
        </div>
      </div>
    </div>
  );
}
