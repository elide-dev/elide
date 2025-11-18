import * as React from 'react'
import type {
  ColumnDef,
  ColumnFiltersState,
  ColumnSizingState,
  SortingState,
  VisibilityState,
} from '@tanstack/react-table'
import {
  flexRender,
  getCoreRowModel,
  getFilteredRowModel,
  getSortedRowModel,
  useReactTable,
} from '@tanstack/react-table'
import {
  ArrowUpDown,
  ArrowUpWideNarrow,
  ArrowDownWideNarrow,
  ChevronDown,
  Settings2,
  Search,
  Key,
  Link,
  Filter as FilterIcon,
  X,
  Plus,
  RefreshCw,
} from 'lucide-react'

import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuCheckboxItem,
  DropdownMenuContent,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { Input } from '@/components/ui/input'
import { TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Skeleton } from '@/components/ui/skeleton'
import { HoverCard, HoverCardContent, HoverCardTrigger } from '@/components/ui/hover-card'
import { Badge } from '@/components/ui/badge'
import { ColumnInfo } from './ColumnInfo'
import { formatRowCount } from '@/lib/utils'
import type { Filter } from '@/lib/types'
import { FILTER_OPERATORS } from '@/lib/types'

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

export type DataTableSorting = {
  column: string | null
  direction: 'asc' | 'desc' | null
}

interface DataTableProps {
  data: DataTableData
  showControls?: boolean // Show sorting, column toggle, and column resizing (default: true)
  showPagination?: boolean // Show pagination controls (default: true)
  showMetadata?: boolean // Show execution time and row count metadata (default: true when available)
  // Server-side pagination props (required)
  totalRows: number
  tableRowCount?: number // Total rows in the table (unfiltered) - shown in badge next to table name
  pagination: DataTablePagination
  onPaginationChange: (limit: number, offset: number) => void
  // Server-side sorting props (optional - if not provided, sorting is disabled)
  sorting?: DataTableSorting
  onSortChange?: (column: string | null, direction: 'asc' | 'desc' | null) => void
  // Server-side filtering props (optional)
  filters?: Filter[]
  onFiltersChange?: (filters: Filter[]) => void
  isLoading?: boolean // Show skeleton loaders when fetching new data
  tableName?: string // Table name to display in toolbar
  onRefresh?: () => void // Callback to refetch the current query state
}

/**
 * Reusable data table component for displaying database query results
 * Uses TanStack Table for sorting, filtering, and column visibility
 * Always uses server-side pagination via totalRows, limit, offset props and onPaginationChange callback
 * Supports server-side sorting via sorting and onSortChange props
 * Pagination is controlled - limit/offset come from props (URL query params)
 * Handles NULL values, empty strings, and other data types appropriately
 */
