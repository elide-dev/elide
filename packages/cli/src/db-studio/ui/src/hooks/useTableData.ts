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

async function fetchTableData(
  dbIndex: string,
  tableName: string
): Promise<TableData> {
  const res = await fetch(
    `${API_BASE_URL}/api/databases/${dbIndex}/tables/${encodeURIComponent(tableName)}`
  )
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}: ${res.statusText}`)
  }
  return res.json()
}

export function useTableData(
  dbIndex: string | undefined,
  tableName: string | undefined
) {
  return useQuery({
    queryKey: ['databases', dbIndex, 'tables', tableName],
    queryFn: () => fetchTableData(dbIndex!, tableName!),
    enabled: !!dbIndex && !!tableName,
  })
}
