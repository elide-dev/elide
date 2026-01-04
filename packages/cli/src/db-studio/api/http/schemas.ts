import { z } from "zod/v3";

/**
 * Foreign key reference information
 */
export const ForeignKeyReferenceSchema = z.object({
  table: z.string(),
  column: z.string(),
  onUpdate: z.string().optional(),
  onDelete: z.string().optional(),
});

export type ForeignKeyReference = z.infer<typeof ForeignKeyReferenceSchema>;

/**
 * Column metadata schema
 * Includes type information, constraints, and relationships
 */
export const ColumnMetadataSchema = z.object({
  name: z.string(),
  type: z.string(),
  nullable: z.boolean(),
  primaryKey: z.boolean(),
  defaultValue: z.union([z.string(), z.number(), z.null()]).optional(),
  foreignKey: ForeignKeyReferenceSchema.optional(),
  unique: z.boolean().optional(),
  autoIncrement: z.boolean().optional(),
});

export type ColumnMetadata = z.infer<typeof ColumnMetadataSchema>;

/**
 * Query execution metadata
 */
export const QueryMetadataSchema = z.object({
  executionTimeMs: z.number(),
  sql: z.string(),
  rowCount: z.number().optional(), // Not available for write queries
});

export type QueryMetadata = z.infer<typeof QueryMetadataSchema>;

/**
 * SELECT query result schema
 */
export const SelectQueryResultSchema = z.object({
  success: z.literal(true),
  data: z.array(z.record(z.string(), z.unknown())),
  columns: z.array(ColumnMetadataSchema),
  metadata: QueryMetadataSchema,
});

export type SelectQueryResult = z.infer<typeof SelectQueryResultSchema>;

/**
 * Write query (INSERT/UPDATE/DELETE) result schema
 * Note: rowsAffected and lastInsertRowid are not currently available from elide sqlite
 */
export const WriteQueryResultSchema = z.object({
  success: z.literal(true),
  metadata: QueryMetadataSchema,
});

export type WriteQueryResult = z.infer<typeof WriteQueryResultSchema>;

/**
 * Union of all successful query results
 */
export const QueryResultSchema = z.union([
  SelectQueryResultSchema,
  WriteQueryResultSchema,
]);

export type QueryResult = z.infer<typeof QueryResultSchema>;

/**
 * Table data schema with enhanced column metadata
 */
export const TableDataSchema = z.object({
  name: z.string(),
  columns: z.array(ColumnMetadataSchema),
  rows: z.array(z.array(z.unknown())),
  totalRows: z.number(),
  metadata: QueryMetadataSchema,
});

export type TableDataResponse = z.infer<typeof TableDataSchema>;

/**
 * Error response schema
 */
export const ErrorResponseSchema = z.object({
  success: z.literal(false),
  error: z.string(),
  details: z.string().optional(),
  sql: z.string().optional(), // The SQL query that caused the error
  executionTimeMs: z.number().optional(),
});

export type ErrorResponse = z.infer<typeof ErrorResponseSchema>;

/**
 * Filter operator types for WHERE clause filtering
 */
export type FilterOperator =
  | 'eq'           // equals (=)
  | 'neq'          // not equals (<>)
  | 'gt'           // greater than (>)
  | 'gte'          // greater or equal (>=)
  | 'lt'           // less than (<)
  | 'lte'          // less or equal (<=)
  | 'like'         // LIKE
  | 'not_like'     // NOT LIKE
  | 'in'           // IN (value is array)
  | 'is_null'      // IS NULL (no value)
  | 'is_not_null'  // IS NOT NULL (no value)

/**
 * Filter for WHERE clause conditions
 */
export type Filter = {
  column: string
  operator: FilterOperator
  value?: string | number | null | string[] // undefined for is_null/is_not_null, array for 'in'
}

/**
 * Filter schema for runtime validation
 */
export const FilterSchema = z.object({
  column: z.string().min(1, "Column name cannot be empty"),
  operator: z.enum(['eq', 'neq', 'gt', 'gte', 'lt', 'lte', 'like', 'not_like', 'in', 'is_null', 'is_not_null']),
  value: z.union([z.string(), z.number(), z.null(), z.array(z.string())]).optional(),
});

export const FiltersArraySchema = z.array(FilterSchema);

/**
 * Delete rows request schema
 * Expects an array of primary key objects
 */
export const DeleteRowsRequestSchema = z.object({
  primaryKeys: z.array(z.record(z.string(), z.unknown())).min(1, "At least one primary key required"),
});

export type DeleteRowsRequest = z.infer<typeof DeleteRowsRequestSchema>;

/**
 * Delete rows response schema
 * Note: rowsAffected is not currently available from elide sqlite
 */
export const DeleteRowsResponseSchema = z.object({
  success: z.literal(true),
});

export type DeleteRowsResponse = z.infer<typeof DeleteRowsResponseSchema>;

/**
 * Execute query request schema
 */
