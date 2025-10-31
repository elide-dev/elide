package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.frame.VirtualFrame;
import dev.truffle.php.nodes.PhpExpressionNode;

/**
 * Node for post-increment operation ($var++).
 * Increments the variable and returns the old value.
 */
public final class PhpPostIncrementNode extends PhpExpressionNode {

    private final int slot;

    private PhpPostIncrementNode(int slot) {
        this.slot = slot;
    }

    public static PhpPostIncrementNode create(int slot) {
        return new PhpPostIncrementNode(slot);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        // Read current value
        Object current = frame.getObject(slot);

        // Store old value to return
        Object oldValue;
        Object newValue;

        if (current instanceof Long) {
            oldValue = current;
            newValue = (Long) current + 1;
        } else if (current instanceof Double) {
            oldValue = current;
            newValue = (Double) current + 1.0;
        } else if (current == null) {
            oldValue = null;
            newValue = 1L;
        } else {
            // Fallback: treat as 0
            oldValue = 0L;
            newValue = 1L;
        }

        // Write back new value
        frame.setObject(slot, newValue);

        // Return old value
        return oldValue;
    }
}
