import * as React from 'react'
import type { VisibilityState } from '@tanstack/react-table'

type UseColumnVisibilityReturn = {
  columnSearch: string
  setColumnSearch: (value: string) => void
  columnVisibility: VisibilityState
  setColumnVisibility: (state: VisibilityState | ((old: VisibilityState) => VisibilityState)) => void
}

export function useColumnVisibility(): UseColumnVisibilityReturn {
  const [columnVisibility, setColumnVisibility] = React.useState<VisibilityState>({})
  const [columnSearch, setColumnSearch] = React.useState('')

  return {
    columnSearch,
    setColumnSearch,
    columnVisibility,
    setColumnVisibility,
  }
}