export const ExecuteQueryRequestSchema = z.object({
  sql: z.string().min(1, "SQL query cannot be empty"),
  params: z.array(z.unknown()).optional(),
});

export type ExecuteQueryRequest = z.infer<typeof ExecuteQueryRequestSchema>;

/**
 * Column definition for table creation/editing
 */
export const ColumnDefinitionSchema = z.object({
  name: z.string().min(1, "Column name cannot be empty"),
  type: z.enum(['INTEGER', 'TEXT', 'REAL', 'BLOB', 'NUMERIC']),
  nullable: z.boolean(),
  primaryKey: z.boolean(),
  autoIncrement: z.boolean().optional(),
  unique: z.boolean(),
  defaultValue: z.union([z.string(), z.number(), z.null()]),
});

export type ColumnDefinition = z.infer<typeof ColumnDefinitionSchema>;

/**
 * Create table column schema (legacy)
 */
export const CreateTableColumnSchema = z.object({
  name: z.string().min(1, "Column name cannot be empty"),
  type: z.string().min(1, "Column type cannot be empty"),
  constraints: z.string().optional(),
});

/**
 * Create table request schema (updated to support new editor)
 */
export const CreateTableRequestSchema = z.union([
  // New format from table editor
  z.object({
    name: z.string().min(1, "Table name cannot be empty"),
    columns: z.array(ColumnDefinitionSchema).min(1, "At least one column required"),
  }).refine(
    (data) => {
      const pkCount = data.columns.filter(c => c.primaryKey).length;
      return pkCount <= 1;
    },
    { message: "Maximum one primary key column allowed" }
  ).refine(
    (data) => {
      const names = data.columns.map(c => c.name.toLowerCase());
      return names.length === new Set(names).size;
    },
    { message: "Column names must be unique" }
  ),
  // Legacy format (for backwards compatibility)
  z.object({
    name: z.string().min(1, "Table name cannot be empty"),
    schema: z.array(CreateTableColumnSchema).min(1, "Schema must contain at least one column"),
  }),
]);

export type CreateTableRequest = z.infer<typeof CreateTableRequestSchema>;

/**
 * ALTER TABLE operations
 */
export const AddColumnOperationSchema = z.object({
  type: z.literal('add_column'),
  column: z.object({
    name: z.string().min(1, "Column name cannot be empty"),
    type: z.enum(['INTEGER', 'TEXT', 'REAL', 'BLOB', 'NUMERIC']),
    nullable: z.boolean(),
    defaultValue: z.union([z.string(), z.number(), z.null()]),
  }),
});

export const DropColumnOperationSchema = z.object({
  type: z.literal('drop_column'),
  columnName: z.string().min(1),
});

export const RenameColumnOperationSchema = z.object({
  type: z.literal('rename_column'),
  oldName: z.string().min(1),
  newName: z.string().min(1),
});

export const AlterTableOperationSchema = z.discriminatedUnion('type', [
  AddColumnOperationSchema,
  DropColumnOperationSchema,
  RenameColumnOperationSchema,
]);

export type AlterTableOperation = z.infer<typeof AlterTableOperationSchema>;

export const AlterTableRequestSchema = z.object({
  operations: z.array(AlterTableOperationSchema).min(1, "At least one operation required"),
});

export type AlterTableRequest = z.infer<typeof AlterTableRequestSchema>;

/**
 * Table schema response
 */
export const TableSchemaResponseSchema = z.object({
  tableName: z.string(),
  columns: z.array(ColumnDefinitionSchema),
});

export type TableSchemaResponse = z.infer<typeof TableSchemaResponseSchema>;

/**
 * Insert row request schema
 * Expects an object mapping column names to values
 */
export const InsertRowRequestSchema = z.object({
  row: z.record(z.string(), z.unknown()).refine(
    (data) => Object.keys(data).length > 0,
    { message: "Row must contain at least one column" }
  ),
});

export type InsertRowRequest = z.infer<typeof InsertRowRequestSchema>;

/**
 * Insert row response schema
 * Note: rowsAffected and lastInsertRowid are not currently available from elide sqlite
 */
export const InsertRowResponseSchema = z.object({
  success: z.literal(true),
});

export type InsertRowResponse = z.infer<typeof InsertRowResponseSchema>;

/**
 * Update row request schema
 * Expects a primary key object and an updates object
 */
export const UpdateRowRequestSchema = z.object({
  primaryKey: z.record(z.string(), z.unknown()).refine(
    (data) => Object.keys(data).length > 0,
    { message: "Primary key must contain at least one column" }
  ),
  updates: z.record(z.string(), z.unknown()).refine(
    (data) => Object.keys(data).length > 0,
    { message: "Updates must contain at least one column" }
  ),
});

export type UpdateRowRequest = z.infer<typeof UpdateRowRequestSchema>;

/**
 * Update row response schema
 */
export const UpdateRowResponseSchema = z.object({
  success: z.literal(true),
  sql: z.string(),
});

export type UpdateRowResponse = z.infer<typeof UpdateRowResponseSchema>;

