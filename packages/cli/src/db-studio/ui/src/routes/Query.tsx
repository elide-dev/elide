import { useState, useMemo, useRef, useEffect } from 'react'
import { useParams } from 'react-router-dom'
import { Play, X, AlignLeft } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Kbd } from '@/components/ui/kbd'
import CodeMirror from '@uiw/react-codemirror'
import { sql as sqlLang } from '@codemirror/lang-sql'
import { oneDark } from '@codemirror/theme-one-dark'
import { keymap } from '@codemirror/view'
import { Prec } from '@codemirror/state'
import { format } from 'sql-formatter'
import { useQueryExecution } from '../hooks/useQueryExecution'
import { useDatabaseTables } from '../hooks/useDatabaseTables'
import { DataTable } from '../components/DataTable'
import { DataTableProvider } from '@/contexts/DataTableContext'
import { getCoreRowModel, useReactTable } from '@tanstack/react-table'
import type { ColumnDef } from '@tanstack/react-table'
import { ResizablePanelGroup, ResizablePanel, ResizableHandle } from '@/components/ui/resizable'

export default function Query() {
  const { dbIndex } = useParams()

  const [sql, setSql] = useState('')

  const { data: tables = [] } = useDatabaseTables(dbIndex)
  const { mutate: executeQuery, data: result, isPending: loading, error } = useQueryExecution(dbIndex)

  useEffect(() => {
    if (tables.length > 0) {
      const firstTable = tables.find((t) => t.type === 'table')
      if (firstTable) {
        setSql(`SELECT * FROM "${firstTable.name}";`)
      }
    }
  }, [tables])

  const handleExecute = () => executeQuery({ sql: sql.trim() })

  const handleClear = () => setSql('')

  const handleFormat = () => {
    if (!sql.trim()) return
    try {
      const formatted = format(sql, {
        language: 'sql',
        tabWidth: 2,
        keywordCase: 'upper',
      })
      setSql(formatted)
    } catch (err) {
      // If formatting fails, just keep the original SQL
      console.error('Formatting error:', err)
    }
  }

  // Memoize data object for DataTable
  const tableData = useMemo(() => {
    if (!result || !('data' in result)) return null
    return {
      columns: result.columns,
      rows: result.data.map((row) => result.columns.map((col) => row[col.name])),
      metadata: result.metadata,
    }
  }, [result])

  const sqlRef = useRef(sql)
  const executeQueryRef = useRef(executeQuery)

  useEffect(() => {
    sqlRef.current = sql
    executeQueryRef.current = executeQuery
  }, [sql, executeQuery])

  const executeKeymap = useMemo(
    () =>
      Prec.highest(
        keymap.of([
          {
            key: 'Mod-Enter',
            run: () => {
              const currentSql = sqlRef.current.trim()
              if (!currentSql) return false
              executeQueryRef.current({ sql: currentSql })
              return true
            },
          },
        ])
      ),
    []
  )

  return (
    <div className="flex-1 p-0 overflow-hidden font-mono flex flex-col h-full">
      <ResizablePanelGroup direction="vertical" className="h-full">
        <ResizablePanel defaultSize={40} minSize={20} maxSize={70}>
          <div className="border-b border-border h-full flex flex-col">
            <div className="flex items-center gap-2 px-6 py-4 border-b border-border bg-background shrink-0">
              <h2 className="text-lg font-semibold tracking-tight">SQL Query Editor</h2>
              <Button onClick={handleClear} variant="outline" size="sm" disabled={loading} className="h-9 gap-2">
                <X className="h-4 w-4" />
                Clear
              </Button>
              <Button onClick={handleFormat} variant="outline" disabled={loading || !sql.trim()}>
                <AlignLeft className="w-3 h-3 mr-1" />
                Format
              </Button>
              <Button
                onClick={handleExecute}
                disabled={loading || !sql.trim()}
                size="sm"
                className="h-9 gap-2 bg-primary text-primary-foreground hover:bg-primary/90 border-0"
              >
                <Play className="h-4 w-4" />
                Execute Query
                <Kbd>⌘↵</Kbd>
              </Button>
            </div>
            <div className="w-full flex-1 flex flex-col min-h-0 border border-border">
              <div className="flex-1 min-h-0">
                <CodeMirror
                  value={sql}
                  onChange={(value) => setSql(value)}
                  placeholder="Enter your SQL query here... (Cmd/Ctrl + Enter to execute)"
                  height="100%"
                  extensions={[sqlLang(), executeKeymap]}
                  theme={oneDark}
                  basicSetup={{
                    lineNumbers: true,
                    foldGutter: false,
                    dropCursor: false,
                    allowMultipleSelections: false,
                  }}
                  editable={!loading}
                  className="w-full h-full [&_.cm-editor]:bg-background [&_.cm-editor]:border-0 [&_.cm-editor]:rounded-none [&_.cm-scroller]:font-mono [&_.cm-content]:text-foreground [&_.cm-content]:text-sm [&_.cm-placeholder]:text-muted-foreground [&_.cm-editor]:w-full [&_.cm-gutters]:bg-background [&_.cm-lineNumbers]:text-muted-foreground [&_.cm-editor]:p-0 [&_.cm-scroller]:p-0 [&_.cm-content]:p-0 [&_.cm-editor]:h-full"
                />
              </div>
            </div>
          </div>
        </ResizablePanel>

        <ResizableHandle withHandle className="bg-border" />

        <ResizablePanel defaultSize={60} minSize={30}>
          <div className="flex-1 overflow-auto h-full">
            {loading && (
              <div className="flex items-center justify-center h-full text-muted-foreground">Executing query...</div>
            )}

            {error && (
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

                  {/* Try to extract and display SQL from error response */}
                  {(() => {
                    try {
                      // The error might have additional data in its response
                      const errorData = (error as any).response || {}
                      if (errorData.sql) {
                        return (
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
                        )
                      }
                    } catch {
                      // Ignore error parsing errors
                    }
                    return null
                  })()}
                </div>
              </div>
            )}

            {result && !error && (
              <>
                {'data' in result ? (
                  <>
                    {result.data.length > 0 && tableData ? (
                      <QueryResultsTable tableData={tableData} totalRows={result.data.length} />
                    ) : (
                      <div className="px-6 pt-6 text-muted-foreground text-sm">No rows returned</div>
                    )}
                  </>
                ) : (
                  <div className="px-6 pt-6">
                    <div className="bg-muted/50 border border-border p-4">
                      <div className="text-sm text-foreground">
                        <div className="mb-2">
                          <span className="text-muted-foreground">Execution time: </span>
                          <span className="font-mono font-semibold text-emerald-500">
                            {result.metadata.executionTimeMs}ms
                          </span>
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
                )}
              </>
            )}

            {!result && !loading && !error && (
              <div className="flex items-center justify-center h-full text-muted-foreground">
                Enter a SQL query and click Execute Query to see results
              </div>
            )}
          </div>
        </ResizablePanel>
      </ResizablePanelGroup>
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
  tableData: { columns: any[]; rows: unknown[][]; metadata: any }
  totalRows: number
}) {
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
      cell: ({ getValue }) => {
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
      columns: tableData.columns,
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
    [table, tableData, totalRows]
  )

  return (
    <DataTableProvider value={contextValue}>
      <DataTable />
    </DataTableProvider>
  )
}
