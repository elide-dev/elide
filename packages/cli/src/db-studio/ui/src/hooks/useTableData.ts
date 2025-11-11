import { useQuery } from '@tanstack/react-query'
import { API_BASE_URL } from '../config'

export type TableData = {
  name: string
  columns: string[]
  rows: unknown[][]
  totalRows: number
  primaryKeys?: string[]
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
