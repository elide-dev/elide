import { useMutation, useQueryClient } from '@tanstack/react-query'
import { API_BASE_URL } from '../config'

async function dropTable(dbId: string, tableName: string): Promise<void> {
  const res = await fetch(`${API_BASE_URL}/api/databases/${dbId}/tables/${encodeURIComponent(tableName)}`, {
    method: 'DELETE',
  })

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({ message: res.statusText }))
    throw new Error(errorData.message || `HTTP ${res.status}: ${res.statusText}`)
  }
}

export function useDropTable(dbId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (tableName: string) => dropTable(dbId, tableName),
    onSuccess: () => {
      // Invalidate the tables list to refetch after dropping
      queryClient.invalidateQueries({
        queryKey: ['databases', dbId, 'tables'],
      })
    },
  })
}
