package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.frame.VirtualFrame;
import dev.truffle.php.nodes.PhpExpressionNode;
import dev.truffle.php.runtime.PhpReference;

/**
 * Node for pre-increment operation (++$var).
 * Increments the variable and returns the new value.
 * Automatically handles PhpReference objects for by-reference variables.
 */
public final class PhpPreIncrementNode extends PhpExpressionNode {

    private final int slot;

    private PhpPreIncrementNode(int slot) {
        this.slot = slot;
    }

    public static PhpPreIncrementNode create(int slot) {
        return new PhpPreIncrementNode(slot);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        // Read current value (may be a PhpReference)
        Object slotValue = frame.getObject(slot);
        Object current;
        PhpReference reference = null;

        // Unwrap reference if present
        if (slotValue instanceof PhpReference) {
            reference = (PhpReference) slotValue;
            current = reference.getValue();
        } else {
            current = slotValue;
        }

        // Increment
        Object newValue;
        if (current instanceof Long) {
            newValue = (Long) current + 1;
        } else if (current instanceof Double) {
            newValue = (Double) current + 1.0;
        } else if (current == null) {
            newValue = 1L;
        } else {
            // Fallback: treat as 0
            newValue = 1L;
        }

        // Write back new value (update reference or slot)
        if (reference != null) {
            reference.setValue(newValue);
        } else {
            frame.setObject(slot, newValue);
        }

        // Return new value
        return newValue;
    }
}
