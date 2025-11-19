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
      <HoverCardContent className="w-80 bg-gray-900 border-gray-700" side="bottom" align="start">
        <div className="space-y-3">
          <div className="flex items-center gap-2">
            <h4 className="text-sm font-semibold text-gray-100 font-mono">{column.name}</h4>
          </div>

          {column.primaryKey && (
            <div className="flex items-center justify-between py-1 border-b border-gray-800">
              <div className="flex items-center gap-1.5">
                <Key className="w-3.5 h-3.5 text-yellow-400 shrink-0" />
                <span className="text-gray-500">Primary Key</span>
              </div>
              <span className="text-gray-200">Yes</span>
            </div>
          )}

          {column.foreignKey && (
            <div className="py-1 space-y-1">
              <div className="flex items-center gap-1.5 text-gray-500 mb-1">
                <Link className="w-3.5 h-3.5 text-blue-400 shrink-0" />
                <span>Foreign Key</span>
              </div>
              <div className="pl-2 space-y-1 border-l-2 border-gray-700">
                <div className="flex items-center justify-between">
                  <span className="text-gray-500">References</span>
                  <span className="font-mono text-gray-200">
                    {column.foreignKey.table}.{column.foreignKey.column}
                  </span>
                </div>
                {column.foreignKey.onUpdate && (
                  <div className="flex items-center justify-between">
                    <span className="text-gray-500">On Update</span>
                    <span className="font-mono text-gray-300">{column.foreignKey.onUpdate}</span>
                  </div>
                )}
                {column.foreignKey.onDelete && (
                  <div className="flex items-center justify-between">
                    <span className="text-gray-500">On Delete</span>
                    <span className="font-mono text-gray-300">{column.foreignKey.onDelete}</span>
                  </div>
                )}
              </div>
            </div>
          )}

          <div className="space-y-2 text-xs">
            <div className="flex items-center justify-between py-1 border-b border-gray-800">
              <span className="text-gray-500">Type</span>
              <span className="font-mono text-gray-200">{column.type}</span>
            </div>

            <div className="flex items-center justify-between py-1 border-b border-gray-800">
              <span className="text-gray-500">Nullable</span>
              <span className="text-gray-200">{column.nullable ? 'Yes' : 'No'}</span>
            </div>

            {column.unique && (
              <div className="flex items-center justify-between py-1 border-b border-gray-800">
                <span className="text-gray-500">Unique</span>
                <span className="text-gray-200">Yes</span>
              </div>
            )}

            {column.autoIncrement && (
              <div className="flex items-center justify-between py-1 border-b border-gray-800">
                <span className="text-gray-500">Auto Increment</span>
                <span className="text-gray-200">Yes</span>
              </div>
            )}

            {column.defaultValue !== undefined && column.defaultValue !== null && (
              <div className="flex items-center justify-between py-1 border-b border-gray-800">
                <span className="text-gray-500">Default</span>
                <span className="font-mono text-gray-200">{String(column.defaultValue)}</span>
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
          isSortable ? 'cursor-pointer select-none hover:text-gray-200 transition-colors' : ''
        }`}
        onClick={isSortable ? handleSort : undefined}
      >
        {isKey && <Key className="w-3.5 h-3.5 text-yellow-400 shrink-0" />}
        {isForeignKey && <Link className="w-3.5 h-3.5 text-blue-400 shrink-0" />}
        <div className="flex flex-col gap-0.5 flex-1 min-w-0">
          <span className="text-xs font-semibold text-gray-200 tracking-wider truncate">{column.name}</span>
          <span className="text-[10px] text-gray-500 font-normal truncate">{column.type}</span>
        </div>
        {isSortable && <SortIcon sorted={sorted} className="h-3.5 w-3.5 text-gray-400 shrink-0" />}
      </div>
    </ColumnInfo>
  )
}
