package dev.truffle.php.nodes.types;

import com.oracle.truffle.api.dsl.TypeSystem;
import dev.truffle.php.runtime.PhpArray;

/**
 * PHP Type System for Truffle DSL.
 *
 * Defines the basic types in PHP:
 * - long (integer)
 * - double (float)
 * - boolean
 * - String
 * - null
 */
@TypeSystem({long.class, double.class, boolean.class, String.class})
public abstract class PhpTypes {

    /**
     * Check if a value is null (PHP's null type).
     */
    public static boolean isNull(Object value) {
        return value == null;
    }

    /**
     * Convert a value to boolean using PHP's truthiness rules.
     *
     * Falsy values in PHP:
     * - null
     * - false
     * - 0 (integer)
     * - 0.0 (float)
     * - "" (empty string)
     * - "0" (string containing zero)
     * - empty array
     *
     * Everything else is truthy.
     */
    public static boolean toBoolean(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Long) {
            return ((Long) value) != 0L;
        }
        if (value instanceof Double) {
            return ((Double) value) != 0.0;
        }
        if (value instanceof String) {
            String str = (String) value;
            return !str.isEmpty() && !str.equals("0");
        }
        if (value instanceof PhpArray) {
            return ((PhpArray) value).size() > 0;
        }
        // Objects and other types are truthy
        return true;
    }

    /**
     * Check if a value is empty using PHP's empty() rules.
     *
     * Empty values in PHP:
     * - null
     * - false
     * - 0 (integer)
     * - 0.0 (float)
     * - "" (empty string)
     * - "0" (string containing zero)
     * - empty array
     *
     * Same as !toBoolean(value)
     */
    public static boolean isEmpty(Object value) {
        return !toBoolean(value);
    }
}
