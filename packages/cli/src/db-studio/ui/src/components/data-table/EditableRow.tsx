import * as React from 'react'
import type { ColumnMetadata } from '@/lib/types'
import type { Table } from '@tanstack/react-table'
import { TableRow, TableCell } from '@/components/ui/table'
import { X } from 'lucide-react'
import { EditableCell } from './EditableCell'

type EditableRowProps = {
  rowId: string
  table: Table<Record<string, unknown>>
  columns: ColumnMetadata[]
  rowData: Record<string, unknown>
  onCellChange: (rowId: string, columnName: string, value: unknown) => void
  onRemove: (rowId: string) => void
}

export const EditableRow = React.memo(function EditableRow({
  rowId,
  table,
  columns,
  rowData,
  onCellChange,
  onRemove,
}: EditableRowProps) {
  // Get all column definitions from the table (including checkbox)
  const allColumns = table.getAllColumns()

  return (
    <TableRow className="bg-blue-50 dark:bg-blue-950/20 border-b border-blue-200 dark:border-blue-800 hover:bg-blue-100 dark:hover:bg-blue-950/30">
      {allColumns.map((column) => {
        const width = column.getSize()
        const isCheckboxColumn = column.id === 'select'

        if (isCheckboxColumn) {
          // Render red X for removing this row
          return (
            <TableCell
              key={column.id}
              className="text-xs text-foreground border-r border-border px-2 py-2"
              style={{ width, maxWidth: width }}
            >
              <div className="flex items-center justify-center">
                <button
                  type="button"
                  onClick={() => onRemove(rowId)}
                  className="p-0.5 rounded hover:bg-red-100 dark:hover:bg-red-900/30 transition-colors"
                  aria-label="Remove row"
                >
                  <X className="h-4 w-4 text-red-500" />
                </button>
              </div>
            </TableCell>
          )
        }

        // Find the corresponding column metadata
        const columnMeta = columns.find((col) => col.name === column.id)
        if (!columnMeta) return null

        return (
          <TableCell
            key={column.id}
            className="text-xs text-foreground border-r border-border overflow-hidden p-0"
            style={{ width, maxWidth: width }}
          >
            <EditableCell
              column={columnMeta}
              value={rowData[columnMeta.name]}
              onChange={(value) => onCellChange(rowId, columnMeta.name, value)}
            />
          </TableCell>
        )
      })}
    </TableRow>
  )
})
