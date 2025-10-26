import { Sidebar } from "./Sidebar.tsx";
import { TopToolbar } from "./TopToolbar.tsx";
import { WelcomeView } from "./WelcomeView.tsx";

export type DatabaseStudioProps = {
  dbPath: string;
  tables: string[];
  dbIndex?: number;
}

export function DatabaseStudio({ dbPath, tables, dbIndex }: DatabaseStudioProps) {
  return (
    <div className="app-container">
      <TopToolbar  />
      <div className="app-layout">
        <Sidebar tables={tables} dbIndex={dbIndex} />
        <WelcomeView dbPath={dbPath} tables={tables} />
      </div>
    </div>
  );
}
