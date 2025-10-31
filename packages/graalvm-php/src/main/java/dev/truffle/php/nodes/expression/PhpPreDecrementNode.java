package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.frame.VirtualFrame;
import dev.truffle.php.nodes.PhpExpressionNode;

/**
 * Node for pre-decrement operation (--$var).
 * Decrements the variable and returns the new value.
 */
public final class PhpPreDecrementNode extends PhpExpressionNode {

    private final int slot;

    private PhpPreDecrementNode(int slot) {
        this.slot = slot;
    }

    public static PhpPreDecrementNode create(int slot) {
        return new PhpPreDecrementNode(slot);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        // Read current value
        Object current = frame.getObject(slot);

        // Decrement
        Object newValue;
        if (current instanceof Long) {
            newValue = (Long) current - 1;
        } else if (current instanceof Double) {
            newValue = (Double) current - 1.0;
        } else if (current == null) {
            newValue = -1L;
        } else {
            // Fallback: treat as 0
            newValue = -1L;
        }

        // Write back
        frame.setObject(slot, newValue);

        // Return new value
        return newValue;
    }
}
