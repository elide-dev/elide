import * as React from 'react'
import type { ColumnMetadata } from '@/lib/types'
import { Input } from '@/components/ui/input'

type EditableCellProps = {
  column: ColumnMetadata
  value: unknown
  onChange: (value: unknown) => void
}

export const EditableCell = React.memo(function EditableCell({ column, value, onChange }: EditableCellProps) {
  const handleValueChange = React.useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const newValue = e.target.value
      if (newValue === '') {
        onChange(undefined)
      } else {
        onChange(newValue)
      }
    },
    [onChange]
  )

  // Auto-increment hint
  const placeholder = React.useMemo(() => {
    if (column.autoIncrement) {
      return '(auto)'
    }
    if (column.defaultValue !== undefined && column.defaultValue !== null) {
      return String(column.defaultValue)
    }
    return ''
  }, [column])

  // Determine input type based on column type
  const inputType = React.useMemo(() => {
    const normalizedType = column.type.toUpperCase()
    if (normalizedType.includes('INT')) return 'number'
    if (normalizedType.includes('REAL') || normalizedType.includes('FLOAT') || normalizedType.includes('DOUBLE'))
      return 'number'
    return 'text'
  }, [column.type])

  const step = React.useMemo(() => {
    const normalizedType = column.type.toUpperCase()
    if (normalizedType.includes('INT')) return '1'
    if (normalizedType.includes('REAL') || normalizedType.includes('FLOAT') || normalizedType.includes('DOUBLE'))
      return 'any'
    return undefined
  }, [column.type])

  return (
    <Input
      type={inputType}
      step={step}
      value={value === null ? '' : String(value ?? '')}
      onChange={handleValueChange}
      placeholder={placeholder}
      className="h-full w-full border-0 rounded-none bg-transparent px-4 py-2 text-xs font-mono focus-visible:ring-0 focus-visible:ring-offset-0"
    />
  )
})
