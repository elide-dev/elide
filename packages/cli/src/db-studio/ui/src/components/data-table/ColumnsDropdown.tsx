import * as React from 'react'
import { Settings2, Search } from 'lucide-react'

import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuCheckboxItem,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { useDataTable } from '@/contexts/DataTableContext'

type ColumnsDropdownProps = {
  disabled?: boolean
}

export const ColumnsDropdown = function ColumnsDropdown({ disabled }: ColumnsDropdownProps) {
  const { table } = useDataTable()

  // Local search state
  const [search, setSearch] = React.useState('')

  const hasHiddenColumns = table.getAllColumns().some((col) => col.getCanHide() && !col.getIsVisible())

  return (
    <DropdownMenu onOpenChange={(open) => !open && setSearch('')}>
      <DropdownMenuTrigger asChild disabled={disabled}>
        <div className="relative">
          <Button variant="outline" disabled={disabled}>
            <Settings2 className="mr-2 h-4 w-4" />
            Columns
          </Button>
          {hasHiddenColumns && (
            <Badge
              variant="secondary"
              className="absolute -top-1 -right-1 h-4 w-4 p-0 flex items-center justify-center text-[10px] rounded-full bg-white text-black"
            >
              !
            </Badge>
          )}
        </div>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="center" className="w-[240px]" onCloseAutoFocus={(e) => e.preventDefault()}>
        <DropdownMenuLabel>Toggle columns</DropdownMenuLabel>
        <DropdownMenuSeparator />
        <div className="px-2 py-2" onKeyDown={(e) => e.stopPropagation()}>
          <div className="relative">
            <Search className="absolute left-2 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-muted-foreground" />
            <Input
              placeholder="Search..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="h-8 text-xs pl-8 w-full"
              onClick={(e) => e.stopPropagation()}
              onKeyDown={(e) => e.stopPropagation()}
              autoFocus
            />
          </div>
        </div>

        <DropdownMenuSeparator />
        <div className="max-h-[300px] overflow-y-auto">
          <DropdownMenuItem
            onClick={(e) => {
              e.preventDefault()
              const hasHiddenColumns = table.getAllColumns().some((col) => !col.getIsVisible() && col.getCanHide())
              table.getAllColumns().forEach((col) => {
                if (col.getCanHide()) {
                  col.toggleVisibility(hasHiddenColumns)
                }
              })
            }}
          >
            {table.getAllColumns().some((col) => !col.getIsVisible() && col.getCanHide())
              ? 'Select all'
              : 'Deselect all'}
          </DropdownMenuItem>
          {table
            .getAllColumns()
            .filter((column) => column.getCanHide())
            .filter((column) => column.id.toLowerCase().includes(search.toLowerCase()))
            .map((column) => {
              return (
                <DropdownMenuCheckboxItem
                  key={column.id}
                  checked={column.getIsVisible()}
                  onCheckedChange={(value) => column.toggleVisibility(!!value)}
                  onSelect={(e) => e.preventDefault()}
                >
                  {column.id}
                </DropdownMenuCheckboxItem>
              )
            })}
          {table
            .getAllColumns()
            .filter((column) => column.getCanHide())
            .filter((column) => column.id.toLowerCase().includes(search.toLowerCase())).length === 0 && (
            <div className="px-2 py-2 text-xs text-muted-foreground text-center">No columns found</div>
          )}
        </div>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
