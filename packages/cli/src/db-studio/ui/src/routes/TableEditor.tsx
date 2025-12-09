import { useEffect, useState } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { ArrowLeft, Plus, Trash2, X } from 'lucide-react'
import { useForm, useFieldArray, FormProvider } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { ColumnCard } from '@/components/table-editor/ColumnCard'
import { useTableSchema } from '@/hooks/useTableSchema'
import { useCreateTable } from '@/hooks/useCreateTable'
import { useAlterTable } from '@/hooks/useAlterTable'
import { tableEditorSchema, type TableEditorFormData } from '@/lib/schemas/table-editor'

export function TableEditor() {
  const { dbId, tableName } = useParams<{ dbId: string; tableName?: string }>()
  const navigate = useNavigate()
  const isEditMode = !!tableName

  // Selection state for bulk deletion
  const [selectedIndices, setSelectedIndices] = useState<Set<number>>(new Set())

  // Hooks
  const { data: schema, isLoading } = useTableSchema(dbId!, tableName)
  const createMutation = useCreateTable(dbId!)
  const alterMutation = useAlterTable(dbId!, tableName!)

  // Form setup
  const form = useForm<TableEditorFormData>({
    resolver: zodResolver(tableEditorSchema),
    defaultValues: {
      name: tableName || '',
      columns: [
        {
          name: 'column_1',
          type: 'INTEGER',
          nullable: false,
          primaryKey: true,
          autoIncrement: true,
          unique: true,
          defaultValue: null,
        },
      ],
    },
  })

  const { fields, append, remove } = useFieldArray({
    control: form.control,
    name: 'columns',
  })

  // Load schema in edit mode
  useEffect(() => {
    if (schema && isEditMode) {
      form.reset({
        name: schema.tableName,
        columns: schema.columns.map((col: any) => ({
          name: col.name,
          type: col.type,
          nullable: col.nullable,
          primaryKey: col.primaryKey,
          autoIncrement: col.autoIncrement || false,
          unique: col.unique || false,
          defaultValue: col.defaultValue,
        })),
      })
    }
  }, [schema, isEditMode, form])

  const addColumn = () => {
    append({
      name: `column_${fields.length + 1}`,
      type: 'TEXT',
      nullable: true,
      primaryKey: false,
      autoIncrement: false,
      unique: false,
      defaultValue: null,
    })
  }

  const deleteSelectedColumns = () => {
    if (selectedIndices.size === 0) return

    // Convert indices to array and sort in descending order to remove from end first
    const indicesToRemove = Array.from(selectedIndices).sort((a, b) => b - a)

    // Check if we're trying to delete all columns
    if (fields.length - selectedIndices.size < 1) {
      form.setError('columns', {
        message: 'Table must have at least one column',
      })
      return
    }

    // Remove columns
    indicesToRemove.forEach((index) => remove(index))

    // Clear selection
    setSelectedIndices(new Set())
  }

  const toggleColumnSelection = (index: number) => {
    setSelectedIndices((prev) => {
      const next = new Set(prev)
      if (next.has(index)) {
        next.delete(index)
      } else {
        next.add(index)
      }
      return next
    })
  }

  const deselectAll = () => {
    setSelectedIndices(new Set())
  }

  const onSubmit = async (data: TableEditorFormData) => {
    if (isEditMode) {
      // Compute diff and generate ALTER operations
      const operations = computeAlterOperations(schema?.columns || [], data.columns)

      if (operations.length === 0) {
        form.setError('root', { message: 'No changes detected' })
        return
      }

      try {
        await alterMutation.mutateAsync({ operations })
        navigate(`/database/${dbId}/table/${encodeURIComponent(data.name)}`)
      } catch (err) {
        form.setError('root', {
          message: err instanceof Error ? err.message : 'Failed to alter table',
        })
      }
    } else {
      // Create new table
      try {
        await createMutation.mutateAsync({
          name: data.name,
          columns: data.columns.map((col) => ({
            name: col.name,
            type: col.type,
            nullable: col.nullable,
            primaryKey: col.primaryKey,
            autoIncrement: col.autoIncrement,
            unique: col.unique,
            defaultValue: col.defaultValue,
          })),
        })
        navigate(`/database/${dbId}/table/${encodeURIComponent(data.name)}`)
      } catch (err) {
        form.setError('root', {
          message: err instanceof Error ? err.message : 'Failed to create table',
        })
      }
    }
  }

  if (isLoading && isEditMode) {
    return (
      <div className="flex items-center justify-center h-full">
        <div className="text-muted-foreground">Loading schema...</div>
      </div>
    )
  }

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="flex items-center gap-3 px-6 py-4 border-b border-border">
        <Button variant="ghost" size="icon" asChild>
          <Link to={`/database/${dbId}/tables`}>
            <ArrowLeft className="h-4 w-4" />
          </Link>
        </Button>
        <h1 className="text-xl font-semibold">{isEditMode ? `Edit Table: ${tableName}` : 'Create New Table'}</h1>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-auto p-6">
        <FormProvider {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="max-w-4xl mx-auto space-y-6">
            {/* Table Name */}
            <div>
              <label className="block text-sm font-medium mb-2">Table Name</label>
              <Input {...form.register('name')} disabled={isEditMode} placeholder="users" />
              {form.formState.errors.name && (
                <p className="text-sm text-destructive mt-1">{form.formState.errors.name.message}</p>
              )}
            </div>

            {/* Columns */}
            <div>
              <div className="flex items-center justify-between mb-4">
                <div>
                  <label className="block text-sm font-medium">Columns</label>
                  <p className="text-xs text-muted-foreground mt-1">Define the structure of your table</p>
                </div>
                <div className="flex items-center gap-2">
                  {selectedIndices.size > 0 && (
                    <>
                      <Button type="button" variant="outline" size="sm" onClick={deselectAll}>
                        <X className="h-4 w-4 mr-1" />
                        Deselect All
                      </Button>
                      <Button type="button" variant="destructive" size="sm" onClick={deleteSelectedColumns}>
                        <Trash2 className="h-4 w-4 mr-1" />
                        Delete {selectedIndices.size} Column{selectedIndices.size > 1 ? 's' : ''}
                      </Button>
                    </>
                  )}
                  <Button type="button" variant="outline" size="sm" onClick={addColumn}>
                    <Plus className="h-4 w-4 mr-1" />
                    Add Column
                  </Button>
                </div>
              </div>

              <div className="space-y-3">
                {fields.map((field, index) => (
                  <ColumnCard
                    key={field.id}
                    index={index}
                    control={form.control}
                    isSelected={selectedIndices.has(index)}
                    onToggleSelect={() => toggleColumnSelection(index)}
                    isEditMode={isEditMode}
                    isNewColumn={
                      isEditMode && !schema?.columns.some((c: any) => c.name === form.watch(`columns.${index}.name`))
                    }
                  />
                ))}
              </div>
              {form.formState.errors.columns && (
                <p className="text-sm text-destructive mt-2">{form.formState.errors.columns.message}</p>
              )}
            </div>

            {/* Root Error */}
            {form.formState.errors.root && (
              <div className="border border-destructive bg-destructive/10 rounded p-3">
                <p className="text-sm text-destructive">{form.formState.errors.root.message}</p>
              </div>
            )}

            {/* Actions */}
            <div className="flex justify-end gap-2">
              <Button type="button" variant="outline" asChild>
                <Link to={`/database/${dbId}/tables`}>Cancel</Link>
              </Button>
              <Button type="submit" disabled={form.formState.isSubmitting}>
                {form.formState.isSubmitting ? 'Saving...' : 'Save Table'}
              </Button>
            </div>
          </form>
        </FormProvider>
      </div>
    </div>
  )
}

function computeAlterOperations(original: any[], updated: any[]): any[] {
  const operations: any[] = []

  const originalNames = new Set(original.map((c: any) => c.name))
  const updatedNames = new Set(updated.map((c) => c.name))

  // Dropped columns
  for (const col of original) {
    if (!updatedNames.has(col.name)) {
      operations.push({
        type: 'drop_column',
        columnName: col.name,
      })
    }
  }

  // Added columns
  for (const col of updated) {
    if (!originalNames.has(col.name)) {
      operations.push({
        type: 'add_column',
        column: {
          name: col.name,
          type: col.type,
          nullable: col.nullable,
          defaultValue: col.defaultValue,
        },
      })
    }
  }

  return operations
}
