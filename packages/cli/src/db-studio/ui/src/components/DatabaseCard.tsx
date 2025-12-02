import { Link } from 'react-router-dom'
import { Database, ChevronRight } from 'lucide-react'
import { formatBytes, formatDate } from '../lib/utils'

interface DatabaseInfo {
  path: string
  size: number
  lastModified: number
}

interface DatabaseCardProps {
  database: DatabaseInfo
  index: number
}

export function DatabaseCard({ database, index }: DatabaseCardProps) {
  const dbName = database.path.split('/').pop() || 'Unknown'

  return (
    <Link
      to={`/database/${index}/tables`}
      className="bg-card border border-border rounded-lg p-5 hover:border-ring hover:bg-accent transition-all text-left group hover:-translate-y-0.5 hover:shadow-lg"
    >
      <div className="flex items-start gap-4">
        <div className="bg-muted p-3 rounded-lg group-hover:bg-accent transition-colors">
          <Database className="w-6 h-6 text-muted-foreground" />
        </div>
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 mb-1.5">
            <h3 className="font-medium text-base">{dbName}</h3>
          </div>
          <p className="text-xs text-muted-foreground truncate mb-2" title={database.path}>
            {database.path}
          </p>
          <div className="flex items-center gap-3 text-xs text-muted-foreground">
            <span>{formatBytes(database.size)}</span>
            <span>•</span>
            <span>{formatDate(database.lastModified)}</span>
          </div>
        </div>
        <div className="self-center ml-2 text-muted-foreground transition-all transform group-hover:text-foreground group-hover:translate-x-0.5 group-hover:scale-110">
          <ChevronRight className="w-5 h-5" />
        </div>
      </div>
    </Link>
  )
}
