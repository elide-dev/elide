import { Link, Outlet, useParams, useLocation, useNavigate } from 'react-router-dom'
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
  Eraser,
} from 'lucide-react'
import { useDatabaseTables } from '../hooks/useDatabaseTables'
import { Button } from '@/components/ui/button'
import { formatRowCount } from '../lib/utils'
import {
  DropdownMenu,
  DropdownMenuCheckboxItem,
  DropdownMenuContent,
  DropdownMenuTrigger,
  DropdownMenuItem,
  DropdownMenuSeparator,
} from '@/components/ui/dropdown-menu'
import { DropTableDialog } from '@/components/DropTableDialog'
import { TruncateTableDialog } from '@/components/TruncateTableDialog'
import {
  Sidebar,
  SidebarContent,
  SidebarGroup,
  SidebarGroupContent,
  SidebarHeader,
  SidebarInset,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarMenuAction,
  SidebarMenuBadge,
  SidebarProvider,
} from '@/components/ui/sidebar'
import { ResizablePanelGroup, ResizablePanel, ResizableHandle } from '@/components/ui/resizable'
import { HoverCard, HoverCardContent, HoverCardTrigger } from '@/components/ui/hover-card'
import { Skeleton } from '@/components/ui/skeleton'

export default function Database() {
  const { dbIndex, tableName } = useParams()
  const location = useLocation()
  const navigate = useNavigate()
  const { data: tables = [], isLoading: loading, refetch } = useDatabaseTables(dbIndex)
  const [query, setQuery] = useState('')
  const [showTables, setShowTables] = useState(true)
  const [showViews, setShowViews] = useState(true)
  const [dropDialogOpen, setDropDialogOpen] = useState(false)
  const [truncateDialogOpen, setTruncateDialogOpen] = useState(false)
  const [selectedTable, setSelectedTable] = useState<{ name: string; type: 'table' | 'view' } | null>(null)

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

  const handleDropSuccess = () => {
    // If we're viewing the dropped table, navigate back to query editor
    if (selectedTable && decodeURIComponent(tableName || '') === selectedTable.name) {
      navigate(`/database/${dbIndex}/query`)
    }
    setDropDialogOpen(false)
    setSelectedTable(null)
  }

  const handleTruncateSuccess = () => {
    setTruncateDialogOpen(false)
    setSelectedTable(null)
  }

  const openDropDialog = (name: string, type: 'table' | 'view') => {
    setSelectedTable({ name, type })
    setDropDialogOpen(true)
  }

  const openTruncateDialog = (name: string, type: 'table' | 'view') => {
    setSelectedTable({ name, type })
    setTruncateDialogOpen(true)
  }

  return (
    <SidebarProvider defaultOpen={true} className="h-screen">
      <ResizablePanelGroup direction="horizontal" className="h-full">
        <ResizablePanel defaultSize={20} minSize={15} maxSize={40} className="min-w-[200px]">
          <Sidebar collapsible="none" className="border-border h-full">
            <SidebarHeader className="border-b border-border shrink-0">
              <Link to="/" className="flex items-center gap-2 px-2 py-3">
                <img src="/elide-logo.svg" alt="Elide" className="w-8 h-8" />
                <h1 className="text-lg font-medium">Database Studio</h1>
              </Link>
              <Button asChild variant="outline" size="sm" className="w-full justify-start">
                <Link to="/" aria-label="Back to databases">
                  <ArrowLeft className="w-4 h-4" />
                  <span>Back to databases</span>
                </Link>
              </Button>
            </SidebarHeader>

            <div className="px-4 py-4 shrink-0 border-b border-border">
              <div className="mb-3">
                <Button
                  asChild
                  variant={isQueryActive ? 'secondary' : 'outline'}
                  size="sm"
                  className="w-full justify-start"
                >
                  <Link to={`/database/${dbIndex}/query`}>
                    <Code2 className="w-4 h-4" />
                    <span>Query Editor</span>
                  </Link>
                </Button>
              </div>
              <div className="flex items-center gap-2">
                <div className="relative flex-1">
                  <input
                    type="text"
                    placeholder="Search..."
                    className="w-full bg-muted border border-border rounded px-3 py-2 pr-10 text-sm placeholder:text-muted-foreground focus:outline-none focus:border-ring focus:ring-0"
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                    disabled={loading}
                  />
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <button
                        className="absolute right-2 top-1/2 -translate-y-1/2 p-1 hover:bg-accent rounded transition-colors cursor-pointer"
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
                      size="sm"
                      onClick={() => refetch()}
                      disabled={loading}
                      className="h-9 w-9 p-0"
                    >
                      <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
                    </Button>
                  </HoverCardTrigger>
                  <HoverCardContent side="bottom" className="w-auto px-3 py-1.5">
                    <span className="text-xs font-semibold">Refresh tables</span>
                  </HoverCardContent>
                </HoverCard>
              </div>
            </div>

            <SidebarContent className="flex-1 min-h-0">
              <SidebarGroup>
                <SidebarGroupContent>
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
                                <Link to={`/database/${dbIndex}/table/${encodeURIComponent(name)}`}>
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
                                    <>
                                      <DropdownMenuItem
                                        className="text-destructive focus:text-destructive"
                                        onClick={() => openTruncateDialog(name, type)}
                                      >
                                        <Eraser className="w-4 h-4" />
                                        <span>Truncate Table</span>
                                      </DropdownMenuItem>
                                      <DropdownMenuSeparator />
                                    </>
                                  )}
                                  <DropdownMenuItem
                                    className="text-destructive focus:text-destructive"
                                    onClick={() => openDropDialog(name, type)}
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
          </Sidebar>
        </ResizablePanel>

        <ResizableHandle withHandle className="bg-border" />

        <ResizablePanel defaultSize={80}>
          <SidebarInset className="h-full overflow-hidden">
            <Outlet />
          </SidebarInset>
        </ResizablePanel>
      </ResizablePanelGroup>

      {selectedTable && dbIndex && (
        <>
          <DropTableDialog
            open={dropDialogOpen}
            onOpenChange={(open) => {
              if (!open) handleDropSuccess()
            }}
            dbIndex={dbIndex}
            tableName={selectedTable.name}
            tableType={selectedTable.type}
          />
          {selectedTable.type === 'table' && (
            <TruncateTableDialog
              open={truncateDialogOpen}
              onOpenChange={(open) => {
                if (!open) handleTruncateSuccess()
              }}
              dbIndex={dbIndex}
              tableName={selectedTable.name}
            />
          )}
        </>
      )}
    </SidebarProvider>
  )
}
