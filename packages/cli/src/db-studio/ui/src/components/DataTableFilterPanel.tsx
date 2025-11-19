import * as React from 'react'
import { Plus } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { FilterRow } from './FilterRow'
import { useDataTable } from '@/contexts/DataTableContext'
import type { Filter } from '@/lib/types'

/**
 * Deep equality check for filters
 */
function areFiltersEqual(a: Filter[], b: Filter[]): boolean {
  if (a.length !== b.length) return false
  return a.every((filterA, index) => {
    const filterB = b[index]
    return (
      filterA.column === filterB.column &&
      filterA.operator === filterB.operator &&
      JSON.stringify(filterA.value) === JSON.stringify(filterB.value)
    )
  })
}

type DataTableFilterPanelProps = {
  onClearFilters: () => void
}

export const DataTableFilterPanel = React.memo(function DataTableFilterPanel({
  onClearFilters,
}: DataTableFilterPanelProps) {
  const { columns, appliedFilters, onFiltersChange } = useDataTable()

  // Local draft state (edit here, apply to URL)
  const [draftFilters, setDraftFilters] = React.useState<Filter[]>(() => {
    // Always start with at least one filter
    if (appliedFilters.length > 0) {
      return appliedFilters
    }
    return [
      {
        column: columns[0]?.name || '',
        operator: 'eq',
        value: '',
      },
    ]
  })

  // Sync draft when appliedFilters change (from URL navigation)
  React.useEffect(() => {
    // Always maintain at least one filter
    if (appliedFilters.length > 0) {
      setDraftFilters(appliedFilters)
    } else {
      setDraftFilters([
        {
          column: columns[0]?.name || '',
          operator: 'eq',
          value: '',
        },
      ])
    }
  }, [appliedFilters, columns])

  // Check if draft differs from applied
  const hasUnappliedChanges = React.useMemo(() => {
    return !areFiltersEqual(draftFilters, appliedFilters)
  }, [draftFilters, appliedFilters])

  const handleAdd = React.useCallback(() => {
    const newFilter: Filter = {
      column: columns[0]?.name || '',
      operator: 'eq',
      value: '',
    }
    setDraftFilters((prev) => [...prev, newFilter])
  }, [columns])

  const handleUpdate = React.useCallback((index: number, updates: Partial<Filter>) => {
    setDraftFilters((prev) =>
      prev.map((filter, i) => {
        if (i === index) {
          const updatedFilter = { ...filter, ...updates }
          // Reset value if operator changes to one that doesn't need a value
          if (updates.operator && (updates.operator === 'is_null' || updates.operator === 'is_not_null')) {
            updatedFilter.value = undefined
          }
          // Reset value if operator changes to 'in' (needs array)
          if (updates.operator === 'in' && !Array.isArray(updatedFilter.value)) {
            updatedFilter.value = []
          }
          // Reset value to string if operator changes from 'in' to something else
          if (updates.operator && updates.operator !== 'in' && Array.isArray(updatedFilter.value)) {
            updatedFilter.value = ''
          }
          return updatedFilter
        }
        return filter
      })
    )
  }, [])

  const handleApply = React.useCallback(() => {
    onFiltersChange(draftFilters)
  }, [draftFilters, onFiltersChange])

  const handleClear = React.useCallback(() => {
    setDraftFilters([])
    onFiltersChange([])
    onClearFilters()
  }, [onFiltersChange, onClearFilters])

  const handleRemove = React.useCallback(
    (index: number) => {
      if (draftFilters.length === 1) {
        handleClear()
      }
      setDraftFilters((prev) => prev.filter((_, i) => i !== index))
    },
    [handleClear, draftFilters.length]
  )

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
              onApply={handleApply}
              onUpdate={(updates) => handleUpdate(index, updates)}
              onRemove={() => handleRemove(index)}
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
            onClick={handleApply}
            disabled={!hasUnappliedChanges}
            className="h-8 text-xs"
          >
            Apply
          </Button>
          <Button variant="outline" size="sm" onClick={handleAdd} className="h-8 text-xs">
            <Plus className="mr-1 h-3 w-3" />
            Add filter
          </Button>
          <Button variant="ghost" size="sm" onClick={handleClear} className="h-8 text-xs text-gray-400">
            Clear filters
          </Button>
        </div>
      </div>
    </div>
  )
})
