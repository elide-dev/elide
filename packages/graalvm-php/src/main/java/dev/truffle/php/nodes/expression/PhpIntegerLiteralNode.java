package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import dev.truffle.php.nodes.PhpExpressionNode;

/**
 * Node representing an integer literal in PHP.
 * Integers in PHP are 64-bit signed values.
 */
public final class PhpIntegerLiteralNode extends PhpExpressionNode {

    private final long value;

    public PhpIntegerLiteralNode(long value) {
        this.value = value;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return value;
    }

    @Override
    public long executeLong(VirtualFrame frame) throws UnexpectedResultException {
        return value;
    }
}
