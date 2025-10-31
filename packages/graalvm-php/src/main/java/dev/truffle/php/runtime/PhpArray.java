package dev.truffle.php.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a PHP array, which can be both indexed and associative.
 * PHP arrays maintain insertion order and support both integer and string keys.
 */
public final class PhpArray {

    private final Map<Object, Object> map;
    private final List<Object> insertionOrder;
    private long nextIntKey = 0;

    public PhpArray() {
        this.map = new HashMap<>();
        this.insertionOrder = new ArrayList<>();
    }

    /**
     * Append a value to the array (next integer index).
     */
    public void append(Object value) {
        while (map.containsKey(nextIntKey)) {
            nextIntKey++;
        }
        put(nextIntKey, value);
        // Note: put() increments nextIntKey for us
    }

    /**
     * Put a value at a specific key.
     */
    public void put(Object key, Object value) {
        Object normalizedKey = normalizeKey(key);

        if (!map.containsKey(normalizedKey)) {
            insertionOrder.add(normalizedKey);
        }

        map.put(normalizedKey, value);

        // Update nextIntKey if this is an integer key
        if (normalizedKey instanceof Long) {
            long intKey = (Long) normalizedKey;
            if (intKey >= nextIntKey) {
                nextIntKey = intKey + 1;
            }
        }
    }

    /**
     * Get a value by key.
     */
    public Object get(Object key) {
        Object normalizedKey = normalizeKey(key);
        return map.get(normalizedKey);
    }

    /**
     * Check if a key exists.
     */
    public boolean containsKey(Object key) {
        Object normalizedKey = normalizeKey(key);
        return map.containsKey(normalizedKey);
    }

    /**
     * Get the size of the array.
     */
    public int size() {
        return map.size();
    }

    /**
     * Get all keys in insertion order.
     */
    public List<Object> keys() {
        return new ArrayList<>(insertionOrder);
    }

    /**
     * Remove a key from the array.
     */
    public void remove(Object key) {
        Object normalizedKey = normalizeKey(key);
        map.remove(normalizedKey);
        insertionOrder.remove(normalizedKey);
    }

    /**
     * Normalize keys according to PHP rules:
     * - Strings that look like integers become integers
     * - Booleans become integers (true=1, false=0)
     * - Floats become integers (truncated)
     * - null becomes empty string
     */
    private Object normalizeKey(Object key) {
        if (key == null) {
            return "";
        }
        if (key instanceof Long || key instanceof String) {
            // Try to convert string to integer if it looks like one
            if (key instanceof String) {
                String strKey = (String) key;
                try {
                    return Long.parseLong(strKey);
                } catch (NumberFormatException e) {
                    return strKey;
                }
            }
            return key;
        }
        if (key instanceof Double) {
            return ((Double) key).longValue();
        }
        if (key instanceof Boolean) {
            return ((Boolean) key) ? 1L : 0L;
        }
        return key.toString();
    }

    @Override
    public String toString() {
        return "Array(" + size() + ")";
    }
}
