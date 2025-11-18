import * as React from 'react'
import type { Filter } from '@/lib/types'
import type { ColumnMetadata } from '@/components/DataTable'

type UseFilterStateReturn = {
  draftFilters: Filter[]
  showFilters: boolean
  hasUnappliedChanges: boolean
  handleAddFilter: () => void
  handleRemoveFilter: (index: number) => void
  handleUpdateFilter: (index: number, updates: Partial<Filter>) => void
  handleApplyFilters: () => void
  handleClearFilters: () => void
  setShowFilters: (show: boolean) => void
}

export function useFilterState(
  filters: Filter[],
  columns: ColumnMetadata[],
  onFiltersChange?: (filters: Filter[]) => void
): UseFilterStateReturn {
  const [showFilters, setShowFilters] = React.useState(filters.length > 0)
  // Track draft filters (being edited) separately from applied filters
  const [draftFilters, setDraftFilters] = React.useState<Filter[]>(filters)

  // Sync draft filters when applied filters change externally
  React.useEffect(() => {
    setDraftFilters(filters)
  }, [filters])

  // Check if draft filters differ from applied filters
  const hasUnappliedChanges = React.useMemo(() => {
    return JSON.stringify(draftFilters) !== JSON.stringify(filters)
  }, [draftFilters, filters])

  // Filter management functions (work with draft filters)
  const handleAddFilter = React.useCallback(() => {
    if (!onFiltersChange) return
    const newFilter: Filter = {
      column: columns[0]?.name || '',
      operator: 'eq',
      value: '',
    }
    setDraftFilters([...draftFilters, newFilter])
    setShowFilters(true)
  }, [onFiltersChange, columns, draftFilters])

  const handleRemoveFilter = React.useCallback(
    (index: number) => {
      if (!onFiltersChange) return
      const newFilters = draftFilters.filter((_, i) => i !== index)
      if (newFilters.length === 0) {
        // If removing the last filter, clear all and close panel
        setDraftFilters([])
        onFiltersChange([])
        setShowFilters(false)
      } else {
        setDraftFilters(newFilters)
      }
    },
    [onFiltersChange, draftFilters]
  )

  const handleUpdateFilter = React.useCallback(
    (index: number, updates: Partial<Filter>) => {
      if (!onFiltersChange) return
      const newFilters = draftFilters.map((filter, i) => {
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
      setDraftFilters(newFilters)
    },
    [onFiltersChange, draftFilters]
  )

  const handleApplyFilters = React.useCallback(() => {
    if (!onFiltersChange) return
    onFiltersChange(draftFilters)
  }, [onFiltersChange, draftFilters])

  const handleClearFilters = React.useCallback(() => {
    if (!onFiltersChange) return
    setDraftFilters([])
    onFiltersChange([])
    setShowFilters(false)
  }, [onFiltersChange])

  return {
    draftFilters,
    showFilters,
    hasUnappliedChanges,
    handleAddFilter,
    handleRemoveFilter,
    handleUpdateFilter,
    handleApplyFilters,
    handleClearFilters,
    setShowFilters,
  }
}
