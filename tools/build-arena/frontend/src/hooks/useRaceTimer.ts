import { useState, useRef, useEffect, useCallback } from 'react';

export interface RaceTimers {
  elideElapsed: number;
  standardElapsed: number;
  start: (initialElapsedSeconds?: number) => void;
  stop: () => void;
  reset: () => void;
}

/**
 * Hook to manage race timers for both runners
 */
export function useRaceTimer(): RaceTimers {
  const [elideElapsed, setElideElapsed] = useState(0);
  const [standardElapsed, setStandardElapsed] = useState(0);

  const elideStartTime = useRef<number>(0);
  const standardStartTime = useRef<number>(0);
  const timerIntervalRef = useRef<NodeJS.Timeout | null>(null);

  const start = useCallback((initialElapsedSeconds: number = 0) => {
    // Set start time in the past if we have an initial offset
    const now = Date.now();
    const offsetMs = initialElapsedSeconds * 1000;

    elideStartTime.current = now - offsetMs;
    standardStartTime.current = now - offsetMs;
    setElideElapsed(initialElapsedSeconds);
    setStandardElapsed(initialElapsedSeconds);

    if (timerIntervalRef.current) {
      clearInterval(timerIntervalRef.current);
    }

    timerIntervalRef.current = setInterval(() => {
      const elideElapsed = Math.floor((Date.now() - elideStartTime.current) / 1000);
      const standardElapsed = Math.floor((Date.now() - standardStartTime.current) / 1000);
      setElideElapsed(elideElapsed);
      setStandardElapsed(standardElapsed);
    }, 1000);
  }, []);

  const stop = useCallback(() => {
    if (timerIntervalRef.current) {
      clearInterval(timerIntervalRef.current);
      timerIntervalRef.current = null;
    }
  }, []);

  const reset = useCallback(() => {
    stop();
    setElideElapsed(0);
    setStandardElapsed(0);
    elideStartTime.current = 0;
    standardStartTime.current = 0;
  }, [stop]);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      if (timerIntervalRef.current) {
        clearInterval(timerIntervalRef.current);
      }
    };
  }, []);

  return {
    elideElapsed,
    standardElapsed,
    start,
    stop,
    reset,
  };
}

/**
 * Format elapsed time as MM:SS
 */
export function formatTime(seconds: number): string {
  const mins = Math.floor(seconds / 60);
  const secs = seconds % 60;
  return `${mins}:${secs.toString().padStart(2, '0')}`;
}
