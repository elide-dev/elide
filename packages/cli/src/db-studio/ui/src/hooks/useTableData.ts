import { useQuery } from '@tanstack/react-query'
import { API_BASE_URL } from '../config'
import type { Filter, ColumnMetadata, QueryMetadata, PaginationParams, SortingParams } from '@/lib/types'

export type TableData = {
  name: string
  columns: ColumnMetadata[]
  rows: unknown[][]
  totalRows: number
  metadata: QueryMetadata
}

async function fetchTableData(
  dbIndex: string,
  tableName: string,
  pagination?: PaginationParams,
  sorting?: SortingParams,
  filters?: Filter[]
): Promise<TableData> {
  const params = new URLSearchParams()
  if (pagination) {
    params.set('limit', pagination.limit.toString())
    params.set('offset', pagination.offset.toString())
  }
  if (sorting?.column && sorting.direction) {
    params.set('sort', sorting.column)
    params.set('order', sorting.direction)
  }
  if (filters && filters.length > 0) {
    // Convert filters to JSON and URL encode
    params.set('where', encodeURIComponent(JSON.stringify(filters)))
  }

  const queryString = params.toString()
  const url = `${API_BASE_URL}/api/databases/${dbIndex}/tables/${encodeURIComponent(tableName)}${queryString ? `?${queryString}` : ''}`

  const res = await fetch(url)
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}: ${res.statusText}`)
  }
  return res.json()
}

export function useTableData(
  dbIndex: string | undefined,
  tableName: string | undefined,
  pagination?: PaginationParams,
  sorting?: SortingParams,
  filters?: Filter[]
) {
  return useQuery({
    queryKey: ['databases', dbIndex, 'tables', tableName, pagination, sorting, filters],
    queryFn: () => fetchTableData(dbIndex!, tableName!, pagination, sorting, filters),
    enabled: !!dbIndex && !!tableName,
    placeholderData: (previousData) => previousData, // Keep previous data while fetching
  })
}
