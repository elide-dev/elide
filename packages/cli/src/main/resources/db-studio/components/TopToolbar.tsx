import { ElideLogoGray } from "./ElideLogoGray.tsx";


export function TopToolbar() {
  return (
    <div className="top-toolbar">
      <div className="top-toolbar-left">
        <ElideLogoGray />
        <span className="top-toolbar-title">Database Studio</span>
      </div>
        <div className="top-toolbar-right">
          <a href="/" className="back-button">
            <svg className="back-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
            </svg>
            <span>Go back to database viewer</span>
          </a>
        </div>
    </div>
  );
}
