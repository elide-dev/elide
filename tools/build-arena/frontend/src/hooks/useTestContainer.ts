import { useState } from 'react';
import { Terminal } from '@xterm/xterm';

interface UseTestContainerOptions {
  terminal: Terminal | null;
  onContainerStarted?: (containerId: string) => void;
  onError?: (error: string) => void;
}

interface StartContainerOptions {
  image: string;
  repoUrl: string;
  autoRunClaude: boolean;
}

/**
 * Hook to manage test container lifecycle
 */
export function useTestContainer(options: UseTestContainerOptions) {
  const { terminal, onContainerStarted, onError } = options;

  const [containerId, setContainerId] = useState<string>('');
  const [containerStatus, setContainerStatus] = useState<string>('');
  const [loading, setLoading] = useState(false);

  const startContainer = async (startOptions: StartContainerOptions) => {
    if (!terminal) {
      onError?.('Terminal not initialized');
      return;
    }

    setLoading(true);
    terminal.writeln('\x1b[1;33mStarting Docker container...\x1b[0m');
    setContainerStatus('Starting...');

    try {
      // Choose endpoint based on whether auto-run is enabled
      const endpoint = startOptions.autoRunClaude
        ? '/api/test/start-container-with-minder'
        : '/api/test/start-container';

      if (startOptions.autoRunClaude) {
        terminal.writeln('\x1b[1;35m🤖 Starting with autonomous minder process...\x1b[0m');
      }

      // Start a test container (with or without minder)
      const response = await fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          image: startOptions.image,
          repoUrl: startOptions.repoUrl,
        }),
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const data = await response.json();
      setContainerId(data.containerId);
      setContainerStatus('Running');

      terminal.writeln(`\x1b[1;32mContainer started: ${data.containerId}\x1b[0m`);
      if (data.minderPid) {
        terminal.writeln(`\x1b[1;35m🤖 Minder process started (PID: ${data.minderPid})\x1b[0m`);
        terminal.writeln(
          '\x1b[1;35m✨ Autonomous mode: The minder will handle all Claude Code interactions\x1b[0m'
        );
      }
      terminal.writeln('\x1b[1;33mConnecting WebSocket...\x1b[0m');
      terminal.writeln('\x1b[1;36m📺 View-only mode - no keyboard input required\x1b[0m\n');

      onContainerStarted?.(data.containerId);
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Unknown error';
      terminal.writeln(`\x1b[1;31mError: ${errorMessage}\x1b[0m`);
      setContainerStatus('Failed');
      onError?.(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const stopContainer = async () => {
    if (!containerId || !terminal) return;

    terminal.writeln('\n\x1b[1;33mStopping container...\x1b[0m');
    setContainerStatus('Stopping...');

    try {
      const response = await fetch(`/api/test/stop-container/${containerId}`, {
        method: 'POST',
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      terminal.writeln('\x1b[1;32m✓ Container stopped\x1b[0m');
      setContainerStatus('Stopped');
      setContainerId('');
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Unknown error';
      terminal.writeln(`\x1b[1;31mError stopping container: ${errorMessage}\x1b[0m`);
      onError?.(errorMessage);
    }
  };

  const clearTerminal = () => {
    terminal?.clear();
    terminal?.writeln('\x1b[1;32m=== Build Arena Terminal Test ===\x1b[0m');
    terminal?.writeln('\x1b[1;36m📺 View-Only Mode - Watch the build like a movie\x1b[0m');
    terminal?.writeln('Click "Start Container" to launch a test environment\n');
  };

  return {
    containerId,
    containerStatus,
    loading,
    startContainer,
    stopContainer,
    clearTerminal,
  };
}
