import { useState, useEffect, useRef, useMemo } from 'react';
import { useSearchParams, useParams, useNavigate } from 'react-router-dom';
import '@xterm/xterm/css/xterm.css';

// Hooks
import { useTerminal } from '../hooks/useTerminal';
import { useRaceTimer } from '../hooks/useRaceTimer';
import { useRaceWebSocket } from '../hooks/useRaceWebSocket';
import { playRaceRecording } from '../hooks/useRaceReplay';

// Components
import { TerminalPanel } from '../components/race/TerminalPanel';
import { RaceStats } from '../components/race/RaceStats';
import { RepositorySuggestions } from '../components/race/RepositorySuggestions';

// Constants & Types
import { pickRandomRepos } from '../constants/repositories';
import type { RaceStatus } from '../types/race';
import { validateRepositoryUrl, normalizeRepositoryUrl } from '../utils/validation';
import { ERROR_MESSAGES, CONFIG } from '../constants/config';

export function RacePage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const { jobId } = useParams<{ jobId?: string }>();
  const navigate = useNavigate();
  const [repoUrl, setRepoUrl] = useState(searchParams.get('repo') || '');
  const [raceStatus, setRaceStatus] = useState<RaceStatus | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [waitingForReady, setWaitingForReady] = useState(false);

  // Pick random examples on component mount
  const suggestedRepos = useMemo(() => pickRandomRepos(), []);

  // Terminal refs
  const elideTerminalRef = useRef<HTMLDivElement>(null);
  const standardTerminalRef = useRef<HTMLDivElement>(null);

  // Initialize terminals
  const elideTerminal = useTerminal(elideTerminalRef, !!raceStatus);
  const standardTerminal = useTerminal(standardTerminalRef, !!raceStatus);

  // Load race by job ID if provided
  useEffect(() => {
    if (!jobId) return;

    const loadRaceById = async () => {
      setLoading(true);
      setError(null);

      try {
        const response = await fetch(`/api/races/${jobId}`);

        if (!response.ok) {
          throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }

        const data = await response.json();

        // If race is live but not ready yet, poll until ready
        if (data.status === 'running' && !data.ready) {
          setWaitingForReady(true);
          setLoading(false);

          // Poll every 1 second until ready
          const pollInterval = setInterval(async () => {
            try {
              const pollResponse = await fetch(`/api/races/${jobId}`);
              if (!pollResponse.ok) {
                clearInterval(pollInterval);
                throw new Error(`HTTP ${pollResponse.status}: ${pollResponse.statusText}`);
              }

              const pollData = await pollResponse.json();

              if (pollData.ready || pollData.status === 'completed') {
                clearInterval(pollInterval);
                setWaitingForReady(false);
                setRaceStatus({
                  ...pollData,
                  mode: pollData.status === 'completed' ? 'replay' : 'live',
                });
              }
            } catch (err) {
              clearInterval(pollInterval);
              const errorMessage =
                err instanceof Error ? err.message : ERROR_MESSAGES.RACE_START_FAILED;
              setError(errorMessage);
              console.error('Error polling race:', err);
            }
          }, 1000);

          // Cleanup on unmount
          return () => clearInterval(pollInterval);
        } else {
          // Race is ready or completed
          setRaceStatus({
            ...data,
            mode: data.status === 'completed' ? 'replay' : 'live',
          });

          // If completed and has recordings, play them
          if (data.status === 'completed' && elideTerminal?.terminal && standardTerminal?.terminal) {
            await playRaceRecording(
              data.jobId,
              elideTerminal.terminal,
              standardTerminal.terminal
            );
          }
        }
      } catch (err) {
        const errorMessage =
          err instanceof Error ? err.message : ERROR_MESSAGES.RACE_START_FAILED;
        setError(errorMessage);
        console.error('Error loading race:', err);
      } finally {
        setLoading(false);
      }
    };

    loadRaceById();
  }, [jobId]); // Only depend on jobId - terminals are checked inside the effect

  // Race timers
  const { elideElapsed, standardElapsed, start: startTimers, reset: resetTimers, setElideElapsed, setStandardElapsed } = useRaceTimer();

  // WebSocket connections for live race
  useRaceWebSocket({
    elideContainerId: raceStatus?.mode === 'live' ? raceStatus.elide.containerId : undefined,
    standardContainerId: raceStatus?.mode === 'live' ? raceStatus.standard.containerId : undefined,
    elideTerminal: elideTerminal?.terminal || null,
    standardTerminal: standardTerminal?.terminal || null,
    enabled: raceStatus?.mode === 'live',
    onElideElapsedUpdate: setElideElapsed,
    onStandardElapsedUpdate: setStandardElapsed,
  });

  // Start timers when live race begins
  useEffect(() => {
    if (raceStatus?.mode === 'live' && raceStatus.status === 'running') {
      // For live races, DON'T use the local timer - rely on WebSocket metadata
      // The WebSocket will provide elapsed time with each message
      console.log('[RacePage] Live race detected - timers will be updated from WebSocket metadata');
      // Initialize to 0, WebSocket will update immediately
      setElideElapsed(0);
      setStandardElapsed(0);
    }
  }, [raceStatus?.mode, raceStatus?.status, raceStatus?.startedAt, setElideElapsed, setStandardElapsed]);

  // Start race
  const startRace = async () => {
    // Validate repository URL
    const validation = validateRepositoryUrl(repoUrl);
    if (!validation.valid) {
      setError(validation.error || ERROR_MESSAGES.INVALID_REPO_URL);
      return;
    }

    const normalizedUrl = normalizeRepositoryUrl(repoUrl);

    setLoading(true);
    setError(null);

    try {
      // Check if race exists
      const checkResponse = await fetch(`/api/races/check?repo=${encodeURIComponent(normalizedUrl)}`);

      if (!checkResponse.ok) {
        throw new Error(`HTTP ${checkResponse.status}: ${checkResponse.statusText}`);
      }

      const checkData = await checkResponse.json();

      if (checkData.exists) {
        // Load replay
        setRaceStatus({
          ...checkData.race,
          mode: 'replay',
        });

        // Play recording once terminals are ready
        if (elideTerminal?.terminal && standardTerminal?.terminal) {
          await playRaceRecording(
            checkData.race.jobId,
            elideTerminal.terminal,
            standardTerminal.terminal
          );

          // Update status to completed after replay
          setRaceStatus((prev) =>
            prev
              ? {
                  ...prev,
                  status: 'completed',
                  elide: { ...prev.elide, status: 'completed' },
                  standard: { ...prev.standard, status: 'completed' },
                }
              : null
          );
        }
      } else {
        // Start new live race
        const startResponse = await fetch('/api/races/start', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ repositoryUrl: normalizedUrl }),
        });

        if (!startResponse.ok) {
          throw new Error(`HTTP ${startResponse.status}: ${startResponse.statusText}`);
        }

        const startData = await startResponse.json();

        setRaceStatus({
          ...startData,
          mode: 'live',
        });

        // Navigate to race URL with jobId
        navigate(`/race/${startData.jobId}`, { replace: true });
        return; // Don't update search params, we're navigating away
      }

      setSearchParams({ repo: normalizedUrl });
    } catch (err) {
      const errorMessage =
        err instanceof Error ? err.message : ERROR_MESSAGES.RACE_START_FAILED;
      setError(errorMessage);
      console.error('Error starting race:', err);
    } finally {
      setLoading(false);
    }
  };

  // Determine winner when both complete
  useEffect(() => {
    if (!raceStatus) return;

    if (raceStatus.elide.status === 'completed' && raceStatus.standard.status === 'completed') {
      const elideDuration = raceStatus.elide.duration || 0;
      const standardDuration = raceStatus.standard.duration || 0;

      let winner: 'elide' | 'standard' | 'tie';
      if (Math.abs(elideDuration - standardDuration) < 1) {
        winner = 'tie';
      } else if (elideDuration < standardDuration) {
        winner = 'elide';
      } else {
        winner = 'standard';
      }

      setRaceStatus((prev) => (prev ? { ...prev, winner, status: 'completed' } : null));
    }
  }, [raceStatus?.elide.status, raceStatus?.standard.status]);

  return (
    <div className="min-h-screen bg-gradient-to-b from-slate-900 to-slate-800 text-white">
      <div className="container mx-auto px-4 py-8">
        {/* Header */}
        <header className="text-center mb-8">
          <h1 className="text-4xl font-bold mb-2">
            Build <span className="text-indigo-400">Arena</span> Race
          </h1>
          <p className="text-gray-300">Watch Elide race against Maven/Gradle</p>
          {/* Build Info */}
          <div className="mt-4 text-xs text-gray-500">
            Build: {__COMMIT_HASH__} • {new Date(__BUILD_TIME__).toLocaleString()}
          </div>
        </header>

        {/* Loading State - when loading race by jobId */}
        {loading && jobId && !raceStatus && (
          <div className="max-w-2xl mx-auto mb-8">
            <div className="bg-slate-800 p-12 rounded-lg shadow-xl text-center">
              <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-500 mb-4"></div>
              <p className="text-lg text-gray-300">Loading race...</p>
            </div>
          </div>
        )}

        {/* Waiting for Ready State - when race exists but containers not ready */}
        {waitingForReady && !raceStatus && (
          <div className="max-w-2xl mx-auto mb-8">
            <div className="bg-slate-800 p-12 rounded-lg shadow-xl text-center">
              <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-500 mb-4"></div>
              <p className="text-lg text-gray-300">Starting containers...</p>
              <p className="text-sm text-gray-500 mt-2">Waiting for runners to initialize</p>
            </div>
          </div>
        )}

        {/* Repository Input */}
        {!raceStatus && !loading && !waitingForReady && (
          <div className="max-w-2xl mx-auto mb-8">
            <div className="bg-slate-800 p-6 rounded-lg shadow-xl">
              <label className="block text-sm font-medium mb-2">Repository URL</label>
              <input
                type="text"
                value={repoUrl}
                onChange={(e) => setRepoUrl(e.target.value)}
                placeholder="https://github.com/google/gson"
                className="w-full px-4 py-2 bg-slate-700 border border-slate-600 rounded-lg focus:outline-none focus:border-indigo-500 text-white mb-4"
              />
              {error && <p className="text-red-400 text-sm mb-4">{error}</p>}
              <button
                onClick={startRace}
                disabled={loading}
                className="w-full px-6 py-3 bg-indigo-600 hover:bg-indigo-700 disabled:bg-slate-700 rounded-lg font-medium transition-colors"
              >
                {loading ? 'Starting Race...' : 'Start Race'}
              </button>

              {/* Suggested Repositories */}
              <RepositorySuggestions suggestions={suggestedRepos} onSelect={setRepoUrl} />
            </div>
          </div>
        )}

        {/* Race Status */}
        {raceStatus && (
          <div className="mb-6">
            <div className="bg-slate-800 p-4 rounded-lg shadow-xl">
              <div className="flex items-center justify-between">
                <div className="flex-1">
                  <h2 className="text-xl font-bold">{raceStatus.repositoryName}</h2>
                  <p className="text-xs text-gray-500 font-mono">{raceStatus.repositoryUrl}</p>
                </div>

                <div className="flex items-center gap-4">
                  <div className="text-right">
                    <p className="text-sm text-gray-400 flex items-center gap-2">
                      {raceStatus.mode === 'replay' ? (
                        <>
                          <span className="inline-block w-2 h-2 rounded-full bg-gray-400"></span>
                          Pre-recorded Race
                        </>
                      ) : (
                        <>
                          <span className="inline-block w-2 h-2 rounded-full bg-green-500 animate-pulse"></span>
                          Live Race
                        </>
                      )}
                    </p>
                  </div>

                  {raceStatus.winner && (
                    <div className="text-right">
                      <p className="text-sm text-gray-400">Winner</p>
                      <p className="text-2xl font-bold">
                        {raceStatus.winner === 'elide' && '🏆 Elide'}
                        {raceStatus.winner === 'standard' && '🏆 Maven/Gradle'}
                        {raceStatus.winner === 'tie' && '🤝 Tie'}
                      </p>
                    </div>
                  )}
                </div>
              </div>

              {/* Stats */}
              {raceStatus.stats && <RaceStats stats={raceStatus.stats} />}
            </div>
          </div>
        )}

        {/* Side-by-side Terminals */}
        {raceStatus && (
          <div className="grid grid-cols-2 gap-4">
            <TerminalPanel
              title="Elide"
              status={raceStatus.elide.status}
              duration={raceStatus.elide.duration}
              elapsedTime={elideElapsed}
              terminalRef={elideTerminalRef}
            />
            <TerminalPanel
              title="Maven/Gradle"
              status={raceStatus.standard.status}
              duration={raceStatus.standard.duration}
              elapsedTime={standardElapsed}
              terminalRef={standardTerminalRef}
            />
          </div>
        )}

        {/* New Race Button */}
        {raceStatus && raceStatus.status === 'completed' && (
          <div className="text-center mt-6">
            <button
              onClick={() => {
                setRaceStatus(null);
                setRepoUrl('');
                setSearchParams({});
                resetTimers();
              }}
              className="px-6 py-3 bg-indigo-600 hover:bg-indigo-700 rounded-lg font-medium transition-colors"
            >
              Try a New Repository
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
