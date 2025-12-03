import * as React from 'react'
import { flexRender, type Row } from '@tanstack/react-table'

import { TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Skeleton } from '@/components/ui/skeleton'
import { useDataTable } from '@/contexts/DataTableContext'
import { EditableRow } from './EditableRow'
import { InlineEditableCell } from './InlineEditableCell'
import type { CellEdit, EditableRowData } from './DataTable'

type SelectedCell = { rowId: string; columnId: string } | null
type EditingCell = { rowIndex: number; columnId: string } | null

type DataTableGridProps = {
  editableRows?: EditableRowData[]
  onEditCellChange?: (rowId: string, columnName: string, value: unknown) => void
  onEditRowRemove?: (rowId: string) => void
  cellEdits?: CellEdit[]
  onCellEditCommit?: (rowIndex: number, columnName: string, newValue: unknown) => void
  onCellEditCancel?: (rowIndex: number, columnName: string) => void
}

type MemoizedRowProps = {
  row: Row<Record<string, unknown>>
  rowIndex: number
  isLoading: boolean
  isResizing: boolean
  selectedCell: SelectedCell
  editingCell: EditingCell
  editingValue: unknown
  cellEdits: CellEdit[]
  columns: { name: string; type: string; nullable: boolean; primaryKey: boolean; autoIncrement?: boolean }[]
  onCellClick: (rowId: string, columnId: string) => void
  onCellDoubleClick?: (rowIndex: number, columnId: string) => void
  onEditChange: (value: unknown) => void
  onEditCommit: () => void
  onEditCancel: () => void
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

  // Rerender if editing state for this row changed
  const prevIsEditing = prev.editingCell?.rowIndex === prev.rowIndex
  const nextIsEditing = next.editingCell?.rowIndex === next.rowIndex
  if (prevIsEditing !== nextIsEditing) return false
  if (prevIsEditing && nextIsEditing && prev.editingCell?.columnId !== next.editingCell?.columnId) return false
  // Rerender if editing value changed for this row
  if (nextIsEditing && prev.editingValue !== next.editingValue) return false

  // Rerender if cell edits for this row changed
  const prevEdits = prev.cellEdits.filter((e) => e.rowIndex === prev.rowIndex)
  const nextEdits = next.cellEdits.filter((e) => e.rowIndex === next.rowIndex)
  if (prevEdits.length !== nextEdits.length) return false
  for (let i = 0; i < prevEdits.length; i++) {
    if (prevEdits[i].columnName !== nextEdits[i].columnName || prevEdits[i].newValue !== nextEdits[i].newValue)
      return false
  }

  // Skip rerender only if we're currently resizing
  return next.isResizing
}

/**
 * Memoized table row that prevents rerenders during column resizing
 * Maintains smooth performance with large datasets by blocking rerenders during resize operations
 */
const MemoizedRow = React.memo(
  ({
    row,
    rowIndex,
    isLoading,
    selectedCell,
    editingCell,
    editingValue,
    cellEdits,
    columns,
    onCellClick,
    onCellDoubleClick,
    onEditChange,
    onEditCommit,
    onEditCancel,
  }: MemoizedRowProps) => {
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
          const isEditing =
            !isCheckboxColumn && editingCell?.rowIndex === rowIndex && editingCell?.columnId === column.id

          // Check if this cell has a pending edit
          const cellEdit = cellEdits.find((e) => e.rowIndex === rowIndex && e.columnName === column.id)
          const hasEdit = !!cellEdit

          // Get column metadata for the editable cell
          const columnMeta = columns.find((col) => col.name === column.id)

          // Get the display value: use editingValue when actively editing, otherwise edited or original
          const displayValue = isEditing ? editingValue : hasEdit ? cellEdit.newValue : row.original[column.id]

          // Style cells that are selected, editing, or have edits with blue highlight
          const hasBlueHighlight = isCellSelected || isEditing || hasEdit

          return (
            <TableCell
              key={id}
              onClick={isCheckboxColumn || isEditing ? undefined : () => onCellClick(row.id, column.id)}
              onDoubleClick={
                isCheckboxColumn || isEditing || !onCellDoubleClick
                  ? undefined
                  : () => onCellDoubleClick(rowIndex, column.id)
              }
              className={`text-xs text-foreground border-r border-border overflow-hidden truncate font-mono transition-colors ${
                isCheckboxColumn ? 'px-4 py-2' : 'cursor-pointer'
              } ${!isCheckboxColumn && !isEditing ? 'px-4 py-2' : ''} ${
                hasBlueHighlight ? 'bg-blue-500/20 ring-2 ring-blue-500 ring-inset' : ''
              } ${isEditing ? 'p-0' : ''}`}
              style={{ width, maxWidth: width }}
            >
              {isLoading ? (
                <Skeleton className="h-4 w-full" />
              ) : isEditing && columnMeta ? (
                <InlineEditableCell
                  column={columnMeta}
                  value={editingValue}
                  originalValue={row.original[column.id]}
                  onChange={onEditChange}
                  onCommit={onEditCommit}
                  onCancel={onEditCancel}
                />
              ) : hasEdit ? (
                // Show edited value
                displayValue === null ? (
                  <span className="text-muted-foreground">NULL</span>
                ) : displayValue === '' ? (
                  <span className="text-muted-foreground">EMPTY STRING</span>
                ) : (
                  String(displayValue)
                )
              ) : (
                flexRender(column.columnDef.cell, getContext())
              )}
            </TableCell>
          )
        })}
      </TableRow>
    )
  },
  shouldSkipRowRerender
)

