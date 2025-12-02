import { Outlet, useParams, useNavigate } from 'react-router-dom'
import { useState } from 'react'
import { useDatabaseTables } from '../hooks/useDatabaseTables'
import { SidebarInset, SidebarProvider } from '@/components/ui/sidebar'
import { ResizablePanelGroup, ResizablePanel, ResizableHandle } from '@/components/ui/resizable'
import { DropTableDialog } from '@/components/DropTableDialog'
import { TruncateTableDialog } from '@/components/TruncateTableDialog'
import { DatabaseSidebar } from '@/components/DatabaseSidebar'

export default function Database() {
  const { dbIndex, tableName } = useParams()
  const navigate = useNavigate()
  const { data: tables = [], isLoading: loading, refetch } = useDatabaseTables(dbIndex)
  const [dropDialogOpen, setDropDialogOpen] = useState(false)
  const [truncateDialogOpen, setTruncateDialogOpen] = useState(false)
  const [selectedTable, setSelectedTable] = useState<{ name: string; type: 'table' | 'view' } | null>(null)

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
          <DatabaseSidebar
            dbIndex={dbIndex!}
            tableName={tableName}
            tables={tables}
            loading={loading}
            onRefetch={refetch}
            onDropTable={openDropDialog}
            onTruncateTable={openTruncateDialog}
          />
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
