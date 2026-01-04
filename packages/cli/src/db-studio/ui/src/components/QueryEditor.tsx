import { useMemo, useRef, useEffect } from 'react'
import { Play, X, AlignLeft } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Kbd } from '@/components/ui/kbd'
import CodeMirror from '@uiw/react-codemirror'
import { sql as sqlLang } from '@codemirror/lang-sql'
import { oneDark } from '@codemirror/theme-one-dark'
import { keymap } from '@codemirror/view'
import { Prec } from '@codemirror/state'
import { format } from 'sql-formatter'

interface QueryEditorProps {
  sql: string
  onSqlChange: (sql: string) => void
  onExecute: () => void
  loading: boolean
}

export function QueryEditor({ sql, onSqlChange, onExecute, loading }: QueryEditorProps) {
  const handleClear = () => onSqlChange('')

  const handleFormat = () => {
    if (!sql.trim()) return
    try {
      const formatted = format(sql, {
        language: 'sql',
        tabWidth: 2,
        keywordCase: 'upper',
      })
      onSqlChange(formatted)
    } catch (err) {
      // If formatting fails, just keep the original SQL
      console.error('Formatting error:', err)
    }
  }

  const sqlRef = useRef(sql)
  const onExecuteRef = useRef(onExecute)

  useEffect(() => {
    sqlRef.current = sql
    onExecuteRef.current = onExecute
  }, [sql, onExecute])

  const executeKeymap = useMemo(
    () =>
      Prec.highest(
        keymap.of([
          {
            key: 'Mod-Enter',
            run: () => {
              const currentSql = sqlRef.current.trim()
              if (!currentSql) return false
              onExecuteRef.current()
              return true
            },
          },
        ])
      ),
    []
  )

  return (
    <div className="border-b border-border h-full flex flex-col">
      <div className="flex font-sans items-center gap-2 px-6 py-4 border-b border-border bg-background shrink-0">
        <h2 className="text-lg font-semibold tracking-tight">SQL Query Editor</h2>
        <Button onClick={handleClear} variant="outline" disabled={loading} className="h-9 gap-2">
          <X className="h-4 w-4" />
          Clear
        </Button>
        <Button onClick={handleFormat} variant="outline" disabled={loading || !sql.trim()}>
          <AlignLeft className="w-3 h-3 mr-1" />
          Format
        </Button>
        <Button
          onClick={onExecute}
          disabled={loading || !sql.trim()}
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
            onChange={(value) => onSqlChange(value)}
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
  )
}
