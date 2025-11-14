import { useRef, useEffect, useState } from 'react';
import { Terminal } from '@xterm/xterm';

interface UseTestWebSocketOptions {
  containerId: string | undefined;
  terminal: Terminal | null;
  autoRunClaude: boolean;
  enabled: boolean;
}

/**
 * Hook to manage WebSocket connection for test terminal
 */
export function useTestWebSocket(options: UseTestWebSocketOptions) {
  const { containerId, terminal, autoRunClaude, enabled } = options;

  const wsRef = useRef<WebSocket | null>(null);
  const [connected, setConnected] = useState(false);

  useEffect(() => {
    if (!enabled || !containerId || !terminal) {
      return;
    }

    // Enable recording and set interactive mode based on whether we're using the minder
    // If using minder, we connect in monitoring mode (interactive=false)
    // Vite will proxy /ws to localhost:3001 in dev mode
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = autoRunClaude
      ? `${protocol}//${window.location.host}/ws/terminal/${containerId}?record=true&interactive=false`
      : `${protocol}//${window.location.host}/ws/terminal/${containerId}?record=true`;

    const websocket = new WebSocket(wsUrl);
    terminal.writeln('\x1b[1;35m🎥 Recording enabled - session will be saved\x1b[0m');

    websocket.onopen = () => {
      terminal.writeln('\x1b[1;32m✓ WebSocket connected\x1b[0m');
      terminal.writeln('\x1b[1;36m📺 Watching build output in real-time...\x1b[0m\n');
      setConnected(true);
    };

    websocket.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data);
        if (message.type === 'output' && message.data) {
          terminal.write(message.data);
        }
      } catch (error) {
        console.error('Failed to parse WebSocket message:', error);
      }
    };

    websocket.onerror = (error) => {
      terminal.writeln(`\x1b[1;31m✗ WebSocket error: ${error}\x1b[0m`);
      setConnected(false);
    };

    websocket.onclose = () => {
      terminal.writeln('\n\x1b[1;33m✗ WebSocket disconnected\x1b[0m');
      setConnected(false);
    };

    wsRef.current = websocket;

    return () => {
      websocket.close();
      wsRef.current = null;
    };
  }, [enabled, containerId, terminal, autoRunClaude]);

  return {
    connected,
    websocket: wsRef.current,
  };
}
