import { Link } from 'react-router-dom'
import { Database } from 'lucide-react'
import { useDatabases } from '../hooks/useDatabases'

export default function Databases() {
  const { data: databases = [], isLoading: loading, error } = useDatabases()

  const formatBytes = (bytes: number) => {
    if (bytes < 1024) return `${bytes} B`
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(2)} KB`
    return `${(bytes / (1024 * 1024)).toFixed(2)} MB`
  }

  const formatDate = (timestamp: number) => {
    const now = Date.now()
    const diff = now - timestamp
    const minutes = Math.floor(diff / 60000)
    const hours = Math.floor(diff / 3600000)
    const days = Math.floor(diff / 86400000)

    if (minutes < 60) return `${minutes}m ago`
    if (hours < 24) return `${hours}h ago`
    return `${days}d ago`
  }

  if (loading) {
    return <div className="min-h-[calc(100vh-73px)] bg-black text-white flex items-center justify-center">Loading...</div>
  }

  if (error) {
    return <div className="min-h-[calc(100vh-73px)] bg-black text-white flex items-center justify-center">Error: {error.message}</div>
  }

  return (
    <div className="p-8">
      <div className="max-w-5xl mx-auto">
        <div className="flex flex-col items-center mb-12">
          <img src="/elide-logo.svg" alt="Elide" className="w-16 h-16 mb-6" />
          <h2 className="text-4xl font-semibold mb-3">Elide Database Studio</h2>
          <p className="text-gray-400 mb-2">Select a database to inspect</p>
          <p className="text-sm text-gray-500">{databases.length} databases found</p>
        </div>

        <div className="grid grid-cols-2 gap-4">
          {databases.map((db, index) => {
            const dbName = db.path.split('/').pop() || 'Unknown'
            return (
              <Link
                key={index}
                to={`/database/${index}/tables`}
                className="bg-gray-900 border border-gray-800 rounded-lg p-5 hover:border-gray-700 hover:bg-gray-850 transition-all text-left group"
              >
                <div className="flex items-start gap-4">
                  <div className="bg-gray-800 p-3 rounded-lg group-hover:bg-gray-750 transition-colors">
                    <Database className="w-6 h-6 text-gray-400" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-1.5">
                      <h3 className="font-medium text-base">{dbName}</h3>
                      {db.isLocal && (
                        <span className="text-[10px] font-medium bg-emerald-500/20 text-emerald-400 px-2 py-0.5 rounded uppercase tracking-wide">
                          LOCAL
                        </span>
                      )}
                    </div>
                    <p className="text-xs text-gray-500 truncate mb-2">{db.path}</p>
                    <div className="flex items-center gap-3 text-xs text-gray-400">
                      <span>{formatBytes(db.size)}</span>
                      <span>•</span>
                      <span>{formatDate(db.lastModified)}</span>
                    </div>
                  </div>
                </div>
              </Link>
            )
          })}
        </div>
      </div>
    </div>
  )
}


