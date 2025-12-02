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
  isOpen: boolean
  onOpenChange: (isOpen: boolean) => void
  dbIndex: string
  tableName: string
}

export function TruncateTableDialog({ isOpen, onOpenChange, dbIndex, tableName }: TruncateTableDialogProps) {
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
    if (!isOpen) {
      setErrorMessage(null)
    }
  }, [isOpen])

  // Show error dialog if truncate failed
  if (errorMessage) {
    return (
      <AlertDialog open={isOpen} onOpenChange={onOpenChange}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Truncate Failed</AlertDialogTitle>
          </AlertDialogHeader>

          <AlertDialogDescription>
            <div className="flex items-start flex-col space-y-5">
              <p>Failed to truncate this table:</p>
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
    <AlertDialog open={isOpen} onOpenChange={onOpenChange}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Truncate Table</AlertDialogTitle>
        </AlertDialogHeader>

        <AlertDialogDescription>
          <div className="flex items-start flex-col space-y-5">
            <p>
              You're about to <span className="font-bold">permanently</span> delete all rows in this table:
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
