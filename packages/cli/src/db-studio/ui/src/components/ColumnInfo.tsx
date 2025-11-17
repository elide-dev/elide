import * as React from "react"
import { Key, Link } from "lucide-react"
import {
  HoverCard,
  HoverCardContent,
  HoverCardTrigger,
} from "@/components/ui/hover-card"
import type { ColumnMetadata } from "./DataTable"

interface ColumnInfoProps {
  column: ColumnMetadata
  children: React.ReactNode
}

/**
 * Hover card component that displays detailed column metadata
 * Shows type, nullable status, constraints, foreign keys, etc.
 */
export function ColumnInfo({ column, children }: ColumnInfoProps) {
  return (
    <HoverCard openDelay={300}>
      <HoverCardTrigger asChild>
        {children}
      </HoverCardTrigger>
      <HoverCardContent className="w-80 bg-gray-900 border-gray-700" side="bottom" align="start">
        <div className="space-y-3">
          <div className="flex items-center gap-2">
            <h4 className="text-sm font-semibold text-gray-100 font-mono">{column.name}</h4>
          </div>
          
          <div className="space-y-2 text-xs">
            <div className="flex items-center justify-between py-1 border-b border-gray-800">
              <span className="text-gray-500">Type</span>
              <span className="font-mono text-gray-200">{column.type}</span>
            </div>
            
            {column.primaryKey && (
              <div className="flex items-center justify-between py-1 border-b border-gray-800">
                <div className="flex items-center gap-1.5">
                  <Key className="w-3.5 h-3.5 text-yellow-400 shrink-0" />
                  <span className="text-gray-500">Primary Key</span>
                </div>
                <span className="text-gray-200">Yes</span>
              </div>
            )}
            
            <div className="flex items-center justify-between py-1 border-b border-gray-800">
              <span className="text-gray-500">Nullable</span>
              <span className="text-gray-200">
                {column.nullable ? 'Yes' : 'No'}
              </span>
            </div>
            
            {column.unique && (
              <div className="flex items-center justify-between py-1 border-b border-gray-800">
                <span className="text-gray-500">Unique</span>
                <span className="text-gray-200">Yes</span>
              </div>
            )}
            
            {column.autoIncrement && (
              <div className="flex items-center justify-between py-1 border-b border-gray-800">
                <span className="text-gray-500">Auto Increment</span>
                <span className="text-gray-200">Yes</span>
              </div>
            )}
            
            {column.defaultValue !== undefined && column.defaultValue !== null && (
              <div className="flex items-center justify-between py-1 border-b border-gray-800">
                <span className="text-gray-500">Default</span>
                <span className="font-mono text-gray-200">{String(column.defaultValue)}</span>
              </div>
            )}
            
            {column.foreignKey && (
              <div className="py-1 space-y-1">
                <div className="flex items-center gap-1.5 text-gray-500 mb-1">
                  <Link className="w-3.5 h-3.5 text-blue-400 shrink-0" />
                  <span>Foreign Key</span>
                </div>
                <div className="pl-2 space-y-1 border-l-2 border-gray-700">
                  <div className="flex items-center justify-between">
                    <span className="text-gray-500">References</span>
                    <span className="font-mono text-gray-200">
                      {column.foreignKey.table}.{column.foreignKey.column}
                    </span>
                  </div>
                  {column.foreignKey.onUpdate && (
                    <div className="flex items-center justify-between">
                      <span className="text-gray-500">On Update</span>
                      <span className="font-mono text-gray-300">{column.foreignKey.onUpdate}</span>
                    </div>
                  )}
                  {column.foreignKey.onDelete && (
                    <div className="flex items-center justify-between">
                      <span className="text-gray-500">On Delete</span>
                      <span className="font-mono text-gray-300">{column.foreignKey.onDelete}</span>
                    </div>
                  )}
                </div>
              </div>
            )}
          </div>
        </div>
      </HoverCardContent>
    </HoverCard>
  )
}

