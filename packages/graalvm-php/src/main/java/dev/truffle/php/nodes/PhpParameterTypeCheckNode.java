package dev.truffle.php.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import dev.truffle.php.runtime.PhpContext;
import dev.truffle.php.runtime.PhpTypeHint;

/**
 * Node that performs runtime type checking for function/method parameters.
 */
public final class PhpParameterTypeCheckNode extends PhpStatementNode {

    private final String parameterName;
    private final int parameterSlot;
    private final PhpTypeHint typeHint;
    private final String currentClassName;

    public PhpParameterTypeCheckNode(String parameterName, int parameterSlot, PhpTypeHint typeHint, String currentClassName) {
        this.parameterName = parameterName;
        this.parameterSlot = parameterSlot;
        this.typeHint = typeHint;
        this.currentClassName = currentClassName;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        Object value = frame.getValue(parameterSlot);
        PhpContext context = PhpContext.get(this);

        if (!typeHint.matches(value, context, currentClassName)) {
            String actualType = getActualType(value);
            throw new RuntimeException(
                "TypeError: Argument #1 ($" + parameterName + ") must be of type " +
                typeHint.getDisplayName() + ", " + actualType + " given"
            );
        }
    }

    private String getActualType(Object value) {
        if (value == null) return "null";
        if (value instanceof String) return "string";
        if (value instanceof Long) return "int";
        if (value instanceof Double) return "float";
        if (value instanceof Boolean) return "bool";
        if (value instanceof dev.truffle.php.runtime.PhpArray) return "array";
        if (value instanceof dev.truffle.php.runtime.PhpObject) {
            return ((dev.truffle.php.runtime.PhpObject) value).getPhpClass().getName();
        }
        return "unknown";
    }
}
