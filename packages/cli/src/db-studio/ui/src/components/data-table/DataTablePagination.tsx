import * as React from 'react'
import { ChevronDown } from 'lucide-react'
import { useParams } from 'react-router-dom'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { HoverCard, HoverCardContent, HoverCardTrigger } from '@/components/ui/hover-card'
import { useDataTable } from '@/contexts/DataTableContext'
import { MIN_LIMIT, MAX_LIMIT, MIN_OFFSET } from '@/lib/constants'
import { useDbLocalStorage } from '@/hooks/useLocalStorage'

export const DataTablePagination = React.memo(function DataTablePagination() {
  const { dbId } = useParams()
  const { pagination, onPaginationChange } = useDataTable()
  const [, saveLimitPreference] = useDbLocalStorage(dbId, 'pagination-limit', pagination.limit)

  // Pagination is always enabled now (no editMode in context anymore)
  const isDisabled = false

  // Local state for input values (allows typing before applying)
  const [limitInput, setLimitInput] = React.useState(String(pagination.limit))
  const [offsetInput, setOffsetInput] = React.useState(String(pagination.offset))

  // Sync input values when pagination changes externally (from URL)
  React.useEffect(() => {
    setLimitInput(String(pagination.limit))
    setOffsetInput(String(pagination.offset))
  }, [pagination.limit, pagination.offset])

  return (
    <div className="flex items-center">
      <HoverCard openDelay={200}>
        <HoverCardTrigger asChild>
          <Button
            variant="outline"
            size="sm"
            onClick={() => {
              const newOffset = Math.max(MIN_OFFSET, pagination.offset - pagination.limit)
              onPaginationChange({ limit: pagination.limit, offset: newOffset })
            }}
            disabled={pagination.offset === 0 || isDisabled}
            className="h-9 w-9 p-0 rounded-r-none border-r-0"
          >
            <ChevronDown className="h-4 w-4 rotate-90" />
          </Button>
        </HoverCardTrigger>
        <HoverCardContent side="bottom" className="w-auto px-3 py-1.5">
          <span className="text-xs font-semibold">{isDisabled ? 'Finish editing to navigate' : 'Previous Page'}</span>
        </HoverCardContent>
      </HoverCard>
      <HoverCard openDelay={200}>
        <HoverCardTrigger asChild>
          <Input
            type="number"
            value={limitInput}
            onChange={(e) => {
              setLimitInput(e.target.value)
            }}
            onBlur={(e) => {
              const newLimit = Math.max(MIN_LIMIT, Math.min(MAX_LIMIT, parseInt(e.target.value) || MIN_LIMIT))
              setLimitInput(String(newLimit))
              saveLimitPreference(newLimit)
              onPaginationChange({ limit: newLimit, offset: pagination.offset })
            }}
            onKeyDown={(e) => {
              if (e.key === 'Enter') {
                e.currentTarget.blur()
              }
            }}
            className="h-9 w-20 text-center font-mono text-sm rounded-none border-r-0 [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none focus-visible:z-10"
            min={MIN_LIMIT}
            max={MAX_LIMIT}
            disabled={isDisabled}
          />
        </HoverCardTrigger>
        <HoverCardContent side="bottom" className="w-auto px-3 py-1.5">
          <span className="text-xs font-semibold">LIMIT</span>
        </HoverCardContent>
      </HoverCard>
      <HoverCard openDelay={200}>
        <HoverCardTrigger asChild>
          <Input
            type="number"
            value={offsetInput}
            onChange={(e) => {
              setOffsetInput(e.target.value)
            }}
            onBlur={(e) => {
              const newOffset = Math.max(MIN_OFFSET, parseInt(e.target.value) || MIN_OFFSET)
              setOffsetInput(String(newOffset))
              onPaginationChange({ limit: pagination.limit, offset: newOffset })
            }}
            onKeyDown={(e) => {
              if (e.key === 'Enter') {
                e.currentTarget.blur()
              }
            }}
            className="h-9 w-20 text-center font-mono text-sm rounded-none border-r-0 [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none focus-visible:z-10"
            min={MIN_OFFSET}
            disabled={isDisabled}
          />
        </HoverCardTrigger>
        <HoverCardContent side="bottom" className="w-auto px-3 py-1.5">
          <span className="text-xs font-semibold">OFFSET</span>
        </HoverCardContent>
      </HoverCard>
      <HoverCard openDelay={200}>
        <HoverCardTrigger asChild>
          <Button
            variant="outline"
            size="sm"
            onClick={() => {
              const newOffset = pagination.offset + pagination.limit
              onPaginationChange({ limit: pagination.limit, offset: newOffset })
            }}
            disabled={isDisabled}
            className="h-9 w-9 p-0 rounded-l-none"
          >
            <ChevronDown className="h-4 w-4 -rotate-90" />
          </Button>
        </HoverCardTrigger>
        <HoverCardContent side="bottom" className="w-auto px-3 py-1.5">
          <span className="text-xs font-semibold">{isDisabled ? 'Finish editing to navigate' : 'Next Page'}</span>
        </HoverCardContent>
      </HoverCard>
    </div>
  )
})
