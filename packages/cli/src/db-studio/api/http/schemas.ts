import { z } from "zod";

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

