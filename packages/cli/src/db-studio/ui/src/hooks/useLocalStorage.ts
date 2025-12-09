import { useState, useCallback } from 'react'

/**
 * Schema for all localStorage keys and their value types.
 * Add new keys here to make them available throughout the app.
 */
export type StorageSchema = {
  'db-studio:pagination-limit': number
  'db-studio:query-editor-sql': string
}

export type StorageKey = keyof StorageSchema

/**
 * A hook that syncs state with localStorage
 * @param key - The localStorage key (typed from StorageSchema)
 * @param defaultValue - The default value if nothing is stored
 * @returns [value, setValue, removeValue] tuple
 */
export function useLocalStorage<K extends StorageKey>(
  key: K,
  defaultValue: StorageSchema[K]
): [StorageSchema[K], (value: StorageSchema[K]) => void, () => void] {
  const [storedValue, setStoredValue] = useState<StorageSchema[K]>(() => {
    try {
      const item = window.localStorage.getItem(key)
      return item !== null ? (JSON.parse(item) as StorageSchema[K]) : defaultValue
    } catch {
      return defaultValue
    }
  })

  const setValue = useCallback(
    (value: StorageSchema[K]) => {
      setStoredValue(value)
      try {
        window.localStorage.setItem(key, JSON.stringify(value))
      } catch {
        // Ignore write errors (e.g., quota exceeded)
      }
    },
    [key]
  )

  const removeValue = useCallback(() => {
    setStoredValue(defaultValue)
    try {
      window.localStorage.removeItem(key)
    } catch {
      // Ignore removal errors
    }
  }, [key, defaultValue])

  return [storedValue, setValue, removeValue]
}

/**
 * Get a value from localStorage (non-reactive, for use outside React)
 */
export function getStorageValue<K extends StorageKey>(key: K, defaultValue: StorageSchema[K]): StorageSchema[K] {
  try {
    const item = window.localStorage.getItem(key)
    return item !== null ? (JSON.parse(item) as StorageSchema[K]) : defaultValue
  } catch {
    return defaultValue
  }
}

/**
 * Set a value in localStorage (non-reactive, for use outside React)
 */
export function setStorageValue<K extends StorageKey>(key: K, value: StorageSchema[K]): void {
  try {
    window.localStorage.setItem(key, JSON.stringify(value))
  } catch {
    // Ignore write errors (e.g., quota exceeded)
  }
}
