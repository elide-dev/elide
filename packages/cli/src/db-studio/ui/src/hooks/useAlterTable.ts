import { useMutation, useQueryClient } from '@tanstack/react-query'
import { API_BASE_URL } from '../config'

type AlterTableRequest = {
  operations: Array<
    | { type: 'add_column'; column: any }
    | { type: 'drop_column'; columnName: string }
    | { type: 'rename_column'; oldName: string; newName: string }
  >
}

export function useAlterTable(dbId: string, tableName: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (request: AlterTableRequest) => {
      const res = await fetch(`${API_BASE_URL}/api/databases/${dbId}/tables/${encodeURIComponent(tableName)}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(request),
      })
      if (!res.ok) {
        const error = await res.json()
        throw new Error(error.error || 'Failed to alter table')
      }
      return res.json()
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['databases', dbId, 'tables'] })
      queryClient.invalidateQueries({ queryKey: ['databases', dbId, 'tables', tableName] })
    },
  })
}
