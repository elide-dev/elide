import { Link, Outlet, useParams } from 'react-router-dom'
import { Database as DatabaseIcon } from 'lucide-react'
import { useDatabaseTables } from '../hooks/useDatabaseTables'

export default function Database() {
  const { dbIndex } = useParams()
  const { data: tables = [], isLoading: loading, error } = useDatabaseTables(dbIndex)

  return (
    <div className="flex h-[calc(100vh-73px)]">
      <div className="w-64 border-r border-gray-800 p-4 bg-gray-950">
        <div className="text-xs text-gray-500 uppercase tracking-wider mb-3 font-medium">
          {loading ? 'Loading…' : `${tables.length} TABLES`}
        </div>
        {error && (
          <div className="text-xs text-red-400 mb-2">{error.message}</div>
        )}
        {!loading && tables.map(({ name }) => (
          <Link
            key={name}
            to={`/database/${dbIndex}/table/${encodeURIComponent(name)}`}
            className="w-full block text-left px-3 py-2.5 rounded-md mb-1 flex items-center gap-2.5 text-sm transition-colors text-gray-300 hover:bg-gray-900 hover:text-white"
          >
            <DatabaseIcon className="w-4 h-4 flex-shrink-0" />
            <span className="truncate">{name}</span>
          </Link>
        ))}
      </div>
      <Outlet />
    </div>
  )
}


