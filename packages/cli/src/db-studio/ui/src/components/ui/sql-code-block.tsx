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
 * Styled for inline use without border or background
 */
export function SQLCodeBlock({ sql, format = false, className = '' }: SQLCodeBlockProps) {
  const formattedSql = format
    ? formatSql(sql, {
        language: 'sql',
        tabWidth: 2,
        keywordCase: 'upper',
      })
    : sql

  return (
    <div className={className}>
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
        className="[&_.cm-editor]:bg-transparent [&_.cm-editor]:border-0 [&_.cm-scroller]:font-mono [&_.cm-content]:text-sm [&_.cm-content]:py-0 [&_.cm-content]:px-0 [&_.cm-editor]:cursor-text [&_.cm-focused]:outline-none"
      />
    </div>
  )
}
