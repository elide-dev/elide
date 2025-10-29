import { useParams } from 'react-router-dom'
import { useTableData } from '../hooks/useTableData'
import {
  Table as UiTable,
  TableHeader,
  TableBody,
  TableRow,
  TableHead,
  TableCell,
} from "@/components/ui/table"

export default function TableView() {
  const { dbIndex, tableName } = useParams()
  const { data, isLoading: loading, error } = useTableData(dbIndex, tableName)

  if (loading) {
    return <div className="flex-1 p-6 overflow-auto flex items-center justify-center text-gray-500 font-mono">Loading…</div>
  }
  if (error) {
    return <div className="flex-1 p-6 overflow-auto flex items-center justify-center text-red-400 font-mono">Error: {error.message}</div>
  }
  if (!data) {
    return <div className="flex-1 p-6 overflow-auto font-mono" />
  }

  return (
    <div className="flex-1 p-6 overflow-auto font-mono">
      <div className="mb-6">
        <h2 className="text-2xl font-semibold mb-1">{data.name}</h2>
        <p className="text-sm text-gray-400">{data.rows.length} rows</p>
      </div>
      <div className="border border-gray-800 rounded-lg overflow-hidden">
        <UiTable className="w-full">
          <TableHeader>
            <TableRow className="bg-gray-900/50">
              {data.columns.map(col => (
                <TableHead key={col} className="text-left px-6 py-3 text-xs font-medium text-gray-400 tracking-wider border-b border-gray-800">
                  {col}
                </TableHead>
              ))}
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.rows.map((row, i) => (
              <TableRow key={i} className="hover:bg-gray-900/30 transition-colors">
                {row.map((cell, j) => (
                  <TableCell key={j} className="px-6 py-4 text-sm text-gray-200 whitespace-nowrap">
                    {String(cell ?? '')}
                  </TableCell>
                ))}
              </TableRow>
            ))}
          </TableBody>
        </UiTable>
      </div>
    </div>
  )
}


