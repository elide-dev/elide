import { useMutation, useQueryClient } from '@tanstack/react-query'
import { API_BASE_URL } from '../config'

async function truncateTable(dbIndex: string, tableName: string): Promise<void> {
  const res = await fetch(`${API_BASE_URL}/api/databases/${dbIndex}/tables/${encodeURIComponent(tableName)}/truncate`, {
    method: 'POST',
  })

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({ message: res.statusText }))
    throw new Error(errorData.message || `HTTP ${res.status}: ${res.statusText}`)
  }
}

export function useTruncateTable(dbIndex: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (tableName: string) => truncateTable(dbIndex, tableName),
    onSuccess: () => {
      // Invalidate the tables list to refetch after truncating
      queryClient.invalidateQueries({
        queryKey: ['databases', dbIndex, 'tables'],
      })
    },
  })
}
