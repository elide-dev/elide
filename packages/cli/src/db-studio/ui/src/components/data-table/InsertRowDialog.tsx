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

type InsertRowDialogProps = {
  isOpen: boolean
  onOpenChange: (isOpen: boolean) => void
  errorMessage: string | null
  isPending: boolean
  onRetry: () => void
  onClose: () => void
}

export function InsertRowDialog({
  isOpen,
  onOpenChange,
  errorMessage,
  isPending,
  onRetry,
  onClose,
}: InsertRowDialogProps) {
  // Show loading state while mutation is pending
  if (isPending) {
    return (
      <AlertDialog open={isOpen} onOpenChange={onOpenChange}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Inserting Row</AlertDialogTitle>
            <AlertDialogDescription>Please wait while the row is being inserted...</AlertDialogDescription>
          </AlertDialogHeader>
        </AlertDialogContent>
      </AlertDialog>
    )
  }

  // Show error dialog if insertion failed
  if (errorMessage) {
    return (
      <AlertDialog open={isOpen} onOpenChange={onOpenChange}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Insert Failed</AlertDialogTitle>
            <AlertDialogDescription>Failed to insert the new row.</AlertDialogDescription>
          </AlertDialogHeader>

          {/* Show error message */}
          <div className="rounded-md border border-destructive/50 bg-destructive/10 p-3">
            <div className="text-sm text-destructive">{errorMessage}</div>
          </div>

          <AlertDialogFooter>
            <AlertDialogCancel onClick={onClose}>Close</AlertDialogCancel>
            <AlertDialogAction
              onClick={(e) => {
                e.preventDefault()
                onRetry()
              }}
            >
              Retry
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    )
  }

  // Don't show anything if no error and not pending
  return null
}
