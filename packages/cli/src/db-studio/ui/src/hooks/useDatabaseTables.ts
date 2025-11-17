import { useQuery } from '@tanstack/react-query'
import { API_BASE_URL } from '../config'

export type TableMeta = {
  name: string
  type: 'table' | 'view'
  rowCount: number
}

type TablesResponse = {
  tables: TableMeta[]
}

async function fetchDatabaseTables(dbIndex: string): Promise<TableMeta[]> {
  const res = await fetch(`${API_BASE_URL}/api/databases/${dbIndex}/tables`)
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}: ${res.statusText}`)
  }
  const data: TablesResponse = await res.json()
  return data.tables
}

export function useDatabaseTables(dbIndex: string | undefined) {
  return useQuery({
    queryKey: ['databases', dbIndex, 'tables'],
    queryFn: () => fetchDatabaseTables(dbIndex!),
    enabled: !!dbIndex,
  })
}
