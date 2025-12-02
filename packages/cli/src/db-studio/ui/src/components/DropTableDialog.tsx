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
import { useDropTable } from '@/hooks/useDropTable'

type DropTableDialogProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
  dbIndex: string
  tableName: string
  tableType: 'table' | 'view'
}

export function DropTableDialog({ open, onOpenChange, dbIndex, tableName, tableType }: DropTableDialogProps) {
  const dropTableMutation = useDropTable(dbIndex)
  const [errorMessage, setErrorMessage] = React.useState<string | null>(null)

  const handleDrop = React.useCallback(async () => {
    try {
      setErrorMessage(null)
      await dropTableMutation.mutateAsync(tableName)
      onOpenChange(false)
    } catch (error) {
      const message = error instanceof Error ? error.message : 'An unknown error occurred'
      setErrorMessage(message)
      console.error('Drop table failed:', error)
    }
  }, [dropTableMutation, tableName, onOpenChange])

  // Reset error when dialog opens/closes
  React.useEffect(() => {
    if (!open) {
      setErrorMessage(null)
    }
  }, [open])

  // Show error dialog if drop failed
  if (errorMessage) {
    return (
      <AlertDialog open={open} onOpenChange={onOpenChange}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Drop Failed</AlertDialogTitle>
            <AlertDialogDescription>
              Failed to drop the {tableType} "{tableName}".
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
                handleDrop()
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
          <AlertDialogTitle>
            Drop {tableType === 'table' ? 'Table' : 'View'}: {tableName}
          </AlertDialogTitle>
          <AlertDialogDescription>
            Are you sure you want to drop this {tableType}? This action cannot be undone and all data will be
            permanently lost.
          </AlertDialogDescription>
        </AlertDialogHeader>

        <div className="rounded-md border border-destructive/50 bg-destructive/10 p-3">
          <div className="text-sm font-medium text-destructive">Warning: This is a destructive operation</div>
          <div className="text-xs text-destructive/80 mt-1">
            The {tableType} "{tableName}" and all its data will be permanently deleted.
          </div>
        </div>

        <AlertDialogFooter>
          <AlertDialogCancel disabled={dropTableMutation.isPending}>Cancel</AlertDialogCancel>
          <AlertDialogAction
            onClick={(e) => {
              e.preventDefault()
              handleDrop()
            }}
            disabled={dropTableMutation.isPending}
            className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
          >
            {dropTableMutation.isPending ? 'Dropping...' : 'Drop'}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
