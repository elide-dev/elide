import * as React from 'react'

import { Badge } from '@/components/ui/badge'
import { HoverCard, HoverCardContent, HoverCardTrigger } from '@/components/ui/hover-card'
import { Skeleton } from '@/components/ui/skeleton'
import { formatRowCount } from '@/lib/utils'
import { FilterButton } from './FilterButton'
import { ColumnsDropdown } from './ColumnsDropdown'
import { DataTablePagination } from './DataTablePagination'
import { useDataTable } from '@/contexts/DataTableContext'
import { Button } from './ui/button'
import { RefreshCw } from 'lucide-react'

type DataTableToolbarProps = {
  showFilterPanel: boolean
  onFilterToggle: () => void
  onAddFilter: () => void
}

export const DataTableToolbar = React.memo(function DataTableToolbar({
  showFilterPanel,
  onFilterToggle,
  onAddFilter,
}: DataTableToolbarProps) {
  const { rowCount, metadata, config, appliedFilters, onRefresh } = useDataTable()

  // Filter toggle handler
  const handleFilterToggle = React.useCallback(() => {
    if (appliedFilters.length === 0) {
      // If no filters yet, add the first one
      onAddFilter()
    } else {
      // If filters exist, just toggle visibility
      onFilterToggle()
    }
  }, [appliedFilters.length, onAddFilter, onFilterToggle])

  return (
    <div className="flex items-center gap-2 px-6 py-4 border-b border-gray-800 bg-gray-950 shrink-0">
      {config.tableName && (
        <div className="flex items-center gap-3 mr-4">
          <h2 className="text-lg font-semibold tracking-tight truncate">{config.tableName}</h2>
          {config.tableRowCount ? (
            <HoverCard openDelay={200}>
              <HoverCardTrigger asChild>
                <Badge variant="secondary" className="shrink-0 cursor-default">
                  {formatRowCount(config.tableRowCount)} rows
                </Badge>
              </HoverCardTrigger>
              <HoverCardContent side="bottom" className="w-auto px-3 py-1.5">
                <span className="text-xs font-semibold">{config.tableRowCount.toLocaleString()} total rows</span>
              </HoverCardContent>
            </HoverCard>
          ) : (
            <Skeleton className="w-16 h-4" />
          )}
        </div>
      )}
      {config.showControls && (
        <>
          <FilterButton
            showFilters={showFilterPanel}
            activeFilterCount={appliedFilters.length}
            onToggle={handleFilterToggle}
          />
          <ColumnsDropdown />
        </>
      )}

      <div className="flex items-center gap-2 ml-auto">
        {metadata?.executionTimeMs !== undefined && (
          <div className="flex items-center gap-1.5 px-3 py-1.5 bg-gray-900/50 border border-gray-800 rounded-md">
            <span className="text-xs text-gray-400">{rowCount} rows ⋅</span>
            <span className="text-xs font-mono font-semibold text-gray-400">{metadata.executionTimeMs}ms</span>
          </div>
        )}

        {config.showPagination && <DataTablePagination />}
      </div>
      {onRefresh && (
        <Button
          variant="outline"
          size="sm"
          onClick={onRefresh}
          disabled={config.isLoading}
          className="h-9 w-9 p-0 ml-2"
        >
          <RefreshCw className={`h-4 w-4 ${config.isLoading ? 'animate-spin' : ''}`} />
        </Button>
      )}
    </div>
  )
})
