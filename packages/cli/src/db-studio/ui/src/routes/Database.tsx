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
  SidebarProvider,
} from '@/components/ui/sidebar'
import { ResizablePanelGroup, ResizablePanel, ResizableHandle } from '@/components/ui/resizable'

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
              <div className="relative">
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
            </div>

            <SidebarContent className="flex-1 min-h-0">
              <SidebarGroup>
                <SidebarGroupContent>
                  <SidebarMenu>
                    {!loading &&
                      filteredTables.map(({ name, type, rowCount }) => {
                        const isActive = decodeURIComponent(tableName || '') === name
                        const Icon = type === 'view' ? ScanEye : TableIcon
                        return (
                          <SidebarMenuItem key={name}>
                            <SidebarMenuButton asChild isActive={isActive}>
                              <Link to={`/database/${dbIndex}/table/${encodeURIComponent(name)}`}>
                                <Icon className="w-4 h-4" />
                                <span className="flex-1 truncate">{name}</span>
                                <span className="text-xs text-muted-foreground">{formatRowCount(rowCount)}</span>
                              </Link>
                            </SidebarMenuButton>
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
    </SidebarProvider>
  )
}
