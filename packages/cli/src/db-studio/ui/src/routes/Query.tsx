import { useState, useEffect } from 'react'
import { useParams } from 'react-router-dom'
import { useQueryExecution } from '../hooks/useQueryExecution'
import { useDatabaseTables } from '../hooks/useDatabaseTables'
import { QueryEditor } from '../components/QueryEditor'
import { QueryResults } from '../components/QueryResults'
import { ResizablePanelGroup, ResizablePanel, ResizableHandle } from '@/components/ui/resizable'

export default function Query() {
  const { dbId } = useParams()
  const [sql, setSql] = useState('')

  const { data: tables = [] } = useDatabaseTables(dbId)
  const { mutate: executeQuery, data: result, isPending: loading, error } = useQueryExecution(dbId)

  useEffect(() => {
    if (tables.length > 0) {
      const firstTable = tables.find((t) => t.type === 'table')
      if (firstTable) {
        setSql(`SELECT * FROM "${firstTable.name}";`)
      }
    }
  }, [tables])

  const handleExecute = () => executeQuery({ sql: sql.trim() })

  return (
    <div className="flex-1 p-0 overflow-hidden font-mono flex flex-col h-full">
      <ResizablePanelGroup direction="vertical" className="h-full">
        <ResizablePanel defaultSize={40} minSize={20} maxSize={70}>
          <QueryEditor sql={sql} onSqlChange={setSql} onExecute={handleExecute} loading={loading} />
        </ResizablePanel>

        <ResizableHandle withHandle className="bg-border" />

        <ResizablePanel defaultSize={60} minSize={30}>
          <div className="flex-1 overflow-auto h-full">
            <QueryResults result={result ?? null} loading={loading} error={error} />
          </div>
        </ResizablePanel>
      </ResizablePanelGroup>
    </div>
  )
}
