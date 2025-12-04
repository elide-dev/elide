import CodeMirror from '@uiw/react-codemirror'
import { sql as sqlLang } from '@codemirror/lang-sql'
import { oneDark } from '@codemirror/theme-one-dark'
import { EditorView } from '@codemirror/view'
import { format as formatSql } from 'sql-formatter'

interface SQLCodeBlockProps {
  sql: string
  format?: boolean
  className?: string
}

/**
 * A read-only SQL code block with syntax highlighting
 * Used to display SQL queries in error messages and dialogs
 */
export function SQLCodeBlock({ sql, format = false, className = '' }: SQLCodeBlockProps) {
  // Truncate very long SQL for display

  const formattedSql = format
    ? formatSql(sql, {
        language: 'sql',
        tabWidth: 2,
        keywordCase: 'upper',
      })
    : sql

  return (
    <div className={`rounded-md border border-border overflow-hidden ${className}`}>
      <CodeMirror
        value={formattedSql}
        height="auto"
        maxHeight="200px"
        extensions={[sqlLang(), EditorView.lineWrapping, EditorView.editable.of(false)]}
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
        className="[&_.cm-editor]:bg-zinc-900 [&_.cm-editor]:border-0 [&_.cm-scroller]:font-mono [&_.cm-content]:text-sm [&_.cm-content]:py-3 [&_.cm-content]:px-4 [&_.cm-editor]:cursor-text [&_.cm-focused]:outline-none"
      />
    </div>
  )
}
