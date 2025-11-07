'use client'

import { useState } from 'react'
import { RepositoryForm } from '@/components/RepositoryForm'
import { BuildArena } from '@/components/BuildArena'

export default function Home() {
  const [jobId, setJobId] = useState<string | null>(null)

  const handleJobSubmitted = (newJobId: string) => {
    setJobId(newJobId)
  }

  const handleReset = () => {
    setJobId(null)
  }

  return (
    <main className="min-h-screen bg-gradient-to-b from-slate-900 to-slate-800">
      <div className="container mx-auto px-4 py-8">
        <header className="text-center mb-12">
          <h1 className="text-5xl font-bold text-white mb-4">
            Build <span className="text-elide-primary">Arena</span>
          </h1>
          <p className="text-xl text-gray-300">
            Watch Elide and standard toolchains battle it out in real-time
          </p>
        </header>

        {!jobId ? (
          <div className="max-w-2xl mx-auto">
            <RepositoryForm onJobSubmitted={handleJobSubmitted} />
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
    </main>
  )
}
