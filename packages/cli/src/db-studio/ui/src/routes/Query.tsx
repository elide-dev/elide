import { useState, useMemo, useRef, useEffect } from 'react'
import { useParams } from 'react-router-dom'
import { Play, X, AlignLeft } from 'lucide-react'
import { Button } from '@/components/ui/button'
import CodeMirror from '@uiw/react-codemirror'
import { sql as sqlLang } from '@codemirror/lang-sql'
import { oneDark } from '@codemirror/theme-one-dark'
import { keymap, EditorView } from '@codemirror/view'
import { Prec } from '@codemirror/state'
import { format } from 'sql-formatter'
import { useQueryExecution } from '../hooks/useQueryExecution'
import { useDatabaseTables } from '../hooks/useDatabaseTables'
import { DataTable } from '../components/DataTable'
import { DataTableProvider } from '@/contexts/DataTableContext'
import { getCoreRowModel, useReactTable } from '@tanstack/react-table'
import type { ColumnDef } from '@tanstack/react-table'

export default function Query() {
  const { dbIndex } = useParams()

  const [sql, setSql] = useState('')
  const [cursorPosition, setCursorPosition] = useState({ line: 1, column: 1 })

  const { data: tables = [] } = useDatabaseTables(dbIndex)
  const { mutate: executeQuery, data: result, isPending: loading, error } = useQueryExecution(dbIndex)

  useEffect(() => {
    if (tables.length > 0) {
      const firstTable = tables[0].name
      setSql(`SELECT * FROM "${firstTable}";`)
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

  const cursorPositionExtension = useMemo(
    () =>
      EditorView.updateListener.of((update) => {
        if (update.selectionSet) {
          const { state } = update.view
          const { head } = state.selection.main
          const line = state.doc.lineAt(head)
          const lineNumber = line.number
          const column = head - line.from + 1
          setCursorPosition({ line: lineNumber, column })
        }
      }),
    []
  )

  return (
    <div className="flex-1 p-0 overflow-auto font-mono flex flex-col">
      <div className="pt-6 border-b border-gray-800">
        <div className="flex items-center justify-between mb-4 px-6">
          <h2 className="text-2xl font-semibold tracking-tight">SQL Query Editor</h2>
          <div className="flex gap-2">
            <Button
              onClick={handleExecute}
              disabled={loading || !sql.trim()}
              className="bg-gray-200 hover:bg-gray-300 text-gray-900 border-0"
            >
              <Play className="w-4 h-4" />
              Execute Query
            </Button>
            <Button
              onClick={handleClear}
              variant="outline"
              disabled={loading}
              className="border-gray-800 bg-gray-950 text-gray-200 hover:bg-gray-900 hover:text-white"
            >
              <X className="w-4 h-4" />
              Clear
            </Button>
          </div>
        </div>
        <div className="w-full border border-gray-800">
          <CodeMirror
            value={sql}
            onChange={(value) => setSql(value)}
            placeholder="Enter your SQL query here... (Cmd/Ctrl + Enter to execute)"
            height="30vh"
            extensions={[sqlLang(), executeKeymap, cursorPositionExtension]}
            theme={oneDark}
            basicSetup={{
              lineNumbers: true,
              foldGutter: false,
              dropCursor: false,
              allowMultipleSelections: false,
            }}
            editable={!loading}
            className="w-full [&_.cm-editor]:bg-gray-900 [&_.cm-editor]:border-0 [&_.cm-editor]:rounded-none [&_.cm-scroller]:font-mono [&_.cm-content]:text-gray-200 [&_.cm-content]:text-sm [&_.cm-placeholder]:text-gray-600 [&_.cm-editor]:w-full [&_.cm-gutter]:bg-gray-900 [&_.cm-lineNumbers]:text-gray-500 [&_.cm-editor]:p-0 [&_.cm-scroller]:p-0 [&_.cm-content]:p-0"
          />
          <div className="flex items-center gap-3 px-3 py-1.5 bg-gray-900 border-t border-gray-800">
            <div className="text-xs text-gray-400 font-mono">
              Line {cursorPosition.line}, Column {cursorPosition.column}
            </div>
            <Button
              onClick={handleFormat}
              variant="outline"
              size="sm"
              disabled={loading || !sql.trim()}
              className="border-gray-800 bg-gray-950 text-gray-200 hover:bg-gray-800 hover:text-white h-6 px-2 text-xs"
            >
              <AlignLeft className="w-3 h-3 mr-1" />
              Format
            </Button>
          </div>
        </div>
      </div>

      <div className="flex-1 overflow-auto">
        {loading && <div className="flex items-center justify-center h-full text-gray-500">Executing query...</div>}

        {error && (
          <div className="px-6 pt-6">
            <div className="bg-red-950/30 border border-red-800 p-4 rounded-lg">
              <h3 className="text-red-400 font-semibold mb-3 flex items-center gap-2">
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
              <p className="text-red-300 text-sm mb-3 leading-relaxed">{error.message}</p>

              {/* Try to extract and display SQL from error response */}
              {(() => {
                try {
                  // The error might have additional data in its response
                  const errorData = (error as any).response || {}
                  if (errorData.sql) {
                    return (
                      <div className="mt-3 pt-3 border-t border-red-800/50">
                        <div className="text-xs text-red-400/70 mb-1 font-semibold">Failed Query:</div>
                        <pre className="text-xs text-red-200 bg-red-950/50 p-2 rounded border border-red-800/30 overflow-x-auto font-mono">
                          {errorData.sql}
                        </pre>
                        {errorData.executionTimeMs !== undefined && (
                          <div className="text-xs text-red-400/70 mt-2">
                            Execution time: <span className="font-mono">{errorData.executionTimeMs}ms</span>
                          </div>
                        )}
                      </div>
                    )
                  }
                } catch {}
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
                  <div className="px-6 pt-6 text-gray-500 text-sm">No rows returned</div>
                )}
              </>
            ) : (
              <div className="px-6 pt-6">
                <div className="bg-gray-900/50 border border-gray-800 p-4">
                  <div className="text-sm text-gray-300">
                    <div className="mb-2">
                      <span className="text-gray-400">Execution time: </span>
                      <span className="font-mono font-semibold text-green-400">
                        {result.metadata.executionTimeMs}ms
                      </span>
                    </div>
                    <div className="mb-2">
                      <span className="text-gray-400">Rows affected: </span>
                      <span className="font-semibold">{result.rowsAffected}</span>
                    </div>
                    {result.lastInsertRowid !== undefined && (
                      <div>
                        <span className="text-gray-400">Last insert row ID: </span>
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
          <div className="flex items-center justify-center h-full text-gray-500">
            Enter a SQL query and click Execute Query to see results
          </div>
        )}
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
  tableData: { columns: any[]; rows: unknown[][]; metadata: any }
  totalRows: number
}) {
  // Build TanStack Table columns
  const tableColumns: ColumnDef<Record<string, unknown>>[] = useMemo(() => {
    return tableData.columns.map((col) => ({
      accessorKey: col.name,
      header: col.name,
      cell: ({ getValue }) => {
        const value = getValue()
        if (value === null || value === undefined)
          return <span className="text-gray-500 font-normal">NULL</span>
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
