import * as React from 'react'
import type { Row } from '@tanstack/react-table'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { SQLCodeBlock } from '@/components/ui/sql-code-block'
import { useDeleteRows } from '@/hooks/useDeleteRows'
import { useDataTable } from '@/contexts/DataTableContext'
import type { ColumnMetadata } from '@/lib/types'

type DeleteRowsDialogProps = {
  isOpen: boolean
  onOpenChange: (isOpen: boolean) => void
}

/**
 * Extract primary key values from a row
 */
function extractPrimaryKeys(row: Row<Record<string, unknown>>, columns: ColumnMetadata[]): Record<string, unknown> {
  const pkColumns = columns.filter((col) => col.primaryKey)
  const pkValues: Record<string, unknown> = {}

  for (const col of pkColumns) {
    pkValues[col.name] = row.original[col.name]
  }

  return pkValues
}

/**
 * Format primary key values for display
 */
function formatPrimaryKeyDisplay(pk: Record<string, unknown>): string {
  const entries = Object.entries(pk)
  if (entries.length === 0) return '(no primary key)'
  return entries.map(([key, value]) => `${key}: ${value}`).join(', ')
}

export function DeleteRowsDialog({ isOpen, onOpenChange }: DeleteRowsDialogProps) {
  const { table, columns } = useDataTable()
  const selectedRows = table.getFilteredSelectedRowModel().rows
  const deleteRowsMutation = useDeleteRows()
  const [errorMessage, setErrorMessage] = React.useState<string | null>(null)
  const [errorSql, setErrorSql] = React.useState<string | null>(null)

  // Extract primary keys from all selected rows
  const primaryKeysToDelete = React.useMemo(() => {
    return selectedRows.map((row) => extractPrimaryKeys(row, columns))
  }, [selectedRows, columns])

  // Check if table has primary keys
  const hasPrimaryKeys = columns.some((col) => col.primaryKey)

  const handleDelete = React.useCallback(async () => {
    if (!hasPrimaryKeys) {
      console.error('Cannot delete rows: table has no primary key')
      return
    }

    console.log('Starting delete operation...')
    try {
      setErrorMessage(null) // Clear any previous errors
      setErrorSql(null)
      await deleteRowsMutation.mutateAsync(primaryKeysToDelete)
      console.log('Delete successful, closing dialog')
      onOpenChange(false)
    } catch (error: unknown) {
      // Show error message in alert dialog
      const message = error instanceof Error ? error.message : 'An unknown error occurred'
      // Extract SQL from error response if available
      const sql = (error as { response?: { sql?: string } })?.response?.sql ?? null
      console.log('Delete failed with error:', message, error)
      setErrorMessage(message)
      setErrorSql(sql)
      console.error('Delete failed:', error)
      // Keep dialog open to show error
    }
  }, [hasPrimaryKeys, deleteRowsMutation, primaryKeysToDelete, onOpenChange])

  // Reset error when dialog opens/closes
  React.useEffect(() => {
    if (!isOpen) {
      setErrorMessage(null)
      setErrorSql(null)
    }
  }, [isOpen])

  if (!hasPrimaryKeys) {
    return (
      <AlertDialog open={isOpen} onOpenChange={onOpenChange}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Cannot Delete Rows</AlertDialogTitle>
            <AlertDialogDescription>
              This table has no primary key. Deleting rows requires a primary key to identify specific rows.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Close</AlertDialogCancel>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    )
  }

  // Show error dialog if deletion failed
  if (errorMessage) {
    return (
      <AlertDialog open={isOpen} onOpenChange={onOpenChange}>
        <AlertDialogContent className="max-w-lg">
          <AlertDialogHeader>
            <AlertDialogTitle>Delete Failed</AlertDialogTitle>
            <AlertDialogDescription>
              Failed to delete the selected {selectedRows.length === 1 ? 'row' : 'rows'}.
            </AlertDialogDescription>
          </AlertDialogHeader>

          {/* Show error message */}
          <div className="rounded-md border border-destructive/50 bg-destructive/10 p-3">
            <div className="text-sm text-destructive">{errorMessage}</div>
          </div>

          {/* Show SQL query if available */}
          {errorSql && <SQLCodeBlock sql={errorSql} />}

          <AlertDialogFooter>
            <AlertDialogCancel
              onClick={() => {
                setErrorMessage(null)
                setErrorSql(null)
              }}
            >
              Close
            </AlertDialogCancel>
            <AlertDialogAction
              onClick={(e) => {
                e.preventDefault() // Prevent dialog from auto-closing
                setErrorMessage(null)
                setErrorSql(null)
                // Retry the delete
                handleDelete()
              }}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              Retry
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    )
  }

  return (
    <AlertDialog open={isOpen} onOpenChange={onOpenChange}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>
            Delete {selectedRows.length} {selectedRows.length === 1 ? 'Row' : 'Rows'}
          </AlertDialogTitle>
          <AlertDialogDescription>
            Are you sure you want to delete {selectedRows.length === 1 ? 'this row' : 'these rows'}? This action cannot
            be undone.
          </AlertDialogDescription>
        </AlertDialogHeader>

        {/* Show preview of rows to be deleted (limit to first 5) */}
        {selectedRows.length > 0 && (
          <div className="max-h-32 overflow-auto rounded-md border border-border bg-muted/30 p-3">
            <div className="space-y-1 font-mono text-xs">
              {primaryKeysToDelete.slice(0, 5).map((pk, idx) => (
                <div key={idx} className="text-muted-foreground">
                  {formatPrimaryKeyDisplay(pk)}
                </div>
              ))}
              {selectedRows.length > 5 && (
                <div className="text-muted-foreground italic">
                  ... and {selectedRows.length - 5} more {selectedRows.length - 5 === 1 ? 'row' : 'rows'}
                </div>
              )}
            </div>
          </div>
        )}

        <AlertDialogFooter>
          <AlertDialogCancel disabled={deleteRowsMutation.isPending}>Cancel</AlertDialogCancel>
          <AlertDialogAction
            onClick={(e) => {
              e.preventDefault() // Prevent dialog from auto-closing
              handleDelete()
            }}
            disabled={deleteRowsMutation.isPending}
            className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
          >
            {deleteRowsMutation.isPending ? 'Deleting...' : 'Delete'}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
