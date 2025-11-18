import { useCallback, useMemo } from 'react'
import { useParams, useSearchParams } from 'react-router-dom'
import { useTableData } from '../hooks/useTableData'
import type { SortingParams } from '../hooks/useTableData'
import { useDatabaseTables } from '../hooks/useDatabaseTables'
import { DataTable } from '../components/DataTable'
import type { DataTableSorting } from '../components/DataTable'
import type { Filter } from '@/lib/types'

export default function TableView() {
  const { dbIndex, tableName } = useParams()
  const [searchParams, setSearchParams] = useSearchParams()

  // Get tables list to access the total row count
  const { data: tables = [] } = useDatabaseTables(dbIndex)

  const pagination = useMemo(
    () => ({
      limit: parseInt(searchParams.get('limit') || '100', 10),
      offset: parseInt(searchParams.get('offset') || '0', 10),
    }),
    [searchParams]
  )

  const sorting: SortingParams = useMemo(() => {
    const sort = searchParams.get('sort')
    const order = searchParams.get('order')
    if (sort && order && (order === 'asc' || order === 'desc')) {
      return { column: sort, direction: order as 'asc' | 'desc' }
    }
    return { column: null, direction: null }
  }, [searchParams])

  const filters: Filter[] = useMemo(() => {
    const whereParam = searchParams.get('where')
    if (!whereParam) return []
    try {
      const decoded = decodeURIComponent(whereParam)
      const parsed = JSON.parse(decoded)
      return Array.isArray(parsed) ? parsed : []
    } catch {
      return []
    }
  }, [searchParams])

  const {
    data,
    isLoading: loading,
    isFetching,
    error,
    refetch,
  } = useTableData(dbIndex, tableName, pagination, sorting, filters)

  const handlePaginationChange = useCallback(
    (limit: number, offset: number) => {
      const newParams: Record<string, string> = {
        limit: limit.toString(),
        offset: offset.toString(),
      }
      // Preserve sorting params
      if (sorting.column && sorting.direction) {
        newParams.sort = sorting.column
        newParams.order = sorting.direction
      }
      // Preserve filters
      if (filters.length > 0) {
        newParams.where = encodeURIComponent(JSON.stringify(filters))
      }
      setSearchParams(newParams)
    },
    [setSearchParams, sorting, filters]
  )

  const handleSortChange = useCallback(
    (column: string | null, direction: 'asc' | 'desc' | null) => {
      const newParams: Record<string, string> = {
        limit: pagination.limit.toString(),
        offset: '0', // Reset to first page when sorting changes
      }
      // Add sorting params if provided
      if (column && direction) {
        newParams.sort = column
        newParams.order = direction
      }
      // Preserve filters
      if (filters.length > 0) {
        newParams.where = encodeURIComponent(JSON.stringify(filters))
      }
      setSearchParams(newParams)
    },
    [setSearchParams, pagination.limit, filters]
  )

  const handleFiltersChange = useCallback(
    (newFilters: Filter[]) => {
      const newParams: Record<string, string> = {
        limit: pagination.limit.toString(),
        offset: '0', // Reset to first page when filters change
      }
      // Preserve sorting params
      if (sorting.column && sorting.direction) {
        newParams.sort = sorting.column
        newParams.order = sorting.direction
      }
      // Add filters if provided
      if (newFilters.length > 0) {
        newParams.where = encodeURIComponent(JSON.stringify(newFilters))
      }
      setSearchParams(newParams)
    },
    [setSearchParams, pagination.limit, sorting]
  )

  const handleRefresh = useCallback(() => {
    refetch()
  }, [refetch])

  const tableSorting: DataTableSorting = useMemo(
    () => ({
      column: sorting.column,
      direction: sorting.direction,
    }),
    [sorting]
  )

  // Find the current table's row count from the tables list
  const tableRowCount = useMemo(() => {
    const table = tables.find((t) => t.name === tableName)
    return table?.rowCount
  }, [tables, tableName])

  if (loading && !data) {
    return (
      <div className="flex-1 p-0 overflow-auto flex items-center justify-center text-gray-500 font-mono">Loading…</div>
    )
  }
  if (error && !data) {
    return (
      <div className="flex-1 p-0 overflow-auto flex items-center justify-center text-red-400 font-mono">
        Error: {error.message}
      </div>
    )
  }
  if (!data) {
    return <div className="flex-1 p-0 overflow-auto font-mono" />
  }

  return (
    <div className="flex-1 flex flex-col overflow-hidden">
      <div className="flex-1 overflow-auto">
        <DataTable
          data={data}
          totalRows={data.totalRows}
          tableRowCount={tableRowCount}
          tableName={data.name}
          pagination={pagination}
          onPaginationChange={handlePaginationChange}
          sorting={tableSorting}
          onSortChange={handleSortChange}
          filters={filters}
          onFiltersChange={handleFiltersChange}
          isLoading={isFetching}
          onRefresh={handleRefresh}
        />
      </div>
    </div>
  )
}
