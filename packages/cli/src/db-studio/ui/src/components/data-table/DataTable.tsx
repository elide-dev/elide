import * as React from 'react'
import { useBlocker, useParams } from 'react-router-dom'
import { useQueryClient } from '@tanstack/react-query'
import { useDataTable } from '@/contexts/DataTableContext'
import { useInsertRow } from '@/hooks/useInsertRow'
import { useUpdateRow } from '@/hooks/useUpdateRow'
import { DataTableToolbar } from './DataTableToolbar'
import { DataTableFilterPanel } from './DataTableFilterPanel'
import { DataTableGrid } from './DataTableGrid'
import { DeleteRowsDialog } from './DeleteRowsDialog'
import { InsertRowDialog, type InsertResult } from './InsertRowDialog'
import { DiscardChangesDialog } from './DiscardChangesDialog'

/**
 * Represents a pending cell edit on an existing row
 */
export type CellEdit = {
  /** Unique ID for tracking this edit (rowIndex-columnName) */
  id: string
  /** The row index in the table (for display purposes) */
  rowIndex: number
  /** Primary key values to identify the row */
  primaryKey: Record<string, unknown>
  /** Column name being edited */
  columnName: string
  /** Original value before edit */
  originalValue: unknown
  /** New value after edit */
  newValue: unknown
}

/**
 * Reusable data table component for displaying database query results
 * Uses TanStack Table for sorting, filtering, and column visibility
 * All state management is handled by Table.tsx route component
 * Must be wrapped in a DataTableProvider
 */
export type EditableRowData = {
  id: string
  data: Record<string, unknown>
}

