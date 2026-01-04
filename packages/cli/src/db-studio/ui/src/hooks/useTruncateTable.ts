import { useMutation, useQueryClient } from '@tanstack/react-query'
import { API_BASE_URL } from '../config'

async function truncateTable(dbId: string, tableName: string): Promise<void> {
  const res = await fetch(`${API_BASE_URL}/api/databases/${dbId}/tables/${encodeURIComponent(tableName)}/truncate`, {
    method: 'POST',
  })

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({ message: res.statusText }))
    throw new Error(errorData.message || `HTTP ${res.status}: ${res.statusText}`)
  }
}

export function useTruncateTable(dbId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (tableName: string) => truncateTable(dbId, tableName),
    onSuccess: () => {
      // Invalidate the tables list to refetch after truncating
      queryClient.invalidateQueries({
        queryKey: ['databases', dbId, 'tables'],
      })
    },
  })
}
