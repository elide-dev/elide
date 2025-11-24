import * as React from 'react'
import { flexRender, type Row } from '@tanstack/react-table'

import { TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Skeleton } from '@/components/ui/skeleton'
import { useDataTable } from '@/contexts/DataTableContext'

type MemoizedRowProps = {
  row: Row<Record<string, unknown>>
  isLoading: boolean
  isResizing: boolean
}

/**
 * Checks if a row should skip rerendering
 * Returns true to skip rerender, false to allow rerender
 */
function shouldSkipRowRerender(prev: MemoizedRowProps, next: MemoizedRowProps): boolean {
  // Rerender if loading state changed
  if (prev.isLoading !== next.isLoading) return false

  // Rerender if row identity changed
  if (prev.row.id !== next.row.id) return false

  // Rerender if visible columns changed (column visibility toggled)
  if (prev.row.getVisibleCells().length !== next.row.getVisibleCells().length) return false

  // Skip rerender only if we're currently resizing
  return next.isResizing
}

/**
 * Memoized table row that prevents rerenders during column resizing
 * Maintains smooth performance with large datasets by blocking rerenders during resize operations
 */
const MemoizedRow = React.memo(({ row, isLoading }: MemoizedRowProps) => {
  const isSelected = row.getIsSelected()

  return (
    <TableRow key={row.id} data-state={isSelected && 'selected'} className="hover:bg-accent/30 transition-colors">
      {row.getVisibleCells().map((cell) => {
        const { id, column, getContext } = cell
        const width = column.getSize()

        return (
          <TableCell
            key={id}
            className="px-4 py-2 text-xs text-foreground border-r border-border overflow-hidden font-mono"
            style={{ width, maxWidth: width }}
          >
            {isLoading ? (
              <Skeleton className="h-4 w-full" />
            ) : (
              <div className="truncate">{flexRender(column.columnDef.cell, getContext())}</div>
            )}
          </TableCell>
        )
      })}
    </TableRow>
  )
}, shouldSkipRowRerender)

MemoizedRow.displayName = 'MemoizedRow'

export function DataTableGrid() {
  const { table, config, pagination } = useDataTable()

  // Check if any column is currently being resized
  const isResizing = table.getState().columnSizingInfo.isResizingColumn !== false

  return (
    <div className="overflow-auto flex-1 relative">
      <table
        className="w-full caption-bottom text-sm"
        style={{
          width: config.showControls ? table.getCenterTotalSize() : '100%',
          tableLayout: config.showControls ? 'fixed' : 'auto',
        }}
      >
        <TableHeader>
          {table.getHeaderGroups().map((headerGroup) => (
            <TableRow key={headerGroup.id}>
              {headerGroup.headers.map((header) => {
                return (
                  <TableHead
                    key={header.id}
                    className={`text-left border-b border-r border-border ${config.showControls ? 'p-0 hover:bg-accent relative' : 'px-4 py-2'} sticky top-0 z-10 bg-card overflow-hidden font-mono`}
                    style={{ width: header.getSize(), maxWidth: header.getSize() }}
                  >
                    {header.isPlaceholder ? null : flexRender(header.column.columnDef.header, header.getContext())}
                    {config.showControls && (
                      <div
                        onMouseDown={header.getResizeHandler()}
                        onTouchStart={header.getResizeHandler()}
                        className={`absolute right-0 top-0 h-full w-1 cursor-col-resize select-none touch-none hover:bg-blue-500 ${
                          header.column.getIsResizing() ? 'bg-blue-500' : ''
                        }`}
                      />
                    )}
                  </TableHead>
                )
              })}
            </TableRow>
          ))}
        </TableHeader>
        <TableBody>
          {table.getRowModel().rows?.length > 0 &&
            table
              .getRowModel()
              .rows?.map((row) => (
                <MemoizedRow key={row.id} row={row} isLoading={config.isLoading} isResizing={isResizing} />
              ))}
        </TableBody>
      </table>
      {table.getRowModel().rows?.length === 0 && (
        <div className=" flex items-center justify-center h-full">
          <div className="flex flex-col items-start gap-1">
            <div className="text-xs text-center text-muted-foreground font-mono">
              <div className="font-semibold">No rows</div>
              <div>limit {pagination.limit}</div>
              <div>offset {pagination.offset}</div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
