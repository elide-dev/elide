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
import {
  Table as UiTable,
  TableHeader,
  TableBody,
  TableRow,
  TableHead,
  TableCell,
} from '@/components/ui/table'
import { useQueryExecution } from '../hooks/useQueryExecution'
import { useDatabaseTables } from '../hooks/useDatabaseTables'

export default function Query() {
  const { dbIndex } = useParams()

  const [sql, setSql] = useState('')
  const [cursorPosition, setCursorPosition] = useState({ line: 1, column: 1 })
  
  const { data: tables = [] } = useDatabaseTables(dbIndex)
  const { mutate: executeQuery, data: result, isPending: loading, error } = useQueryExecution(dbIndex)

  useEffect(() => {
    if (tables.length > 0) {
      const firstTable = tables[0].name
      setSql(`SELECT * FROM ${firstTable};`)
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
      <div className="px-6 pt-6 pb-4 border-b border-gray-800">
        <div className="flex items-center justify-between mb-4">
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
            className="w-full [&_.cm-editor]:bg-gray-900 [&_.cm-editor]:border-0 [&_.cm-editor]:rounded-none [&_.cm-scroller]:font-mono [&_.cm-content]:text-gray-200 [&_.cm-content]:text-sm [&_.cm-placeholder]:text-gray-600 [&_.cm-editor]:w-full [&_.cm-gutter]:bg-gray-900 [&_.cm-lineNumbers]:text-gray-500"
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
        {loading && (
          <div className="flex items-center justify-center h-full text-gray-500">
            Executing query...
          </div>
        )}

        {error && (
          <div className="px-6 pt-6">
            <div className="bg-red-950/30 border border-red-800 p-4">
              <h3 className="text-red-400 font-semibold mb-2">Error</h3>
              <p className="text-red-300 text-sm">{error.message}</p>
            </div>
          </div>
        )}

        {result && !error && (
          <>
            {result.rows !== undefined ? (
              <>
                {result.rows.length > 0 ? (
                  <div className="overflow-hidden">
                    <UiTable className="w-full">
                      <TableHeader>
                        <TableRow className="bg-gray-900/50">
                          {Object.keys(result.rows[0] as Record<string, unknown>).map((col) => (
                            <TableHead
                              key={col}
                              className="text-left px-4 py-3 text-xs font-medium text-gray-400 tracking-wider border-b border-gray-800"
                            >
                              {col}
                            </TableHead>
                          ))}
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {result.rows.map((row, i) => (
                          <TableRow key={i} className="hover:bg-gray-900/30 transition-colors">
                            {Object.values(row as Record<string, unknown>).map((cell, j) => (
                              <TableCell
                                key={j}
                                className="px-4 py-3 text-sm text-gray-200 whitespace-nowrap"
                              >
                                {String(cell ?? '')}
                              </TableCell>
                            ))}
                          </TableRow>
                        ))}
                      </TableBody>
                    </UiTable>
                  </div>
                ) : (
                  <div className="px-6 pt-6 text-gray-500 text-sm">No rows returned</div>
                )}
              </>
            ) : (
              <div className="px-6 pt-6">
                <div className="bg-gray-900/50 border border-gray-800 p-4">
                  <div className="text-sm text-gray-300">
                    {result.rowsAffected !== undefined && (
                      <div className="mb-2">
                        <span className="text-gray-400">Rows affected: </span>
                        <span className="font-semibold">{result.rowsAffected}</span>
                      </div>
                    )}
                    {result.lastInsertRowid !== undefined && (
                      <div>
                        <span className="text-gray-400">Last insert row ID: </span>
                        <span className="font-semibold">{result.lastInsertRowid}</span>
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

