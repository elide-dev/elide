import { useState, useCallback } from 'react'

/**
 * Schema for per-database localStorage keys and their value types.
 * These keys are prefixed with the database ID to keep settings separate per database.
 */
export type PerDbStorageSchema = {
  'pagination-limit': number
  'query-editor-sql': string
}

export type PerDbStorageKey = keyof PerDbStorageSchema

/**
 * Generate a database-specific localStorage key
 */
function makeDbKey(dbId: string, key: PerDbStorageKey): string {
  return `db-studio:${dbId}:${key}`
}

/**
 * A hook that syncs state with localStorage, scoped to a specific database
 * @param dbId - The database identifier
 * @param key - The storage key (will be prefixed with dbId)
 * @param defaultValue - The default value if nothing is stored
 * @returns [value, setValue, removeValue] tuple
 */
export function useDbLocalStorage<K extends PerDbStorageKey>(
  dbId: string | undefined,
  key: K,
  defaultValue: PerDbStorageSchema[K]
): [PerDbStorageSchema[K], (value: PerDbStorageSchema[K]) => void, () => void] {
  const fullKey = dbId ? makeDbKey(dbId, key) : null

  const [storedValue, setStoredValue] = useState<PerDbStorageSchema[K]>(() => {
    if (!fullKey) return defaultValue
    try {
      const item = window.localStorage.getItem(fullKey)
      return item !== null ? (JSON.parse(item) as PerDbStorageSchema[K]) : defaultValue
    } catch {
      return defaultValue
    }
  })

  const setValue = useCallback(
    (value: PerDbStorageSchema[K]) => {
      setStoredValue(value)
      if (!fullKey) return
      try {
        window.localStorage.setItem(fullKey, JSON.stringify(value))
      } catch {
        // Ignore write errors (e.g., quota exceeded)
      }
    },
    [fullKey]
  )

  const removeValue = useCallback(() => {
    setStoredValue(defaultValue)
    if (!fullKey) return
    try {
      window.localStorage.removeItem(fullKey)
    } catch {
      // Ignore removal errors
    }
  }, [fullKey, defaultValue])

  return [storedValue, setValue, removeValue]
}

/**
 * Get a per-database value from localStorage (non-reactive, for use outside React)
 */
export function getDbStorageValue<K extends PerDbStorageKey>(
  dbId: string | undefined,
  key: K,
  defaultValue: PerDbStorageSchema[K]
): PerDbStorageSchema[K] {
  if (!dbId) return defaultValue
  try {
    const fullKey = makeDbKey(dbId, key)
    const item = window.localStorage.getItem(fullKey)
    return item !== null ? (JSON.parse(item) as PerDbStorageSchema[K]) : defaultValue
  } catch {
    return defaultValue
  }
}

/**
 * Set a per-database value in localStorage (non-reactive, for use outside React)
 */
export function setDbStorageValue<K extends PerDbStorageKey>(
  dbId: string | undefined,
  key: K,
  value: PerDbStorageSchema[K]
): void {
  if (!dbId) return
  try {
    const fullKey = makeDbKey(dbId, key)
    window.localStorage.setItem(fullKey, JSON.stringify(value))
  } catch {
    // Ignore write errors (e.g., quota exceeded)
  }
}
