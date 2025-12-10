import * as React from 'react'

import { Badge } from '@/components/ui/badge'
import { HoverCard, HoverCardContent, HoverCardTrigger } from '@/components/ui/hover-card'
import { Skeleton } from '@/components/ui/skeleton'
import { formatRowCount } from '@/lib/utils'
import { FilterButton } from './FilterButton'
import { ColumnsDropdown } from './ColumnsDropdown'
import { DataTablePagination } from './DataTablePagination'
import { AddRowButton } from './AddRowButton'
import { useDataTable } from '@/contexts/DataTableContext'
import { Button } from '@/components/ui/button'
import { RefreshCw, Trash2, Save, X } from 'lucide-react'

type DataTableToolbarProps = {
  showFilterPanel: boolean
  onFilterToggle: () => void
  onAddFilter: () => void
  onDeleteRows?: () => void
  onAddRow?: () => void
  onSaveChanges?: () => void
  onDiscardChanges?: () => void
  hasEditableRows?: boolean
}

export const DataTableToolbar = React.memo(function DataTableToolbar({
  showFilterPanel,
  onFilterToggle,
  onAddFilter,
  onDeleteRows,
  onAddRow,
  onSaveChanges,
  onDiscardChanges,
  hasEditableRows,
}: DataTableToolbarProps) {
  const { table, rowCount, metadata, config, appliedFilters, onRefresh } = useDataTable()

  // Get selected row count
  const selectedRowCount = table.getFilteredSelectedRowModel().rows.length

  const handleFilterToggle = React.useCallback(() => {
    onFilterToggle()
    if (!showFilterPanel && appliedFilters.length === 0) {
      onAddFilter()
    }
  }, [showFilterPanel, appliedFilters.length, onAddFilter, onFilterToggle])

  return (
    <div className="flex items-center gap-2 px-6 py-4 border-b border-border bg-background shrink-0">
      {config.tableName && (
        <div className="flex items-center gap-3 mr-4">
          <h2 className="text-lg font-semibold tracking-tight truncate">{config.tableName}</h2>
          {config.tableRowCount != undefined ? (
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
      {config.showControls && !hasEditableRows && (
        <>
          <FilterButton
            showFilters={showFilterPanel}
            activeFilterCount={appliedFilters.length}
            onToggle={handleFilterToggle}
          />
          <ColumnsDropdown />
        </>
      )}

      {onAddRow && <AddRowButton onClick={onAddRow} />}

      {onSaveChanges && hasEditableRows && (
        <Button
          variant="outline"
          size="sm"
          onClick={onSaveChanges}
          className="h-9 gap-2 bg-green-600 hover:bg-green-700 text-white"
        >
          <Save className="h-4 w-4" />
          Save Changes
        </Button>
      )}

      {onDiscardChanges && hasEditableRows && (
        <Button variant="ghost" size="sm" onClick={onDiscardChanges} className="h-9 gap-2">
          <X className="h-4 w-4" />
          Discard
        </Button>
      )}

      {selectedRowCount > 0 && onDeleteRows && !hasEditableRows && (
        <Button variant="destructive" size="sm" onClick={onDeleteRows} className="h-9 gap-2">
          <Trash2 className="h-4 w-4" />
          Delete {selectedRowCount} {selectedRowCount === 1 ? 'row' : 'rows'}
        </Button>
      )}

      <div className="flex items-center gap-2 ml-auto">
        {metadata?.executionTimeMs !== undefined && (
          <div className="flex items-center gap-1.5 px-3 py-1.5 bg-muted/50 border border-border rounded-md">
            <span className="text-xs text-muted-foreground">{rowCount} rows ⋅</span>
            <span className="text-xs font-mono font-semibold text-muted-foreground">{metadata.executionTimeMs}ms</span>
          </div>
        )}

        {config.showPagination && <DataTablePagination />}
      </div>
      {onRefresh && (
        <HoverCard openDelay={200}>
          <HoverCardTrigger asChild>
            <Button
              variant="outline"
              size="sm"
              onClick={onRefresh}
              disabled={config.isLoading}
              className="h-9 w-9 p-0 ml-2"
            >
              <RefreshCw className={`h-4 w-4 ${config.isLoading ? 'animate-spin' : ''}`} />
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
