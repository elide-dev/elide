import { Sidebar } from "./Sidebar.tsx";
import { WelcomeView } from "./WelcomeView.tsx";

export type DatabaseStudioProps = {
  dbPath: string;
  tables: string[];
}

export function DatabaseStudio({ dbPath, tables }: DatabaseStudioProps) {
  return (
    <div className="app-layout">
      <Sidebar tables={tables} />
      <WelcomeView dbPath={dbPath} tables={tables} />
    </div>
  );
}
