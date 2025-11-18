import * as React from 'react'
import type { ColumnDef, ColumnFiltersState, ColumnSizingState, SortingState } from '@tanstack/react-table'
import { getCoreRowModel, getFilteredRowModel, getSortedRowModel, useReactTable } from '@tanstack/react-table'

import { ColumnHeader } from './ColumnHeader'
import type { Filter } from '@/lib/types'
import { useFilterState } from '@/hooks/useFilterState'
import { usePaginationInputs } from '@/hooks/usePaginationInputs'
import { useColumnVisibility } from '@/hooks/useColumnVisibility'
import { DataTableToolbar } from './DataTableToolbar'
import { DataTableFilterPanel } from './DataTableFilterPanel'
import { DataTableGrid } from './DataTableGrid'

export type ColumnMetadata = {
  name: string
  type: string
  nullable: boolean
  primaryKey: boolean
  defaultValue?: string | number | null
  foreignKey?: {
    table: string
    column: string
    onUpdate?: string
    onDelete?: string
  }
  unique?: boolean
  autoIncrement?: boolean
}

export type QueryMetadata = {
  executionTimeMs: number
  sql: string
  rowCount: number
}

export type DataTableData = {
  columns: ColumnMetadata[]
  rows: unknown[][]
  metadata?: QueryMetadata
}

export type DataTablePagination = {
  limit: number
  offset: number
}

export type DataTableSorting = {
  column: string | null
  direction: 'asc' | 'desc' | null
}

type DataTableProps = {
  data: DataTableData
  showControls?: boolean // Show sorting, column toggle, and column resizing (default: true)
  showPagination?: boolean // Show pagination controls (default: true)
  showMetadata?: boolean // Show execution time and row count metadata (default: true when available)
  // Server-side pagination props (required)
  totalRows: number
  tableRowCount?: number // Total rows in the table (unfiltered) - shown in badge next to table name
  pagination: DataTablePagination
  onPaginationChange: (limit: number, offset: number) => void
  // Server-side sorting props (optional - if not provided, sorting is disabled)
  sorting?: DataTableSorting
  onSortChange?: (column: string | null, direction: 'asc' | 'desc' | null) => void
  // Server-side filtering props (optional)
  filters?: Filter[]
  onFiltersChange?: (filters: Filter[]) => void
  isLoading?: boolean // Show skeleton loaders when fetching new data
  tableName?: string // Table name to display in toolbar
  onRefresh?: () => void // Callback to refetch the current query state
}

/**
 * Reusable data table component for displaying database query results
 * Uses TanStack Table for sorting, filtering, and column visibility
 * Always uses server-side pagination via totalRows, limit, offset props and onPaginationChange callback
 * Supports server-side sorting via sorting and onSortChange props
 * Pagination is controlled - limit/offset come from props (URL query params)
 * Handles NULL values, empty strings, and other data types appropriately
 */
