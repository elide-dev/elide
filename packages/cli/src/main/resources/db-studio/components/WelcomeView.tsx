import { ElideLogoGray } from "./ElideLogoGray.tsx";

export type WelcomeViewProps = {
  dbPath: string;
  tables: string[];
}

export function WelcomeView({ dbPath, tables }: WelcomeViewProps) {
  const dbName = dbPath.split('/').pop() || dbPath;

  return (
    <div className="welcome-container">
      <div className="welcome-content">
        <ElideLogoGray />
        <h1>{dbName}</h1>
        <p className="welcome-subtitle">{dbPath}</p>
        <div className="welcome-stats">
          <div className="stat">
            <div className="stat-value">{tables.length}</div>
            <div className="stat-label">{tables.length === 1 ? 'Table' : 'Tables'}</div>
          </div>
        </div>
      </div>
    </div>
  );
}
