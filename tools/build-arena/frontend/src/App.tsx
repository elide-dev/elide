import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { HomePage } from './pages/HomePage';
import { TerminalTest } from './pages/TerminalTest';
import { RacePage } from './pages/RacePage';
import { ErrorBoundary } from './components/ErrorBoundary';

function App() {
  return (
    <ErrorBoundary>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/race" element={<RacePage />} />
          <Route path="/race/:jobId" element={<RacePage />} />
          <Route path="/test/terminal" element={<TerminalTest />} />
        </Routes>
      </BrowserRouter>
    </ErrorBoundary>
  );
}

export default App;
