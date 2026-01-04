import { useMutation, useQueryClient } from '@tanstack/react-query'
import { API_BASE_URL } from '../config'

type CreateTableRequest = {
  name: string
  columns: Array<{
    name: string
    type: string
    nullable: boolean
    primaryKey: boolean
    unique: boolean
    defaultValue: string | number | null
  }>
}

export function useCreateTable(dbId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (request: CreateTableRequest) => {
      const res = await fetch(`${API_BASE_URL}/api/databases/${dbId}/tables`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(request),
      })
      if (!res.ok) {
        const error = await res.json()
        throw new Error(error.error || 'Failed to create table')
      }
      return res.json()
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['databases', dbId, 'tables'] })
    },
  })
}
