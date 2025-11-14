import { useState, useMemo } from 'react'

interface RepositoryFormProps {
  onJobSubmitted: (jobId: string) => void
}

// Popular Java repositories categorized by build size
const EXAMPLE_REPOS = {
  small: [
    { name: 'Spring PetClinic', url: 'https://github.com/spring-projects/spring-petclinic.git', time: '<1 min' },
    { name: 'Apache Commons Lang', url: 'https://github.com/apache/commons-lang.git', time: '<1 min' },
    { name: 'Apache Commons IO', url: 'https://github.com/apache/commons-io.git', time: '<1 min' },
    { name: 'Google Gson', url: 'https://github.com/google/gson.git', time: '<1 min' },
    { name: 'Square Okio', url: 'https://github.com/square/okio.git', time: '<1 min' },
    { name: 'Joda Time', url: 'https://github.com/JodaOrg/joda-time.git', time: '<1 min' },
  ],
  medium: [
    { name: 'Google Guava', url: 'https://github.com/google/guava.git', time: '1-5 min' },
    { name: 'Square OkHttp', url: 'https://github.com/square/okhttp.git', time: '1-5 min' },
    { name: 'Square Retrofit', url: 'https://github.com/square/retrofit.git', time: '1-5 min' },
    { name: 'JUnit 5', url: 'https://github.com/junit-team/junit5.git', time: '1-5 min' },
    { name: 'Mockito', url: 'https://github.com/mockito/mockito.git', time: '1-5 min' },
    { name: 'Google Guice', url: 'https://github.com/google/guice.git', time: '1-5 min' },
    { name: 'Jackson Core', url: 'https://github.com/FasterXML/jackson-core.git', time: '1-5 min' },
  ],
  large: [
    { name: 'Spring Boot', url: 'https://github.com/spring-projects/spring-boot.git', time: '5-15 min' },
    { name: 'Spring Framework', url: 'https://github.com/spring-projects/spring-framework.git', time: '5-15 min' },
    { name: 'Apache Kafka', url: 'https://github.com/apache/kafka.git', time: '5-15 min' },
    { name: 'Elasticsearch', url: 'https://github.com/elastic/elasticsearch.git', time: '5-15 min' },
    { name: 'Gradle', url: 'https://github.com/gradle/gradle.git', time: '5-15 min' },
  ],
}

function pickRandomRepos() {
  const pickRandom = <T,>(arr: T[], count: number): T[] => {
    const shuffled = [...arr].sort(() => Math.random() - 0.5)
    return shuffled.slice(0, count)
  }

  return [
    ...pickRandom(EXAMPLE_REPOS.small, 2),
    ...pickRandom(EXAMPLE_REPOS.medium, 2),
    ...pickRandom(EXAMPLE_REPOS.large, 1),
  ]
}

export function RepositoryForm({ onJobSubmitted }: RepositoryFormProps) {
  const [repoUrl, setRepoUrl] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Pick random examples on component mount
  const suggestedRepos = useMemo(() => pickRandomRepos(), [])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setLoading(true)

    try {
      const response = await fetch('/api/jobs', {
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
            className="w-full px-4 py-3 bg-slate-700 border border-slate-600 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
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
          className="w-full px-6 py-3 bg-indigo-600 hover:bg-indigo-700 disabled:bg-gray-600 text-white font-semibold rounded-lg transition-colors duration-200 disabled:cursor-not-allowed"
        >
          {loading ? 'Submitting...' : 'Start AI Build Competition'}
        </button>
      </form>

      <div className="mt-8 pt-6 border-t border-slate-700">
        <h3 className="text-lg font-semibold text-white mb-3">Suggested Repositories</h3>
        <p className="text-sm text-gray-400 mb-4">
          Try these popular Java projects to see Elide's performance improvements
        </p>
        <div className="space-y-2">
          {suggestedRepos.map((repo, index) => (
            <button
              key={index}
              onClick={() => setRepoUrl(repo.url)}
              className="block w-full text-left px-4 py-3 bg-slate-700 hover:bg-slate-600 text-gray-300 rounded-lg transition-colors group"
            >
              <div className="flex items-center justify-between">
                <span className="font-medium group-hover:text-white transition-colors">
                  {repo.name}
                </span>
                <span className="text-xs text-gray-500 bg-slate-800 px-2 py-1 rounded">
                  ~{repo.time}
                </span>
              </div>
            </button>
          ))}
        </div>
        <p className="mt-4 text-xs text-gray-500 text-center">
          Refresh page for different suggestions • 18 popular projects available
        </p>
      </div>
    </div>
  )
}
