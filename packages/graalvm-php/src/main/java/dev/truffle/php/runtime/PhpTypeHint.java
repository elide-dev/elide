package dev.truffle.php.runtime;

/**
 * Represents a PHP type hint for parameters and return values.
 *
 * Supports:
 * - Scalar types: int, float, string, bool
 * - Special types: array, callable, void, mixed, null
 * - Class types: ClassName
 * - Nullable types: ?int, ?ClassName
 * - Union types (future): string|int
 */
public final class PhpTypeHint {

    private final String typeName;
    private final boolean nullable;
    private final TypeKind kind;

    public enum TypeKind {
        INT,
        FLOAT,
        STRING,
        BOOL,
        ARRAY,
        CALLABLE,
        VOID,
        MIXED,
        NULL,
        CLASS,
        SELF,
        PARENT
    }

    public PhpTypeHint(String typeName, boolean nullable) {
        this.typeName = typeName;
        this.nullable = nullable;
        this.kind = determineKind(typeName);
    }

    private static TypeKind determineKind(String typeName) {
        switch (typeName.toLowerCase()) {
            case "int":
            case "integer":
                return TypeKind.INT;
            case "float":
            case "double":
                return TypeKind.FLOAT;
            case "string":
                return TypeKind.STRING;
            case "bool":
            case "boolean":
                return TypeKind.BOOL;
            case "array":
                return TypeKind.ARRAY;
            case "callable":
                return TypeKind.CALLABLE;
            case "void":
                return TypeKind.VOID;
            case "mixed":
                return TypeKind.MIXED;
            case "null":
                return TypeKind.NULL;
            case "self":
                return TypeKind.SELF;
            case "parent":
                return TypeKind.PARENT;
            default:
                return TypeKind.CLASS;
        }
    }

    public String getTypeName() {
        return typeName;
    }

    public boolean isNullable() {
        return nullable;
    }

    public TypeKind getKind() {
        return kind;
    }

    /**
     * Check if a value matches this type hint.
     */
    public boolean matches(Object value, PhpContext context, String currentClassName) {
        // Null is only valid for nullable types or mixed
        if (value == null) {
            return nullable || kind == TypeKind.MIXED || kind == TypeKind.NULL;
        }

        switch (kind) {
            case MIXED:
                return true;

            case INT:
                return value instanceof Long;

            case FLOAT:
                return value instanceof Double || value instanceof Long;

            case STRING:
                return value instanceof String;

            case BOOL:
                return value instanceof Boolean;

            case ARRAY:
                return value instanceof PhpArray;

            case CALLABLE:
                // Check if it's a function name, closure, or array [obj, method]
                return value instanceof String || value instanceof PhpArray; // Simplified

            case VOID:
                // Void is only for return types, not parameter types
                return value == null;

            case NULL:
                return value == null;

            case CLASS:
                // Check if value is an instance of the specified class
                if (value instanceof PhpObject) {
                    PhpObject obj = (PhpObject) value;
                    return obj.getPhpClass().getName().equals(typeName) ||
                           isInstanceOf(obj.getPhpClass(), typeName, context);
                }
                return false;

            case SELF:
                if (value instanceof PhpObject && currentClassName != null) {
                    return ((PhpObject) value).getPhpClass().getName().equals(currentClassName);
                }
                return false;

            case PARENT:
                if (value instanceof PhpObject && currentClassName != null) {
                    PhpClass currentClass = context.getClass(currentClassName);
                    if (currentClass != null && currentClass.getParentClass() != null) {
                        return ((PhpObject) value).getPhpClass().getName().equals(
                            currentClass.getParentClass().getName()
                        );
                    }
                }
                return false;

            default:
                return true;
        }
    }

    private boolean isInstanceOf(PhpClass phpClass, String targetClassName, PhpContext context) {
        // Check parent classes
        PhpClass current = phpClass.getParentClass();
        while (current != null) {
            if (current.getName().equals(targetClassName)) {
                return true;
            }
            current = current.getParentClass();
        }

        // Check implemented interfaces
        return phpClass.implementsInterface(targetClassName);
    }

    public String getDisplayName() {
        return (nullable ? "?" : "") + typeName;
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
