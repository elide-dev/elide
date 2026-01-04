/**
 * Shared type definitions for the DB Studio frontend
 */

/**
 * Column metadata from database schema
 */
export type ColumnMetadata = {
  name: string
  type: string
  nullable: boolean
  primaryKey: boolean
  defaultValue?: string | number | null
  foreignKey?: {
    table: string
    column: string
    onUpdate?: string
    onDelete?: string
  }
  unique?: boolean
  autoIncrement?: boolean
}

/**
 * Query execution metadata
 */
export type QueryMetadata = {
  executionTimeMs: number
  sql: string
  rowCount: number
}

/**
 * Data table data structure
 */
export type DataTableData = {
  columns: ColumnMetadata[]
  rows: unknown[][]
  metadata?: QueryMetadata
}

/**
 * Pagination parameters
 */
export type PaginationParams = {
  limit: number
  offset: number
}

/**
 * Sorting parameters
 */
export type SortingParams = {
  column: string | null
  direction: 'asc' | 'desc' | null
}

/**
 * Filter operator types for WHERE clause filtering
 */
export type FilterOperator =
  | 'eq' // equals (=)
  | 'neq' // not equals (<>)
  | 'gt' // greater than (>)
  | 'gte' // greater or equal (>=)
  | 'lt' // less than (<)
  | 'lte' // less or equal (<=)
  | 'like' // LIKE
  | 'not_like' // NOT LIKE
  | 'in' // IN (value is array)
  | 'is_null' // IS NULL (no value)
  | 'is_not_null' // IS NOT NULL (no value)

/**
 * Filter for WHERE clause conditions
 */
export type Filter = {
  column: string
  operator: FilterOperator
  value?: string | number | null | string[] // undefined for is_null/is_not_null, array for 'in'
}

/**
 * Operator metadata for UI display
 */
export type OperatorMeta = {
  value: FilterOperator
  label: string
  symbol: string
  requiresValue: boolean // false for is_null/is_not_null
  isArrayValue: boolean // true for 'in'
}

/**
 * Available filter operators with display metadata
 */
export const FILTER_OPERATORS: OperatorMeta[] = [
  { value: 'eq', label: 'equals', symbol: '=', requiresValue: true, isArrayValue: false },
  { value: 'neq', label: 'not equals', symbol: '<>', requiresValue: true, isArrayValue: false },
  { value: 'gt', label: 'greater', symbol: '>', requiresValue: true, isArrayValue: false },
  { value: 'gte', label: 'greater or equals', symbol: '>=', requiresValue: true, isArrayValue: false },
  { value: 'lt', label: 'less', symbol: '<', requiresValue: true, isArrayValue: false },
  { value: 'lte', label: 'less or equals', symbol: '<=', requiresValue: true, isArrayValue: false },
  { value: 'like', label: 'like', symbol: 'LIKE', requiresValue: true, isArrayValue: false },
  { value: 'not_like', label: 'not like', symbol: 'NOT LIKE', requiresValue: true, isArrayValue: false },
  { value: 'in', label: 'in', symbol: 'IN', requiresValue: true, isArrayValue: true },
  { value: 'is_null', label: 'is null', symbol: 'IS NULL', requiresValue: false, isArrayValue: false },
  { value: 'is_not_null', label: 'is not null', symbol: 'IS NOT NULL', requiresValue: false, isArrayValue: false },
]

/**
 * Edit mode state for inline row editing
 */
export type EditModeState = {
  isActive: boolean
  rowData: Record<string, unknown>
  hasUnsavedChanges: boolean
}
