import CodeMirror from '@uiw/react-codemirror'
import { sql as sqlLang } from '@codemirror/lang-sql'
import { oneDark } from '@codemirror/theme-one-dark'
import { EditorView } from '@codemirror/view'

interface SQLCodeBlockProps {
  sql: string
  className?: string
}

/**
 * A read-only SQL code block with syntax highlighting
 * Used to display SQL queries in error messages and dialogs
 */
export function SQLCodeBlock({ sql, className = '' }: SQLCodeBlockProps) {
  // Truncate very long SQL for display
  const displaySql = sql.length > 500 ? sql.slice(0, 500) + '...' : sql

  return (
    <div className={`rounded-md border border-border overflow-hidden ${className}`}>
      <div className="px-2 py-1 bg-muted/50 border-b border-border">
        <span className="text-xs font-medium text-muted-foreground">SQL Query</span>
      </div>
      <CodeMirror
        value={displaySql}
        height="auto"
        maxHeight="150px"
        extensions={[
          sqlLang(),
          EditorView.lineWrapping,
          EditorView.editable.of(false),
        ]}
        theme={oneDark}
        basicSetup={{
          lineNumbers: false,
          foldGutter: false,
          dropCursor: false,
          allowMultipleSelections: false,
          highlightActiveLine: false,
          highlightSelectionMatches: false,
        }}
        editable={false}
        className="[&_.cm-editor]:bg-zinc-900 [&_.cm-editor]:border-0 [&_.cm-scroller]:font-mono [&_.cm-content]:text-xs [&_.cm-content]:py-2 [&_.cm-content]:px-3 [&_.cm-editor]:cursor-text [&_.cm-focused]:outline-none"
      />
    </div>
  )
}

