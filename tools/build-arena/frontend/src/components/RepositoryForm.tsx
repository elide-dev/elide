'use client'

import { useState } from 'react'

interface RepositoryFormProps {
  onJobSubmitted: (jobId: string) => void
}

export function RepositoryForm({ onJobSubmitted }: RepositoryFormProps) {
  const [repoUrl, setRepoUrl] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setLoading(true)

    try {
      const response = await fetch('http://localhost:3001/api/jobs', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ repositoryUrl: repoUrl }),
      })

      if (!response.ok) {
        const errorData = await response.json()
        throw new Error(errorData.error || 'Failed to submit job')
      }

      const data = await response.json()
      onJobSubmitted(data.jobId)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="bg-slate-800 rounded-lg shadow-xl p-8">
      <h2 className="text-2xl font-bold text-white mb-6">
        Submit a Java Repository
      </h2>

      <form onSubmit={handleSubmit} className="space-y-6">
        <div>
          <label htmlFor="repoUrl" className="block text-sm font-medium text-gray-300 mb-2">
            Repository URL
          </label>
          <input
            id="repoUrl"
            type="url"
            value={repoUrl}
            onChange={(e) => setRepoUrl(e.target.value)}
            placeholder="https://github.com/username/repo.git"
            required
            className="w-full px-4 py-3 bg-slate-700 border border-slate-600 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-elide-primary focus:border-transparent"
          />
          <p className="mt-2 text-sm text-gray-400">
            Enter a public GitHub, GitLab, or Git repository URL with a Maven or Gradle build
          </p>
        </div>

        {error && (
          <div className="bg-red-900/50 border border-red-700 text-red-200 px-4 py-3 rounded-lg">
            {error}
          </div>
        )}

        <button
          type="submit"
          disabled={loading || !repoUrl}
          className="w-full px-6 py-3 bg-elide-primary hover:bg-elide-secondary disabled:bg-gray-600 text-white font-semibold rounded-lg transition-colors duration-200 disabled:cursor-not-allowed"
        >
          {loading ? 'Submitting...' : 'Start Build Comparison'}
        </button>
      </form>

      <div className="mt-8 pt-6 border-t border-slate-700">
        <h3 className="text-lg font-semibold text-white mb-3">Example Repositories</h3>
        <div className="space-y-2">
          <button
            onClick={() => setRepoUrl('https://github.com/spring-projects/spring-petclinic.git')}
            className="block w-full text-left px-4 py-2 bg-slate-700 hover:bg-slate-600 text-gray-300 rounded transition-colors"
          >
            Spring PetClinic
          </button>
          <button
            onClick={() => setRepoUrl('https://github.com/apache/commons-lang.git')}
            className="block w-full text-left px-4 py-2 bg-slate-700 hover:bg-slate-600 text-gray-300 rounded transition-colors"
          >
            Apache Commons Lang
          </button>
        </div>
      </div>
    </div>
  )
}
