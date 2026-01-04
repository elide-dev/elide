import * as React from 'react'
import { CheckCircle2, XCircle, ChevronDown, ChevronRight } from 'lucide-react'
import {
  AlertDialog,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { Item, ItemMedia, ItemContent, ItemTitle } from '@/components/ui/item'
import { SQLCodeBlock } from '@/components/ui/sql-code-block'
import { cn } from '@/lib/utils'

export type InsertResult = {
  id: string
  status: 'success' | 'error'
  sql?: string
  error?: string
}

type InsertRowDialogProps = {
  isOpen: boolean
  onOpenChange: (isOpen: boolean) => void
  results: InsertResult[]
  onClose: () => void
}

function SQLResultItem({ result }: { result: InsertResult }) {
  const [expanded, setExpanded] = React.useState(false)
  const isError = result.status === 'error'
  const isSuccess = result.status === 'success'

  // Show SQL in syntax-highlighted block when expanded
  const showExpandable = !!result.sql || isError

  return (
    <Item
      variant="outline"
      size="sm"
      className={cn('transition-all', showExpandable && 'cursor-pointer')}
      onClick={() => showExpandable && setExpanded(!expanded)}
    >
      <ItemMedia>
        {isSuccess && <CheckCircle2 className="h-5 w-5 text-green-500 shrink-0" />}
        {isError && <XCircle className="h-5 w-5 text-destructive shrink-0" />}
      </ItemMedia>

      <ItemContent className="min-w-0 flex-1">
        {!expanded ? (
          <ItemTitle className="font-mono text-xs">
            {/* SQL on one line, truncated */}
            <span className="break-all line-clamp-1">{result.sql || 'Insert row'}</span>
          </ItemTitle>
        ) : (
          <div className="space-y-3">
            {/* Show SQL with syntax highlighting when expanded */}
            {result.sql && <SQLCodeBlock sql={result.sql} format={true} />}

            {/* Show error message if there's an error */}
            {isError && result.error && (
              <div className="bg-destructive/10 px-4 py-3 rounded-md border border-destructive/20">
                <div className="text-xs font-semibold text-destructive/80 mb-1">Error:</div>
                <div className="text-sm text-destructive font-medium">{result.error}</div>
              </div>
            )}
          </div>
        )}
      </ItemContent>

      {/* Expand/collapse indicator */}
      {showExpandable && (
        <div className="text-muted-foreground shrink-0">
          {expanded ? <ChevronDown className="h-4 w-4" /> : <ChevronRight className="h-4 w-4" />}
        </div>
      )}
    </Item>
  )
}

export function InsertRowDialog({ isOpen, onOpenChange, results, onClose }: InsertRowDialogProps) {
  const hasResults = results.length > 0
  const successCount = results.filter((r) => r.status === 'success').length
  const errorCount = results.filter((r) => r.status === 'error').length

  const getDescription = () => {
    if (successCount > 0 && errorCount > 0) {
      return `${successCount} row${successCount !== 1 ? 's' : ''} inserted successfully, but ${errorCount} row${errorCount !== 1 ? 's' : ''} failed.`
    }
    if (errorCount > 0) {
      return `All ${errorCount} row${errorCount !== 1 ? 's' : ''} failed to insert.`
    }
    return 'Review the results below.'
  }

  return (
    <AlertDialog open={isOpen} onOpenChange={onOpenChange}>
      <AlertDialogContent className="max-w-3xl">
        <AlertDialogHeader>
          <AlertDialogTitle className="flex items-center gap-2">
            <XCircle className="h-5 w-5 text-destructive" />
            Insert Failed
          </AlertDialogTitle>
          <AlertDialogDescription>{getDescription()}</AlertDialogDescription>
        </AlertDialogHeader>

        {/* Results list */}
        {hasResults && (
          <div className="max-h-96 overflow-y-auto rounded-md space-y-6">
            {results.map((result) => (
              <SQLResultItem key={result.id} result={result} />
            ))}
          </div>
        )}

        <AlertDialogFooter>
          <AlertDialogCancel onClick={onClose}>Close</AlertDialogCancel>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
