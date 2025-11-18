import * as React from 'react'

type UsePaginationInputsReturn = {
  limitInput: string
  offsetInput: string
  setLimitInput: (value: string) => void
  setOffsetInput: (value: string) => void
}

export function usePaginationInputs(limit: number, offset: number): UsePaginationInputsReturn {
  // Local state for input values (only update on submit)
  const [limitInput, setLimitInput] = React.useState<string>(String(limit))
  const [offsetInput, setOffsetInput] = React.useState<string>(String(offset))

  // Sync local state when props change externally
  React.useEffect(() => {
    setLimitInput(String(limit))
  }, [limit])

  React.useEffect(() => {
    setOffsetInput(String(offset))
  }, [offset])

  return {
    limitInput,
    offsetInput,
    setLimitInput,
    setOffsetInput,
  }
}
