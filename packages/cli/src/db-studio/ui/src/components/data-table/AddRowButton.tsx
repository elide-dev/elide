import * as React from 'react'
import { PlusCircle } from 'lucide-react'
import { Button } from '@/components/ui/button'

type AddRowButtonProps = {
  onClick: () => void
  disabled?: boolean
}

export const AddRowButton = React.memo(function AddRowButton({ onClick, disabled }: AddRowButtonProps) {
  return (
    <Button variant="default" size="sm" onClick={onClick} disabled={disabled}>
      <PlusCircle className="h-4 w-4" />
      Add Row
    </Button>
  )
})
