import { useRef, useEffect } from 'react';
import { Terminal } from '@xterm/xterm';

export interface UseRaceWebSocketParams {
  elideContainerId: string | undefined;
  standardContainerId: string | undefined;
  elideTerminal: Terminal | null;
  standardTerminal: Terminal | null;
  enabled: boolean;
}

/**
 * Hook to manage WebSocket connections for live race viewing
 */
export function useRaceWebSocket(params: UseRaceWebSocketParams) {
  const { elideContainerId, standardContainerId, elideTerminal, standardTerminal, enabled } = params;

  const elideWsRef = useRef<WebSocket | null>(null);
  const standardWsRef = useRef<WebSocket | null>(null);

  useEffect(() => {
    // Only connect if enabled, container IDs are available, AND terminals are ready
    if (!enabled || !elideContainerId || !standardContainerId || !elideTerminal || !standardTerminal) {
      return;
    }

    // Clear terminals and write headers
    elideTerminal.clear();
    standardTerminal.clear();

    elideTerminal.writeln('\x1b[1;32m🏁 ELIDE RUNNER - Live Race\x1b[0m');
    elideTerminal.writeln('\x1b[90m' + '='.repeat(50) + '\x1b[0m\n');

    standardTerminal.writeln('\x1b[1;32m🏁 MAVEN/GRADLE RUNNER - Live Race\x1b[0m');
    standardTerminal.writeln('\x1b[90m' + '='.repeat(50) + '\x1b[0m\n');

    // Connect to Elide WebSocket (view-only mode)
    // Vite will proxy /ws to localhost:3001 in dev mode
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const elideWs = new WebSocket(
      `${protocol}//${window.location.host}/ws/terminal/${elideContainerId}?interactive=false`
    );

    elideWs.onmessage = (event) => {
      const message = JSON.parse(event.data);
      // Only log non-output messages for debugging
      if (message.type !== 'output') {
        console.log('[Elide WS]', message.type, message.message);
      }
      if (message.type === 'output' && message.data && elideTerminal) {
        elideTerminal.write(message.data);
      }
    };

    elideWs.onerror = (error) => {
      console.error('Elide WebSocket error:', error);
    };

    elideWsRef.current = elideWs;

    // Connect to Standard WebSocket (view-only mode)
    const standardWs = new WebSocket(
      `${protocol}//${window.location.host}/ws/terminal/${standardContainerId}?interactive=false`
    );

    standardWs.onmessage = (event) => {
      const message = JSON.parse(event.data);
      // Only log non-output messages for debugging
      if (message.type !== 'output') {
        console.log('[Standard WS]', message.type, message.message);
      }
      if (message.type === 'output' && message.data && standardTerminal) {
        standardTerminal.write(message.data);
      }
    };

    standardWs.onerror = (error) => {
      console.error('Standard WebSocket error:', error);
    };

    standardWsRef.current = standardWs;

    // Cleanup on unmount
    return () => {
      elideWs.close();
      standardWs.close();
      elideWsRef.current = null;
      standardWsRef.current = null;
    };
  }, [enabled, elideContainerId, standardContainerId, elideTerminal, standardTerminal]);

  return {
    elideWs: elideWsRef.current,
    standardWs: standardWsRef.current,
  };
}
