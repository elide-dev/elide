import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useParams } from 'react-router-dom'
import { API_BASE_URL } from '../config'
import { useDataTable } from '@/contexts/DataTableContext'

type DeleteRowsResponse = {
  success: true
}

async function deleteRows(
  dbId: string,
  tableName: string,
  primaryKeys: Record<string, unknown>[]
): Promise<DeleteRowsResponse> {
  const res = await fetch(`${API_BASE_URL}/api/databases/${dbId}/tables/${encodeURIComponent(tableName)}/rows`, {
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
  const { dbId, tableName } = useParams<{ dbId: string; tableName: string }>()
  const { table } = useDataTable()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (primaryKeys: Record<string, unknown>[]) => {
      if (!dbId || !tableName) {
        throw new Error('Database ID and table name are required')
      }
      return deleteRows(dbId, tableName, primaryKeys)
    },
    onSuccess: () => {
      // Invalidate table data to trigger a refetch
      queryClient.invalidateQueries({
        queryKey: ['databases', dbId, 'tables', tableName],
      })

      // Clear row selection
      table.resetRowSelection()
    },
    onError: (error: Error) => {
      console.error('Failed to delete rows:', error)
    },
  })
}
