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
  isOpen: boolean
  onOpenChange: (isOpen: boolean) => void
  onSuccess?: () => void
  dbId: string
  tableName: string
  tableType: 'table' | 'view'
}

export function DropTableDialog({ isOpen, onOpenChange, onSuccess, dbId, tableName, tableType }: DropTableDialogProps) {
  const dropTableMutation = useDropTable(dbId)
  const [errorMessage, setErrorMessage] = React.useState<string | null>(null)

  const handleDrop = React.useCallback(async () => {
    try {
      setErrorMessage(null)
      await dropTableMutation.mutateAsync(tableName)
      onSuccess?.()
      onOpenChange(false)
    } catch (error) {
      const message = error instanceof Error ? error.message : 'An unknown error occurred'
      setErrorMessage(message)
      console.error('Drop table failed:', error)
    }
  }, [dropTableMutation, tableName, onOpenChange, onSuccess])

  // Reset error when dialog opens/closes
  React.useEffect(() => {
    if (!isOpen) {
      setErrorMessage(null)
    }
  }, [isOpen])

  // Show error dialog if drop failed
  if (errorMessage) {
    return (
      <AlertDialog open={isOpen} onOpenChange={onOpenChange}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Drop Failed</AlertDialogTitle>
          </AlertDialogHeader>

          <AlertDialogDescription>
            <div className="flex items-start flex-col space-y-5">
              <p>Failed to drop this {tableType}:</p>
              <ul className="list-disc list-inside">
                <li>
                  <span className="font-mono text-sm font-semibold text-foreground px-1.5 py-0.5 rounded border bg-muted/50">
                    {tableName}
                  </span>
                </li>
              </ul>
            </div>
          </AlertDialogDescription>

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
    <AlertDialog open={isOpen} onOpenChange={onOpenChange}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Drop {tableType === 'table' ? 'Table' : 'View'}</AlertDialogTitle>
        </AlertDialogHeader>

        <AlertDialogDescription>
          <div className="flex items-start flex-col space-y-5">
            <p>
              You're about to <span className="font-bold">permanently</span> drop this {tableType} and all its data:
            </p>
            <ul className="list-disc list-inside">
              <li>
                <span className="font-mono text-sm font-semibold text-foreground px-1.5 py-0.5 rounded border bg-muted/50">
                  {tableName}
                </span>
              </li>
            </ul>
          </div>
        </AlertDialogDescription>

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
