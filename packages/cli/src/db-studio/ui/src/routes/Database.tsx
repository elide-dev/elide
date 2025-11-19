import { Link, Outlet, useParams, useLocation } from 'react-router-dom'
import { useMemo, useState } from 'react'
import { TableProperties as TableIcon, Code2, ScanEye, ListFilter, ArrowLeft } from 'lucide-react'
import { useDatabaseTables } from '../hooks/useDatabaseTables'
import { Button } from '@/components/ui/button'
import { formatRowCount } from '../lib/utils'
import {
  DropdownMenu,
  DropdownMenuCheckboxItem,
  DropdownMenuContent,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'

export default function Database() {
  const { dbIndex, tableName } = useParams()
  const location = useLocation()
  const { data: tables = [], isLoading: loading } = useDatabaseTables(dbIndex)
  const [query, setQuery] = useState('')
  const [showTables, setShowTables] = useState(true)
  const [showViews, setShowViews] = useState(true)

  const filteredTables = useMemo(() => {
    let filtered = tables

    // Filter by type
    filtered = filtered.filter(({ type }) => {
      if (type === 'table') return showTables
      if (type === 'view') return showViews
      return true
    })

    // Filter by search query
    if (query) {
      const q = query.toLowerCase()
      filtered = filtered.filter(({ name }) => name.toLowerCase().includes(q))
    }

    // Sort: tables first (alphabetically), then views (alphabetically)
    return filtered.sort((a, b) => {
      if (a.type === b.type) {
        return a.name.localeCompare(b.name)
      }
      return a.type === 'table' ? -1 : 1
    })
  }, [tables, query, showTables, showViews])

  const isQueryActive = location.pathname.includes('/query')

  return (
    <div className="flex h-screen">
      <div className="w-64 border-r border-gray-800 bg-gray-950 flex flex-col">
        {/* Logo and Navigation */}
        <div className="p-4 border-b border-gray-800 shrink-0">
          <Link to="/" className="flex items-center gap-2 mb-4">
            <img src="/elide-logo.svg" alt="Elide" className="w-8 h-8" />
            <h1 className="text-lg font-medium">Database Studio</h1>
          </Link>
          <Button
            asChild
            variant="outline"
            size="sm"
            className="w-full justify-start border-gray-800 bg-gray-950 text-gray-200 hover:bg-gray-900 hover:text-white"
          >
            <Link to="/" aria-label="Back to databases">
              <ArrowLeft className="w-4 h-4" />
              <span>Back to databases</span>
            </Link>
          </Button>
        </div>

        <div className="p-4 shrink-0">
          <div className="mb-3">
            <Button
              asChild
              variant={isQueryActive ? 'default' : 'outline'}
              size="sm"
              className={[
                'w-full justify-start border-gray-800 bg-gray-950 text-gray-200 hover:bg-gray-900 hover:text-white',
                isQueryActive ? 'bg-gray-900 text-white' : '',
              ].join(' ')}
            >
              <Link to={`/database/${dbIndex}/query`}>
                <Code2 className="w-4 h-4" />
                <span>Query Editor</span>
              </Link>
            </Button>
          </div>
          <div className="mb-3 relative">
            <input
              type="text"
              placeholder="Search..."
              className="w-full bg-gray-900 border border-gray-800 rounded px-3 py-2 pr-10 text-sm placeholder:text-gray-600 focus:outline-none focus:border-gray-700 focus:ring-0"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              disabled={loading}
            />
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <button
                  className="absolute right-2 top-1/2 -translate-y-1/2 p-1 hover:bg-gray-800 rounded transition-colors cursor-pointer"
                  disabled={loading}
                >
                  <ListFilter className="w-4 h-4 text-gray-400" />
                </button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="bg-gray-900 border-gray-800">
                <DropdownMenuCheckboxItem
                  checked={showTables}
                  onCheckedChange={setShowTables}
                  className="text-gray-200 focus:bg-gray-800 focus:text-white"
                >
                  Show tables
                </DropdownMenuCheckboxItem>
                <DropdownMenuCheckboxItem
                  checked={showViews}
                  onCheckedChange={setShowViews}
                  className="text-gray-200 focus:bg-gray-800 focus:text-white"
                >
                  Show views
                </DropdownMenuCheckboxItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </div>
        <div className="flex-1 overflow-y-auto px-4 pb-4">
          {!loading &&
            filteredTables.map(({ name, type, rowCount }) => {
              const isActive = decodeURIComponent(tableName || '') === name
              const Icon = type === 'view' ? ScanEye : TableIcon
              return (
                <Link
                  key={name}
                  to={`/database/${dbIndex}/table/${encodeURIComponent(name)}`}
                  className={[
                    'w-full text-left px-3 py-2 mb-1 flex items-center gap-2 text-sm transition-colors rounded border',
                    isActive
                      ? 'bg-blue-950/50 text-white border-blue-800/60 hover:bg-blue-950/70'
                      : 'bg-gray-950 text-gray-200 hover:bg-gray-900 hover:text-white border-transparent',
                  ].join(' ')}
                  aria-current={isActive ? 'page' : undefined}
                >
                  <Icon className="w-4 h-4 shrink-0" />
                  <span className="truncate flex-1">{name}</span>
                  <span className={['text-xs shrink-0', isActive ? 'text-gray-400' : 'text-gray-500'].join(' ')}>
                    {formatRowCount(rowCount)}
                  </span>
                </Link>
              )
            })}
        </div>
      </div>
      <Outlet />
    </div>
  )
}
