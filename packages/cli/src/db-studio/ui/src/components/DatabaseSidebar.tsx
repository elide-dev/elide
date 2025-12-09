import { Link, useLocation } from 'react-router-dom'
import { useMemo, useState } from 'react'
import {
  TableProperties as TableIcon,
  Code2,
  ScanEye,
  ListFilter,
  ArrowLeft,
  RefreshCw,
  Trash2,
  MoreHorizontal,
  Scissors,
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { formatRowCount } from '../lib/utils'
import { VERSION } from '../lib/constants'
import {
  DropdownMenu,
  DropdownMenuCheckboxItem,
  DropdownMenuContent,
  DropdownMenuTrigger,
  DropdownMenuItem,
} from '@/components/ui/dropdown-menu'
import {
  Sidebar,
  SidebarContent,
  SidebarGroup,
  SidebarGroupContent,
  SidebarFooter,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarMenuAction,
  SidebarMenuBadge,
  SidebarSeparator,
} from '@/components/ui/sidebar'
import { HoverCard, HoverCardContent, HoverCardTrigger } from '@/components/ui/hover-card'
import { Skeleton } from '@/components/ui/skeleton'

interface TableInfo {
  name: string
  type: 'table' | 'view'
  rowCount: number
}

interface DatabaseSidebarProps {
  dbId: string
  tableName?: string
  tables: TableInfo[]
  loading: boolean
  onRefetch: () => void
  onDropTable: (name: string, type: 'table' | 'view') => void
  onTruncateTable: (name: string, type: 'table' | 'view') => void
}

export function DatabaseSidebar({
  dbId,
  tableName,
  tables,
  loading,
  onRefetch,
  onDropTable,
  onTruncateTable,
}: DatabaseSidebarProps) {
  const location = useLocation()
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
    <Sidebar collapsible="none" className="border-border h-full">
      <SidebarContent className="flex-1 min-h-0 overflow-hidden">
        {/* Navigation Group */}
        <SidebarGroup className="shrink-0">
          <SidebarGroupContent>
            <SidebarMenu>
              <SidebarMenuItem>
                <SidebarMenuButton asChild>
                  <Link to="/">
                    <ArrowLeft className="w-4 h-4" />
                    <span>Back to databases</span>
                  </Link>
                </SidebarMenuButton>
              </SidebarMenuItem>
              <SidebarMenuItem>
                <SidebarMenuButton asChild isActive={isQueryActive}>
                  <Link to={`/database/${dbId}/query`}>
                    <Code2 className="w-4 h-4" />
                    <span>Query Editor</span>
                  </Link>
                </SidebarMenuButton>
              </SidebarMenuItem>
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>

        <SidebarSeparator className="shrink-0" />

        {/* Tables Group */}
        <SidebarGroup className="flex-1 min-h-0 flex flex-col p-0">
          <div className="flex items-center gap-2 px-2 pb-2 shrink-0">
            <div className="relative flex-1">
              <input
                type="text"
                placeholder="Search tables..."
                className="w-full bg-muted border border-border rounded px-3 py-1.5 pr-8 text-sm placeholder:text-muted-foreground focus:outline-none focus:border-ring focus:ring-0"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                disabled={loading}
              />
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <button
                    className="absolute right-2 top-1/2 -translate-y-1/2 p-0.5 hover:bg-accent rounded transition-colors cursor-pointer"
                    disabled={loading}
                  >
                    <ListFilter className="w-4 h-4 text-muted-foreground" />
                  </button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end">
                  <DropdownMenuCheckboxItem checked={showTables} onCheckedChange={setShowTables}>
                    Show tables
                  </DropdownMenuCheckboxItem>
                  <DropdownMenuCheckboxItem checked={showViews} onCheckedChange={setShowViews}>
                    Show views
                  </DropdownMenuCheckboxItem>
                </DropdownMenuContent>
              </DropdownMenu>
            </div>
            <HoverCard openDelay={200}>
              <HoverCardTrigger asChild>
                <Button
                  variant="outline"
                  size="icon"
                  onClick={onRefetch}
                  disabled={loading}
                  className="h-[30px] w-[30px] shrink-0"
                >
                  <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
                </Button>
              </HoverCardTrigger>
              <HoverCardContent side="bottom" className="w-auto px-3 py-1.5">
                <span className="text-xs font-semibold">Refresh tables</span>
              </HoverCardContent>
            </HoverCard>
          </div>
          <SidebarGroupContent className="flex-1 min-h-0 overflow-auto px-2">
            <SidebarMenu>
              {loading
                ? // Show skeleton items while loading
                  Array.from({ length: 8 }).map((_, index) => (
                    <SidebarMenuItem key={index}>
                      <SidebarMenuButton disabled>
                        <Skeleton className="w-4 h-4" />
                        <Skeleton className="h-4 flex-1" />
                        <Skeleton className="h-3 w-8" />
                      </SidebarMenuButton>
                    </SidebarMenuItem>
                  ))
                : filteredTables.map(({ name, type, rowCount }) => {
                    const isActive = decodeURIComponent(tableName || '') === name
                    const Icon = type === 'view' ? ScanEye : TableIcon
                    return (
                      <SidebarMenuItem key={name} className="group">
                        <SidebarMenuButton asChild isActive={isActive}>
                          <Link to={`/database/${dbId}/table/${encodeURIComponent(name)}`}>
                            <Icon className="w-4 h-4" />
                            <span className="flex-1 truncate">{name}</span>
                          </Link>
                        </SidebarMenuButton>
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <SidebarMenuAction showOnHover className="peer">
                              <MoreHorizontal />
                              <span className="sr-only">More</span>
                            </SidebarMenuAction>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent side="bottom" align="start">
                            {type === 'table' && (
                              <DropdownMenuItem
                                className="text-destructive focus:text-destructive"
                                onClick={() => onTruncateTable(name, type)}
                              >
                                <Scissors className="w-4 h-4" />
                                <span>Truncate Table</span>
                              </DropdownMenuItem>
                            )}
                            <DropdownMenuItem
                              className="text-destructive focus:text-destructive"
                              onClick={() => onDropTable(name, type)}
                            >
                              <Trash2 className="w-4 h-4" />
                              <span>Drop {type === 'table' ? 'Table' : 'View'}</span>
                            </DropdownMenuItem>
                          </DropdownMenuContent>
                        </DropdownMenu>
                        <SidebarMenuBadge className="group-hover:opacity-0 group-focus-within:opacity-0 peer-data-[state=open]:opacity-0">
                          {formatRowCount(rowCount)}
                        </SidebarMenuBadge>
                      </SidebarMenuItem>
                    )
                  })}
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>
      </SidebarContent>

      <SidebarFooter className="border-t border-border">
        <div className="flex items-center justify-between px-2 py-2">
          <div className="flex items-center gap-2">
            <img src="/elide-logo.svg" alt="Elide" className="w-6 h-6" />
            <span className="text-sm font-medium text-muted-foreground">Database Studio</span>
          </div>
          <Badge variant="secondary" className="text-[10px] px-1.5 py-0">
            v{VERSION}
          </Badge>
        </div>
      </SidebarFooter>
    </Sidebar>
  )
}
