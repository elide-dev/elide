import * as React from 'react'
import { ChevronDown, X } from 'lucide-react'

import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuCheckboxItem,
  DropdownMenuContent,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import type { Filter } from '@/lib/types'
import { FILTER_OPERATORS } from '@/lib/types'
import type { ColumnMetadata } from './DataTable'

type FilterRowProps = {
  filter: Filter
  columns: ColumnMetadata[]
  isFirst: boolean
  onUpdate: (updates: Partial<Filter>) => void
  onRemove: () => void
}

export const FilterRow = React.memo(function FilterRow({
  filter,
  columns,
  isFirst,
  onUpdate,
  onRemove,
}: FilterRowProps) {
  const operatorMeta = FILTER_OPERATORS.find((op) => op.value === filter.operator)
  const requiresValue = operatorMeta?.requiresValue ?? true
  const isArrayValue = operatorMeta?.isArrayValue ?? false

  return (
    <div className="flex items-center gap-2">
      {/* First filter shows "where", others show "and" */}
      <span className="text-xs text-gray-400 font-mono w-12 uppercase shrink-0">{isFirst ? 'where' : 'and'}</span>

      {/* Column selector */}
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button variant="outline" size="sm" className="min-w-[140px] w-auto max-w-[280px] justify-between font-mono text-xs">
            <span className="flex-1 text-left">{filter.column}</span>
            <ChevronDown className="ml-2 h-3 w-3 shrink-0" />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent className="max-h-[300px] overflow-y-auto">
          {columns.map((col) => (
            <DropdownMenuCheckboxItem
              key={col.name}
              checked={filter.column === col.name}
              onCheckedChange={() => onUpdate({ column: col.name })}
              onSelect={(e) => e.preventDefault()}
            >
              <span className="font-mono text-xs pr-8">{col.name}</span>
            </DropdownMenuCheckboxItem>
          ))}
        </DropdownMenuContent>
      </DropdownMenu>

      {/* Operator selector */}
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button variant="outline" size="sm" className="w-[160px] justify-between text-xs">
            <span className="truncate">{operatorMeta?.label || filter.operator}</span>
            <ChevronDown className="ml-2 h-3 w-3 shrink-0" />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent className="w-[200px]">
          {FILTER_OPERATORS.map((op) => (
            <DropdownMenuCheckboxItem
              key={op.value}
              checked={filter.operator === op.value}
              onCheckedChange={() => onUpdate({ operator: op.value })}
              onSelect={(e) => e.preventDefault()}
              className="pr-2"
            >
              <div className="flex items-center justify-between gap-2 w-full">
                <span className="text-xs flex-1">{op.label}</span>
                <Badge variant="secondary" className="ml-auto font-mono text-[10px] px-1.5 py-0">
                  {op.symbol}
                </Badge>
              </div>
            </DropdownMenuCheckboxItem>
          ))}
        </DropdownMenuContent>
      </DropdownMenu>

      {/* Value input (conditional based on operator) */}
      {requiresValue && !isArrayValue && (
        <Input
          placeholder="Value..."
          value={String(filter.value ?? '')}
          onChange={(e) => onUpdate({ value: e.target.value })}
          className="h-8 w-[200px] text-xs font-mono"
        />
      )}

      {/* Array value input for 'in' operator */}
      {requiresValue && isArrayValue && (
        <Input
          placeholder="Value1, Value2, Value3..."
          value={Array.isArray(filter.value) ? filter.value.join(', ') : ''}
          onChange={(e) => {
            const values = e.target.value
              .split(',')
              .map((v) => v.trim())
              .filter((v) => v !== '')
            onUpdate({ value: values })
          }}
          className="h-8 w-[280px] text-xs font-mono"
        />
      )}

      {/* Remove button */}
      <Button
        variant="ghost"
        size="sm"
        onClick={onRemove}
        className="h-8 w-8 p-0 shrink-0 text-gray-400 hover:text-gray-200"
      >
        <X className="h-4 w-4" />
      </Button>
    </div>
  )
})
