import { useParams } from 'react-router-dom'
import {  KeyRound } from 'lucide-react'
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
    return <div className="flex-1 p-0 overflow-auto flex items-center justify-center text-gray-500 font-mono">Loading…</div>
  }
  if (error) {
    return <div className="flex-1 p-0 overflow-auto flex items-center justify-center text-red-400 font-mono">Error: {error.message}</div>
  }
  if (!data) {
    return <div className="flex-1 p-0 overflow-auto font-mono" />
  }

  return (
    <div className="flex-1 p-0 overflow-auto font-mono">
      <div className="px-6 pt-6 pb-4 border-b border-gray-800">
        <h2 className="text-2xl font-semibold tracking-tight flex items-center gap-3">
          <span className="truncate">{data.name}</span>
          <span className="inline-flex items-center rounded-md bg-gray-800/60 text-gray-300 border border-gray-700 px-2.5 py-0.5 text-xs font-medium">
            {data.rows.length} rows
          </span>
        </h2>
      </div>
      <div className="order border-gray-800 overflow-hidden">
        <UiTable className="w-full">
          <TableHeader>
            <TableRow className="bg-gray-900/50">
              {data.columns.map(col => {
                const isKey = (data.primaryKeys?.includes(col)) || /(^id$|_id$)/i.test(col)
                return (
                  <TableHead key={col} className="text-left px-4 py-3 text-xs font-medium text-gray-400 tracking-wider border-b border-gray-800">
                    <span className="inline-flex items-center gap-1.5">
                      {isKey && <KeyRound className="w-3.5 h-3.5 text-amber-300" />}
                      <span>{col}</span>
                    </span>
                  </TableHead>
                )
              })}
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.rows.map((row, i) => (
              <TableRow key={i} className="hover:bg-gray-900/30 transition-colors">
                {row.map((cell, j) => (
                  <TableCell key={j} className="px-4 py-3 text-sm text-gray-200 whitespace-nowrap">
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


