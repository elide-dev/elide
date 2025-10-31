package dev.truffle.php.runtime;

import com.oracle.truffle.api.CallTarget;

/**
 * Utility methods for PHP string operations.
 */
public final class PhpStringUtil {

    private PhpStringUtil() {
        // Utility class, no instances
    }

    /**
     * Convert a PHP value to its string representation.
     * Handles __toString magic method for objects.
     *
     * @param value The value to convert
     * @return The string representation
     */
    public static String convertToString(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Boolean) {
            return (Boolean) value ? "1" : "";
        }
        // Check for __toString magic method on objects
        if (value instanceof PhpObject) {
            PhpObject obj = (PhpObject) value;
            PhpClass phpClass = obj.getPhpClass();

            // Check if class has __toString method
            if (phpClass.hasMethod("__toString")) {
                PhpClass.MethodMetadata method = phpClass.getMethod("__toString");
                CallTarget callTarget = method.getCallTarget();

                if (callTarget != null) {
                    // Call __toString with $this as the only argument
                    Object result = callTarget.call(obj);

                    // __toString must return a string
                    if (result instanceof String) {
                        return (String) result;
                    } else {
                        throw new RuntimeException("Method " + phpClass.getName() + "::__toString() must return a string value");
                    }
                }
            }
        }
        return value.toString();
    }
}
