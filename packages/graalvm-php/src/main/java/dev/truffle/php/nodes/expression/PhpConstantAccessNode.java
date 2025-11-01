package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.frame.VirtualFrame;
import dev.truffle.php.nodes.PhpExpressionNode;
import dev.truffle.php.runtime.PhpContext;

/**
 * Node for accessing a constant value.
 * Constants can be defined via define() or the const keyword.
 */
public final class PhpConstantAccessNode extends PhpExpressionNode {

    private final String constantName;

    public PhpConstantAccessNode(String constantName) {
        this.constantName = constantName;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        PhpContext context = PhpContext.get(this);

        // Try to get constant value from context
        Object value = context.getConstant(constantName);

        if (value == null) {
            // PHP 8.0+ behavior: undefined constants throw errors
            // For compatibility, we could also issue a warning and treat as string
            throw new RuntimeException("Undefined constant: " + constantName);
        }

        return value;
    }
}
