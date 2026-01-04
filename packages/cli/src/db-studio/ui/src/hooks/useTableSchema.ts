import { useQuery } from '@tanstack/react-query'
import { API_BASE_URL } from '../config'

export function useTableSchema(dbId: string, tableName: string | undefined) {
  return useQuery({
    queryKey: ['databases', dbId, 'tables', tableName, 'schema'],
    queryFn: async () => {
      const res = await fetch(`${API_BASE_URL}/api/databases/${dbId}/tables/${encodeURIComponent(tableName!)}/schema`)
      if (!res.ok) throw new Error('Failed to fetch table schema')
      return res.json()
    },
    enabled: !!tableName,
  })
}
