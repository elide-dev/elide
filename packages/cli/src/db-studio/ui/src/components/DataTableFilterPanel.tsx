import * as React from 'react'
import { Plus } from 'lucide-react'

import { Button } from '@/components/ui/button'
import type { Filter } from '@/lib/types'
import type { ColumnMetadata } from './DataTable'
import { FilterRow } from './FilterRow'

type DataTableFilterPanelProps = {
  draftFilters: Filter[]
  columns: ColumnMetadata[]
  hasUnappliedChanges: boolean
  onAddFilter: () => void
  onRemoveFilter: (index: number) => void
  onUpdateFilter: (index: number, updates: Partial<Filter>) => void
  onApplyFilters: () => void
  onClearFilters: () => void
}

export const DataTableFilterPanel = React.memo(function DataTableFilterPanel({
  draftFilters,
  columns,
  hasUnappliedChanges,
  onAddFilter,
  onRemoveFilter,
  onUpdateFilter,
  onApplyFilters,
  onClearFilters,
}: DataTableFilterPanelProps) {
  return (
    <div className="border-b border-gray-800 bg-gray-950/50 shrink-0">
      <div className="flex items-start gap-6 px-6 py-3">
        <div className="space-y-2">
          {draftFilters.map((filter, index) => (
            <FilterRow
              key={index}
              filter={filter}
              columns={columns}
              isFirst={index === 0}
              onUpdate={(updates) => onUpdateFilter(index, updates)}
              onRemove={() => onRemoveFilter(index)}
            />
          ))}
        </div>
        {/* Separator */}
        <div className="w-px bg-gray-800 self-stretch"></div>
        {/* Action buttons column */}
        <div className="flex items-end gap-2 pt-0.5">
          <Button
            variant="default"
            size="sm"
            onClick={onApplyFilters}
            disabled={!hasUnappliedChanges}
            className="h-8 text-xs"
          >
            Apply
          </Button>
          <Button variant="outline" size="sm" onClick={onAddFilter} className="h-8 text-xs">
            <Plus className="mr-1 h-3 w-3" />
            Add filter
          </Button>
          <Button variant="ghost" size="sm" onClick={onClearFilters} className="h-8 text-xs text-gray-400">
            Clear filters
          </Button>
        </div>
      </div>
    </div>
  )
})
