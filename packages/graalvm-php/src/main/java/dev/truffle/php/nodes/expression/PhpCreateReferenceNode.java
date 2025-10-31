package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.frame.VirtualFrame;
import dev.truffle.php.nodes.PhpExpressionNode;
import dev.truffle.php.runtime.PhpReference;

/**
 * Node that creates a PhpReference wrapping a value.
 * Used for by-reference capture in closures.
 */
public final class PhpCreateReferenceNode extends PhpExpressionNode {

    @Child
    private PhpExpressionNode valueNode;

    public PhpCreateReferenceNode(PhpExpressionNode valueNode) {
        this.valueNode = valueNode;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object value = valueNode.execute(frame);

        // If already a reference, return it
        if (value instanceof PhpReference) {
            return value;
        }

        // Wrap in a new reference
        return new PhpReference(value);
    }
}