export function DataTable() {
  const { config, appliedFilters, columns, table } = useDataTable()
  const insertRowMutation = useInsertRow()
  const updateRowMutation = useUpdateRow()
  const queryClient = useQueryClient()
  const { dbIndex, tableName } = useParams<{ dbIndex: string; tableName: string }>()

  // Local state for filter panel visibility
  const [showFilterPanel, setShowFilterPanel] = React.useState(appliedFilters.length > 0)

  // Local state for delete dialog visibility
  const [showDeleteDialog, setShowDeleteDialog] = React.useState(false)

  // Local state for editable rows - array of rows to insert
  const [editableRows, setEditableRows] = React.useState<EditableRowData[]>([])

  // Local state for cell edits - pending updates to existing rows
  const [cellEdits, setCellEdits] = React.useState<CellEdit[]>([])

  // Local state for dialogs
  const [showInsertDialog, setShowInsertDialog] = React.useState(false)
  const [insertResults, setInsertResults] = React.useState<InsertResult[]>([])

  // Block navigation when there are unsaved editable rows or cell edits
  const blocker = useBlocker(editableRows.length > 0 || cellEdits.length > 0)

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

  // Get primary key columns
  const pkColumns = React.useMemo(() => columns.filter((col) => col.primaryKey), [columns])

  // Handler for committing a cell edit (on existing row)
  const handleCellEditCommit = React.useCallback(
    (rowIndex: number, columnName: string, newValue: unknown) => {
      // Get the row data from the table
      const row = table.getRowModel().rows[rowIndex]
      if (!row) return

      const rowData = row.original
      const originalValue = rowData[columnName]

      // If value hasn't changed, do nothing
      if (originalValue === newValue) return

      // Build primary key
      const primaryKey: Record<string, unknown> = {}
      for (const pkCol of pkColumns) {
        primaryKey[pkCol.name] = rowData[pkCol.name]
      }

      const editId = `${rowIndex}-${columnName}`

      setCellEdits((prev) => {
        // Check if there's already an edit for this cell
        const existingIndex = prev.findIndex((edit) => edit.id === editId)

        if (existingIndex >= 0) {
          // Update existing edit
          const existingEdit = prev[existingIndex]
          // If new value equals original, remove the edit
          if (existingEdit.originalValue === newValue) {
            return prev.filter((_, i) => i !== existingIndex)
          }
          // Otherwise update the edit
          return prev.map((edit, i) => (i === existingIndex ? { ...edit, newValue } : edit))
        }

        // Add new edit
        return [
          ...prev,
          {
            id: editId,
            rowIndex,
            primaryKey,
            columnName,
            originalValue,
            newValue,
          },
        ]
      })
    },
    [table, pkColumns]
  )

  // Handler for canceling a cell edit
  const handleCellEditCancel = React.useCallback((rowIndex: number, columnName: string) => {
    const editId = `${rowIndex}-${columnName}`
    setCellEdits((prev) => prev.filter((edit) => edit.id !== editId))
  }, [])

  const handleSaveAll = React.useCallback(async () => {
    if (editableRows.length === 0 && cellEdits.length === 0) return

    const results: InsertResult[] = []
    const successfulRowIds: string[] = []
    const successfulEditIds: string[] = []
    let hasFailures = false

    // Process cell edits (updates) first - group by row for efficiency
    const editsByRow = new Map<string, CellEdit[]>()
    for (const edit of cellEdits) {
      const key = JSON.stringify(edit.primaryKey)
      const existing = editsByRow.get(key) || []
      editsByRow.set(key, [...existing, edit])
    }

    // Process each row's updates
    for (const [, edits] of editsByRow) {
      const primaryKey = edits[0].primaryKey
      const updates: Record<string, unknown> = {}
      for (const edit of edits) {
        updates[edit.columnName] = edit.newValue
      }

      try {
        const response = await updateRowMutation.mutateAsync({ primaryKey, updates })

        // Mark all edits for this row as successful
        for (const edit of edits) {
          results.push({
            id: edit.id,
            status: 'success' as const,
            sql: response.sql,
          })
          successfulEditIds.push(edit.id)
        }
      } catch (error: unknown) {
        hasFailures = true
        const message = error instanceof Error ? error.message : 'An unknown error occurred'
        const sql = (error as { response?: { sql?: string } })?.response?.sql ?? undefined

        // Mark all edits for this row as failed
        for (const edit of edits) {
          results.push({
            id: edit.id,
            status: 'error' as const,
            sql,
            error: message,
          })
        }
      }
    }

    // Process new rows (inserts)
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

    // Remove successful cell edits
    setCellEdits((prev) => prev.filter((edit) => !successfulEditIds.includes(edit.id)))

    // Refresh table data to show changes
    if (successfulRowIds.length > 0 || successfulEditIds.length > 0) {
      queryClient.invalidateQueries({
        queryKey: ['databases', dbIndex, 'tables', tableName],
      })
    }

    // Only show dialog if there were failures
    if (hasFailures) {
      setInsertResults(results)
      setShowInsertDialog(true)
    }
  }, [editableRows, cellEdits, insertRowMutation, updateRowMutation, queryClient, dbIndex, tableName])

  // Immediate discard when user clicks the discard button (intentional action)
  const handleDiscardAll = React.useCallback(() => {
    setEditableRows([])
    setCellEdits([])
  }, [])

  // Handle navigation blocking - discard and proceed with navigation
  const handleBlockedDiscard = React.useCallback(() => {
    setEditableRows([])
    setCellEdits([])
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
  const hasCellEdits = cellEdits.length > 0
  const hasPendingChanges = hasEditableRows || hasCellEdits
  const canEditCells = !isView && pkColumns.length > 0

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
          onSaveChanges={hasPendingChanges ? handleSaveAll : undefined}
          onDiscardChanges={hasPendingChanges ? handleDiscardAll : undefined}
          hasEditableRows={hasPendingChanges}
        />
      )}

      {/* Filter rows section */}
      {showFilterPanel && <DataTableFilterPanel onClearFilters={handleClearFilters} />}

      {/* Table */}
      <DataTableGrid
        editableRows={editableRows}
        onEditCellChange={handleCellChange}
        onEditRowRemove={handleRemoveRow}
        cellEdits={cellEdits}
        onCellEditCommit={canEditCells ? handleCellEditCommit : undefined}
        onCellEditCancel={handleCellEditCancel}
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
