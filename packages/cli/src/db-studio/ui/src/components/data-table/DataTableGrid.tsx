import * as React from 'react'
import { flexRender, type Row } from '@tanstack/react-table'

import { TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Skeleton } from '@/components/ui/skeleton'
import { useDataTable } from '@/contexts/DataTableContext'

type SelectedCell = { rowId: string; columnId: string } | null

type MemoizedRowProps = {
  row: Row<Record<string, unknown>>
  isLoading: boolean
  isResizing: boolean
  selectedCell: SelectedCell
  onCellClick: (rowId: string, columnId: string) => void
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

  // Rerender if selected cell in this row changed
  const prevHasSelection = prev.selectedCell?.rowId === prev.row.id
  const nextHasSelection = next.selectedCell?.rowId === next.row.id
  if (prevHasSelection !== nextHasSelection) return false
  if (prevHasSelection && nextHasSelection && prev.selectedCell?.columnId !== next.selectedCell?.columnId) return false

  // Skip rerender only if we're currently resizing
  return next.isResizing
}

/**
 * Memoized table row that prevents rerenders during column resizing
 * Maintains smooth performance with large datasets by blocking rerenders during resize operations
 */
const MemoizedRow = React.memo(({ row, isLoading, selectedCell, onCellClick }: MemoizedRowProps) => {
  const isSelected = row.getIsSelected()

  return (
    <TableRow
      key={row.id}
      data-state={isSelected && 'selected'}
      className="hover:bg-accent/70 transition-colors duration-75"
    >
      {row.getVisibleCells().map((cell) => {
        const { id, column, getContext } = cell
        const width = column.getSize()
        const isCheckboxColumn = column.id === 'select'
        const isCellSelected =
          !isCheckboxColumn && selectedCell?.rowId === row.id && selectedCell?.columnId === column.id

        return (
          <TableCell
            key={id}
            onClick={isCheckboxColumn ? undefined : () => onCellClick(row.id, column.id)}
            className={`text-xs text-foreground border-r border-border overflow-hidden truncate px-4 py-2 font-mono transition-colors ${
              isCheckboxColumn ? '' : 'cursor-pointer'
            } ${isCellSelected ? 'bg-blue-500/20 ring-2 ring-blue-500 ring-inset' : ''}`}
            style={{ width, maxWidth: width }}
          >
            {isLoading ? <Skeleton className="h-4 w-full" /> : flexRender(column.columnDef.cell, getContext())}
          </TableCell>
        )
      })}
    </TableRow>
  )
}, shouldSkipRowRerender)

MemoizedRow.displayName = 'MemoizedRow'

export function DataTableGrid() {
  const { table, config, pagination } = useDataTable()
  const [selectedCell, setSelectedCell] = React.useState<SelectedCell>(null)

  // Check if any column is currently being resized
  const isResizing = table.getState().columnSizingInfo.isResizingColumn !== false

  const handleCellClick = React.useCallback((rowId: string, columnId: string) => {
    setSelectedCell((prev) => {
      // Toggle off if clicking the same cell
      if (prev?.rowId === rowId && prev?.columnId === columnId) {
        return null
      }
      return { rowId, columnId }
    })
  }, [])

  return (
    <div className="overflow-auto flex-1 relative">
      <table
        className="w-full caption-bottom text-sm"
        style={{
          width: table.getCenterTotalSize(),
          tableLayout: 'fixed',
        }}
      >
        <TableHeader>
          {table.getHeaderGroups().map((headerGroup) => (
            <TableRow key={headerGroup.id}>
              {headerGroup.headers.map((header) => {
                const isCheckboxColumn = header.column.id === 'select'
                const isResizable = header.column.getCanResize()
                return (
                  <TableHead
                    key={header.id}
                    className={`text-left border-b border-r border-border ${isCheckboxColumn ? 'p-0 relative' : 'p-0 hover:bg-accent relative'} sticky top-0 z-10 bg-card overflow-hidden ${isCheckboxColumn ? '' : 'font-mono'}`}
                    style={{ width: header.getSize(), maxWidth: header.getSize() }}
                  >
                    {header.isPlaceholder ? null : flexRender(header.column.columnDef.header, header.getContext())}
                    {isResizable && !isCheckboxColumn && (
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
                <MemoizedRow
                  key={row.id}
                  row={row}
                  isLoading={config.isLoading}
                  isResizing={isResizing}
                  selectedCell={selectedCell}
                  onCellClick={handleCellClick}
                />
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
