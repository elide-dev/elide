import * as React from 'react'
import { useParams, useSearchParams } from 'react-router-dom'
import type { ColumnDef, SortingState, RowSelectionState } from '@tanstack/react-table'
import { getCoreRowModel, getFilteredRowModel, getSortedRowModel, useReactTable } from '@tanstack/react-table'
import { useTableData } from '../hooks/useTableData'
import { useDatabaseTables } from '../hooks/useDatabaseTables'
import { DataTable, DataTableProvider, ColumnHeader } from '@/components/data-table'
import { parsePaginationParams, parseSortingParams, parseFilterParams, buildSearchParams } from '@/lib/urlParams'
import { DEFAULT_OFFSET } from '@/lib/constants'
import type { ColumnMetadata, PaginationParams, SortingParams, Filter } from '@/lib/types'
import { Checkbox } from '@/components/ui/checkbox'

/**
 * Format cell values with special handling for NULL values
 */
function formatCellValue(value: unknown): React.ReactNode {
  if (value === null || value === undefined) return <span className="text-muted-foreground font-normal">NULL</span>
  return String(value)
}

export default function TableView() {
  const { dbId, tableName } = useParams()
  const [searchParams, setSearchParams] = useSearchParams()

  // Parse URL params once (source of truth)
  const pagination = React.useMemo(() => parsePaginationParams(searchParams, dbId), [searchParams, dbId])
  const sorting = React.useMemo(() => parseSortingParams(searchParams), [searchParams])
  const appliedFilters = React.useMemo(() => parseFilterParams(searchParams), [searchParams])

  // Get tables list to access the total row count
  const { data: tables = [] } = useDatabaseTables(dbId)

  // Fetch table data with server params
  const { data, isLoading, isFetching, error, refetch } = useTableData(
    dbId,
    tableName,
    pagination,
    sorting,
    appliedFilters
  )

  // Find the current table's metadata from the tables list
  const tableMetadata = React.useMemo(() => {
    const table = tables.find((t) => t.name === tableName)
    return {
      rowCount: table?.rowCount,
      type: table?.type,
    }
  }, [tables, tableName])

  // Change handlers - update URL which triggers re-fetch
  const handlePaginationChange = React.useCallback(
    (newPagination: PaginationParams) => {
      setSearchParams(buildSearchParams(newPagination, sorting, appliedFilters))
    },
    [setSearchParams, sorting, appliedFilters]
  )

  const handleSortingChange = React.useCallback(
    (newSorting: SortingParams) => {
      // Reset offset when sorting changes
      const resetPagination = { ...pagination, offset: DEFAULT_OFFSET }
      setSearchParams(buildSearchParams(resetPagination, newSorting, appliedFilters))
    },
    [setSearchParams, pagination, appliedFilters]
  )

  const handleFiltersChange = React.useCallback(
    (newFilters: Filter[]) => {
      // Reset offset when filters change
      const resetPagination = { ...pagination, offset: DEFAULT_OFFSET }
      setSearchParams(buildSearchParams(resetPagination, sorting, newFilters))
    },
    [setSearchParams, pagination, sorting]
  )

  // Row selection state
  const [rowSelection, setRowSelection] = React.useState<RowSelectionState>({})

  // Convert server-side sorting to TanStack Table format
  const sortingState: SortingState = React.useMemo(() => {
    if (sorting.column && sorting.direction) {
      return [
        {
          id: sorting.column,
          desc: sorting.direction === 'desc',
        },
      ]
    }
    return []
  }, [sorting.column, sorting.direction])

  // Build TanStack Table columns
  const tableColumns: ColumnDef<Record<string, unknown>>[] = React.useMemo(() => {
    if (!data) return []

    // Create checkbox column for row selection
    const checkboxColumn: ColumnDef<Record<string, unknown>> = {
      id: 'select',
      size: 50,
      minSize: 50,
      maxSize: 50,
      enableResizing: false,
      enableHiding: false,
      header: ({ table }) => (
        <div className="px-4">
          <Checkbox
            checked={table.getIsAllPageRowsSelected()}
            onCheckedChange={(value) => table.toggleAllPageRowsSelected(!!value)}
            aria-label="Select all"
          />
        </div>
      ),
      cell: ({ row }) => (
        <Checkbox
          checked={row.getIsSelected()}
          onCheckedChange={(value) => row.toggleSelected(!!value)}
          aria-label="Select row"
        />
      ),
    }

    const dataColumns = data.columns.map((col: ColumnMetadata) => {
      return {
        accessorKey: col.name,
        size: 200,
        minSize: 100,
        maxSize: 1000,
        enableResizing: true,
        header: ({ column }: { column: any }) => {
          const sorted = column.getIsSorted()
          return <ColumnHeader column={col} sorted={sorted} showControls={true} />
        },
        cell: ({ getValue }: { getValue: () => unknown }) => {
          return formatCellValue(getValue())
        },
      }
    })

    return [checkboxColumn, ...dataColumns]
  }, [data])

  // Build TanStack Table data
  const tableData: Record<string, unknown>[] = React.useMemo(() => {
    if (!data) return []

    return data.rows.map((row) =>
      data.columns.reduce(
        (acc, col, idx) => {
          acc[col.name] = row[idx]
          return acc
        },
        {} as Record<string, unknown>
      )
    )
  }, [data])

  // Create TanStack Table instance
  const table = useReactTable({
    data: tableData,
    columns: tableColumns,
    manualSorting: true,
    manualPagination: true,
    pageCount: data ? Math.ceil(data.totalRows / pagination.limit) : 0,
    getCoreRowModel: getCoreRowModel(),
    getSortedRowModel: getSortedRowModel(),
    getFilteredRowModel: getFilteredRowModel(),
    enableColumnResizing: true,
    columnResizeMode: 'onChange',
    enableRowSelection: true,
    onRowSelectionChange: setRowSelection,
    state: {
      sorting: sortingState,
      rowSelection,
    },
  })

  // Reset column visibility and row selection when table changes
  React.useEffect(() => {
    table.resetColumnVisibility()
    setRowSelection({})
  }, [tableName, table])

  // Clear row selection when pagination changes
  React.useEffect(() => {
    setRowSelection({})
  }, [pagination.offset, pagination.limit])

  // Loading state
  if (isLoading && !data) {
    return (
      <div className="flex-1 p-0 overflow-auto flex items-center justify-center text-muted-foreground font-mono">
        Loading…
      </div>
    )
  }

  // Error state
  if (error && !data) {
    return (
      <div className="flex-1 p-0 overflow-auto flex items-center justify-center text-destructive font-mono">
        Error: {error.message}
      </div>
    )
  }

  // No data state
  if (!data) {
    return <div className="flex-1 p-0 overflow-auto font-mono" />
  }

  // Build context value
  const contextValue = {
    table,
    columns: data.columns,
    rowCount: data.rows.length,
    metadata: data.metadata,
    pagination,
    sorting,
    appliedFilters,
    onPaginationChange: handlePaginationChange,
    onSortingChange: handleSortingChange,
    onFiltersChange: handleFiltersChange,
    onRefresh: refetch,
    config: {
      tableName: data.name,
      tableType: tableMetadata.type,
      tableRowCount: tableMetadata.rowCount,
      totalRows: data.totalRows,
      isLoading: isFetching,
      showControls: true,
      showPagination: true,
    },
  }

  return (
    <div className="flex-1 flex flex-col overflow-hidden">
      <DataTableProvider key={tableName} value={contextValue}>
        <DataTable />
      </DataTableProvider>
    </div>
  )
}
