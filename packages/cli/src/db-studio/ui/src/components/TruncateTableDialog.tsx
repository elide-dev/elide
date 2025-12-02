import * as React from 'react'
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
import { useTruncateTable } from '@/hooks/useTruncateTable'

type TruncateTableDialogProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
  dbIndex: string
  tableName: string
}

export function TruncateTableDialog({ open, onOpenChange, dbIndex, tableName }: TruncateTableDialogProps) {
  const truncateTableMutation = useTruncateTable(dbIndex)
  const [errorMessage, setErrorMessage] = React.useState<string | null>(null)

  const handleTruncate = React.useCallback(async () => {
    try {
      setErrorMessage(null)
      await truncateTableMutation.mutateAsync(tableName)
      onOpenChange(false)
    } catch (error) {
      const message = error instanceof Error ? error.message : 'An unknown error occurred'
      setErrorMessage(message)
      console.error('Truncate table failed:', error)
    }
  }, [truncateTableMutation, tableName, onOpenChange])

  // Reset error when dialog opens/closes
  React.useEffect(() => {
    if (!open) {
      setErrorMessage(null)
    }
  }, [open])

  // Show error dialog if truncate failed
  if (errorMessage) {
    return (
      <AlertDialog open={open} onOpenChange={onOpenChange}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Truncate Failed</AlertDialogTitle>
            <AlertDialogDescription>
              Failed to truncate the table "{tableName}".
            </AlertDialogDescription>
          </AlertDialogHeader>

          {/* Show error message */}
          <div className="rounded-md border border-destructive/50 bg-destructive/10 p-3">
            <div className="text-sm text-destructive">{errorMessage}</div>
          </div>

          <AlertDialogFooter>
            <AlertDialogCancel onClick={() => setErrorMessage(null)}>Close</AlertDialogCancel>
            <AlertDialogAction
              onClick={(e) => {
                e.preventDefault()
                setErrorMessage(null)
                handleTruncate()
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
    <AlertDialog open={open} onOpenChange={onOpenChange}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Truncate Table: {tableName}</AlertDialogTitle>
          <AlertDialogDescription>
            Are you sure you want to truncate this table? All rows will be deleted, but the table structure will remain intact.
          </AlertDialogDescription>
        </AlertDialogHeader>

        <div className="rounded-md border border-destructive/50 bg-destructive/10 p-3">
          <div className="text-sm font-medium text-destructive">Warning: This will delete all data</div>
          <div className="text-xs text-destructive/80 mt-1">
            All rows in the table "{tableName}" will be permanently deleted. The table structure and schema will be preserved.
          </div>
        </div>

        <AlertDialogFooter>
          <AlertDialogCancel disabled={truncateTableMutation.isPending}>Cancel</AlertDialogCancel>
          <AlertDialogAction
            onClick={(e) => {
              e.preventDefault()
              handleTruncate()
            }}
            disabled={truncateTableMutation.isPending}
            className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
          >
            {truncateTableMutation.isPending ? 'Truncating...' : 'Truncate'}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
