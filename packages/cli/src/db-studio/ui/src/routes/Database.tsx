import { Outlet, useParams, useNavigate } from 'react-router-dom'
import { useState } from 'react'
import { useDatabaseTables } from '../hooks/useDatabaseTables'
import { SidebarInset, SidebarProvider } from '@/components/ui/sidebar'
import { ResizablePanelGroup, ResizablePanel, ResizableHandle } from '@/components/ui/resizable'
import { DropTableDialog } from '@/components/DropTableDialog'
import { TruncateTableDialog } from '@/components/TruncateTableDialog'
import { DatabaseSidebar } from '@/components/DatabaseSidebar'

export default function Database() {
  const { dbId, tableName } = useParams()
  const navigate = useNavigate()
  const { data: tables = [], isLoading: loading, refetch } = useDatabaseTables(dbId)
  const [dropDialogOpen, setDropDialogOpen] = useState(false)
  const [truncateDialogOpen, setTruncateDialogOpen] = useState(false)
  const [selectedTable, setSelectedTable] = useState<{ name: string; type: 'table' | 'view' } | null>(null)

  const handleDropDialogClose = () => {
    setDropDialogOpen(false)
    setSelectedTable(null)
  }

  const handleDropSuccess = () => {
    // If we're viewing the dropped table, navigate to database index
    if (selectedTable && decodeURIComponent(tableName || '') === selectedTable.name) {
      navigate(`/database/${dbId}`)
    }
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
            dbId={dbId!}
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

      {selectedTable && dbId && (
        <>
          <DropTableDialog
            isOpen={dropDialogOpen}
            onOpenChange={(isOpen) => {
              if (!isOpen) handleDropDialogClose()
            }}
            onSuccess={handleDropSuccess}
            dbId={dbId}
            tableName={selectedTable.name}
            tableType={selectedTable.type}
          />
          {selectedTable.type === 'table' && (
            <TruncateTableDialog
              isOpen={truncateDialogOpen}
              onOpenChange={(isOpen) => {
                if (!isOpen) handleTruncateSuccess()
              }}
              dbId={dbId}
              tableName={selectedTable.name}
            />
          )}
        </>
      )}
    </SidebarProvider>
  )
}
