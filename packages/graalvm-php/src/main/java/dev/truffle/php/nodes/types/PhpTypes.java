package dev.truffle.php.nodes.types;

import com.oracle.truffle.api.dsl.TypeSystem;

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
}
