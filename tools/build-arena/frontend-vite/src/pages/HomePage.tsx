import { useState } from 'react'
import { Link } from 'react-router-dom'
import { RepositoryForm } from '../components/RepositoryForm'
import { BuildArena } from '../components/BuildArena'
import { ResultsTable } from '../components/ResultsTable'

export function HomePage() {
  const [jobId, setJobId] = useState<string | null>(null)

  const handleJobSubmitted = (newJobId: string) => {
    setJobId(newJobId)
  }

  const handleReset = () => {
    setJobId(null)
  }

  return (
    <div className="min-h-screen bg-gradient-to-b from-slate-900 to-slate-800">
      <div className="container mx-auto px-4 py-8">
        <header className="text-center mb-12">
          <h1 className="text-5xl font-bold text-white mb-4">
            Build <span className="text-indigo-400">Arena</span>
          </h1>
          <p className="text-xl text-gray-300">
            Watch AI agents battle it out: Elide vs Standard Toolchains
          </p>
          <Link
            to="/test/terminal"
            className="inline-block mt-4 text-sm text-gray-400 hover:text-gray-300 underline"
          >
            Terminal Test Page
          </Link>
        </header>

        {!jobId ? (
          <div>
            <ResultsTable limit={10} />
            <div className="max-w-2xl mx-auto">
              <RepositoryForm onJobSubmitted={handleJobSubmitted} />
            </div>
          </div>
        ) : (
          <div>
            <button
              onClick={handleReset}
              className="mb-4 px-4 py-2 bg-gray-700 hover:bg-gray-600 text-white rounded-lg transition-colors"
            >
              ← New Comparison
            </button>
            <BuildArena jobId={jobId} />
          </div>
        )}
      </div>
    </div>
  )
}
