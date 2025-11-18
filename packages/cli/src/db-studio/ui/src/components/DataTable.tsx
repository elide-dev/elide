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
import { ArrowUpDown, ArrowUpWideNarrow, ArrowDownWideNarrow, ChevronDown, Settings2, Search, Key, Link } from "lucide-react"

import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuCheckboxItem,
  DropdownMenuContent,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Input } from "@/components/ui/input"
import {
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { Skeleton } from "@/components/ui/skeleton"
import { ColumnInfo } from "./ColumnInfo"

export type ColumnMetadata = {
  name: string
  type: string
  nullable: boolean
  primaryKey: boolean
  defaultValue?: string | number | null
  foreignKey?: {
    table: string
    column: string
    onUpdate?: string
    onDelete?: string
  }
  unique?: boolean
  autoIncrement?: boolean
}

export type QueryMetadata = {
  executionTimeMs: number
  sql: string
  rowCount: number
}

export type DataTableData = {
  columns: ColumnMetadata[]
  rows: unknown[][]
  metadata?: QueryMetadata
}

export type DataTablePagination = {
  limit: number
  offset: number
}

interface DataTableProps {
  data: DataTableData
  showControls?: boolean // Show search, column toggle, and pagination (default: true)
  // Server-side pagination props (optional)
  totalRows?: number
  pagination?: DataTablePagination
  onPaginationChange?: (limit: number, offset: number) => void
  isLoading?: boolean // Show skeleton loaders when fetching new data
}

/**
 * Reusable data table component for displaying database query results
 * Uses TanStack Table for sorting, filtering, and column visibility
 * Supports server-side pagination via totalRows, limit, offset props and onPaginationChange callback
 * Pagination is controlled - limit/offset come from props (URL query params)
 * Handles NULL values, empty strings, and other data types appropriately
 */
export function DataTable({ 
  data, 
  showControls = true, 
  totalRows: totalRowsProp,
  pagination: paginationProp,
  onPaginationChange,
  isLoading = false
}: DataTableProps) {
  const [sorting, setSorting] = React.useState<SortingState>([])
  const [columnFilters, setColumnFilters] = React.useState<ColumnFiltersState>([])
  const [columnVisibility, setColumnVisibility] = React.useState<VisibilityState>({})
  const [columnSizing, setColumnSizing] = React.useState<ColumnSizingState>({})
  const [columnSearch, setColumnSearch] = React.useState("")

  const { columns, rows, metadata } = data
  
  // Server-side pagination detection and values
  const hasServerPagination = !!onPaginationChange && !!totalRowsProp && !!paginationProp
  const totalRows = totalRowsProp ?? rows.length
  const limit = paginationProp?.limit ?? 100
  const offset = paginationProp?.offset ?? 0

  const formatCellValue = (value: unknown): React.ReactNode => {
    return value === null || value === undefined ? (
      <span className="text-gray-500 font-normal">NULL</span>
    ) : (
      String(value)
    )
  }

  // Columns are already ColumnMetadata format
  const normalizedColumns: ColumnMetadata[] = React.useMemo(() => {
    return columns
  }, [columns])

  // Convert ColumnMetadata[] and unknown[][] rows to TanStack Table format
  const tableColumns: ColumnDef<Record<string, unknown>>[] = React.useMemo(
    () =>
      normalizedColumns.map((col) => {
        const isKey = col.primaryKey

        return {
          accessorKey: col.name,
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
            
            const headerContent = showControls ? (
              <div
                className="flex items-center gap-1.5 cursor-pointer select-none hover:text-gray-200 transition-colors w-full h-full px-4 py-2"
                onClick={handleSort}
              >
                {isKey && (
                  <Key className="w-3.5 h-3.5 text-yellow-400 shrink-0" />
                )}
                {col.foreignKey && (
                  <Link className="w-3.5 h-3.5 text-blue-400 shrink-0" />
                )}
                <div className="flex flex-col gap-0.5 flex-1 min-w-0">
                  <span className="text-xs font-semibold text-gray-200 tracking-wider truncate">
                    {col.name}
                  </span>
                  {col.type && col.type !== 'UNKNOWN' && (
                    <span className="text-[10px] text-gray-500 font-normal truncate">
                      {col.type}
                    </span>
                  )}
                </div>
                <SortIcon className="h-3.5 w-3.5 text-gray-400 shrink-0" />
              </div>
            ) : (
              <div className="flex items-center gap-1.5 px-4 py-2">
                {isKey && (
                  <Key className="w-3.5 h-3.5 text-yellow-400 shrink-0" />
                )}
                {col.foreignKey && (
                  <Link className="w-3.5 h-3.5 text-blue-400 shrink-0" />
                )}
                <div className="flex flex-col gap-0.5 flex-1 min-w-0">
                  <span className="text-xs font-semibold text-gray-200 tracking-wider truncate">
                    {col.name}
                  </span>
                  {col.type && col.type !== 'UNKNOWN' && (
                    <span className="text-[10px] text-gray-500 font-normal truncate">
                      {col.type}
                    </span>
                  )}
                </div>
              </div>
            )
            
            return (
              <ColumnInfo column={col}>
                {headerContent}
              </ColumnInfo>
            )
          },
          cell: ({ getValue }) => {
            return formatCellValue(getValue())
          },
        }
      }),
    [normalizedColumns, showControls]
  )

  // Convert rows array to object format for TanStack Table
  const tableData: Record<string, unknown>[] = React.useMemo(
    () =>
      rows.map((row) =>
        normalizedColumns.reduce((acc, col, idx) => {
          acc[col.name] = row[idx]
          return acc
        }, {} as Record<string, unknown>)
      ),
    [rows, normalizedColumns]
  )

  const table = useReactTable({
    data: tableData,
    columns: tableColumns,
    onSortingChange: setSorting,
    onColumnFiltersChange: setColumnFilters,
    getCoreRowModel: getCoreRowModel(),
    // Only use client-side pagination if server-side pagination is not enabled
    ...(hasServerPagination ? {
      manualPagination: true,
      pageCount: Math.ceil(totalRows / limit),
    } : {
      getPaginationRowModel: getPaginationRowModel(),
    }),
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
      ...(!hasServerPagination && {
        pagination: {
          pageIndex: 0,
          pageSize: 100,
        },
      }),
    },
  })

  return (
    <div className="w-full flex flex-col h-full">
      {/* Toolbar with search and column visibility */}
      {showControls && (
        <div className="flex items-center gap-2 px-6 py-4 border-b border-gray-800 bg-gray-950 shrink-0">
          <DropdownMenu onOpenChange={(open) => !open && setColumnSearch("")}>
            <DropdownMenuTrigger asChild>
              <Button variant="outline">
                <Settings2 className="mr-2 h-4 w-4" />
                Columns <ChevronDown className="ml-2 h-4 w-4" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-[240px]" onCloseAutoFocus={(e) => e.preventDefault()}>
              <DropdownMenuLabel>Toggle columns
              </DropdownMenuLabel>
              <DropdownMenuSeparator />
              <div className="px-2 py-2" onKeyDown={(e) => e.stopPropagation()}>
                <div className="relative">
                  <Search className="absolute left-2 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-gray-400" />
                  <Input
                    placeholder="Search..."
                    value={columnSearch}
                    onChange={(e) => setColumnSearch(e.target.value)}
                    className="h-8 text-xs pl-8 w-full"
                    onClick={(e) => e.stopPropagation()}
                    onKeyDown={(e) => e.stopPropagation()}
                    autoFocus
                  />
                </div>
              </div>

              <DropdownMenuSeparator />
              <div className="max-h-[300px] overflow-y-auto">
                <Button
                  variant="ghost"
                  size="sm"
                  className="h-7 w-full justify-start text-xs font-normal"
                  onClick={(e) => {
                    e.preventDefault()
                    const hasHiddenColumns = table.getAllColumns().some(col => !col.getIsVisible() && col.getCanHide())
                    table.getAllColumns().forEach(col => {
                      if (col.getCanHide()) {
                        col.toggleVisibility(hasHiddenColumns)
                      }
                    })
                  }}
                >
                  {table.getAllColumns().some(col => !col.getIsVisible() && col.getCanHide()) 
                    ? "Select all" 
                    : "Deselect all"}
                </Button>
                {table
                  .getAllColumns()
                  .filter((column) => column.getCanHide())
                  .filter((column) => 
                    column.id.toLowerCase().includes(columnSearch.toLowerCase())
                  )
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
                  .filter((column) => 
                    column.id.toLowerCase().includes(columnSearch.toLowerCase())
                  ).length === 0 && (
                  <div className="px-2 py-2 text-xs text-gray-500 text-center">
                    No columns found
                  </div>
                )}
              </div>
            </DropdownMenuContent>
          </DropdownMenu>
          
          <div className="flex items-center gap-2 ml-auto">
            {metadata?.executionTimeMs !== undefined && (
              <div className="flex items-center gap-1.5 px-3 py-1.5 bg-gray-900/50 border border-gray-800 rounded-md">
                <span className="text-xs text-gray-400">{rows.length} rows ⋅</span>
                <span className="text-xs font-mono font-semibold text-gray-400">{metadata.executionTimeMs}ms</span>
              </div>
            )}
            
            {/* Server-side pagination controls */}
            {hasServerPagination && onPaginationChange && (
              <>
              <Button
                variant="outline"
                size="sm"
                onClick={() => {
                  const newOffset = Math.max(0, offset - limit)
                  onPaginationChange(limit, newOffset)
                }}
                disabled={offset === 0}
                className="h-9 w-9 p-0"
              >
                <ChevronDown className="h-4 w-4 rotate-90" />
              </Button>
              
              <Input
                type="number"
                value={limit}
                onChange={(e) => {
                  const newLimit = Math.max(1, Math.min(1000, parseInt(e.target.value) || 100))
                  onPaginationChange(newLimit, offset)
                }}
                className="h-9 w-20 text-center font-mono text-sm [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none"
                min="1"
                max="1000"
              />
              
              <Input
                type="number"
                value={offset}
                onChange={(e) => {
                  const newOffset = Math.max(0, parseInt(e.target.value) || 0)
                  onPaginationChange(limit, newOffset)
                }}
                className="h-9 w-20 text-center font-mono text-sm [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none"
                min="0"
              />
              
              <Button
                variant="outline"
                size="sm"
                onClick={() => {
                  const newOffset = Math.min(Math.max(0, totalRows - limit), offset + limit)
                  onPaginationChange(limit, newOffset)
                }}
                disabled={offset + limit >= totalRows}
                className="h-9 w-9 p-0"
              >
                <ChevronDown className="h-4 w-4 -rotate-90" />
              </Button>
              </>
            )}
          </div>
        </div>
      )}

      {/* Table */}
      <div className="overflow-auto flex-1">
        <table className="w-full caption-bottom text-sm" style={{ width: showControls ? table.getCenterTotalSize() : '100%' }}>
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
                        className="px-4 py-2 text-xs text-gray-200 border-r border-gray-800 overflow-hidden font-mono"
                        style={{ width: cell.column.getSize(), maxWidth: cell.column.getSize() }}
                      >
                        {isLoading ? (
                          <Skeleton className="h-4 w-full" />
                        ) : (
                          <div className="truncate">
                            {flexRender(cell.column.columnDef.cell, cell.getContext())}
                          </div>
                        )}
                      </TableCell>
                    )
                  })}
                </TableRow>
              ))
            ) : (
              <TableRow>
                <TableCell colSpan={normalizedColumns.length} className="h-24 text-center">
                  No results.
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </table>
      </div>

    </div>
  )
}
