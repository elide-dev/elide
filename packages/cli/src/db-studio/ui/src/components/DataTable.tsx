import * as React from "react"
import type {
  ColumnDef,
  ColumnFiltersState,
  ColumnSizingState,
  SortingState,
  VisibilityState,
} from "@tanstack/react-table"
import {
  flexRender,
  getCoreRowModel,
  getFilteredRowModel,
  getPaginationRowModel,
  getSortedRowModel,
  useReactTable,
} from "@tanstack/react-table"
import { ArrowUpDown, ArrowUpWideNarrow, ArrowDownWideNarrow, ChevronDown, Settings2 } from "lucide-react"

import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuCheckboxItem,
  DropdownMenuContent,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Input } from "@/components/ui/input"
import {
  Table as UiTable,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"

interface DataTableProps {
  columns: string[]
  rows: unknown[][]
  primaryKeys?: string[]
  showControls?: boolean // Show search, column toggle, and pagination (default: true)
}

/**
 * Reusable data table component for displaying database query results
 * Uses TanStack Table for sorting, filtering, pagination, and column visibility
 * Handles NULL values, empty strings, and other data types appropriately
 */
export function DataTable({ columns, rows, primaryKeys, showControls = true }: DataTableProps) {
  const [sorting, setSorting] = React.useState<SortingState>([])
  const [columnFilters, setColumnFilters] = React.useState<ColumnFiltersState>([])
  const [columnVisibility, setColumnVisibility] = React.useState<VisibilityState>({})
  const [columnSizing, setColumnSizing] = React.useState<ColumnSizingState>({})

  const formatCellValue = (value: unknown): React.ReactNode => {
    return value === null || value === undefined ? (
      <span className="text-gray-500 font-normal">NULL</span>
    ) : (
      String(value)
    )
  }

  // Convert string[] columns and unknown[][] rows to TanStack Table format
  const tableColumns: ColumnDef<Record<string, unknown>>[] = React.useMemo(
    () =>
      columns.map((col) => {
        const isKey = primaryKeys?.includes(col) || /(^id$|_id$)/i.test(col)

        return {
          accessorKey: col,
          size: 200, // Default column width
          minSize: 100, // Minimum column width
          maxSize: 1000, // Maximum column width
          enableResizing: true,
          header: ({ column }) => {
            const sorted = column.getIsSorted()
            const SortIcon = sorted === "asc" ? ArrowUpWideNarrow : sorted === "desc" ? ArrowDownWideNarrow : ArrowUpDown
            
            const handleSort = () => {
              // Three-state sorting: no sort → asc → desc → no sort
              if (!sorted) {
                  column.toggleSorting(false) // asc
            } else if (sorted === "asc") {
                column.toggleSorting(true) // desc
              } else {
                column.clearSorting() // clear sorting
              }
            }
            
            return showControls ? (
              <div
                className="flex items-center gap-1.5 cursor-pointer select-none hover:text-gray-200 transition-colors w-full h-full px-4 py-2"
                onClick={handleSort}
              >
                {isKey && (
                  <svg
                    className="w-3.5 h-3.5 text-amber-300 shrink-0"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1721 9z"
                    />
                  </svg>
                )}
                <span className="text-xs font-semibold text-gray-400 tracking-wider flex-1 truncate">
                  {col}
                </span>
                <SortIcon className="h-3.5 w-3.5 text-gray-400 shrink-0" />
              </div>
            ) : (
              <div className="flex items-center gap-1.5 px-4 py-2">
                {isKey && (
                  <svg
                    className="w-3.5 h-3.5 text-amber-300 shrink-0"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1721 9z"
                    />
                  </svg>
                )}
                <span className="text-xs font-semibold text-gray-400 tracking-wider truncate">
                  {col}
                </span>
              </div>
            )
          },
          cell: ({ getValue }) => {
            return formatCellValue(getValue())
          },
        }
      }),
    [columns, primaryKeys, showControls]
  )

  // Convert rows array to object format for TanStack Table
  const tableData: Record<string, unknown>[] = React.useMemo(
    () =>
      rows.map((row) =>
        columns.reduce((acc, col, idx) => {
          acc[col] = row[idx]
          return acc
        }, {} as Record<string, unknown>)
      ),
    [rows, columns]
  )

  const table = useReactTable({
    data: tableData,
    columns: tableColumns,
    onSortingChange: setSorting,
    onColumnFiltersChange: setColumnFilters,
    getCoreRowModel: getCoreRowModel(),
    getPaginationRowModel: getPaginationRowModel(),
    getSortedRowModel: getSortedRowModel(),
    getFilteredRowModel: getFilteredRowModel(),
    onColumnVisibilityChange: setColumnVisibility,
    onColumnSizingChange: setColumnSizing,
    enableColumnResizing: true,
    columnResizeMode: "onChange",
    state: {
      sorting,
      columnFilters,
      columnVisibility,
      columnSizing,
    },
  })

  return (
    <div className="w-full">
      {/* Toolbar with search and column visibility */}
      {showControls && (
        <div className="flex items-center gap-2 px-6 py-4 border-b border-gray-800">
          <Input
            placeholder="Filter all columns..."
            value={(columnFilters[0]?.value as string) ?? ""}
            onChange={(event) => {
              const value = event.target.value
              // Simple global filter on first column, can be enhanced
              if (columns.length > 0) {
                table.getColumn(columns[0])?.setFilterValue(value)
              }
            }}
            className="max-w-sm"
          />
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="outline" className="ml-auto">
                <Settings2 className="mr-2 h-4 w-4" />
                Columns <ChevronDown className="ml-2 h-4 w-4" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-[200px]">
              {table
                .getAllColumns()
                .filter((column) => column.getCanHide())
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
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      )}

      {/* Table */}
      <div className="overflow-auto">
        <UiTable style={{ width: showControls ? table.getCenterTotalSize() : '100%' }}>
          <TableHeader>
            {table.getHeaderGroups().map((headerGroup) => (
              <TableRow key={headerGroup.id} className="bg-gray-900/50">
                {headerGroup.headers.map((header) => {
                  return (
                    <TableHead
                      key={header.id}
                      className={`text-left border-b border-r border-gray-800 ${showControls ? 'p-0 hover:bg-gray-800/50' : 'px-4 py-2'} relative overflow-hidden`}
                      style={{ width: header.getSize(), maxWidth: header.getSize() }}
                    >
                      {header.isPlaceholder
                        ? null
                        : flexRender(header.column.columnDef.header, header.getContext())}
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
            {table.getRowModel().rows?.length ? (
              table.getRowModel().rows.map((row) => (
                <TableRow
                  key={row.id}
                  data-state={row.getIsSelected() && "selected"}
                  className="hover:bg-gray-900/30 transition-colors"
                >
                  {row.getVisibleCells().map((cell) => {
                    return (
                      <TableCell
                        key={cell.id}
                        className="px-4 py-2 text-xs text-gray-200 border-r border-gray-800 overflow-hidden"
                        style={{ width: cell.column.getSize(), maxWidth: cell.column.getSize() }}
                      >
                        <div className="truncate">
                          {flexRender(cell.column.columnDef.cell, cell.getContext())}
                        </div>
                      </TableCell>
                    )
                  })}
                </TableRow>
              ))
            ) : (
              <TableRow>
                <TableCell colSpan={columns.length} className="h-24 text-center">
                  No results.
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </UiTable>
      </div>

      {/* Pagination Controls */}
      {showControls && (
        <div className="flex items-center justify-end space-x-2 px-6 py-4 border-t border-gray-800">
          <div className="flex-1 text-sm text-muted-foreground">
            Showing {table.getState().pagination.pageIndex * table.getState().pagination.pageSize + 1} to{" "}
            {Math.min(
              (table.getState().pagination.pageIndex + 1) * table.getState().pagination.pageSize,
              table.getFilteredRowModel().rows.length
            )}{" "}
            of {table.getFilteredRowModel().rows.length} row(s)
          </div>
          <div className="space-x-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => table.previousPage()}
              disabled={!table.getCanPreviousPage()}
            >
              Previous
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={() => table.nextPage()}
              disabled={!table.getCanNextPage()}
            >
              Next
            </Button>
          </div>
        </div>
      )}
    </div>
  )
}
