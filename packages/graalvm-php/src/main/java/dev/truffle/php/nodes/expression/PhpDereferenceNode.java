package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.frame.VirtualFrame;
import dev.truffle.php.nodes.PhpExpressionNode;
import dev.truffle.php.runtime.PhpReference;

/**
 * Node that unwraps a PhpReference to get the underlying value.
 * If the value is not a reference, returns it as-is.
 */
public final class PhpDereferenceNode extends PhpExpressionNode {

    @Child
    private PhpExpressionNode referenceNode;

    public PhpDereferenceNode(PhpExpressionNode referenceNode) {
        this.referenceNode = referenceNode;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object value = referenceNode.execute(frame);

        // If it's a reference, unwrap it
        if (value instanceof PhpReference) {
            return ((PhpReference) value).getValue();
        }

        // Otherwise return as-is
        return value;
    }
}
