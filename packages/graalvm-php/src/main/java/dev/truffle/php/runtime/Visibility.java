package dev.truffle.php.runtime;

/**
 * Visibility levels for PHP class members (properties and methods).
 */
public enum Visibility {
    /**
     * Public: accessible from anywhere.
     */
    PUBLIC,

    /**
     * Protected: accessible from within the class and its subclasses.
     */
    PROTECTED,

    /**
     * Private: accessible only from within the class itself (not even subclasses).
     */
    PRIVATE
}
