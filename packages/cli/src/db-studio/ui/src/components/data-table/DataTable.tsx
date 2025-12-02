import * as React from 'react'
import { useBlocker, useParams } from 'react-router-dom'
import { useQueryClient } from '@tanstack/react-query'
import { useDataTable } from '@/contexts/DataTableContext'
import { useInsertRow } from '@/hooks/useInsertRow'
import { DataTableToolbar } from './DataTableToolbar'
import { DataTableFilterPanel } from './DataTableFilterPanel'
import { DataTableGrid } from './DataTableGrid'
import { DeleteRowsDialog } from './DeleteRowsDialog'
import { InsertRowDialog, type InsertResult } from './InsertRowDialog'
import { DiscardChangesDialog } from './DiscardChangesDialog'

/**
 * Reusable data table component for displaying database query results
 * Uses TanStack Table for sorting, filtering, and column visibility
 * All state management is handled by Table.tsx route component
 * Must be wrapped in a DataTableProvider
 */
export function DataTable() {
  const { config, appliedFilters, columns } = useDataTable()
  const insertRowMutation = useInsertRow()
  const queryClient = useQueryClient()
  const { dbIndex, tableName } = useParams<{ dbIndex: string; tableName: string }>()

  // Local state for filter panel visibility
  const [showFilterPanel, setShowFilterPanel] = React.useState(appliedFilters.length > 0)

  // Local state for delete dialog visibility
  const [showDeleteDialog, setShowDeleteDialog] = React.useState(false)

  // Local state for editable rows - array of rows to insert
  type EditableRowData = {
    id: string
    data: Record<string, unknown>
  }
  const [editableRows, setEditableRows] = React.useState<EditableRowData[]>([])

  // Local state for dialogs
  const [showInsertDialog, setShowInsertDialog] = React.useState(false)
  const [insertResults, setInsertResults] = React.useState<InsertResult[]>([])

  // Block navigation when there are unsaved editable rows
  const blocker = useBlocker(editableRows.length > 0)

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

  // Add row handlers
  const handleAddRow = React.useCallback(() => {
    // Create a new row with all values defaulting to null
    const initialRowData: Record<string, unknown> = {}
    columns.forEach((col) => {
      initialRowData[col.name] = null
    })

    const newRow: EditableRowData = {
      id: `new-${Date.now()}-${Math.random()}`,
      data: initialRowData,
    }

    setEditableRows((prev) => [...prev, newRow])
  }, [columns])

  const handleCellChange = React.useCallback((rowId: string, columnName: string, value: unknown) => {
    setEditableRows((prev) =>
      prev.map((row) => {
        if (row.id === rowId) {
          return {
            ...row,
            data: {
              ...row.data,
              [columnName]: value,
            },
          }
        }
        return row
      })
    )
  }, [])

  const handleRemoveRow = React.useCallback((rowId: string) => {
    setEditableRows((prev) => prev.filter((row) => row.id !== rowId))
  }, [])

  const handleSaveAll = React.useCallback(async () => {
    if (editableRows.length === 0) return

    const results: InsertResult[] = []
    const successfulRowIds: string[] = []
    let hasFailures = false

    // Process each row and collect results
    for (const row of editableRows) {
      try {
        const response = await insertRowMutation.mutateAsync(row.data)

        results.push({
          id: row.id,
          status: 'success' as const,
          sql: response.sql,
        })
        successfulRowIds.push(row.id)
      } catch (error: unknown) {
        hasFailures = true
        const message = error instanceof Error ? error.message : 'An unknown error occurred'
        const sql = (error as { response?: { sql?: string } })?.response?.sql ?? undefined

        results.push({
          id: row.id,
          status: 'error' as const,
          sql,
          error: message,
        })
      }
    }

    // Remove successful rows from editable rows
    setEditableRows((prev) => prev.filter((row) => !successfulRowIds.includes(row.id)))

    // Refresh table data to show newly inserted rows
    if (successfulRowIds.length > 0) {
      queryClient.invalidateQueries({
        queryKey: ['databases', dbIndex, 'tables', tableName],
      })
    }

    // Only show dialog if there were failures
    if (hasFailures) {
      setInsertResults(results)
      setShowInsertDialog(true)
    }
  }, [editableRows, insertRowMutation, queryClient, dbIndex, tableName])

  // Immediate discard when user clicks the discard button (intentional action)
  const handleDiscardAll = React.useCallback(() => {
    setEditableRows([])
  }, [])

  // Handle navigation blocking - discard and proceed with navigation
  const handleBlockedDiscard = React.useCallback(() => {
    setEditableRows([])
    if (blocker.state === 'blocked') {
      blocker.proceed()
    }
  }, [blocker])

  const handleRetryInsert = React.useCallback(async () => {
    // Only retry failed rows
    const failedRowIds = insertResults.filter((r) => r.status === 'error').map((r) => r.id)
    const failedRows = editableRows.filter((row) => failedRowIds.includes(row.id))

    if (failedRows.length === 0) return

    // Close the dialog while retrying
    setShowInsertDialog(false)

    const newResults: InsertResult[] = []
    const successfulRowIds: string[] = []
    let hasFailures = false

    for (const row of failedRows) {
      try {
        const response = await insertRowMutation.mutateAsync(row.data)

        newResults.push({
          id: row.id,
          status: 'success' as const,
          sql: response.sql,
        })
        successfulRowIds.push(row.id)
      } catch (error: unknown) {
        hasFailures = true
        const message = error instanceof Error ? error.message : 'An unknown error occurred'
        const sql = (error as { response?: { sql?: string } })?.response?.sql ?? undefined

        newResults.push({
          id: row.id,
          status: 'error' as const,
          sql,
          error: message,
        })
      }
    }

    // Remove successful rows from editable rows
    setEditableRows((prev) => prev.filter((row) => !successfulRowIds.includes(row.id)))

    // Refresh table data to show newly inserted rows
    if (successfulRowIds.length > 0) {
      queryClient.invalidateQueries({
        queryKey: ['databases', dbIndex, 'tables', tableName],
      })
    }

    // Merge with previously successful results
    const successfulResults = insertResults.filter((r) => r.status === 'success')
    const allResults = [...successfulResults, ...newResults]

    // Only show dialog again if there are still failures
    if (hasFailures) {
      setInsertResults(allResults)
      setShowInsertDialog(true)
    } else {
      // All succeeded - clear results
      setInsertResults([])
    }
  }, [editableRows, insertResults, insertRowMutation, queryClient, dbIndex, tableName])

  const handleCloseInsertDialog = React.useCallback(() => {
    setInsertResults([])
    setShowInsertDialog(false)
  }, [])

  // Only allow deletion for tables, not views
  const isView = config.tableType === 'view'
  const deleteHandler = isView ? undefined : handleDeleteRows
  const addRowHandler = isView ? undefined : handleAddRow
  const hasEditableRows = editableRows.length > 0

  return (
    <div className="w-full flex flex-col h-full">
      {/* Toolbar with metadata and controls */}
      {(config.showControls || config.showPagination || config.tableName) && (
        <DataTableToolbar
          showFilterPanel={showFilterPanel}
          onFilterToggle={handleFilterToggle}
          onAddFilter={handleAddFilter}
          onDeleteRows={deleteHandler}
          onAddRow={addRowHandler}
          onSaveChanges={hasEditableRows ? handleSaveAll : undefined}
          onDiscardChanges={hasEditableRows ? handleDiscardAll : undefined}
          hasEditableRows={hasEditableRows}
        />
      )}

      {/* Filter rows section */}
      {showFilterPanel && <DataTableFilterPanel onClearFilters={handleClearFilters} />}

      {/* Table */}
      <DataTableGrid
        editableRows={editableRows}
        onEditCellChange={handleCellChange}
        onEditRowRemove={handleRemoveRow}
      />

      {/* Delete rows dialog */}
      <DeleteRowsDialog isOpen={showDeleteDialog} onOpenChange={setShowDeleteDialog} />

      {/* Insert row dialog */}
      <InsertRowDialog
        isOpen={showInsertDialog}
        onOpenChange={setShowInsertDialog}
        results={insertResults}
        onRetry={handleRetryInsert}
        onClose={handleCloseInsertDialog}
      />

      {/* Discard changes dialog - shown when navigating away with unsaved changes */}
      <DiscardChangesDialog
        isOpen={blocker.state === 'blocked'}
        onOpenChange={(open) => {
          if (!open && blocker.state === 'blocked') {
            blocker.reset()
          }
        }}
        onDiscard={handleBlockedDiscard}
      />
    </div>
  )
}
