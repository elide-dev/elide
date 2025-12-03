import * as React from 'react'
import type { ColumnMetadata } from '@/lib/types'
import { Input } from '@/components/ui/input'
import {
  NullEmptySuggestion,
  EMPTY_STRING_MARKER,
  isEmptyStringMarker,
  getDisplayValue,
  type NullableValue,
} from './NullEmptyDropdown'

type EditableCellProps = {
  column: ColumnMetadata
  value: unknown
  onChange: (value: unknown) => void
}

export const EditableCell = React.memo(function EditableCell({ column, value, onChange }: EditableCellProps) {
  const inputRef = React.useRef<HTMLInputElement>(null)
  const [isFocused, setIsFocused] = React.useState(false)

  // Track internal value that distinguishes null from empty string
  const [internalValue, setInternalValue] = React.useState<NullableValue>(() => {
    // Initialize: null stays null, empty string gets marker, others stay as-is
    if (value === null || value === undefined) return null
    if (value === '') return EMPTY_STRING_MARKER
    return value
  })

  // Sync internal value when external value changes
  React.useEffect(() => {
    if (value === null || value === undefined) {
      setInternalValue(null)
    } else if (value === '') {
      setInternalValue(EMPTY_STRING_MARKER)
    } else {
      setInternalValue(value)
    }
  }, [value])

  const handleValueChange = React.useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const newValue = e.target.value
      if (newValue === '') {
        // When cleared, default to NULL
        setInternalValue(null)
        onChange(null)
      } else {
        setInternalValue(newValue)
        onChange(newValue)
      }
    },
    [onChange]
  )

  const handleNullEmptyChange = React.useCallback(
    (newValue: null | typeof EMPTY_STRING_MARKER) => {
      setInternalValue(newValue)
      // Convert marker to actual empty string for the parent
      onChange(isEmptyStringMarker(newValue) ? '' : null)
    },
    [onChange]
  )

  const isAutoIncrementPK = !!(column.primaryKey && column.autoIncrement)
  const displayValue = getDisplayValue(internalValue)
  const isNull = internalValue === null
  const isEmpty = isEmptyStringMarker(internalValue)
  const isEmptyValue = isNull || isEmpty
  const showSuggestion = isFocused && isEmptyValue

  // Get the label to show when empty
  const emptyLabel = isAutoIncrementPK && isNull ? 'DEFAULT' : isNull ? 'NULL' : isEmpty ? 'EMPTY STRING' : null

  // Determine input type based on column type
  const inputType = React.useMemo(() => {
    const normalizedType = column.type.toUpperCase()
    if (normalizedType.includes('INT')) return 'number'
    if (normalizedType.includes('REAL') || normalizedType.includes('FLOAT') || normalizedType.includes('DOUBLE'))
      return 'number'
    return 'text'
  }, [column.type])

  return (
    <div className="relative h-full w-full flex items-center">
      {emptyLabel && (
        <span className="absolute left-4 text-xs font-mono text-muted-foreground/70 pointer-events-none">
          {emptyLabel}
        </span>
      )}
      <Input
        ref={inputRef}
        type={inputType}
        value={displayValue}
        onChange={handleValueChange}
        onFocus={() => setIsFocused(true)}
        onBlur={() => setIsFocused(false)}
        className="h-full w-full border-0 rounded-none bg-transparent px-4 py-2 text-xs md:text-xs font-mono focus-visible:ring-0 focus-visible:ring-offset-0 [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none"
      />
      <NullEmptySuggestion
        value={internalValue}
        onChange={handleNullEmptyChange}
        visible={showSuggestion}
        anchorRef={inputRef}
      />
    </div>
  )
})
