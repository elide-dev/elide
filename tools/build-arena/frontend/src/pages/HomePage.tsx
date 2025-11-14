import { Link } from 'react-router-dom'

export function HomePage() {
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
