import { useDatabases } from '../hooks/useDatabases'
import { DatabaseCard } from '../components/DatabaseCard'
import { HomeHeader } from '../components/HomeHeader'

export default function Home() {
  const { data: databases = [], isLoading: loading, error } = useDatabases()

  if (loading) {
    return <div className="min-h-screen bg-background text-foreground flex items-center justify-center">Loading...</div>
  }

  if (error) {
    return (
      <div className="min-h-screen bg-background text-foreground flex items-center justify-center">
        Error: {error.message}
      </div>
    )
  }

  return (
    <div className="p-8">
      <div className="max-w-5xl mx-auto">
        <HomeHeader databaseCount={databases.length} />
        <div className="grid grid-cols-2 gap-4">
          {databases.map((db, index) => (
            <DatabaseCard key={index} database={db} index={index} />
          ))}
        </div>
      </div>
    </div>
  )
}
