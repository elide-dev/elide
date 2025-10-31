package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import dev.truffle.php.nodes.PhpExpressionNode;

/**
 * Node representing a string literal in PHP.
 */
public final class PhpStringLiteralNode extends PhpExpressionNode {

    private final String value;

    public PhpStringLiteralNode(String value) {
        this.value = value;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return value;
    }

    @Override
    public String executeString(VirtualFrame frame) throws UnexpectedResultException {
        return value;
    }
}
