import { ElideLogoGray } from "./ElideLogoGray.tsx";

export type SidebarProps = {
  tables: string[];
  activeTable?: string;
}

export function Sidebar({ tables, activeTable }: SidebarProps) {
  return (
    <div className="sidebar">
      <div className="sidebar-header">
        <ElideLogoGray />
        <span className="sidebar-title">Database Studio</span>
      </div>
      <div className="sidebar-section">
        {tables.length > 0 ? (
          <>
            <div className="section-label">{tables.length} {tables.length === 1 ? 'table' : 'tables'}</div>
            <ul className="table-list">
              {tables.map((table) => (
                <li key={table} className="table-item">
                  <a
                    href={`/table/${table}`}
                    className={table === activeTable ? 'table-link active' : 'table-link'}
                  >
                    <svg className="table-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 10h18M3 14h18m-9-4v8m-7 0h14a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z" />
                    </svg>
                    <span>{table}</span>
                  </a>
                </li>
              ))}
            </ul>
          </>
        ) : (
          <div className="empty-state">
            <div className="empty-icon">📋</div>
            <div>No tables found</div>
          </div>
        )}
      </div>
    </div>
  );
}