export function DataTable({
  data,
  showControls = true,
  showPagination = true,
  showMetadata = true,
  totalRows,
  tableRowCount,
  pagination,
  onPaginationChange,
  sorting,
  onSortChange,
  filters = [],
  onFiltersChange,
  isLoading = false,
  tableName,
  onRefresh,
}: DataTableProps) {
  // Convert server-side sorting to TanStack Table format
  const sortingState: SortingState = React.useMemo(() => {
    if (sorting?.column && sorting.direction) {
      return [{ id: sorting.column, desc: sorting.direction === 'desc' }]
    }
    return []
  }, [sorting])

  const [columnFilters, setColumnFilters] = React.useState<ColumnFiltersState>([])
  const [columnSizing, setColumnSizing] = React.useState<ColumnSizingState>({})

  // Custom hooks for state management
  const filterState = useFilterState(filters, data.columns, onFiltersChange)
  const paginationInputs = usePaginationInputs(pagination.limit, pagination.offset)
  const columnVisibilityState = useColumnVisibility()

  // Reset state when switching tables
  React.useEffect(() => {
    columnVisibilityState.setColumnVisibility({})
    columnVisibilityState.setColumnSearch('')
    filterState.setShowFilters(filters.length > 0)
  }, [tableName, filters.length])

  const { columns, rows, metadata } = data

  // Server-side pagination values
  const { limit, offset } = pagination

  const formatCellValue = (value: unknown): React.ReactNode => {
    return value === null || value === undefined ? (
      <span className="text-gray-500 font-normal">NULL</span>
    ) : (
      String(value)
    )
  }

  // Filter toggle handler
  const handleFilterToggle = React.useCallback(() => {
    if (filterState.draftFilters.length === 0 && filters.length === 0) {
      filterState.handleAddFilter()
    } else {
      filterState.setShowFilters(!filterState.showFilters)
    }
  }, [filters.length, filterState])

  // Columns are already ColumnMetadata format
  const normalizedColumns: ColumnMetadata[] = React.useMemo(() => {
    return columns
  }, [columns])

  // Convert ColumnMetadata[] and unknown[][] rows to TanStack Table format
  const tableColumns: ColumnDef<Record<string, unknown>>[] = React.useMemo(
    () =>
      normalizedColumns.map((col) => {
        return {
          accessorKey: col.name,
          size: 200, // Default column width
          minSize: 100, // Minimum column width
          maxSize: 1000, // Maximum column width
          enableResizing: true,
          header: ({ column }) => {
            const sorted = column.getIsSorted()
            return <ColumnHeader column={col} sorted={sorted} showControls={showControls} onSortChange={onSortChange} />
          },
          cell: ({ getValue }) => {
            return formatCellValue(getValue())
          },
        }
      }),
    [normalizedColumns, showControls, onSortChange]
  )

  // Convert rows array to object format for TanStack Table
  const tableData: Record<string, unknown>[] = React.useMemo(
    () =>
      rows.map((row) =>
        normalizedColumns.reduce(
          (acc, col, idx) => {
            acc[col.name] = row[idx]
            return acc
          },
          {} as Record<string, unknown>
        )
      ),
    [rows, normalizedColumns]
  )

  const table = useReactTable({
    data: tableData,
    columns: tableColumns,
    // Server-side sorting: when onSortChange is provided, use manual sorting
    manualSorting: !!onSortChange,
    onColumnFiltersChange: setColumnFilters,
    getCoreRowModel: getCoreRowModel(),
    // Always use server-side pagination
    manualPagination: true,
    pageCount: Math.ceil(totalRows / limit),
    // Only use client-side sorting if server-side sorting is not enabled
    getSortedRowModel: onSortChange ? undefined : getSortedRowModel(),
    getFilteredRowModel: getFilteredRowModel(),
    onColumnVisibilityChange: columnVisibilityState.setColumnVisibility,
    onColumnSizingChange: setColumnSizing,
    enableColumnResizing: true,
    columnResizeMode: 'onChange',
    state: {
      sorting: sortingState,
      columnFilters,
      columnVisibility: columnVisibilityState.columnVisibility,
      columnSizing,
    },
  })

  return (
    <div className="w-full flex flex-col h-full">
      {/* Toolbar with metadata and controls */}
      {(showControls || showMetadata || showPagination || tableName) && (
        <DataTableToolbar
          tableName={tableName}
          tableRowCount={tableRowCount}
          showControls={showControls}
          showMetadata={showMetadata}
          showPagination={showPagination}
          metadata={metadata}
          rows={rows}
          showFilters={filterState.showFilters}
          activeFilterCount={filters.length}
          onFilterToggle={handleFilterToggle}
          onFiltersChange={onFiltersChange}
          table={table}
          columnSearch={columnVisibilityState.columnSearch}
          setColumnSearch={columnVisibilityState.setColumnSearch}
          pagination={pagination}
          paginationInputs={paginationInputs}
          onPaginationChange={onPaginationChange}
          onRefresh={onRefresh}
          isLoading={isLoading}
        />
      )}

      {/* Filter rows section */}
      {filterState.showFilters && onFiltersChange && filterState.draftFilters.length > 0 && (
        <DataTableFilterPanel
          draftFilters={filterState.draftFilters}
          columns={columns}
          hasUnappliedChanges={filterState.hasUnappliedChanges}
          onAddFilter={filterState.handleAddFilter}
          onRemoveFilter={filterState.handleRemoveFilter}
          onUpdateFilter={filterState.handleUpdateFilter}
          onApplyFilters={filterState.handleApplyFilters}
          onClearFilters={filterState.handleClearFilters}
        />
      )}

      {/* Table */}
      <DataTableGrid table={table} isLoading={isLoading} showControls={showControls} limit={limit} offset={offset} />
    </div>
  )
}
