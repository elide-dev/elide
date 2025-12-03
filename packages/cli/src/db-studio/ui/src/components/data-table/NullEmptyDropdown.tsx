import * as React from 'react'
import * as ReactDOM from 'react-dom'

// Special marker to distinguish empty string from null
export const EMPTY_STRING_MARKER = Symbol('EMPTY_STRING')

export type NullableValue = null | typeof EMPTY_STRING_MARKER | string | number | unknown

export function isEmptyStringMarker(value: unknown): value is typeof EMPTY_STRING_MARKER {
  return value === EMPTY_STRING_MARKER
}

export function getDisplayValue(value: NullableValue): string {
  if (value === null) return ''
  if (isEmptyStringMarker(value)) return ''
  return String(value ?? '')
}

export function getPlaceholderText(value: NullableValue, isAutoIncrementPK: boolean): string {
  if (isAutoIncrementPK && value === null) {
    return 'DEFAULT'
  }
  if (value === null) return 'NULL'
  if (isEmptyStringMarker(value)) return 'EMPTY STRING'
  return ''
}

type NullEmptySuggestionProps = {
  value: NullableValue
  onChange: (value: null | typeof EMPTY_STRING_MARKER) => void
  visible: boolean
  anchorRef: React.RefObject<HTMLElement | null>
}

export function NullEmptySuggestion({ value, onChange, visible, anchorRef }: NullEmptySuggestionProps) {
  const [position, setPosition] = React.useState({ top: 0, left: 0 })
  const isCurrentlyNull = value === null
  const isCurrentlyEmpty = isEmptyStringMarker(value)

  // Calculate position based on anchor element
  React.useEffect(() => {
    if (visible && anchorRef.current) {
      const rect = anchorRef.current.getBoundingClientRect()
      setPosition({
        top: rect.bottom + 4, // 4px gap below the input
        left: rect.left,
      })
    }
  }, [visible, anchorRef])

  if (!visible) return null

  const dropdown = (
    <div
      data-null-empty-suggestion
      className="fixed z-[9999] min-w-[140px] rounded-md border bg-popover p-1 shadow-md animate-in fade-in-0 zoom-in-95"
      style={{ top: position.top, left: position.left }}
      onMouseDown={(e) => e.preventDefault()} // Prevent blur on click
    >
      <button
        type="button"
        onClick={() => onChange(null)}
        className={`w-full flex items-center rounded-sm px-2 py-1.5 text-xs font-mono outline-none transition-colors hover:bg-accent hover:text-accent-foreground cursor-pointer ${isCurrentlyNull ? 'bg-accent/50' : ''}`}
      >
        NULL
      </button>
      <button
        type="button"
        onClick={() => onChange(EMPTY_STRING_MARKER)}
        className={`w-full flex items-center rounded-sm px-2 py-1.5 text-xs font-mono outline-none transition-colors hover:bg-accent hover:text-accent-foreground cursor-pointer ${isCurrentlyEmpty ? 'bg-accent/50' : ''}`}
      >
        EMPTY STRING
      </button>
    </div>
  )

  // Render in a portal to escape overflow:hidden containers
  return ReactDOM.createPortal(dropdown, document.body)
}
