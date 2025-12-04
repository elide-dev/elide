import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useParams } from 'react-router-dom'
import { API_BASE_URL } from '../config'

type InsertRowResponse = {
  success: true
  sql: string
}

type InsertRowError = Error & {
  response?: {
    error?: string
    sql?: string
  }
}

async function insertRow(dbId: string, tableName: string, row: Record<string, unknown>): Promise<InsertRowResponse> {
  const res = await fetch(`${API_BASE_URL}/api/databases/${dbId}/tables/${encodeURIComponent(tableName)}/rows`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      row,
    }),
  })

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({ error: res.statusText }))
    const error = new Error(errorData.error || `HTTP ${res.status}: ${res.statusText}`) as InsertRowError
    error.response = errorData
    throw error
  }

  return res.json()
}

export function useInsertRow() {
  const { dbId, tableName } = useParams<{ dbId: string; tableName: string }>()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (row: Record<string, unknown>) => {
      if (!dbId || !tableName) {
        throw new Error('Database ID and table name are required')
      }
      return insertRow(dbId, tableName, row)
    },
    onSuccess: () => {
      // Invalidate table data to trigger a refetch
      queryClient.invalidateQueries({
        queryKey: ['databases', dbId, 'tables', tableName],
      })
    },
    onError: (error: Error) => {
      console.error('Failed to insert row:', error)
    },
  })
}
