import * as React from 'react'
import type { ColumnMetadata } from '@/lib/types'
import { Input } from '@/components/ui/input'

type InlineEditableCellProps = {
  column: ColumnMetadata
  value: unknown
  originalValue: unknown
  onChange: (value: unknown) => void
  onCommit: () => void
  onCancel: () => void
  autoFocus?: boolean
}

export const InlineEditableCell = React.memo(function InlineEditableCell({
  column,
  value,
  onChange,
  onCommit,
  onCancel,
  autoFocus = true,
}: InlineEditableCellProps) {
  const inputRef = React.useRef<HTMLInputElement>(null)

  // Focus and select all on mount
  React.useEffect(() => {
    if (autoFocus && inputRef.current) {
      inputRef.current.focus()
      inputRef.current.select()
    }
  }, [autoFocus])

  const handleValueChange = React.useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const newValue = e.target.value
      if (newValue === '') {
        onChange(null)
      } else {
        onChange(newValue)
      }
    },
    [onChange]
  )

  const handleKeyDown = React.useCallback(
    (e: React.KeyboardEvent<HTMLInputElement>) => {
      if (e.key === 'Enter') {
        e.preventDefault()
        onCommit()
      } else if (e.key === 'Escape') {
        e.preventDefault()
        onCancel()
      }
    },
    [onCommit, onCancel]
  )

  const handleBlur = React.useCallback(() => {
    // Commit on blur
    onCommit()
  }, [onCommit])

  // Placeholder for null values
  const placeholder = React.useMemo(() => {
    if (column.primaryKey && column.autoIncrement) {
      return 'DEFAULT'
    }
    return 'NULL'
  }, [column])

  // Determine input type based on column type
  const inputType = React.useMemo(() => {
    const normalizedType = column.type.toUpperCase()
    if (normalizedType.includes('INT')) return 'number'
    if (normalizedType.includes('REAL') || normalizedType.includes('FLOAT') || normalizedType.includes('DOUBLE'))
      return 'number'
    return 'text'
  }, [column.type])

  return (
    <Input
      ref={inputRef}
      type={inputType}
      value={value === null ? '' : String(value ?? '')}
      onChange={handleValueChange}
      onKeyDown={handleKeyDown}
      onBlur={handleBlur}
      placeholder={placeholder}
      className="h-full w-full border-0 rounded-none bg-transparent px-4 py-2 text-xs font-mono focus-visible:ring-0 focus-visible:ring-offset-0 [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none"
    />
  )
})

