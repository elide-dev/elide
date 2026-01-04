import type { ColumnFormData } from './schemas/table-editor'

/**
 * Format a default value for SQL display
 */
function formatDefault(value: string | null): string {
  if (value === null) return 'NULL'

  // Check if it's a SQL function/keyword (CURRENT_TIMESTAMP, etc.)
  const sqlKeywords = ['CURRENT_TIMESTAMP', 'CURRENT_DATE', 'CURRENT_TIME', 'NULL']
  if (sqlKeywords.includes(value.toUpperCase())) {
    return value.toUpperCase()
  }

  // Check if it's a number
  if (!isNaN(Number(value))) {
    return value
  }

  // Otherwise, treat as string literal
  return `'${value.replace(/'/g, "''")}'`
}

/**
 * Generate SQL column definition from column form data
 */
export function generateColumnSql(col: ColumnFormData): string {
  let sql = `${col.name} ${col.type}`

  // PRIMARY KEY comes before NOT NULL
  if (col.primaryKey) {
    sql += ' PRIMARY KEY'

    // AUTOINCREMENT only valid for INTEGER PRIMARY KEY
    if (col.autoIncrement && col.type === 'INTEGER') {
      sql += ' AUTOINCREMENT'
    }
  }

  // NOT NULL constraint
  if (!col.nullable && !col.primaryKey) {
    sql += ' NOT NULL'
  }

  // UNIQUE constraint (skip if PRIMARY KEY since PK implies unique)
  if (col.unique && !col.primaryKey) {
    sql += ' UNIQUE'
  }

  // DEFAULT value
  if (col.defaultValue !== null && col.defaultValue !== '') {
    sql += ` DEFAULT ${formatDefault(col.defaultValue)}`
  }

  return sql
}

/**
 * Generate foreign key constraint SQL
 */
export function generateForeignKeySql(
  columnName: string,
  fk: { table: string; column: string; onUpdate?: string; onDelete?: string }
): string {
  let sql = `FOREIGN KEY (${columnName}) REFERENCES ${fk.table}(${fk.column})`

  if (fk.onDelete) {
    sql += ` ON DELETE ${fk.onDelete}`
  }

  if (fk.onUpdate) {
    sql += ` ON UPDATE ${fk.onUpdate}`
  }

  return sql
}
