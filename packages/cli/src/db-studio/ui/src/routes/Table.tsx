import { useCallback, useMemo } from 'react'
import { useParams, useSearchParams } from 'react-router-dom'
import { useTableData } from '../hooks/useTableData'
import type { SortingParams } from '../hooks/useTableData'
import { DataTable } from '../components/DataTable'
import type { DataTableSorting } from '../components/DataTable'

export default function TableView() {
  const { dbIndex, tableName } = useParams()
  const [searchParams, setSearchParams] = useSearchParams()

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

  const { data, isLoading: loading, isFetching, error } = useTableData(dbIndex, tableName, pagination, sorting)

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
      setSearchParams(newParams)
    },
    [setSearchParams, sorting]
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
      setSearchParams(newParams)
    },
    [setSearchParams, pagination.limit]
  )

  const tableSorting: DataTableSorting = useMemo(
    () => ({
      column: sorting.column,
      direction: sorting.direction,
    }),
    [sorting]
  )

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
      <div className="px-6 pt-6 pb-4 border-b border-gray-800">
        <h2 className="text-2xl font-semibold tracking-tight flex items-center gap-3">
          <span className="truncate">{data.name}</span>
          <span className="inline-flex items-center rounded-md bg-gray-800/60 text-gray-300 border border-gray-700 px-2.5 py-0.5 text-xs font-medium">
            {data.totalRows} total rows
          </span>
        </h2>
      </div>
      <div className="flex-1 overflow-auto">
        <DataTable
          data={data}
          totalRows={data.totalRows}
          pagination={pagination}
          onPaginationChange={handlePaginationChange}
          sorting={tableSorting}
          onSortChange={handleSortChange}
          isLoading={isFetching}
        />
      </div>
    </div>
  )
}
