package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import dev.truffle.php.nodes.PhpExpressionNode;

/**
 * Node representing a floating-point literal in PHP.
 * Floats in PHP are 64-bit double-precision values.
 */
public final class PhpFloatLiteralNode extends PhpExpressionNode {

    private final double value;

    public PhpFloatLiteralNode(double value) {
        this.value = value;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return value;
    }

    @Override
    public double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
        return value;
    }
}
