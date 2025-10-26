import { ElideLogoGray } from "./ElideLogoGray.tsx";

export type DiscoveredDatabase = {
  path: string;
  name: string;
  size: number;
  lastModified: number;
  isLocal: boolean;
}

export type DatabaseSelectorProps = {
  databases: DiscoveredDatabase[];
}

function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return `${(bytes / Math.pow(k, i)).toFixed(2)} ${sizes[i]}`;
}

function formatDate(timestamp: number): string {
  const date = new Date(timestamp);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMs / 3600000);
  const diffDays = Math.floor(diffMs / 86400000);

  if (diffMins < 1) return 'Just now';
  if (diffMins < 60) return `${diffMins}m ago`;
  if (diffHours < 24) return `${diffHours}h ago`;
  if (diffDays < 7) return `${diffDays}d ago`;

  return date.toLocaleDateString();
}

export function DatabaseSelector({ databases }: DatabaseSelectorProps) {
  return (
    <div className="selector-container">
      <div className="selector-content">
        <div className="selector-header">
          <div className="selector-logo">
            <ElideLogoGray />
          </div>
          <h1>Elide Database Studio</h1>
          <p className="selector-subtitle">
            Select a database to inspect
          </p>
          <p className="selector-count">
            {databases.length} {databases.length === 1 ? 'database' : 'databases'} found
          </p>
        </div>

        <div className="database-grid">
          {databases.map((db, index) => (
            <a
              key={db.path}
              className="database-card"
              href={`/db/${index}`}
            >
              <div className="database-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <ellipse cx="12" cy="5" rx="9" ry="3" />
                  <path d="M3 5v14c0 1.66 4 3 9 3s9-1.34 9-3V5" />
                  <path d="M3 12c0 1.66 4 3 9 3s9-1.34 9-3" />
                </svg>
              </div>
              <div className="database-info">
                <div className="database-name">
                  {db.name}
                  {db.isLocal && <span className="local-badge">Local</span>}
                </div>
                <div className="database-path">{db.path}</div>
                <div className="database-meta">
                  <span className="meta-item">{formatFileSize(db.size)}</span>
                  <span className="meta-divider">•</span>
                  <span className="meta-item">{formatDate(db.lastModified)}</span>
                </div>
              </div>
              <div className="database-arrow">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M9 18l6-6-6-6" />
                </svg>
              </div>
            </a>
          ))}
        </div>
      </div>
    </div>
  );
}
