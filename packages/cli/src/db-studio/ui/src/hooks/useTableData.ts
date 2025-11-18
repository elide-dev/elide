import { useQuery } from '@tanstack/react-query'
import { API_BASE_URL } from '../config'

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

export type TableData = {
  name: string
  columns: ColumnMetadata[]
  rows: unknown[][]
  totalRows: number
  metadata: QueryMetadata
}

export type PaginationParams = {
  limit: number
  offset: number
}

async function fetchTableData(
  dbIndex: string,
  tableName: string,
  pagination?: PaginationParams
): Promise<TableData> {
  const params = new URLSearchParams()
  if (pagination) {
    params.set('limit', pagination.limit.toString())
    params.set('offset', pagination.offset.toString())
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
  pagination?: PaginationParams
) {
  return useQuery({
    queryKey: ['databases', dbIndex, 'tables', tableName, pagination],
    queryFn: () => fetchTableData(dbIndex!, tableName!, pagination),
    enabled: !!dbIndex && !!tableName,
    placeholderData: (previousData) => previousData, // Keep previous data while fetching
  })
}
