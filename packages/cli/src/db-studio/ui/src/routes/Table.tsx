import { useParams } from 'react-router-dom'
import { useTableData } from '../hooks/useTableData'
import { DataTable } from '../components/DataTable'

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
    <div className="flex-1 flex flex-col overflow-hidden">
      <div className="px-6 pt-6 pb-4 border-b border-gray-800">
        <h2 className="text-2xl font-semibold tracking-tight flex items-center gap-3">
          <span className="truncate">{data.name}</span>
          <span className="inline-flex items-center rounded-md bg-gray-800/60 text-gray-300 border border-gray-700 px-2.5 py-0.5 text-xs font-medium">
            {data.totalRows} total rows
          </span>
        </h2>
      </div>
      <div className="flex-1 overflow-auto">
        <DataTable data={data} />
      </div>
    </div>
  )
}


