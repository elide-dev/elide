import * as React from 'react'
import { CheckCircle2, XCircle, ChevronDown, ChevronRight, Loader2 } from 'lucide-react'
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
import { Item, ItemGroup, ItemMedia, ItemContent, ItemTitle, ItemDescription } from '@/components/ui/item'
import { cn } from '@/lib/utils'

export type InsertResult = {
  id: string
  status: 'pending' | 'success' | 'error'
  sql?: string
  error?: string
}

type InsertRowDialogProps = {
  isOpen: boolean
  onOpenChange: (isOpen: boolean) => void
  results: InsertResult[]
  onRetry: () => void
  onClose: () => void
}

function SQLResultItem({ result }: { result: InsertResult }) {
  const [expanded, setExpanded] = React.useState(false)
  const isError = result.status === 'error'
  const isPending = result.status === 'pending'
  const isSuccess = result.status === 'success'

  // Check if SQL is too long (roughly more than ~60 chars)
  const sqlTooLong = result.sql && result.sql.length > 60

  return (
    <Item
      variant="outline"
      size="sm"
      className={cn(
        'cursor-pointer transition-all',
        isError && 'border-destructive/30 bg-destructive/5',
        isSuccess && 'border-green-500/30 bg-green-500/5',
        isPending && 'border-muted bg-muted/30'
      )}
      onClick={() => (sqlTooLong || isError) && setExpanded(!expanded)}
    >
      <ItemMedia>
        {isPending && <Loader2 className="h-5 w-5 text-muted-foreground animate-spin" />}
        {isSuccess && <CheckCircle2 className="h-5 w-5 text-green-500" />}
        {isError && <XCircle className="h-5 w-5 text-destructive" />}
      </ItemMedia>

      <ItemContent className="min-w-0 flex-1">
        <ItemTitle className="font-mono text-xs">
          {/* SQL on one line, truncated */}
          <span className={cn('break-all', !expanded && 'line-clamp-1')}>
            {result.sql || 'Preparing...'}
          </span>
        </ItemTitle>

        {/* Show error message when expanded or always if there's an error */}
        {expanded && isError && result.error && (
          <ItemDescription className="text-destructive mt-1 text-xs">
            {result.error}
          </ItemDescription>
        )}
      </ItemContent>

      {/* Expand/collapse indicator */}
      {(sqlTooLong || isError) && (
        <div className="text-muted-foreground">
          {expanded ? <ChevronDown className="h-4 w-4" /> : <ChevronRight className="h-4 w-4" />}
        </div>
      )}
    </Item>
  )
}

export function InsertRowDialog({ isOpen, onOpenChange, results, onRetry, onClose }: InsertRowDialogProps) {
  const hasResults = results.length > 0
  const isPending = results.some((r) => r.status === 'pending')
  const hasErrors = results.some((r) => r.status === 'error')
  const allSuccess = hasResults && results.every((r) => r.status === 'success')
  const successCount = results.filter((r) => r.status === 'success').length
  const errorCount = results.filter((r) => r.status === 'error').length

  // Auto-close on success after a brief delay
  React.useEffect(() => {
    if (allSuccess && !isPending) {
      const timer = setTimeout(() => {
        onClose()
      }, 800)
      return () => clearTimeout(timer)
    }
  }, [allSuccess, isPending, onClose])

  const getTitle = () => {
    if (isPending) return 'Inserting Rows...'
    if (allSuccess) return 'Insert Complete'
    if (hasErrors) return 'Insert Failed'
    return 'Insert Rows'
  }

  const getDescription = () => {
    if (isPending) return 'Please wait while the rows are being inserted...'
    if (allSuccess) return `Successfully inserted ${successCount} row${successCount !== 1 ? 's' : ''}.`
    if (hasErrors)
      return `${errorCount} of ${results.length} row${results.length !== 1 ? 's' : ''} failed to insert.`
    return 'Review the results below.'
  }

  return (
    <AlertDialog open={isOpen} onOpenChange={onOpenChange}>
      <AlertDialogContent className="max-w-xl">
        <AlertDialogHeader>
          <AlertDialogTitle className="flex items-center gap-2">
            {isPending && <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />}
            {allSuccess && <CheckCircle2 className="h-5 w-5 text-green-500" />}
            {hasErrors && !isPending && <XCircle className="h-5 w-5 text-destructive" />}
            {getTitle()}
          </AlertDialogTitle>
          <AlertDialogDescription>{getDescription()}</AlertDialogDescription>
        </AlertDialogHeader>

        {/* Results list */}
        {hasResults && (
          <div className="max-h-64 overflow-y-auto rounded-md border border-border">
            <ItemGroup>
              {results.map((result) => (
                <SQLResultItem key={result.id} result={result} />
              ))}
            </ItemGroup>
          </div>
        )}

        <AlertDialogFooter>
          <AlertDialogCancel onClick={onClose} disabled={isPending}>
            {allSuccess ? 'Done' : 'Close'}
          </AlertDialogCancel>
          {hasErrors && !isPending && (
            <AlertDialogAction
              onClick={(e) => {
                e.preventDefault()
                onRetry()
              }}
            >
              Retry Failed
            </AlertDialogAction>
          )}
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
