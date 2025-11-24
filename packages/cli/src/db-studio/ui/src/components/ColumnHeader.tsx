import * as React from 'react'
import { Key, Link, ArrowUpDown, ArrowUpWideNarrow, ArrowDownWideNarrow } from 'lucide-react'
import { HoverCard, HoverCardContent, HoverCardTrigger } from '@/components/ui/hover-card'
import type { ColumnMetadata } from '@/lib/types'
import { useDataTable } from '@/contexts/DataTableContext'

type ColumnInfoProps = {
  column: ColumnMetadata
  children: React.ReactNode
}

/**
 * Hover card component that displays detailed column metadata
 * Shows type, nullable status, constraints, foreign keys, etc.
 */
function ColumnInfo({ column, children }: ColumnInfoProps) {
  return (
    <HoverCard openDelay={300}>
      <HoverCardTrigger asChild>{children}</HoverCardTrigger>
      <HoverCardContent className="w-80 bg-card border-border" side="bottom" align="start">
        <div className="space-y-3">
          <div className="flex items-center gap-2">
            <h4 className="text-sm font-semibold text-foreground font-mono">{column.name}</h4>
          </div>

          {column.primaryKey && (
            <div className="flex items-center justify-between py-1 border-b border-border text-xs">
              <div className="flex items-center gap-1.5">
                <Key className="w-3.5 h-3.5 text-amber-500 shrink-0" />
                <span className="text-muted-foreground">Primary Key</span>
              </div>
              <span className="text-foreground">Yes</span>
            </div>
          )}

          {column.foreignKey && (
            <div className="py-1 space-y-1 text-xs">
              <div className="flex items-center gap-1.5 text-muted-foreground mb-1">
                <Link className="w-3.5 h-3.5 text-blue-500 shrink-0" />
                <span>Foreign Key</span>
              </div>
              <div className="pl-2 space-y-1 border-l-2 border-border">
                <div className="flex items-center justify-between">
                  <span className="text-muted-foreground">References</span>
                  <span className="font-mono text-foreground">
                    {column.foreignKey.table}.{column.foreignKey.column}
                  </span>
                </div>
                {column.foreignKey.onUpdate && (
                  <div className="flex items-center justify-between">
                    <span className="text-muted-foreground">On Update</span>
                    <span className="font-mono text-foreground">{column.foreignKey.onUpdate}</span>
                  </div>
                )}
                {column.foreignKey.onDelete && (
                  <div className="flex items-center justify-between">
                    <span className="text-muted-foreground">On Delete</span>
                    <span className="font-mono text-foreground">{column.foreignKey.onDelete}</span>
                  </div>
                )}
              </div>
            </div>
          )}

          <div className="space-y-2 text-xs">
            <div className="flex items-center justify-between py-1 border-b border-border">
              <span className="text-muted-foreground">Type</span>
              <span className="font-mono text-foreground">{column.type}</span>
            </div>

            <div className="flex items-center justify-between py-1 border-b border-border">
              <span className="text-muted-foreground">Nullable</span>
              <span className="text-foreground">{column.nullable ? 'Yes' : 'No'}</span>
            </div>

            {column.unique && (
              <div className="flex items-center justify-between py-1 border-b border-border">
                <span className="text-muted-foreground">Unique</span>
                <span className="text-foreground">Yes</span>
              </div>
            )}

            {column.autoIncrement && (
              <div className="flex items-center justify-between py-1 border-b border-border">
                <span className="text-muted-foreground">Auto Increment</span>
                <span className="text-foreground">Yes</span>
              </div>
            )}

            {column.defaultValue !== undefined && column.defaultValue !== null && (
              <div className="flex items-center justify-between py-1 border-b border-border">
                <span className="text-muted-foreground">Default</span>
                <span className="font-mono text-foreground">{String(column.defaultValue)}</span>
              </div>
            )}
          </div>
        </div>
      </HoverCardContent>
    </HoverCard>
  )
}

type SortIconProps = {
  sorted: false | 'asc' | 'desc'
  className: string
}

function SortIcon({ sorted, className }: SortIconProps) {
  switch (sorted) {
    case 'asc':
      return <ArrowUpWideNarrow className={className} />
    case 'desc':
      return <ArrowDownWideNarrow className={className} />
    default:
      return <ArrowUpDown className={className} />
  }
}

type ColumnHeaderProps = {
  column: ColumnMetadata
  sorted: false | 'asc' | 'desc'
  showControls?: boolean
}

/**
 * Column header component that displays column name, type, and optional sorting controls
 * Wraps content in a ColumnInfo hover card for displaying detailed metadata
 */
export function ColumnHeader({ column, sorted, showControls = true }: ColumnHeaderProps) {
  const { onSortingChange } = useDataTable()

  const isKey = column.primaryKey
  const isForeignKey = !!column.foreignKey
  const isSortable = showControls

  const handleSort = React.useCallback(() => {
    // Three-state sorting: no sort → asc → desc → no sort
    if (!sorted) {
      onSortingChange({ column: column.name, direction: 'asc' })
    } else if (sorted === 'asc') {
      onSortingChange({ column: column.name, direction: 'desc' })
    } else {
      onSortingChange({ column: null, direction: null })
    }
  }, [sorted, onSortingChange, column.name])

  return (
    <ColumnInfo column={column}>
      <div
        className={`flex items-center gap-1.5 px-4 py-2 w-full h-full ${
          isSortable ? 'cursor-pointer select-none hover:text-foreground transition-colors' : ''
        }`}
        onClick={isSortable ? handleSort : undefined}
      >
        {isKey && <Key className="w-3.5 h-3.5 text-amber-500 shrink-0" />}
        {isForeignKey && <Link className="w-3.5 h-3.5 text-blue-500 shrink-0" />}
        <div className="flex flex-col gap-0.5 flex-1 min-w-0">
          <span className="text-xs font-semibold text-foreground tracking-wider truncate">{column.name}</span>
          <span className="text-[10px] text-muted-foreground font-normal truncate">{column.type}</span>
        </div>
        {isSortable && <SortIcon sorted={sorted} className="h-3.5 w-3.5 text-muted-foreground shrink-0" />}
      </div>
    </ColumnInfo>
  )
}
