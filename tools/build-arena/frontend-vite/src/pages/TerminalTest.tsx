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
  useEffect(() => {
    if (!terminalRef.current) return;

    console.log('Creating terminal...');
    // Create terminal
    const term = new Terminal({
      cursorBlink: true,
      fontSize: 14,
      fontFamily: 'Menlo, Monaco, "Courier New", monospace',
      theme: {
        background: '#1e1e1e',
        foreground: '#d4d4d4',
      },
      rows: 30,
      cols: 120,
      convertEol: true,
    });

    const fit = new FitAddon();
    term.loadAddon(fit);
    term.open(terminalRef.current);

    // Small delay to let renderer initialize before fitting
    setTimeout(() => {
      fit.fit();

      // Welcome message
      term.writeln('\x1b[1;32m=== Build Arena Terminal Test ===\x1b[0m');
      term.writeln('Click "Start Container" to launch a test environment\n');

      // Focus terminal
      term.focus();
    }, 0);

    terminal.current = term;
    fitAddon.current = fit;

    // Focus terminal on click
    terminalRef.current.addEventListener('click', () => {
      term.focus();
    });

    // Register terminal input handler
    term.onData((data) => {
      // Send to container via WebSocket if connected
      if (ws.current?.readyState === WebSocket.OPEN) {
        ws.current.send(JSON.stringify({ type: 'input', data }));
      }
    });

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
    };
  }, []);

  const startContainer = async () => {
    if (!terminal.current) return;

    terminal.current.writeln('\x1b[1;33mStarting Docker container...\x1b[0m');
    setContainerStatus('Starting...');

    try {
      // Start a test container
      const response = await fetch('/api/test/start-container', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ image: 'elide-builder:latest' }),
      });

      if (!response.ok) {
        throw new Error(`Failed to start container: ${response.statusText}`);
      }

      const data = await response.json();
      setContainerId(data.containerId);
      setContainerStatus('Running');

      terminal.current.writeln(`\x1b[1;32mContainer started: ${data.containerId}\x1b[0m`);
      terminal.current.writeln('\x1b[1;33mConnecting WebSocket...\x1b[0m\n');

      // Connect WebSocket
      connectWebSocket(data.containerId);
    } catch (error) {
      terminal.current.writeln(`\x1b[1;31mError: ${error instanceof Error ? error.message : 'Unknown error'}\x1b[0m`);
      setContainerStatus('Failed');
    }
  };

  const connectWebSocket = (containerId: string) => {
    if (!terminal.current) return;

    const wsUrl = `ws://localhost:3001/ws/terminal/${containerId}`;
    ws.current = new WebSocket(wsUrl);

    ws.current.onopen = () => {
      terminal.current?.writeln('\x1b[1;32m✓ WebSocket connected\x1b[0m');
      terminal.current?.writeln('\x1b[1;36mType commands and press Enter\x1b[0m\n');
      setConnected(true);

      // Auto-run Claude Code if enabled
      if (autoRunClaude && repoUrl && ws.current) {
        // Wait a moment for the shell to fully initialize
        setTimeout(() => {
          // Start Claude Code in interactive mode
          const claudeCommand = `claude "Clone ${repoUrl}, analyze the project structure, then build it using Elide. Time the build and report the results. Read /workspace/CLAUDE.md for instructions."\n`;
          terminal.current?.writeln(`\x1b[1;35m🤖 Auto-starting Claude Code...\x1b[0m`);
          ws.current?.send(JSON.stringify({ type: 'input', data: claudeCommand }));

          // Auto-answer prompts:
          // After 1.5 seconds, send "1" for dark mode
          setTimeout(() => {
            ws.current?.send(JSON.stringify({ type: 'input', data: '1\n' }));
          }, 1500);

          // After 3 seconds, send Enter to skip account linking
          setTimeout(() => {
            ws.current?.send(JSON.stringify({ type: 'input', data: '\n' }));
          }, 3000);
        }, 2000); // 2 second delay to let bashrc finish
      }

      // Focus the terminal to capture keyboard input
      terminal.current?.focus();
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
          <h2 className="text-xl font-bold text-white mb-4">Test Instructions</h2>
          <ul className="space-y-2 text-gray-300">
            <li className="flex items-start gap-2">
              <span className="text-green-500">1.</span>
              <span>Click "Start Container" to launch a Docker container with Elide builder image</span>
            </li>
            <li className="flex items-start gap-2">
              <span className="text-green-500">2.</span>
              <span>WebSocket will automatically connect to the container's terminal</span>
            </li>
            <li className="flex items-start gap-2">
              <span className="text-green-500">3.</span>
              <span>Try commands: <code className="bg-slate-700 px-2 py-1 rounded text-sm">ls</code>, <code className="bg-slate-700 px-2 py-1 rounded text-sm">pwd</code>, <code className="bg-slate-700 px-2 py-1 rounded text-sm">java -version</code></span>
            </li>
            <li className="flex items-start gap-2">
              <span className="text-green-500">4.</span>
              <span>Test Claude Code if installed: <code className="bg-slate-700 px-2 py-1 rounded text-sm">claude --version</code></span>
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
