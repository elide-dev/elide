import * as React from 'react'
import type { Table } from '@tanstack/react-table'
import { flexRender } from '@tanstack/react-table'

import { TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Skeleton } from '@/components/ui/skeleton'

type DataTableGridProps = {
  table: Table<Record<string, unknown>>
  isLoading: boolean
  showControls: boolean
  limit: number
  offset: number
}

export const DataTableGrid = React.memo(function DataTableGrid({
  table,
  isLoading,
  showControls,
  limit,
  offset,
}: DataTableGridProps) {
  return (
    <div className="overflow-auto flex-1 relative">
      <table
        className="w-full caption-bottom text-sm"
        style={{ width: showControls ? table.getCenterTotalSize() : '100%' }}
      >
        <TableHeader>
          {table.getHeaderGroups().map((headerGroup) => (
            <TableRow key={headerGroup.id}>
              {headerGroup.headers.map((header) => {
                return (
                  <TableHead
                    key={header.id}
                    className={`text-left border-b border-r border-gray-800 ${showControls ? 'p-0 hover:bg-gray-800' : 'px-4 py-2'} sticky top-0 z-10 bg-gray-900 overflow-hidden font-mono`}
                    style={{ width: header.getSize(), maxWidth: header.getSize() }}
                  >
                    {header.isPlaceholder ? null : flexRender(header.column.columnDef.header, header.getContext())}
                    {showControls && (
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
            table.getRowModel().rows?.map((row) => (
              <TableRow
                key={row.id}
                data-state={row.getIsSelected() && 'selected'}
                className="hover:bg-gray-900/30 transition-colors"
              >
                {row.getVisibleCells().map((cell) => {
                  return (
                    <TableCell
                      key={cell.id}
                      className="px-4 py-2 text-xs text-gray-200 border-r border-gray-800 overflow-hidden font-mono"
                      style={{ width: cell.column.getSize(), maxWidth: cell.column.getSize() }}
                    >
                      {isLoading ? (
                        <Skeleton className="h-4 w-full" />
                      ) : (
                        <div className="truncate">{flexRender(cell.column.columnDef.cell, cell.getContext())}</div>
                      )}
                    </TableCell>
                  )
                })}
              </TableRow>
            ))}
        </TableBody>
      </table>
      {table.getRowModel().rows?.length === 0 && (
        <div className=" flex items-center justify-center h-full">
          <div className="flex flex-col items-start gap-1">
            <div className="text-xs text-center text-gray-500 font-mono">
              <div className="font-semibold">No rows</div>
              <div>limit {limit}</div>
              <div>offset {offset}</div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
})
