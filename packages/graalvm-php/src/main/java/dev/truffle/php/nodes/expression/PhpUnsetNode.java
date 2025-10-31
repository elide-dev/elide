package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import dev.truffle.php.nodes.PhpExpressionNode;

/**
 * AST node for unset() language construct.
 * Unsets one or more variables by setting them to null.
 * In PHP, unset() is technically a statement, but we treat it as an expression
 * that returns null for simplicity.
 */
public final class PhpUnsetNode extends PhpExpressionNode {

    private final int[] variableSlots;

    public PhpUnsetNode(int[] variableSlots) {
        this.variableSlots = variableSlots;
    }

    @Override
    @ExplodeLoop
    public Object execute(VirtualFrame frame) {
        // Unset all specified variables by setting them to null
        for (int slot : variableSlots) {
            frame.setObject(slot, null);
        }
        // unset() doesn't return a value, but we return null for consistency
        return null;
    }
}
