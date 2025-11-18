import * as React from 'react'
import { ChevronDown, RefreshCw } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { HoverCard, HoverCardContent, HoverCardTrigger } from '@/components/ui/hover-card'

type DataTablePaginationProps = {
  limit: number
  offset: number
  limitInput: string
  offsetInput: string
  onPaginationChange: (limit: number, offset: number) => void
  onRefresh?: () => void
  isLoading?: boolean
  setLimitInput: (value: string) => void
  setOffsetInput: (value: string) => void
}

export const DataTablePagination = React.memo(function DataTablePagination({
  limit,
  offset,
  limitInput,
  offsetInput,
  onPaginationChange,
  onRefresh,
  isLoading = false,
  setLimitInput,
  setOffsetInput,
}: DataTablePaginationProps) {
  return (
    <div className="flex items-center">
      <HoverCard openDelay={200}>
        <HoverCardTrigger asChild>
          <Button
            variant="outline"
            size="sm"
            onClick={() => {
              const newOffset = Math.max(0, offset - limit)
              onPaginationChange(limit, newOffset)
            }}
            disabled={offset === 0}
            className="h-9 w-9 p-0 rounded-r-none border-r-0"
          >
            <ChevronDown className="h-4 w-4 rotate-90" />
          </Button>
        </HoverCardTrigger>
        <HoverCardContent side="bottom" className="w-auto px-3 py-1.5">
          <span className="text-xs font-semibold">Previous Page</span>
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
              const newLimit = Math.max(1, Math.min(1000, parseInt(e.target.value) || 100))
              setLimitInput(String(newLimit))
              onPaginationChange(newLimit, offset)
            }}
            onKeyDown={(e) => {
              if (e.key === 'Enter') {
                e.currentTarget.blur()
              }
            }}
            className="h-9 w-20 text-center font-mono text-sm rounded-none border-r-0 [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none focus-visible:z-10"
            min="1"
            max="1000"
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
              const newOffset = Math.max(0, parseInt(e.target.value) || 0)
              setOffsetInput(String(newOffset))
              onPaginationChange(limit, newOffset)
            }}
            onKeyDown={(e) => {
              if (e.key === 'Enter') {
                e.currentTarget.blur()
              }
            }}
            className="h-9 w-20 text-center font-mono text-sm rounded-none border-r-0 [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none focus-visible:z-10"
            min="0"
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
              const newOffset = offset + limit
              onPaginationChange(limit, newOffset)
            }}
            className="h-9 w-9 p-0 rounded-l-none"
          >
            <ChevronDown className="h-4 w-4 -rotate-90" />
          </Button>
        </HoverCardTrigger>
        <HoverCardContent side="bottom" className="w-auto px-3 py-1.5">
          <span className="text-xs font-semibold">Next Page</span>
        </HoverCardContent>
      </HoverCard>
      {/* Refresh button */}
      {onRefresh && (
        <HoverCard openDelay={200}>
          <HoverCardTrigger asChild>
            <Button
              variant="outline"
              size="sm"
              onClick={onRefresh}
              disabled={isLoading}
              className="h-9 w-9 p-0 ml-2"
            >
              <RefreshCw className={`h-4 w-4 ${isLoading ? 'animate-spin' : ''}`} />
            </Button>
          </HoverCardTrigger>
          <HoverCardContent side="bottom" className="w-auto px-3 py-1.5">
            <span className="text-xs font-semibold">Refresh rows</span>
          </HoverCardContent>
        </HoverCard>
      )}
    </div>
  )
})
