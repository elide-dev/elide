import { useState, useEffect } from 'react'
import './App.css'
import { API_BASE_URL } from './config'

type DiscoveredDatabase = {
  path: string
  size: number
  lastModified: number
  isLocal: boolean
}

type DatabasesResponse = {
  databases: DiscoveredDatabase[]
}

function App() {
  const [databases, setDatabases] = useState<DiscoveredDatabase[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    fetch(`${API_BASE_URL}/api/databases`)
      .then(res => {
        if (!res.ok) {
          throw new Error(`HTTP ${res.status}: ${res.statusText}`)
        }
        return res.json()
      })
      .then((data: DatabasesResponse) => {
        setDatabases(data.databases)
        setLoading(false)
      })
      .catch(err => {
        setError(err.message)
        setLoading(false)
      })
  }, [])

  if (loading) {
    return (
      <div className="container">
        <h1>Database Studio</h1>
        <p>Loading databases...</p>
      </div>
    )
  }

  if (error) {
    return (
      <div className="container">
        <h1>Database Studio</h1>
        <p style={{ color: 'red' }}>Error: {error}</p>
        <p style={{ fontSize: '0.9em', color: '#666' }}>
          Make sure your Elide server is running on port 8080
        </p>
      </div>
    )
  }

  return (
    <div className="container">
      <h1>Database Studio</h1>
      <p>Found {databases.length} database(s)</p>

      {databases.length === 0 ? (
        <p>No databases discovered</p>
      ) : (
        <div className="databases-list">
          {databases.map((db, index) => (
            <div key={index} className="database-card">
              <h3>Database #{index}</h3>
              <p><strong>Path:</strong> {db.path}</p>
              <p><strong>Size:</strong> {(db.size / 1024).toFixed(2)} KB</p>
              <p><strong>Last Modified:</strong> {new Date(db.lastModified).toLocaleString()}</p>
              <p><strong>Local:</strong> {db.isLocal ? 'Yes' : 'No'}</p>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

export default App
