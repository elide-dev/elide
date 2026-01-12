import { useEffect, useRef, useState } from 'react';
import { Terminal } from '@xterm/xterm';
import { FitAddon } from '@xterm/addon-fit';
import '@xterm/xterm/css/xterm.css';
import { useTestContainer } from '../hooks/useTestContainer';
import { useTestWebSocket } from '../hooks/useTestWebSocket';

// Type declarations for Vite-injected build info
declare const __COMMIT_HASH__: string;
declare const __BUILD_TIME__: string;

export function TerminalTest() {
  const terminalRef = useRef<HTMLDivElement>(null);
  const terminal = useRef<Terminal | null>(null);
  const fitAddon = useRef<FitAddon | null>(null);
  const [repoUrl, setRepoUrl] = useState<string>('https://github.com/google/gson.git');
  const [autoRunClaude, setAutoRunClaude] = useState<boolean>(true);
  const [runnerType, setRunnerType] = useState<string>('elide-runner');
  const [elapsedTime, setElapsedTime] = useState<number>(0);
  const [isRunning, setIsRunning] = useState<boolean>(false);
  const timerIntervalRef = useRef<NodeJS.Timeout | null>(null);

  // Container lifecycle management
  const {
    containerId,
    containerStatus,
    loading: containerLoading,
    startContainer: startContainerHook,
    stopContainer: stopContainerHook,
    clearTerminal: clearTerminalHook,
  } = useTestContainer({
    terminal: terminal.current,
    onContainerStarted: () => {
      // Start timer when container is started
      setElapsedTime(0);
      setIsRunning(true);
    },
    onError: (error) => {
      console.error('Container error:', error);
      setIsRunning(false);
    },
  });

  // WebSocket connection management
  const { connected } = useTestWebSocket({
    containerId,
    terminal: terminal.current,
    autoRunClaude,
    enabled: !!containerId,
  });
  const startTimeRef = useRef<number | null>(null);

  // Initialize terminal
  useEffect(() => {
    if (!terminalRef.current) return;

    console.log('Creating terminal...');
    const term = new Terminal({
      cursorBlink: false,
      fontSize: 14,
      fontFamily: 'Menlo, Monaco, "Courier New", monospace',
      theme: {
        background: '#1e1e1e',
        foreground: '#d4d4d4',
      },
      rows: 30,
      cols: 120,
      convertEol: true,
      disableStdin: true,
    });

    const fit = new FitAddon();
    term.loadAddon(fit);
    term.open(terminalRef.current);

    setTimeout(() => {
      fit.fit();
      term.writeln('\x1b[1;32m=== Build Arena Terminal Test ===\x1b[0m');
      term.writeln('\x1b[1;36m📺 View-Only Mode - Watch the build like a movie\x1b[0m');
      term.writeln('Click "Start Container" to launch a test environment\n');
    }, 0);

    terminal.current = term;
    fitAddon.current = fit;

    const handleResize = () => fit.fit();
    window.addEventListener('resize', handleResize);

    return () => {
      console.log('Cleanup: disposing terminal');
      window.removeEventListener('resize', handleResize);
      term.dispose();
      if (timerIntervalRef.current) {
        clearInterval(timerIntervalRef.current);
      }
    };
  }, []);

  // Timer effect
  useEffect(() => {
    if (isRunning) {
      startTimeRef.current = Date.now();
      timerIntervalRef.current = setInterval(() => {
        if (startTimeRef.current) {
          const elapsed = Math.floor((Date.now() - startTimeRef.current) / 1000);
          setElapsedTime(elapsed);
        }
      }, 1000);
    } else {
      if (timerIntervalRef.current) {
        clearInterval(timerIntervalRef.current);
        timerIntervalRef.current = null;
      }
    }

    return () => {
      if (timerIntervalRef.current) {
        clearInterval(timerIntervalRef.current);
      }
    };
  }, [isRunning]);

  // Wrapper functions for button handlers
  const handleStartContainer = async () => {
    await startContainerHook({
      image: `${runnerType}:latest`,
      repoUrl,
      autoRunClaude,
    });
  };

  const handleStopContainer = async () => {
    setIsRunning(false);
    await stopContainerHook();
  };

  // Format elapsed time as MM:SS
  const formatTime = (seconds: number): string => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  return (
    <div className="min-h-screen bg-gradient-to-b from-slate-900 to-slate-800 p-8">
      <div className="max-w-7xl mx-auto">
        <header className="mb-8">
          <h1 className="text-4xl font-bold text-white mb-2">Terminal Test</h1>
          <p className="text-gray-400">Direct terminal connection to Docker container</p>
        </header>

        <div className="bg-slate-800 rounded-lg shadow-xl p-6 mb-4">
          {/* Configuration Section */}
          <div className="mb-6 space-y-4">
            <div>
              <label htmlFor="repoUrl" className="block text-sm font-medium text-gray-300 mb-2">
                Repository URL
              </label>
              <input
                id="repoUrl"
                type="text"
                value={repoUrl}
                onChange={(e) => setRepoUrl(e.target.value)}
                disabled={connected}
                placeholder="https://github.com/user/repo.git"
                className="w-full px-4 py-2 bg-slate-700 border border-slate-600 rounded text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
              />
            </div>
            <div>
              <label htmlFor="runnerType" className="block text-sm font-medium text-gray-300 mb-2">
                Runner Type
              </label>
              <select
                id="runnerType"
                value={runnerType}
                onChange={(e) => setRunnerType(e.target.value)}
                disabled={connected}
                className="w-full px-4 py-2 bg-slate-700 border border-slate-600 rounded text-white focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <option value="elide-runner">🟣 Elide Runner (downloads & installs Elide)</option>
                <option value="standard-runner">🟢 Standard Runner (downloads Maven/Gradle)</option>
                <option value="elide-builder">Legacy: Elide Builder (pre-installed tools)</option>
              </select>
              <p className="text-xs text-gray-500 mt-1">
                {runnerType === 'elide-runner' && 'Will download and install Elide, then build the project'}
                {runnerType === 'standard-runner' && 'Will download and install Maven/Gradle, then build the project'}
                {runnerType === 'elide-builder' && 'Legacy image with pre-installed tools (no installation shown)'}
              </p>
            </div>
            <div className="flex items-center gap-2">
              <input
                id="autoRun"
                type="checkbox"
                checked={autoRunClaude}
                onChange={(e) => setAutoRunClaude(e.target.checked)}
                disabled={connected}
                className="w-4 h-4 text-blue-600 bg-slate-700 border-slate-600 rounded focus:ring-blue-500 focus:ring-2 disabled:opacity-50 disabled:cursor-not-allowed"
              />
              <label htmlFor="autoRun" className="text-sm text-gray-300">
                Auto-run Claude Code to clone and build
              </label>
            </div>
          </div>

          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-4">
              <button
                onClick={handleStartContainer}
                disabled={connected || containerLoading}
                className="px-4 py-2 bg-green-600 hover:bg-green-700 disabled:bg-gray-600 text-white rounded transition-colors disabled:cursor-not-allowed"
              >
                Start Container
              </button>
              <button
                onClick={handleStopContainer}
                disabled={!containerId || containerStatus === 'Stopping...'}
                className="px-4 py-2 bg-red-600 hover:bg-red-700 disabled:bg-gray-600 text-white rounded transition-colors disabled:cursor-not-allowed"
              >
                Stop Container
              </button>
              <button
                onClick={clearTerminalHook}
                className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded transition-colors"
              >
                Clear Terminal
              </button>
            </div>

            <div className="flex items-center gap-4">
              {isRunning && (
                <div className="flex items-center gap-2 bg-slate-700 px-4 py-2 rounded">
                  <svg className="w-4 h-4 text-blue-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                  <span className="text-lg font-mono text-blue-400 font-bold">
                    {formatTime(elapsedTime)}
                  </span>
                </div>
              )}
              <div className="flex items-center gap-2">
                <div
                  className={`w-3 h-3 rounded-full ${
                    connected ? 'bg-green-500' : 'bg-gray-500'
                  }`}
                />
                <span className="text-sm text-gray-400">
                  {connected ? 'Connected' : 'Disconnected'}
                </span>
              </div>
              {containerStatus && (
                <div className="text-sm text-gray-400">
                  Status: <span className="text-white">{containerStatus}</span>
                </div>
              )}
            </div>
          </div>

          {containerId && (
            <div className="text-xs text-gray-500 mb-4">
              Container ID: <code className="bg-slate-700 px-2 py-1 rounded">{containerId}</code>
            </div>
          )}

          <div
            ref={terminalRef}
            className="bg-[#1e1e1e] rounded border border-slate-700"
            style={{ height: '600px' }}
          />
        </div>

        <div className="bg-slate-800 rounded-lg shadow-xl p-6">
          <h2 className="text-xl font-bold text-white mb-4">📺 View-Only Terminal</h2>
          <ul className="space-y-2 text-gray-300">
            <li className="flex items-start gap-2">
              <span className="text-green-500">1.</span>
              <span>Enter a repository URL and check "Auto-run Claude Code"</span>
            </li>
            <li className="flex items-start gap-2">
              <span className="text-green-500">2.</span>
              <span>Click "Start Container" to launch the build environment</span>
            </li>
            <li className="flex items-start gap-2">
              <span className="text-green-500">3.</span>
              <span>Watch as Claude Code automatically clones, analyzes, and builds the project</span>
            </li>
            <li className="flex items-start gap-2">
              <span className="text-green-500">4.</span>
              <span><strong>No keyboard input needed</strong> - Just watch the build like a movie 🍿</span>
            </li>
            <li className="flex items-start gap-2">
              <span className="text-green-500">5.</span>
              <span>Click "Stop Container" when done to clean up resources</span>
            </li>
          </ul>
        </div>

        {/* Version Footer */}
        <div className="mt-4 text-center text-xs text-gray-600">
          Build Arena Terminal • Commit: {__COMMIT_HASH__} • Built: {new Date(__BUILD_TIME__).toLocaleString()}
        </div>
      </div>
    </div>
  );
}
