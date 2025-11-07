import { useEffect, useRef, useState } from 'react';
import { Terminal } from '@xterm/xterm';
import { FitAddon } from '@xterm/addon-fit';
import '@xterm/xterm/css/xterm.css';

export function TerminalTest() {
  const terminalRef = useRef<HTMLDivElement>(null);
  const terminal = useRef<Terminal | null>(null);
  const fitAddon = useRef<FitAddon | null>(null);
  const ws = useRef<WebSocket | null>(null);
  const [connected, setConnected] = useState(false);
  const [containerId, setContainerId] = useState<string>('');
  const [containerStatus, setContainerStatus] = useState<string>('');
  const [repoUrl, setRepoUrl] = useState<string>('https://github.com/google/gson.git');
  const [autoRunClaude, setAutoRunClaude] = useState<boolean>(true);
  const [elapsedTime, setElapsedTime] = useState<number>(0);
  const [isRunning, setIsRunning] = useState<boolean>(false);
  const timerIntervalRef = useRef<NodeJS.Timeout | null>(null);
  const startTimeRef = useRef<number | null>(null);
  useEffect(() => {
    if (!terminalRef.current) return;

    console.log('Creating terminal...');
    // Create terminal (read-only for "watching like a movie")
    const term = new Terminal({
      cursorBlink: false, // No cursor in view-only mode
      fontSize: 14,
      fontFamily: 'Menlo, Monaco, "Courier New", monospace',
      theme: {
        background: '#1e1e1e',
        foreground: '#d4d4d4',
      },
      rows: 30,
      cols: 120,
      convertEol: true,
      disableStdin: true, // Disable keyboard input - view-only
    });

    const fit = new FitAddon();
    term.loadAddon(fit);
    term.open(terminalRef.current);

    // Small delay to let renderer initialize before fitting
    setTimeout(() => {
      fit.fit();

      // Welcome message
      term.writeln('\x1b[1;32m=== Build Arena Terminal Test ===\x1b[0m');
      term.writeln('\x1b[1;36m📺 View-Only Mode - Watch the build like a movie\x1b[0m');
      term.writeln('Click "Start Container" to launch a test environment\n');
    }, 0);

    terminal.current = term;
    fitAddon.current = fit;

    // Handle window resize
    const handleResize = () => {
      fit.fit();
    };
    window.addEventListener('resize', handleResize);

    return () => {
      console.log('Cleanup: disposing terminal');
      window.removeEventListener('resize', handleResize);
      term.dispose();
      ws.current?.close();
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

  const startContainer = async () => {
    if (!terminal.current) return;

    // Start timer
    setElapsedTime(0);
    setIsRunning(true);

    terminal.current.writeln('\x1b[1;33mStarting Docker container...\x1b[0m');
    setContainerStatus('Starting...');

    try {
      // Choose endpoint based on whether auto-run is enabled
      const endpoint = autoRunClaude
        ? '/api/test/start-container-with-minder'
        : '/api/test/start-container';

      if (autoRunClaude) {
        terminal.current.writeln('\x1b[1;35m🤖 Starting with autonomous minder process...\x1b[0m');
      }

      // Start a test container (with or without minder)
      const response = await fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          image: 'elide-builder:latest',
          repoUrl: repoUrl,
        }),
      });

      if (!response.ok) {
        throw new Error(`Failed to start container: ${response.statusText}`);
      }

      const data = await response.json();
      setContainerId(data.containerId);
      setContainerStatus('Running');

      terminal.current.writeln(`\x1b[1;32mContainer started: ${data.containerId}\x1b[0m`);
      if (data.minderPid) {
        terminal.current.writeln(`\x1b[1;35m🤖 Minder process started (PID: ${data.minderPid})\x1b[0m`);
        terminal.current.writeln('\x1b[1;35m✨ Autonomous mode: The minder will handle all Claude Code interactions\x1b[0m');
      }
      terminal.current.writeln('\x1b[1;33mConnecting WebSocket...\x1b[0m');
      terminal.current.writeln('\x1b[1;36m📺 View-only mode - no keyboard input required\x1b[0m\n');

      // Connect WebSocket in monitoring mode (not interactive)
      connectWebSocket(data.containerId);
    } catch (error) {
      terminal.current.writeln(`\x1b[1;31mError: ${error instanceof Error ? error.message : 'Unknown error'}\x1b[0m`);
      setContainerStatus('Failed');
    }
  };

  const connectWebSocket = (containerId: string) => {
    if (!terminal.current) return;

    // Enable recording and set interactive mode based on whether we're using the minder
    // If using minder, we connect in monitoring mode (interactive=false)
    const wsUrl = autoRunClaude
      ? `ws://localhost:3001/ws/terminal/${containerId}?record=true&interactive=false`
      : `ws://localhost:3001/ws/terminal/${containerId}?record=true`;

    ws.current = new WebSocket(wsUrl);
    terminal.current.writeln('\x1b[1;35m🎥 Recording enabled - session will be saved\x1b[0m');

    ws.current.onopen = () => {
      terminal.current?.writeln('\x1b[1;32m✓ WebSocket connected\x1b[0m');
      terminal.current?.writeln('\x1b[1;36m📺 Watching build output in real-time...\x1b[0m\n');
      setConnected(true);

      // If minder is running, it will handle everything automatically
      // If no minder, we're in manual mode (no auto-run from frontend)
    };

    ws.current.onmessage = (event) => {
      const message = JSON.parse(event.data);
      if (message.type === 'output') {
        terminal.current?.write(message.data);
      }
    };

    ws.current.onerror = (error) => {
      terminal.current?.writeln(`\x1b[1;31m✗ WebSocket error: ${error}\x1b[0m`);
      setConnected(false);
    };

    ws.current.onclose = () => {
      terminal.current?.writeln('\n\x1b[1;33m✗ WebSocket disconnected\x1b[0m');
      setConnected(false);
    };
  };

  const stopContainer = async () => {
    if (!containerId || !terminal.current) return;

    // Stop timer
    setIsRunning(false);

    terminal.current.writeln('\n\x1b[1;33mStopping container...\x1b[0m');
    setContainerStatus('Stopping...');

    try {
      await fetch(`/api/test/stop-container/${containerId}`, {
        method: 'POST',
      });

      terminal.current.writeln('\x1b[1;32m✓ Container stopped\x1b[0m');
      setContainerStatus('Stopped');
      ws.current?.close();
      setConnected(false);
      setContainerId('');
    } catch (error) {
      terminal.current.writeln(`\x1b[1;31m✗ Error: ${error instanceof Error ? error.message : 'Unknown error'}\x1b[0m`);
    }
  };

  const clearTerminal = () => {
    terminal.current?.clear();
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
                Auto-run Claude Code to clone and build with Elide
              </label>
            </div>
          </div>

          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-4">
              <button
                onClick={startContainer}
                disabled={connected || containerStatus === 'Starting...'}
                className="px-4 py-2 bg-green-600 hover:bg-green-700 disabled:bg-gray-600 text-white rounded transition-colors disabled:cursor-not-allowed"
              >
                Start Container
              </button>
              <button
                onClick={stopContainer}
                disabled={!containerId || containerStatus === 'Stopping...'}
                className="px-4 py-2 bg-red-600 hover:bg-red-700 disabled:bg-gray-600 text-white rounded transition-colors disabled:cursor-not-allowed"
              >
                Stop Container
              </button>
              <button
                onClick={clearTerminal}
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
      </div>
    </div>
  );
}
