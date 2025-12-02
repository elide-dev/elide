import { useMemo } from 'react'
import { getCoreRowModel, useReactTable } from '@tanstack/react-table'
import type { ColumnDef } from '@tanstack/react-table'
import { DataTable, DataTableProvider } from '@/components/data-table'
import type { ColumnMetadata } from '@/lib/types'

interface SimpleColumnMetadata {
  name: string
  type: string | null
}

interface QueryMetadata {
  executionTimeMs: number
}

interface SelectResult {
  columns: SimpleColumnMetadata[]
  data: Record<string, unknown>[]
  metadata: QueryMetadata
}

interface MutationResult {
  rowsAffected: number
  lastInsertRowid?: number | bigint
  metadata: QueryMetadata
}

type QueryResult = SelectResult | MutationResult

/**
 * Convert simple query result columns to full ColumnMetadata format
 */
function toFullColumnMetadata(columns: SimpleColumnMetadata[]): ColumnMetadata[] {
  return columns.map((col) => ({
    name: col.name,
    type: col.type ?? 'unknown',
    nullable: true,
    primaryKey: false,
  }))
}

interface QueryResultsProps {
  result: QueryResult | null
  loading: boolean
  error: Error | null
}

export function QueryResults({ result, loading, error }: QueryResultsProps) {
  // Memoize data object for DataTable
  const tableData = useMemo(() => {
    if (!result || !('data' in result)) return null
    return {
      columns: toFullColumnMetadata(result.columns),
      rows: result.data.map((row) => result.columns.map((col) => row[col.name])),
      metadata: result.metadata,
    }
  }, [result])

  if (loading) {
    return <div className="flex items-center justify-center h-full text-muted-foreground">Executing query...</div>
  }

  if (error) {
    return <QueryError error={error} />
  }

  if (result) {
    if ('data' in result) {
      if (result.data.length > 0 && tableData) {
        return <QueryResultsTable tableData={tableData} totalRows={result.data.length} />
      }
      return <div className="px-6 pt-6 text-muted-foreground text-sm">No rows returned</div>
    }
    return <MutationResultDisplay result={result} />
  }

  return (
    <div className="flex items-center justify-center h-full text-muted-foreground">
      Enter a SQL query and click Execute Query to see results
    </div>
  )
}

/**
 * Component to display SQL errors
 */
function QueryError({ error }: { error: Error }) {
  // Try to extract and display SQL from error response
  const errorData = (error as any).response || {}

  return (
    <div className="px-6 pt-6">
      <div className="bg-destructive/10 border border-destructive/50 p-4 rounded-lg">
        <h3 className="text-destructive font-semibold mb-3 flex items-center gap-2">
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
            />
          </svg>
          SQL Error
        </h3>
        <p className="text-destructive/90 text-sm mb-3 leading-relaxed">{error.message}</p>

        {errorData.sql && (
          <div className="mt-3 pt-3 border-t border-destructive/30">
            <div className="text-xs text-destructive/70 mb-1 font-semibold">Failed Query:</div>
            <pre className="text-xs text-destructive/90 bg-destructive/5 p-2 rounded border border-destructive/30 overflow-x-auto font-mono">
              {errorData.sql}
            </pre>
            {errorData.executionTimeMs !== undefined && (
              <div className="text-xs text-destructive/70 mt-2">
                Execution time: <span className="font-mono">{errorData.executionTimeMs}ms</span>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  )
}

/**
 * Component to display mutation (non-SELECT) results
 */
function MutationResultDisplay({ result }: { result: MutationResult }) {
  return (
    <div className="px-6 pt-6">
      <div className="bg-muted/50 border border-border p-4">
        <div className="text-sm text-foreground">
          <div className="mb-2">
            <span className="text-muted-foreground">Execution time: </span>
            <span className="font-mono font-semibold text-emerald-500">{result.metadata.executionTimeMs}ms</span>
          </div>
          <div className="mb-2">
            <span className="text-muted-foreground">Rows affected: </span>
            <span className="font-semibold">{result.rowsAffected}</span>
          </div>
          {result.lastInsertRowid !== undefined && (
            <div>
              <span className="text-muted-foreground">Last insert row ID: </span>
              <span className="font-semibold">{String(result.lastInsertRowid)}</span>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

/**
 * Component to render query results in a DataTable
 * Creates a simple table instance without server-side state
 */
function QueryResultsTable({
  tableData,
  totalRows,
}: {
  tableData: { columns: ColumnMetadata[]; rows: unknown[][]; metadata: QueryMetadata }
  totalRows: number
}) {
  // Full columns for context
  const fullColumns = tableData.columns
  // Build TanStack Table columns
  const tableColumns: ColumnDef<Record<string, unknown>>[] = useMemo(() => {
    return tableData.columns.map((col) => ({
      accessorKey: col.name,
      size: 200,
      minSize: 100,
      maxSize: 1000,
      enableResizing: true,
      header: () => (
        <div className="flex items-center gap-1.5 px-4 py-2 w-full h-full">
          <span className="text-xs font-semibold text-foreground tracking-wider truncate">{col.name}</span>
        </div>
      ),
      cell: ({ getValue }: { getValue: () => unknown }) => {
        const value = getValue()
        if (value === null || value === undefined)
          return <span className="text-muted-foreground font-normal">NULL</span>
        return String(value)
      },
    }))
  }, [tableData.columns])

  // Build TanStack Table data
  const tableRows: Record<string, unknown>[] = useMemo(() => {
    return tableData.rows.map((row) =>
      tableData.columns.reduce(
        (acc, col, idx) => {
          acc[col.name] = row[idx]
          return acc
        },
        {} as Record<string, unknown>
      )
    )
  }, [tableData.rows, tableData.columns])

  // Create TanStack Table instance (no sorting, filtering, or pagination)
  const table = useReactTable({
    data: tableRows,
    columns: tableColumns,
    getCoreRowModel: getCoreRowModel(),
    enableColumnResizing: true,
    columnResizeMode: 'onChange',
  })

  // Build context value
  const contextValue = useMemo(
    () => ({
      table,
      columns: fullColumns,
      rowCount: tableData.rows.length,
      metadata: tableData.metadata,
      pagination: { limit: totalRows, offset: 0 },
      sorting: { column: null, direction: null },
      appliedFilters: [],
      onPaginationChange: () => {},
      onSortingChange: () => {},
      onFiltersChange: () => {},
      config: {
        totalRows,
        isLoading: false,
        showControls: false,
        showPagination: false,
      },
    }),
    [table, fullColumns, tableData.rows.length, tableData.metadata, totalRows]
  )

  return (
    <DataTableProvider value={contextValue}>
      <DataTable />
    </DataTableProvider>
  )
}