MemoizedRow.displayName = 'MemoizedRow'

export function DataTableGrid({
  editableRows,
  onEditCellChange,
  onEditRowRemove,
  cellEdits = [],
  onCellEditCommit,
  onCellEditCancel,
}: DataTableGridProps) {
  const { table, config, pagination, columns } = useDataTable()
  const [selectedCell, setSelectedCell] = React.useState<SelectedCell>(null)
  const [editingCell, setEditingCell] = React.useState<EditingCell>(null)
  const [editingValue, setEditingValue] = React.useState<unknown>(null)

  // Check if any column is currently being resized
  const isResizing = table.getState().columnSizingInfo.isResizingColumn !== false
  const hasEditableRows = (editableRows?.length ?? 0) > 0
  const hasCellEdits = cellEdits.length > 0

  const handleCellClick = React.useCallback(
    (rowId: string, columnId: string) => {
      // Clear editing when clicking elsewhere
      if (editingCell) {
        setEditingCell(null)
        setEditingValue(null)
      }

      setSelectedCell((prev) => {
        // Toggle off if clicking the same cell
        if (prev?.rowId === rowId && prev?.columnId === columnId) {
          return null
        }
        return { rowId, columnId }
      })
    },
    [editingCell]
  )

  const handleCellDoubleClick = React.useCallback(
    (rowIndex: number, columnId: string) => {
      if (!onCellEditCommit) return

      // Get the current value
      const row = table.getRowModel().rows[rowIndex]
      if (!row) return

      // Check if there's already a pending edit for this cell
      const existingEdit = cellEdits.find((e) => e.rowIndex === rowIndex && e.columnName === columnId)
      const currentValue = existingEdit ? existingEdit.newValue : row.original[columnId]

      setEditingCell({ rowIndex, columnId })
      setEditingValue(currentValue)
      setSelectedCell(null)
    },
    [onCellEditCommit, table, cellEdits]
  )

  const handleEditChange = React.useCallback((value: unknown) => {
    setEditingValue(value)
  }, [])

  const handleEditCommit = React.useCallback(() => {
    if (editingCell && onCellEditCommit) {
      onCellEditCommit(editingCell.rowIndex, editingCell.columnId, editingValue)
    }
    setEditingCell(null)
    setEditingValue(null)
  }, [editingCell, editingValue, onCellEditCommit])

  const handleEditCancel = React.useCallback(() => {
    if (editingCell && onCellEditCancel) {
      onCellEditCancel(editingCell.rowIndex, editingCell.columnId)
    }
    setEditingCell(null)
    setEditingValue(null)
  }, [editingCell, onCellEditCancel])

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
          {/* Render editable rows */}
          {editableRows?.map((editableRow) => (
            <EditableRow
              key={editableRow.id}
              rowId={editableRow.id}
              table={table}
              columns={columns}
              rowData={editableRow.data}
              onCellChange={onEditCellChange!}
              onRemove={onEditRowRemove!}
            />
          ))}

          {/* Render regular data rows */}
          {table.getRowModel().rows?.length > 0 &&
            table
              .getRowModel()
              .rows?.map((row, index) => (
                <MemoizedRow
                  key={row.id}
                  row={row}
                  rowIndex={index}
                  isLoading={config.isLoading}
                  isResizing={isResizing}
                  selectedCell={hasEditableRows || hasCellEdits ? null : selectedCell}
                  editingCell={editingCell}
                  editingValue={editingValue}
                  cellEdits={cellEdits}
                  columns={columns}
                  onCellClick={handleCellClick}
                  onCellDoubleClick={onCellEditCommit ? handleCellDoubleClick : undefined}
                  onEditChange={handleEditChange}
                  onEditCommit={handleEditCommit}
                  onEditCancel={handleEditCancel}
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
