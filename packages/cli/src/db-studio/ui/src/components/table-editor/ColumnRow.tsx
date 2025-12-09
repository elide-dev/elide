import { Control, Controller, useWatch, useFormContext } from 'react-hook-form'
import { Input } from '@/components/ui/input'
import { Switch } from '@/components/ui/switch'
import { Checkbox } from '@/components/ui/checkbox'
import { Button } from '@/components/ui/button'
import { Trash2 } from 'lucide-react'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu'
import { columnTypes } from '@/lib/schemas/table-editor'
import type { TableEditorFormData } from '@/lib/schemas/table-editor'

type ColumnRowProps = {
  index: number
  control: Control<TableEditorFormData>
  onDelete: () => void
  canDelete: boolean
  isEditMode: boolean
  isNewColumn: boolean
}

export function ColumnRow({ index, control, onDelete, canDelete, isEditMode, isNewColumn }: ColumnRowProps) {
  const { setValue } = useFormContext<TableEditorFormData>()

  // Watch the primaryKey value to disable nullable when PK is checked
  const primaryKey = useWatch({
    control,
    name: `columns.${index}.primaryKey`,
  })

  return (
    <div className="flex items-center gap-2 p-3 border border-border rounded bg-muted/30">
      {/* Name */}
      <Controller
        name={`columns.${index}.name`}
        control={control}
        render={({ field }) => (
          <Input
            {...field}
            placeholder="column_name"
            className="flex-1 min-w-0"
            disabled={isEditMode && !isNewColumn}
          />
        )}
      />

      {/* Type */}
      <Controller
        name={`columns.${index}.type`}
        control={control}
        render={({ field }) => (
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="outline" className="w-28" disabled={isEditMode && !isNewColumn}>
                {field.value}
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent>
              {columnTypes.map((type) => (
                <DropdownMenuItem key={type} onClick={() => field.onChange(type)}>
                  {type}
                </DropdownMenuItem>
              ))}
            </DropdownMenuContent>
          </DropdownMenu>
        )}
      />

      {/* Nullable - when PK is on, always show as NOT NULL (switch off) */}
      <div className="flex items-center gap-2">
        <Controller
          name={`columns.${index}.nullable`}
          control={control}
          render={({ field }) => (
            <Switch
              checked={primaryKey ? false : field.value}
              onCheckedChange={field.onChange}
              disabled={(isEditMode && !isNewColumn) || primaryKey}
            />
          )}
        />
        <span className="text-sm text-muted-foreground w-8">Null</span>
      </div>

      {/* Primary Key */}
      <div className="flex items-center gap-2">
        <Controller
          name={`columns.${index}.primaryKey`}
          control={control}
          render={({ field }) => (
            <Checkbox
              checked={field.value}
              onCheckedChange={(checked) => {
                field.onChange(checked)
                // When PK is checked, set nullable to false and unique to true (PKs are implicitly NOT NULL and UNIQUE)
                if (checked) {
                  setValue(`columns.${index}.nullable`, false)
                  setValue(`columns.${index}.unique`, true)
                }
              }}
              disabled={isEditMode}
            />
          )}
        />
        <span className="text-sm text-muted-foreground w-6">PK</span>
      </div>

      {/* Unique - when PK is on, always show as checked */}
      <div className="flex items-center gap-2">
        <Controller
          name={`columns.${index}.unique`}
          control={control}
          render={({ field }) => (
            <Checkbox
              checked={primaryKey ? true : field.value}
              onCheckedChange={field.onChange}
              disabled={(isEditMode && !isNewColumn) || primaryKey}
            />
          )}
        />
        <span className="text-sm text-muted-foreground w-6">UQ</span>
      </div>

      {/* Default Value */}
      <Controller
        name={`columns.${index}.defaultValue`}
        control={control}
        render={({ field }) => (
          <Input
            value={field.value ?? ''}
            onChange={(e) => {
              const val = e.target.value.trim()
              field.onChange(val === '' ? null : val)
            }}
            placeholder="default"
            className="w-32"
            disabled={isEditMode && !isNewColumn}
          />
        )}
      />

      {/* Delete */}
      <Button type="button" variant="ghost" size="icon" onClick={onDelete} disabled={!canDelete}>
        <Trash2 className="h-4 w-4" />
      </Button>
    </div>
  )
}
