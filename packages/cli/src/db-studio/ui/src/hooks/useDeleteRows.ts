import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useParams } from 'react-router-dom'
import { API_BASE_URL } from '../config'
import { useDataTable } from '@/contexts/DataTableContext'

type DeleteRowsResponse = {
  success: true
}

async function deleteRows(
  dbIndex: string,
  tableName: string,
  primaryKeys: Record<string, unknown>[]
): Promise<DeleteRowsResponse> {
  const res = await fetch(`${API_BASE_URL}/api/databases/${dbIndex}/tables/${encodeURIComponent(tableName)}/rows`, {
    method: 'DELETE',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      primaryKeys,
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

export function useDeleteRows() {
  const { dbIndex, tableName } = useParams<{ dbIndex: string; tableName: string }>()
  const { table } = useDataTable()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (primaryKeys: Record<string, unknown>[]) => {
      if (!dbIndex || !tableName) {
        throw new Error('Database index and table name are required')
      }
      return deleteRows(dbIndex, tableName, primaryKeys)
    },
    onSuccess: () => {
      // Invalidate table data to trigger a refetch
      queryClient.invalidateQueries({
        queryKey: ['databases', dbIndex, 'tables', tableName],
      })

      // Clear row selection
      table.resetRowSelection()
    },
    onError: (error: Error) => {
      console.error('Failed to delete rows:', error)
    },
  })
}
