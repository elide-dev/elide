import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useParams } from 'react-router-dom'
import { API_BASE_URL } from '../config'

type UpdateRowResponse = {
  success: true
  sql: string
}

type UpdateRowError = Error & {
  response?: {
    error?: string
    sql?: string
  }
}

type UpdateRowParams = {
  primaryKey: Record<string, unknown>
  updates: Record<string, unknown>
}

async function updateRow(
  dbId: string,
  tableName: string,
  params: UpdateRowParams
): Promise<UpdateRowResponse> {
  const res = await fetch(`${API_BASE_URL}/api/databases/${dbId}/tables/${encodeURIComponent(tableName)}/rows`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(params),
  })

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({ error: res.statusText }))
    const error = new Error(errorData.error || `HTTP ${res.status}: ${res.statusText}`) as UpdateRowError
    error.response = errorData
    throw error
  }

  return res.json()
}

export function useUpdateRow() {
  const { dbId, tableName } = useParams<{ dbId: string; tableName: string }>()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (params: UpdateRowParams) => {
      if (!dbId || !tableName) {
        throw new Error('Database ID and table name are required')
      }
      return updateRow(dbId, tableName, params)
    },
    onSuccess: () => {
      // Invalidate table data to trigger a refetch
      queryClient.invalidateQueries({
        queryKey: ['databases', dbId, 'tables', tableName],
      })
    },
    onError: (error: Error) => {
      console.error('Failed to update row:', error)
    },
  })
}