export function DataTable({
  data,
  showControls = true,
  showPagination = true,
  showMetadata = true,
  totalRows,
  tableRowCount,
  pagination,
  onPaginationChange,
  sorting,
  onSortChange,
  filters = [],
  onFiltersChange,
  isLoading = false,
  tableName,
  onRefresh,
}: DataTableProps) {
  // Convert server-side sorting to TanStack Table format
  const sortingState: SortingState = React.useMemo(() => {
    if (sorting?.column && sorting.direction) {
      return [{ id: sorting.column, desc: sorting.direction === 'desc' }]
    }
    return []
  }, [sorting])

  const [columnFilters, setColumnFilters] = React.useState<ColumnFiltersState>([])
  const [columnVisibility, setColumnVisibility] = React.useState<VisibilityState>({})
  const [columnSizing, setColumnSizing] = React.useState<ColumnSizingState>({})
  const [columnSearch, setColumnSearch] = React.useState('')
  const [showFilters, setShowFilters] = React.useState(filters.length > 0)

  // Track draft filters (being edited) separately from applied filters
  const [draftFilters, setDraftFilters] = React.useState<Filter[]>(filters)

  // Sync draft filters when applied filters change externally
  React.useEffect(() => {
    setDraftFilters(filters)
  }, [filters])

  const { columns, rows, metadata } = data

  // Server-side pagination values
  const { limit, offset } = pagination

  // Local state for input values (only update on submit)
  const [limitInput, setLimitInput] = React.useState<string>(String(limit))
  const [offsetInput, setOffsetInput] = React.useState<string>(String(offset))

  // Sync local state when props change externally
  React.useEffect(() => {
    setLimitInput(String(limit))
  }, [limit])

  React.useEffect(() => {
    setOffsetInput(String(offset))
  }, [offset])

  const formatCellValue = (value: unknown): React.ReactNode => {
    return value === null || value === undefined ? (
      <span className="text-gray-500 font-normal">NULL</span>
    ) : (
      String(value)
    )
  }

  // Check if draft filters differ from applied filters
  const hasUnappliedChanges = React.useMemo(() => {
    return JSON.stringify(draftFilters) !== JSON.stringify(filters)
  }, [draftFilters, filters])

  // Filter management functions (work with draft filters)
  const handleAddFilter = () => {
    if (!onFiltersChange) return
    const newFilter: Filter = {
      column: columns[0]?.name || '',
      operator: 'eq',
      value: '',
    }
    setDraftFilters([...draftFilters, newFilter])
    setShowFilters(true)
  }

  const handleRemoveFilter = (index: number) => {
    if (!onFiltersChange) return
    const newFilters = draftFilters.filter((_, i) => i !== index)
    if (newFilters.length === 0) {
      // If removing the last filter, clear all and close panel
      setDraftFilters([])
      onFiltersChange([])
      setShowFilters(false)
    } else {
      setDraftFilters(newFilters)
    }
  }

  const handleUpdateFilter = (index: number, updates: Partial<Filter>) => {
    if (!onFiltersChange) return
    const newFilters = draftFilters.map((filter, i) => {
      if (i === index) {
        const updatedFilter = { ...filter, ...updates }
        // Reset value if operator changes to one that doesn't need a value
        if (updates.operator && (updates.operator === 'is_null' || updates.operator === 'is_not_null')) {
          updatedFilter.value = undefined
        }
        // Reset value if operator changes to 'in' (needs array)
        if (updates.operator === 'in' && !Array.isArray(updatedFilter.value)) {
          updatedFilter.value = []
        }
        // Reset value to string if operator changes from 'in' to something else
        if (updates.operator && updates.operator !== 'in' && Array.isArray(updatedFilter.value)) {
          updatedFilter.value = ''
        }
        return updatedFilter
      }
      return filter
    })
    setDraftFilters(newFilters)
  }

  const handleApplyFilters = () => {
    if (!onFiltersChange) return
    onFiltersChange(draftFilters)
  }

  const handleClearFilters = () => {
    if (!onFiltersChange) return
    setDraftFilters([])
    onFiltersChange([])
    setShowFilters(false)
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
            const SortIcon =
              sorted === 'asc' ? ArrowUpWideNarrow : sorted === 'desc' ? ArrowDownWideNarrow : ArrowUpDown

            const handleSort = () => {
              if (!onSortChange) return // Sorting disabled

              // Three-state sorting: no sort → asc → desc → no sort
              if (!sorted) {
                onSortChange(col.name, 'asc')
              } else if (sorted === 'asc') {
                onSortChange(col.name, 'desc')
              } else {
                onSortChange(null, null)
              }
            }

            const headerContent =
              showControls && onSortChange ? (
                <div
                  className="flex items-center gap-1.5 cursor-pointer select-none hover:text-gray-200 transition-colors w-full h-full px-4 py-2"
                  onClick={handleSort}
                >
                  {isKey && <Key className="w-3.5 h-3.5 text-yellow-400 shrink-0" />}
                  {col.foreignKey && <Link className="w-3.5 h-3.5 text-blue-400 shrink-0" />}
                  <div className="flex flex-col gap-0.5 flex-1 min-w-0">
                    <span className="text-xs font-semibold text-gray-200 tracking-wider truncate">{col.name}</span>
                    {col.type && col.type !== 'UNKNOWN' && (
                      <span className="text-[10px] text-gray-500 font-normal truncate">{col.type}</span>
                    )}
                  </div>
                  <SortIcon className="h-3.5 w-3.5 text-gray-400 shrink-0" />
                </div>
              ) : (
                <div className="flex items-center gap-1.5 px-4 py-2">
                  {isKey && <Key className="w-3.5 h-3.5 text-yellow-400 shrink-0" />}
                  {col.foreignKey && <Link className="w-3.5 h-3.5 text-blue-400 shrink-0" />}
                  <div className="flex flex-col gap-0.5 flex-1 min-w-0">
                    <span className="text-xs font-semibold text-gray-200 tracking-wider truncate">{col.name}</span>
                    {col.type && col.type !== 'UNKNOWN' && (
                      <span className="text-[10px] text-gray-500 font-normal truncate">{col.type}</span>
                    )}
                  </div>
                </div>
              )

            return <ColumnInfo column={col}>{headerContent}</ColumnInfo>
          },
          cell: ({ getValue }) => {
            return formatCellValue(getValue())
          },
        }
      }),
    [normalizedColumns, showControls, onSortChange]
  )

  // Convert rows array to object format for TanStack Table
  const tableData: Record<string, unknown>[] = React.useMemo(
    () =>
      rows.map((row) =>
        normalizedColumns.reduce(
          (acc, col, idx) => {
            acc[col.name] = row[idx]
            return acc
          },
          {} as Record<string, unknown>
        )
      ),
    [rows, normalizedColumns]
  )

  const table = useReactTable({
    data: tableData,
    columns: tableColumns,
    // Server-side sorting: when onSortChange is provided, use manual sorting
    manualSorting: !!onSortChange,
    onColumnFiltersChange: setColumnFilters,
    getCoreRowModel: getCoreRowModel(),
    // Always use server-side pagination
    manualPagination: true,
    pageCount: Math.ceil(totalRows / limit),
    // Only use client-side sorting if server-side sorting is not enabled
    getSortedRowModel: onSortChange ? undefined : getSortedRowModel(),
    getFilteredRowModel: getFilteredRowModel(),
    onColumnVisibilityChange: setColumnVisibility,
    onColumnSizingChange: setColumnSizing,
    enableColumnResizing: true,
    columnResizeMode: 'onChange',
    state: {
      sorting: sortingState,
      columnFilters,
      columnVisibility,
      columnSizing,
    },
  })

  return (
    <div className="w-full flex flex-col h-full">
      {/* Toolbar with metadata and controls */}
      {(showControls || showMetadata || showPagination || tableName) && (
        <div className="flex items-center gap-2 px-6 py-4 border-b border-gray-800 bg-gray-950 shrink-0">
          {tableName && (
            <div className="flex items-center gap-3 mr-4">
              <h2 className="text-lg font-semibold tracking-tight truncate">{tableName}</h2>
              {tableRowCount ? (
                <HoverCard openDelay={200}>
                  <HoverCardTrigger asChild>
                    <Badge variant="secondary" className="shrink-0 cursor-default">
                      {formatRowCount(tableRowCount)} rows
                    </Badge>
                  </HoverCardTrigger>
                  <HoverCardContent side="bottom" className="w-auto px-3 py-1.5">
                    <span className="text-xs font-semibold">{tableRowCount.toLocaleString()} total rows</span>
                  </HoverCardContent>
                </HoverCard>
              ) : (
                <Skeleton className="w-16 h-4" />
              )}
            </div>
          )}
          {showControls && onFiltersChange && (
            <div className="relative">
              <Button
                variant="outline"
                className={showFilters ? 'bg-accent text-accent-foreground' : ''}
                onClick={() => {
                  if (draftFilters.length === 0 && filters.length === 0) {
                    handleAddFilter()
                  } else {
                    setShowFilters(!showFilters)
                  }
                }}
              >
                <FilterIcon className="mr-2 h-4 w-4" />
                Filters
              </Button>
              {filters.length > 0 && (
                <Badge
                  variant="secondary"
                  className="absolute -top-1 -right-1 h-4 w-4 p-0 flex items-center justify-center text-[10px] rounded-full bg-white text-black"
                >
                  !
                </Badge>
              )}
            </div>
          )}
          {showControls && (
            <DropdownMenu onOpenChange={(open) => !open && setColumnSearch('')}>
              <DropdownMenuTrigger asChild>
                <div className="relative">
                  <Button variant="outline">
                    <Settings2 className="mr-2 h-4 w-4" />
                    Columns
                  </Button>
                  {Object.values(columnVisibility).some((visible) => visible === false) && (
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
                      const hasHiddenColumns = table
                        .getAllColumns()
                        .some((col) => !col.getIsVisible() && col.getCanHide())
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
                  </Button>
                  {table
                    .getAllColumns()
                    .filter((column) => column.getCanHide())
                    .filter((column) => column.id.toLowerCase().includes(columnSearch.toLowerCase()))
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
                    .filter((column) => column.id.toLowerCase().includes(columnSearch.toLowerCase())).length === 0 && (
                    <div className="px-2 py-2 text-xs text-gray-500 text-center">No columns found</div>
                  )}
                </div>
              </DropdownMenuContent>
            </DropdownMenu>
          )}

          <div className="flex items-center gap-2 ml-auto">
            {showMetadata && metadata?.executionTimeMs !== undefined && (
              <div className="flex items-center gap-1.5 px-3 py-1.5 bg-gray-900/50 border border-gray-800 rounded-md">
                <span className="text-xs text-gray-400">{rows.length} rows ⋅</span>
                <span className="text-xs font-mono font-semibold text-gray-400">{metadata.executionTimeMs}ms</span>
              </div>
            )}

            {/* Server-side pagination controls */}
            {showPagination && (
              <div className="flex items-center">
                <HoverCard openDelay={200}>
                  <HoverCardTrigger asChild>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => {
                        const newOffset = Math.max(0, offset - limit)
                        onPaginationChange(limit, newOffset)
                      }}
                      disabled={offset === 0}
                      className="h-9 w-9 p-0 rounded-r-none border-r-0"
                    >
                      <ChevronDown className="h-4 w-4 rotate-90" />
                    </Button>
                  </HoverCardTrigger>
                  <HoverCardContent side="bottom" className="w-auto px-3 py-1.5">
                    <span className="text-xs font-semibold">Previous Page</span>
                  </HoverCardContent>
                </HoverCard>
                <HoverCard openDelay={200}>
                  <HoverCardTrigger asChild>
                    <Input
                      type="number"
                      value={limitInput}
                      onChange={(e) => {
                        setLimitInput(e.target.value)
                      }}
                      onBlur={(e) => {
                        const newLimit = Math.max(1, Math.min(1000, parseInt(e.target.value) || 100))
                        setLimitInput(String(newLimit))
                        onPaginationChange(newLimit, offset)
                      }}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter') {
                          e.currentTarget.blur()
                        }
                      }}
                      className="h-9 w-20 text-center font-mono text-sm rounded-none border-r-0 [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none focus-visible:z-10"
                      min="1"
                      max="1000"
                    />
                  </HoverCardTrigger>
                  <HoverCardContent side="bottom" className="w-auto px-3 py-1.5">
                    <span className="text-xs font-semibold">LIMIT</span>
                  </HoverCardContent>
                </HoverCard>
                <HoverCard openDelay={200}>
                  <HoverCardTrigger asChild>
                    <Input
                      type="number"
                      value={offsetInput}
                      onChange={(e) => {
                        setOffsetInput(e.target.value)
                      }}
                      onBlur={(e) => {
                        const newOffset = Math.max(0, parseInt(e.target.value) || 0)
                        setOffsetInput(String(newOffset))
                        onPaginationChange(limit, newOffset)
                      }}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter') {
                          e.currentTarget.blur()
                        }
                      }}
                      className="h-9 w-20 text-center font-mono text-sm rounded-none border-r-0 [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none focus-visible:z-10"
                      min="0"
                    />
                  </HoverCardTrigger>
                  <HoverCardContent side="bottom" className="w-auto px-3 py-1.5">
                    <span className="text-xs font-semibold">OFFSET</span>
                  </HoverCardContent>
                </HoverCard>
                <HoverCard openDelay={200}>
                  <HoverCardTrigger asChild>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => {
                        const newOffset = offset + limit
                        onPaginationChange(limit, newOffset)
                      }}
                      className="h-9 w-9 p-0 rounded-l-none"
                    >
                      <ChevronDown className="h-4 w-4 -rotate-90" />
                    </Button>
                  </HoverCardTrigger>
                  <HoverCardContent side="bottom" className="w-auto px-3 py-1.5">
                    <span className="text-xs font-semibold">Next Page</span>
                  </HoverCardContent>
                </HoverCard>
                {/* Refresh button */}
                {onRefresh && (
                  <HoverCard openDelay={200}>
                    <HoverCardTrigger asChild>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={onRefresh}
                        disabled={isLoading}
                        className="h-9 w-9 p-0 ml-2"
                      >
                        <RefreshCw className={`h-4 w-4 ${isLoading ? 'animate-spin' : ''}`} />
                      </Button>
                    </HoverCardTrigger>
                    <HoverCardContent side="bottom" className="w-auto px-3 py-1.5">
                      <span className="text-xs font-semibold">Refresh rows</span>
                    </HoverCardContent>
                  </HoverCard>
                )}
              </div>
            )}
          </div>
        </div>
      )}

      {/* Filter rows section */}
      {showFilters && onFiltersChange && draftFilters.length > 0 && (
        <div className="border-b border-gray-800 bg-gray-950/50 shrink-0">
          <div className="flex items-start gap-6 px-6 py-3">
            <div className="space-y-2">
              {draftFilters.map((filter, index) => {
                const operatorMeta = FILTER_OPERATORS.find((op) => op.value === filter.operator)
                const requiresValue = operatorMeta?.requiresValue ?? true
                const isArrayValue = operatorMeta?.isArrayValue ?? false

                return (
                  <div key={index} className="flex items-center gap-2">
                    {/* First filter shows "where", others show "and" */}
                    <span className="text-xs text-gray-400 font-mono w-12 uppercase shrink-0">
                      {index === 0 ? 'where' : 'and'}
                    </span>

                    {/* Column selector */}
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button
                          variant="outline"
                          size="sm"
                          className="min-w-[140px] w-auto max-w-[280px] justify-between font-mono text-xs"
                        >
                          <span className="flex-1 text-left">{filter.column}</span>
                          <ChevronDown className="ml-2 h-3 w-3 shrink-0" />
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent className="max-h-[300px] overflow-y-auto">
                        {columns.map((col) => (
                          <DropdownMenuCheckboxItem
                            key={col.name}
                            checked={filter.column === col.name}
                            onCheckedChange={() => handleUpdateFilter(index, { column: col.name })}
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
                            onCheckedChange={() => handleUpdateFilter(index, { operator: op.value })}
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
                        onChange={(e) => handleUpdateFilter(index, { value: e.target.value })}
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
                          handleUpdateFilter(index, { value: values })
                        }}
                        className="h-8 w-[280px] text-xs font-mono"
                      />
                    )}

                    {/* Remove button */}
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => handleRemoveFilter(index)}
                      className="h-8 w-8 p-0 shrink-0 text-gray-400 hover:text-gray-200"
                    >
                      <X className="h-4 w-4" />
                    </Button>
                  </div>
                )
              })}
            </div>
            {/* Separator */}
            <div className="w-px bg-gray-800 self-stretch"></div>
            {/* Action buttons column */}
            <div className="flex items-end gap-2 pt-0.5">
              <Button
                variant="default"
                size="sm"
                onClick={handleApplyFilters}
                disabled={!hasUnappliedChanges}
                className="h-8 text-xs"
              >
                Apply
              </Button>
              <Button variant="outline" size="sm" onClick={handleAddFilter} className="h-8 text-xs">
                <Plus className="mr-1 h-3 w-3" />
                Add filter
              </Button>
              <Button variant="ghost" size="sm" onClick={handleClearFilters} className="h-8 text-xs text-gray-400">
                Clear filters
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* Table */}
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
    </div>
  )
}
