import * as React from 'react'
import { useDataTable } from '@/contexts/DataTableContext'
import { DataTableToolbar } from './DataTableToolbar'
import { DataTableFilterPanel } from './DataTableFilterPanel'
import { DataTableGrid } from './DataTableGrid'
import { DeleteRowsDialog } from './DeleteRowsDialog'

/**
 * Reusable data table component for displaying database query results
 * Uses TanStack Table for sorting, filtering, and column visibility
 * All state management is handled by Table.tsx route component
 * Must be wrapped in a DataTableProvider
 */
export function DataTable() {
  const { config, appliedFilters } = useDataTable()

  // Local state for filter panel visibility
  const [showFilterPanel, setShowFilterPanel] = React.useState(appliedFilters.length > 0)

  // Local state for delete dialog visibility
  const [showDeleteDialog, setShowDeleteDialog] = React.useState(false)

  // Show panel when filters are applied
  React.useEffect(() => {
    if (appliedFilters.length > 0) {
      setShowFilterPanel(true)
    }
  }, [appliedFilters.length])

  const handleFilterToggle = React.useCallback(() => {
    setShowFilterPanel((prev) => !prev)
  }, [])

  const handleAddFilter = React.useCallback(() => {
    setShowFilterPanel(true)
  }, [])

  const handleClearFilters = React.useCallback(() => {
    setShowFilterPanel(false)
  }, [])

  const handleDeleteRows = React.useCallback(() => {
    setShowDeleteDialog(true)
  }, [])

  return (
    <div className="w-full flex flex-col h-full">
      {/* Toolbar with metadata and controls */}
      {(config.showControls || config.showPagination || config.tableName) && (
        <DataTableToolbar
          showFilterPanel={showFilterPanel}
          onFilterToggle={handleFilterToggle}
          onAddFilter={handleAddFilter}
          onDeleteRows={handleDeleteRows}
        />
      )}

      {/* Filter rows section */}
      {showFilterPanel && <DataTableFilterPanel onClearFilters={handleClearFilters} />}

      {/* Table */}
      <DataTableGrid />

      {/* Delete rows dialog */}
      <DeleteRowsDialog open={showDeleteDialog} onOpenChange={setShowDeleteDialog} />
    </div>
  )
}
