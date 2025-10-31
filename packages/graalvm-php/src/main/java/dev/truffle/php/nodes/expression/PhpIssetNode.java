package dev.truffle.php.nodes.expression;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import dev.truffle.php.nodes.PhpExpressionNode;

/**
 * AST node for isset() language construct.
 * Checks if one or more variables are set and not null.
 * Returns true only if ALL variables are set and not null.
 */
public final class PhpIssetNode extends PhpExpressionNode {

    private final int[] variableSlots;

    public PhpIssetNode(int[] variableSlots) {
        this.variableSlots = variableSlots;
    }

    @Override
    @ExplodeLoop
    public Object execute(VirtualFrame frame) {
        // isset returns true only if all variables are set and not null
        for (int slot : variableSlots) {
            try {
                Object value = frame.getObject(slot);
                if (value == null) {
                    return false;
                }
            } catch (Exception e) {
                // Variable not initialized
                return false;
            }
        }
        return true;
    }
}
