import { useRef, useEffect } from 'react';
import { Terminal } from '@xterm/xterm';
import { FitAddon } from '@xterm/addon-fit';

export interface TerminalInstance {
  terminal: Terminal;
  fitAddon: FitAddon;
}

interface UseTerminalOptions {
  fontSize?: number;
  rows?: number;
}

/**
 * Hook to manage xterm.js terminal instances
 */
export function useTerminal(
  elementRef: React.RefObject<HTMLDivElement>,
  enabled: boolean,
  options: UseTerminalOptions = {}
): TerminalInstance | null {
  const terminal = useRef<Terminal | null>(null);
  const fitAddon = useRef<FitAddon | null>(null);

  useEffect(() => {
    // Wait for next tick to ensure DOM is ready
    if (!enabled) return;

    const initializeTerminal = () => {
      // Double-check ref exists and not already initialized
      if (!elementRef.current || terminal.current) return;

      console.log('Initializing terminal...');

      // Create terminal with wider columns
      terminal.current = new Terminal({
        cursorBlink: true,
        fontSize: options.fontSize || 13,
        fontFamily: 'Menlo, Monaco, "Courier New", monospace',
        theme: {
          background: '#1e293b',
          foreground: '#e2e8f0',
        },
        rows: options.rows || 30,
        cols: 120, // Wider terminal to fit Claude's boxes
      });

      // Create and load fit addon
      fitAddon.current = new FitAddon();
      terminal.current.loadAddon(fitAddon.current);

      // Open terminal in DOM element
      terminal.current.open(elementRef.current);
      // Don't call fit() on init - use our explicit cols: 120 setting
      // fitAddon.current.fit();

      console.log('Terminal initialized!');
    };

    // Use setTimeout to wait for DOM to be ready
    const timeoutId = setTimeout(initializeTerminal, 0);

    // Resize handling disabled: We want fixed cols: 120, not responsive width

    return () => {
      clearTimeout(timeoutId);
      terminal.current?.dispose();
      terminal.current = null;
      fitAddon.current = null;
    };
  }, [enabled, elementRef, options.fontSize, options.rows]);

  if (!terminal.current || !fitAddon.current) {
    return null;
  }

  return {
    terminal: terminal.current,
    fitAddon: fitAddon.current,
  };
}
