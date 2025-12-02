import * as React from 'react'
import { Filter as FilterIcon } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'

type FilterButtonProps = {
  showFilters: boolean
  activeFilterCount: number
  onToggle: () => void
}

export const FilterButton = React.memo(function FilterButton({
  showFilters,
  activeFilterCount,
  onToggle,
}: FilterButtonProps) {
  return (
    <div className="relative">
      <Button variant="outline" className={showFilters ? 'bg-accent text-accent-foreground' : ''} onClick={onToggle}>
        <FilterIcon className="mr-2 h-4 w-4" />
        Filters
      </Button>
      {activeFilterCount > 0 && (
        <Badge
          variant="secondary"
          className="absolute -top-1 -right-1 h-4 w-4 p-0 flex items-center justify-center text-[10px] rounded-full bg-white text-black"
        >
          !
        </Badge>
      )}
    </div>
  )
})
