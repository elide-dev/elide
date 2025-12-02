import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useParams } from 'react-router-dom'
import { API_BASE_URL } from '../config'

type InsertRowResponse = {
  success: true
}

async function insertRow(
  dbIndex: string,
  tableName: string,
  row: Record<string, unknown>
): Promise<InsertRowResponse> {
  const res = await fetch(`${API_BASE_URL}/api/databases/${dbIndex}/tables/${encodeURIComponent(tableName)}/rows`, {
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
    const error: any = new Error(errorData.error || `HTTP ${res.status}: ${res.statusText}`)
    error.response = errorData
    throw error
  }

  return res.json()
}

export function useInsertRow() {
  const { dbIndex, tableName } = useParams<{ dbIndex: string; tableName: string }>()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (row: Record<string, unknown>) => {
      if (!dbIndex || !tableName) {
        throw new Error('Database index and table name are required')
      }
      return insertRow(dbIndex, tableName, row)
    },
    onSuccess: () => {
      // Invalidate table data to trigger a refetch
      queryClient.invalidateQueries({
        queryKey: ['databases', dbIndex, 'tables', tableName],
      })
    },
    onError: (error: Error) => {
      console.error('Failed to insert row:', error)
    },
  })
}
