/**
 * Thin DataTable Context - Simple prop distribution for sibling components
 * State management lives in Table.tsx route component
 */

import * as React from 'react'
import type { Table } from '@tanstack/react-table'
import type { ColumnMetadata, Filter, PaginationParams, SortingParams } from '@/lib/types'

type DataTableConfig = {
  tableName?: string
  tableType?: 'table' | 'view'
  tableRowCount?: number
  totalRows: number
  isLoading: boolean
  showControls: boolean
  showPagination: boolean
}

export type DataTableContextValue = {
  // TanStack Table instance (manages column visibility, sizing, etc.)
  table: Table<Record<string, unknown>>

  // Data
  columns: ColumnMetadata[]
  rowCount: number
  metadata?: {
    executionTimeMs?: number
  }

  // Server state (read-only from URL)
  pagination: PaginationParams
  sorting: SortingParams
  appliedFilters: Filter[]

  // Callbacks to update URL params
  onPaginationChange: (pagination: PaginationParams) => void
  onSortingChange: (sorting: SortingParams) => void
  onFiltersChange: (filters: Filter[]) => void
  onRefresh?: () => void

  // Configuration
  config: DataTableConfig
}

const DataTableContext = React.createContext<DataTableContextValue | null>(null)

type DataTableProviderProps = {
  children: React.ReactNode
  value: DataTableContextValue
}

/**
 * Thin provider that just distributes props to children
 * No state management happens here
 */
export function DataTableProvider({ children, value }: DataTableProviderProps) {
  return <DataTableContext.Provider value={value}>{children}</DataTableContext.Provider>
}

/**
 * Hook to access DataTable context
 * Throws if used outside of DataTableProvider
 */
export function useDataTable(): DataTableContextValue {
  const context = React.useContext(DataTableContext)

  if (!context) {
    throw new Error('useDataTable must be used within a DataTableProvider')
  }

  return context
}
