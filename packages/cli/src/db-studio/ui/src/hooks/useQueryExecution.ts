import { useMutation } from '@tanstack/react-query'
import { API_BASE_URL } from '../config'

export type ColumnMetadata = {
  name: string
  type: string
  nullable: boolean
  primaryKey: boolean
  defaultValue?: string | number | null
  foreignKey?: {
    table: string
    column: string
    onUpdate?: string
    onDelete?: string
  }
  unique?: boolean
  autoIncrement?: boolean
}

export type QueryMetadata = {
  executionTimeMs: number
  sql: string
  rowCount: number
}

export type SelectQueryResult = {
  success: true
  data: Record<string, unknown>[]
  columns: ColumnMetadata[]
  metadata: QueryMetadata
}

export type WriteQueryResult = {
  success: true
  metadata: QueryMetadata
}

export type QueryResult = SelectQueryResult | WriteQueryResult

async function executeQuery(dbId: string, sql: string, params?: unknown[]): Promise<QueryResult> {
  const res = await fetch(`${API_BASE_URL}/api/databases/${dbId}/query`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      sql,
      params: params || [],
    }),
  })

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({ error: res.statusText }))

    // Create error with additional context
    const error: any = new Error(errorData.error || `HTTP ${res.status}: ${res.statusText}`)

    // Attach the full error response for display purposes
    error.response = errorData

    throw error
  }

  return res.json()
}

export function useQueryExecution(dbId: string | undefined) {
  return useMutation({
    mutationFn: ({ sql, params }: { sql: string; params?: unknown[] }) => {
      if (!dbId) {
        throw new Error('Database ID is required')
      }
      return executeQuery(dbId, sql, params)
    },
  })
}
