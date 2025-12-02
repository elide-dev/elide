import { useMutation, useQueryClient } from '@tanstack/react-query'
import { API_BASE_URL } from '../config'

async function dropTable(dbIndex: string, tableName: string): Promise<void> {
  const res = await fetch(`${API_BASE_URL}/api/databases/${dbIndex}/tables/${encodeURIComponent(tableName)}`, {
    method: 'DELETE',
  })

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({ message: res.statusText }))
    throw new Error(errorData.message || `HTTP ${res.status}: ${res.statusText}`)
  }
}

export function useDropTable(dbIndex: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (tableName: string) => dropTable(dbIndex, tableName),
    onSuccess: () => {
      // Invalidate the tables list to refetch after dropping
      queryClient.invalidateQueries({
        queryKey: ['databases', dbIndex, 'tables'],
      })
    },
  })
}
