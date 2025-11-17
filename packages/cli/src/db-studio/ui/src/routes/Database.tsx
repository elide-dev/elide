import { Link, Outlet, useParams, useLocation } from 'react-router-dom'
import { useMemo, useState } from 'react'
import { TableProperties as TableIcon, Code2 } from 'lucide-react'
import { useDatabaseTables } from '../hooks/useDatabaseTables'
import { Button } from '@/components/ui/button'
import { formatRowCount } from '../lib/utils'

export default function Database() {
  const { dbIndex, tableName } = useParams()
  const location = useLocation()
  const { data: tables = [], isLoading: loading } = useDatabaseTables(dbIndex)
  const [query, setQuery] = useState('')

  const filteredTables = useMemo(() => {
    if (!query) return tables
    const q = query.toLowerCase()
    return tables.filter(({ name }) => name.toLowerCase().includes(q))
  }, [tables, query])

  const isQueryActive = location.pathname.includes('/query')

  return (
    <div className="flex h-[calc(100vh-73px)]">
      <div className="w-64 border-r border-gray-800 p-4 bg-gray-950">
        <div className="mb-3">
          <Button
            asChild
            variant={isQueryActive ? 'default' : 'outline'}
            size="sm"
            className={[
              'w-full justify-start border-gray-800 bg-gray-950 text-gray-200 hover:bg-gray-900 hover:text-white',
              isQueryActive ? 'bg-gray-900 text-white' : ''
            ].join(' ')}
          >
            <Link to={`/database/${dbIndex}/query`}>
              <Code2 className="w-4 h-4" />
              <span>Query Editor</span>
            </Link>
          </Button>
        </div>
        <div className="mb-3">
          <input
            type="text"
            placeholder="Search tables…"
            className="w-full bg-gray-900 border border-gray-800 rounded px-3 py-2 text-sm placeholder:text-gray-600 focus:outline-none focus:border-gray-700 focus:ring-0"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            disabled={loading}
          />
        </div>
        <div className="text-xs text-gray-500 uppercase  mb-3 font-medium">
          {loading ? 'Loading…' : `${filteredTables.length} TABLES`}
        </div>
        {!loading && filteredTables.map(({ name, rowCount }) => {
          const isActive = decodeURIComponent(tableName || '') === name
          return (
            <Link
              key={name}
              to={`/database/${dbIndex}/table/${encodeURIComponent(name)}`}
              className={[
                'w-full text-left px-3 py-2 mb-1 flex items-center gap-2 text-sm transition-colors rounded bg-gray-950 hover:bg-gray-900',
                isActive ? 'bg-gray-900 text-white' : 'text-gray-200 hover:text-white'
              ].join(' ')}
              aria-current={isActive ? 'page' : undefined}
            >
              <TableIcon className="w-4 h-4 shrink-0" />
              <span className="truncate flex-1">{name}</span>
              <span className="text-xs text-gray-500 shrink-0">{formatRowCount(rowCount)}</span>
            </Link>
          )
        })}
      </div>
      <Outlet />
    </div>
  )
}


