package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.frame.VirtualFrame;
import dev.truffle.php.nodes.PhpExpressionNode;

/**
 * Node representing a null literal in PHP.
 */
public final class PhpNullLiteralNode extends PhpExpressionNode {

    @Override
    public Object execute(VirtualFrame frame) {
        return null;
    }
}
