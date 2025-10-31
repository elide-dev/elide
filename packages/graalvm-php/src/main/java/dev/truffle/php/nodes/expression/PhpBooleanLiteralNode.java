package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import dev.truffle.php.nodes.PhpExpressionNode;

/**
 * Node representing a boolean literal in PHP.
 * PHP has two boolean values: true and false.
 */
public final class PhpBooleanLiteralNode extends PhpExpressionNode {

    private final boolean value;

    public PhpBooleanLiteralNode(boolean value) {
        this.value = value;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return value;
    }

    @Override
    public boolean executeBoolean(VirtualFrame frame) throws UnexpectedResultException {
        return value;
    }
}
