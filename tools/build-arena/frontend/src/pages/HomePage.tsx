import { Link } from 'react-router-dom';
import { useState, useEffect } from 'react';

interface RecentRace {
  jobId: string;
  repositoryUrl: string;
  repositoryName: string;
  status: string;
  startedAt: string;
  completedAt: string;
  hasRecording: boolean;
  elide: { status: string; duration: number } | null;
  standard: { status: string; duration: number } | null;
  winner?: 'elide' | 'standard' | 'tie';
}

export function HomePage() {
  const [recentRaces, setRecentRaces] = useState<RecentRace[]>([]);
  const [loadingRaces, setLoadingRaces] = useState(true);

  useEffect(() => {
    // Fetch recent races
    fetch('/api/races/recent?limit=5')
      .then(res => res.json())
      .then(data => {
        setRecentRaces(data.races || []);
        setLoadingRaces(false);
      })
      .catch(err => {
        console.error('Error loading recent races:', err);
        setLoadingRaces(false);
      });
  }, []);

  return (
    <div className="min-h-screen bg-gradient-to-b from-slate-900 to-slate-800">
      <div className="container mx-auto px-4 py-8">
        <header className="text-center mb-12">
          <h1 className="text-5xl font-bold text-white mb-4">
            Build <span className="text-indigo-400">Arena</span>
          </h1>
          <p className="text-xl text-gray-300 mb-2">
            Watch AI agents battle it out: Elide vs Standard Toolchains
          </p>
          <p className="text-sm text-gray-400">
            Submit a Java repository and watch autonomous agents race to build it
          </p>
        </header>

        {/* Main Navigation Cards */}
        <div className="max-w-4xl mx-auto grid grid-cols-1 md:grid-cols-2 gap-8 mb-12">
          {/* Race Card */}
          <Link
            to="/race"
            className="group bg-slate-800 hover:bg-slate-750 rounded-lg shadow-xl p-8 transition-all transform hover:scale-105"
          >
            <div className="text-center">
              <div className="text-6xl mb-4">🏁</div>
              <h2 className="text-2xl font-bold text-white mb-3">Start a Race</h2>
              <p className="text-gray-300 mb-4">
                Watch Elide race against Maven/Gradle in side-by-side terminals
              </p>
              <ul className="text-sm text-gray-400 text-left space-y-2">
                <li>• Replay previous races instantly</li>
                <li>• Live race view with real-time output</li>
                <li>• Track statistics and win rates</li>
                <li>• Suggested popular repositories</li>
              </ul>
              <div className="mt-6 px-6 py-3 bg-indigo-600 group-hover:bg-indigo-700 text-white font-medium rounded-lg transition-colors inline-block">
                Start Racing
              </div>
            </div>
          </Link>

          {/* Terminal Test Card */}
          <Link
            to="/test/terminal"
            className="group bg-slate-800 hover:bg-slate-750 rounded-lg shadow-xl p-8 transition-all transform hover:scale-105"
          >
            <div className="text-center">
              <div className="text-6xl mb-4">🔧</div>
              <h2 className="text-2xl font-bold text-white mb-3">Terminal Test</h2>
              <p className="text-gray-300 mb-4">
                Test individual runners and watch autonomous builds
              </p>
              <ul className="text-sm text-gray-400 text-left space-y-2">
                <li>• Single terminal view</li>
                <li>• Choose runner type (Elide/Maven/Gradle)</li>
                <li>• Watch builds like a movie</li>
                <li>• Development and debugging</li>
              </ul>
              <div className="mt-6 px-6 py-3 bg-slate-600 group-hover:bg-slate-500 text-white font-medium rounded-lg transition-colors inline-block">
                Open Terminal
              </div>
            </div>
          </Link>
        </div>

        {/* Recent Races Section */}
        {!loadingRaces && recentRaces.length > 0 && (
          <div className="max-w-4xl mx-auto mb-12">
            <h3 className="text-2xl font-semibold text-white mb-6 text-center">Recent Races</h3>
            <div className="space-y-4">
              {recentRaces.map((race) => (
                <Link
                  key={race.jobId}
                  to={`/race/${race.jobId}`}
                  className="block bg-slate-800 hover:bg-slate-750 rounded-lg p-6 transition-all shadow-lg"
                >
                  <div className="flex items-center justify-between">
                    <div className="flex-1">
                      <div className="flex items-center gap-3 mb-2">
                        <h4 className="text-lg font-semibold text-white">{race.repositoryName}</h4>
                        {race.hasRecording && (
                          <span className="px-2 py-1 bg-indigo-600 text-white text-xs font-medium rounded">
                            Replay Available
                          </span>
                        )}
                        {race.winner && (
                          <span className="text-sm">
                            {race.winner === 'elide' && '🏆 Elide'}
                            {race.winner === 'standard' && '🏆 Maven/Gradle'}
                            {race.winner === 'tie' && '🤝 Tie'}
                          </span>
                        )}
                      </div>
                      <p className="text-sm text-gray-400 mb-3">{race.repositoryUrl}</p>
                      <div className="flex gap-6 text-sm">
                        {race.elide && (
                          <div className="flex items-center gap-2">
                            <span className="text-gray-400">Elide:</span>
                            <span className={race.elide.status === 'completed' ? 'text-green-400' : 'text-red-400'}>
                              {race.elide.duration}s
                            </span>
                          </div>
                        )}
                        {race.standard && (
                          <div className="flex items-center gap-2">
                            <span className="text-gray-400">Maven/Gradle:</span>
                            <span className={race.standard.status === 'completed' ? 'text-green-400' : 'text-red-400'}>
                              {race.standard.duration}s
                            </span>
                          </div>
                        )}
                        {race.completedAt && (
                          <div className="text-gray-500">
                            {new Date(race.completedAt).toLocaleString()}
                          </div>
                        )}
                      </div>
                    </div>
                    <div className="text-gray-400">
                      <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                      </svg>
                    </div>
                  </div>
                </Link>
              ))}
            </div>
          </div>
        )}

        {/* Info Section */}
        <div className="max-w-3xl mx-auto text-center">
          <h3 className="text-xl font-semibold text-white mb-4">How It Works</h3>
          <div className="bg-slate-800 rounded-lg p-6 text-left">
            <ol className="space-y-3 text-gray-300">
              <li className="flex items-start gap-3">
                <span className="text-indigo-400 font-bold">1.</span>
                <span>Submit a Java repository URL (GitHub, GitLab, etc.)</span>
              </li>
              <li className="flex items-start gap-3">
                <span className="text-indigo-400 font-bold">2.</span>
                <span>Autonomous agents spin up Docker containers and analyze the project</span>
              </li>
              <li className="flex items-start gap-3">
                <span className="text-indigo-400 font-bold">3.</span>
                <span>Watch in real-time as Elide races against Maven/Gradle</span>
              </li>
              <li className="flex items-start gap-3">
                <span className="text-indigo-400 font-bold">4.</span>
                <span>Compare build times, success rates, and performance metrics</span>
              </li>
            </ol>
          </div>
        </div>
      </div>
    </div>
  )
}
