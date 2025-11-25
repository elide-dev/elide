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
  rowCount: z.number(),
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
 */
export const WriteQueryResultSchema = z.object({
  success: z.literal(true),
  rowsAffected: z.number(),
  lastInsertRowid: z.union([z.number(), z.bigint()]).optional(),
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
 */
export const DeleteRowsResponseSchema = z.object({
  success: z.literal(true),
  rowsAffected: z.number(),
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
 * Create table column schema
 */
export const CreateTableColumnSchema = z.object({
  name: z.string().min(1, "Column name cannot be empty"),
  type: z.string().min(1, "Column type cannot be empty"),
  constraints: z.string().optional(),
});

/**
 * Create table request schema
 */
export const CreateTableRequestSchema = z.object({
  name: z.string().min(1, "Table name cannot be empty"),
  schema: z.array(CreateTableColumnSchema).min(1, "Schema must contain at least one column"),
});

export type CreateTableRequest = z.infer<typeof CreateTableRequestSchema>;

/**
 * Drop table request schema
 */
export const DropTableRequestSchema = z.object({
  confirm: z.literal(true, {
    errorMap: () => ({ message: "Must set 'confirm: true' to drop table (safety check)" }),
  }),
});

export type DropTableRequest = z.infer<typeof DropTableRequestSchema>;

