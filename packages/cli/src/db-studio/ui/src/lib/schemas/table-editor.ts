import { z } from 'zod'

export const columnTypes = ['INTEGER', 'TEXT', 'REAL', 'BLOB', 'NUMERIC'] as const

export const columnSchema = z.object({
  name: z.string().min(1, 'Column name is required'),
  type: z.enum(columnTypes),
  nullable: z.boolean(),
  primaryKey: z.boolean(),
  autoIncrement: z.boolean(),
  unique: z.boolean(),
  defaultValue: z.string().nullable(),
})

export type ColumnFormData = z.infer<typeof columnSchema>

export const tableEditorSchema = z
  .object({
    name: z.string().min(1, 'Table name is required'),
    columns: z.array(columnSchema).min(1, 'At least one column is required'),
  })
  .refine(
    (data) => {
      const pkCount = data.columns.filter((c) => c.primaryKey).length
      return pkCount <= 1
    },
    { message: 'Maximum one primary key column allowed', path: ['columns'] }
  )
  .refine(
    (data) => {
      const names = data.columns.map((c) => c.name.toLowerCase().trim())
      return names.length === new Set(names).size
    },
    { message: 'Column names must be unique', path: ['columns'] }
  )

export type TableEditorFormData = z.infer<typeof tableEditorSchema>
