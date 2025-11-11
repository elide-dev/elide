import { useMutation } from '@tanstack/react-query'
import { API_BASE_URL } from '../config'

export type QueryResult = {
  success: boolean
  rows?: unknown[]
  rowCount?: number
  rowsAffected?: number
  lastInsertRowid?: number
  error?: string
}

async function executeQuery(
  dbIndex: string,
  sql: string,
  params?: unknown[]
): Promise<QueryResult> {
  const res = await fetch(
    `${API_BASE_URL}/api/databases/${dbIndex}/query`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        sql,
        params: params || [],
      }),
    }
  )

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({ error: res.statusText }))
    throw new Error(errorData.error || `HTTP ${res.status}: ${res.statusText}`)
  }

  return res.json()
}

export function useQueryExecution(dbIndex: string | undefined) {
  return useMutation({
    mutationFn: ({ sql, params }: { sql: string; params?: unknown[] }) => {
      if (!dbIndex) {
        throw new Error('Database index is required')
      }
      return executeQuery(dbIndex, sql, params)
    },
  })
}

