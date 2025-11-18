import * as React from 'react'
import type { Table } from '@tanstack/react-table'

import { Badge } from '@/components/ui/badge'
import { HoverCard, HoverCardContent, HoverCardTrigger } from '@/components/ui/hover-card'
import { Skeleton } from '@/components/ui/skeleton'
import { formatRowCount } from '@/lib/utils'
import type { QueryMetadata } from './DataTable'
import { FilterButton } from './FilterButton'
import { ColumnsDropdown } from './ColumnsDropdown'
import { DataTablePagination } from './DataTablePagination'

type DataTableToolbarProps = {
  tableName?: string
  tableRowCount?: number
  showControls?: boolean
  showMetadata?: boolean
  showPagination?: boolean
  metadata?: QueryMetadata
  rows: unknown[][]
  // Filter button props
  showFilters: boolean
  activeFilterCount: number
  onFilterToggle: () => void
  onFiltersChange?: (filters: any[]) => void
  // Column dropdown props
  table: Table<Record<string, unknown>>
  columnSearch: string
  setColumnSearch: (value: string) => void
  // Pagination props
  pagination: {
    limit: number
    offset: number
  }
  paginationInputs: {
    limitInput: string
    offsetInput: string
    setLimitInput: (value: string) => void
    setOffsetInput: (value: string) => void
  }
  onPaginationChange: (limit: number, offset: number) => void
  onRefresh?: () => void
  isLoading?: boolean
}

export const DataTableToolbar = React.memo(function DataTableToolbar({
  tableName,
  tableRowCount,
  showControls = true,
  showMetadata = true,
  showPagination = true,
  metadata,
  rows,
  showFilters,
  activeFilterCount,
  onFilterToggle,
  onFiltersChange,
  table,
  columnSearch,
  setColumnSearch,
  pagination,
  paginationInputs,
  onPaginationChange,
  onRefresh,
  isLoading = false,
}: DataTableToolbarProps) {
  return (
    <div className="flex items-center gap-2 px-6 py-4 border-b border-gray-800 bg-gray-950 shrink-0">
      {tableName && (
        <div className="flex items-center gap-3 mr-4">
          <h2 className="text-lg font-semibold tracking-tight truncate">{tableName}</h2>
          {tableRowCount ? (
            <HoverCard openDelay={200}>
              <HoverCardTrigger asChild>
                <Badge variant="secondary" className="shrink-0 cursor-default">
                  {formatRowCount(tableRowCount)} rows
                </Badge>
              </HoverCardTrigger>
              <HoverCardContent side="bottom" className="w-auto px-3 py-1.5">
                <span className="text-xs font-semibold">{tableRowCount.toLocaleString()} total rows</span>
              </HoverCardContent>
            </HoverCard>
          ) : (
            <Skeleton className="w-16 h-4" />
          )}
        </div>
      )}
      {showControls && onFiltersChange && (
        <FilterButton
          showFilters={showFilters}
          activeFilterCount={activeFilterCount}
          onToggle={onFilterToggle}
        />
      )}
      {showControls && <ColumnsDropdown table={table} columnSearch={columnSearch} setColumnSearch={setColumnSearch} />}

      <div className="flex items-center gap-2 ml-auto">
        {showMetadata && metadata?.executionTimeMs !== undefined && (
          <div className="flex items-center gap-1.5 px-3 py-1.5 bg-gray-900/50 border border-gray-800 rounded-md">
            <span className="text-xs text-gray-400">{rows.length} rows ⋅</span>
            <span className="text-xs font-mono font-semibold text-gray-400">{metadata.executionTimeMs}ms</span>
          </div>
        )}

        {/* Server-side pagination controls */}
        {showPagination && (
          <DataTablePagination
            limit={pagination.limit}
            offset={pagination.offset}
            limitInput={paginationInputs.limitInput}
            offsetInput={paginationInputs.offsetInput}
            onPaginationChange={onPaginationChange}
            onRefresh={onRefresh}
            isLoading={isLoading}
            setLimitInput={paginationInputs.setLimitInput}
            setOffsetInput={paginationInputs.setOffsetInput}
          />
        )}
      </div>
    </div>
  )
})
