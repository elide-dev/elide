import { Controller, useWatch, useFormContext } from 'react-hook-form'
import type { Control } from 'react-hook-form'
import { Input } from '@/components/ui/input'
import { Switch } from '@/components/ui/switch'
import { Checkbox } from '@/components/ui/checkbox'
import { Button } from '@/components/ui/button'
import { Separator } from '@/components/ui/separator'
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from '@/components/ui/accordion'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu'
import { columnTypes } from '@/lib/schemas/table-editor'
import type { TableEditorFormData } from '@/lib/schemas/table-editor'
import { generateColumnSql } from '@/lib/sql-utils'

type ColumnCardProps = {
  index: number
  control: Control<TableEditorFormData>
  isSelected: boolean
  onToggleSelect: () => void
  isEditMode: boolean
  isNewColumn: boolean
}

export function ColumnCard({ index, control, isSelected, onToggleSelect, isEditMode, isNewColumn }: ColumnCardProps) {
  const { setValue } = useFormContext<TableEditorFormData>()

  // Watch all column fields to generate SQL preview
  const column = useWatch({
    control,
    name: `columns.${index}`,
  })

  const primaryKey = column?.primaryKey || false
  const columnType = column?.type || 'TEXT'

  // Generate SQL preview
  const sqlPreview = column ? generateColumnSql(column) : ''

  return (
    <div className="flex items-start gap-3">
      <Checkbox
        checked={isSelected}
        onCheckedChange={onToggleSelect}
        className="shrink-0 mt-4"
        onClick={(e) => e.stopPropagation()}
      />
      <Accordion type="single" collapsible className="flex-1 border rounded-lg">
        <AccordionItem value={`column-${index}`} className="border-none">
          <AccordionTrigger className="px-4 hover:bg-muted/50 transition-colors hover:no-underline [&>svg]:ml-auto cursor-pointer ">
            <div className="font-mono text-sm text-left flex-1">
              {sqlPreview || <span className="text-muted-foreground">New column...</span>}
            </div>
          </AccordionTrigger>

          <AccordionContent>
            <div className="px-4 py-4 space-y-6">
              {/* Basic Properties */}
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium mb-2">Column Name</label>
                  <Controller
                    name={`columns.${index}.name`}
                    control={control}
                    render={({ field }) => (
                      <Input {...field} placeholder="column_name" disabled={isEditMode && !isNewColumn} />
                    )}
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium mb-2">Data Type</label>
                  <Controller
                    name={`columns.${index}.type`}
                    control={control}
                    render={({ field }) => (
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button
                            variant="outline"
                            className="w-full justify-start"
                            disabled={isEditMode && !isNewColumn}
                          >
                            <span className="font-mono">{field.value}</span>
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="start" className="w-full">
                          {columnTypes.map((type) => (
                            <DropdownMenuItem key={type} onClick={() => field.onChange(type)} className="font-mono">
                              {type}
                            </DropdownMenuItem>
                          ))}
                        </DropdownMenuContent>
                      </DropdownMenu>
                    )}
                  />
                </div>
              </div>

              <Separator />

              {/* Constraints */}
              <div className="space-y-4">
                <h4 className="text-sm font-medium">Constraints</h4>

                {/* Not Null - when PK is on, always show as ON (NOT NULL) */}
                <div className="flex items-center justify-between">
                  <div className="space-y-0.5">
                    <label className="text-sm font-medium">Not Null</label>
                    <p className="text-xs text-muted-foreground">Column must not assume the null value</p>
                  </div>
                  <Controller
                    name={`columns.${index}.nullable`}
                    control={control}
                    render={({ field }) => (
                      <Switch
                        checked={primaryKey ? true : !field.value}
                        onCheckedChange={(checked) => field.onChange(!checked)}
                        disabled={(isEditMode && !isNewColumn) || primaryKey}
                      />
                    )}
                  />
                </div>

                {/* Primary Key */}
                <div className="flex items-center justify-between">
                  <div className="space-y-0.5">
                    <label className="text-sm font-medium">Primary Key</label>
                    <p className="text-xs text-muted-foreground">
                      Can be used as a unique identifier for rows in the table
                    </p>
                  </div>
                  <Controller
                    name={`columns.${index}.primaryKey`}
                    control={control}
                    render={({ field }) => (
                      <Switch
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
                </div>

                {/* Auto Increment */}
                {columnType === 'INTEGER' && (
                  <div className="flex items-center justify-between">
                    <div className="space-y-0.5">
                      <label className="text-sm font-medium">Auto Increment</label>
                      <p className="text-xs text-muted-foreground">Automatically generate unique incrementing values</p>
                    </div>
                    <Controller
                      name={`columns.${index}.autoIncrement`}
                      control={control}
                      render={({ field }) => (
                        <Switch
                          checked={field.value}
                          onCheckedChange={field.onChange}
                          disabled={(isEditMode && !isNewColumn) || !primaryKey}
                        />
                      )}
                    />
                  </div>
                )}

                {/* Unique - when PK is on, always show as checked */}
                <div className="flex items-center justify-between">
                  <div className="space-y-0.5">
                    <label className="text-sm font-medium">Unique</label>
                    <p className="text-xs text-muted-foreground">
                      Ensure that the data contained in a column is unique among all the rows
                    </p>
                  </div>
                  <Controller
                    name={`columns.${index}.unique`}
                    control={control}
                    render={({ field }) => (
                      <Switch
                        checked={primaryKey ? true : field.value}
                        onCheckedChange={field.onChange}
                        disabled={(isEditMode && !isNewColumn) || primaryKey}
                      />
                    )}
                  />
                </div>
              </div>

              <Separator />

              {/* Default Value */}
              <div>
                <label className="block text-sm font-medium mb-2">Default Value</label>
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
                      placeholder="NULL"
                      disabled={isEditMode && !isNewColumn}
                    />
                  )}
                />
                <p className="text-xs text-muted-foreground mt-2">
                  Default value when none is specified. Use CURRENT_TIMESTAMP for timestamps.
                </p>
              </div>
            </div>
          </AccordionContent>
        </AccordionItem>
      </Accordion>
    </div>
  )
}
